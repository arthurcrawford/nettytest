package uk.co.b2esoftware.echo;


import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroupFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * User: art
 * Date: 16/01/2013
 * Time: 12:45
 */
public class EchoServerHandler extends SimpleChannelUpstreamHandler
{
    private Logger log = LoggerFactory.getLogger(EchoServerHandler.class);

    // Group that tracks all open channels
    private final MyChannelGroup allChannels = new MyChannelGroup();

    public EchoServerHandler()
    {
        // Start a thread to periodically send alive messages to all client channels
        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
        executor.scheduleAtFixedRate(new BroadcastMessageSender(
                "Server alive! %s client connections\n"), 0, 10000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception
    {
        super.channelDisconnected(ctx, e);
    }

    @Override
    public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception
    {
        // A client opened a channel
        log.info("Client opened a channel");
        allChannels.add(e.getChannel());
        ChannelBuffer cb = ChannelBuffers.dynamicBuffer();
        cb.writeBytes("Client connected successfully\n".getBytes());
        e.getChannel().write(cb);
    }

    @Override
    public void channelOpen(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception
    {
        super.channelOpen(ctx, e);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent e) throws Exception
    {
        log.warn("Unexpected exception from downstream", e.getCause());
        e.getChannel().close();
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception
    {
        log.info("message received");
        // The message has been decoded in the pipeline by this stage
        final String message = ((String) e.getMessage()).trim();
        System.out.print(message);
        System.out.flush();
        // If client message is "close", client wants the following to happen
        //  1. send close message to server and have it processed
        //  2. immediately close client connection
        //  3. receive response to close message on any other available channel
        //
        if ("close".equalsIgnoreCase(message))
        {
            // We received client 'close' message so we asynchronously close the channel, then wait
            ChannelFuture channelFuture = e.getChannel().close();
            channelFuture.addListener(new ChannelFutureListener()
            {
                @Override
                public void operationComplete(final ChannelFuture future) throws Exception
                {
                    // When the channel has fully closed, artificially try to send the message back along it
                    sendResponseOnRequestChannelOrNextAvailable(message, e.getChannel());
                }
            });
        }
        // If client message is "block", client wants the following to happen
        //  1. server handles message and creates a runnable task for it
        //  2. the runnable task blocks, taking up a task thread - i.e. doesn't complete until it is unblocked
        //
        else if ("block".equalsIgnoreCase(message))
        {
            Thread.sleep(20000);
            sendResponseOnRequestChannelOrNextAvailable("UNBLOCK\n", e.getChannel());
        }
        // If client message is not "close", send response back on request channel
        else
        {
            sendResponseOnRequestChannelOrNextAvailable(message, e.getChannel());
        }
    }

    private void sendResponseOnRequestChannelOrNextAvailable(final String message, Channel channel)
    {
        String time = String.valueOf(System.nanoTime());
        final String response = (time + ": " + message.toUpperCase() + "\n");
        // We don't bother to check whether the channel is open - we could do though!
        ChannelFuture channelFuture = channel.write(response);
        channelFuture.addListener(new ChannelFutureListener()
        {
            @Override
            public void operationComplete(final ChannelFuture future) throws Exception
            {
                boolean isSuccess = future.isSuccess();
                log.info(String.format("Operation %s successful ?: %s", message, isSuccess));
                // If operation was unsuccessful
                if (!future.isSuccess())
                {
                    // Operation failed so try to write to next available channel
                    ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
                    buf.writeBytes(response.getBytes());
                    allChannels.writeToNextAvailableChannel(response);
                }
            }
        });
    }

    private class BroadcastMessageSender implements Runnable
    {
        private final String message;

        public BroadcastMessageSender(final String message)
        {
            this.message = message;
        }

        @Override
        public void run()
        {
            // Write the message to the next available channel in the chanel group
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
            String time = String.valueOf(System.nanoTime());
            buf.writeBytes((time + ": " + String.format(message, allChannels.size())).getBytes());
            allChannels.write(buf);
        }
    }

    /**
     * Extends DefaultChannelGroup by adding a method to write to the next available channel in the group.
     */
    private class MyChannelGroup extends DefaultChannelGroup implements ChannelGroup
    {
        // Simply writes to the next available method in the tracked channels
        public ChannelGroupFuture writeToNextAvailableChannel(Object message)
        {
            Map<Integer, ChannelFuture> futures = new LinkedHashMap<Integer, ChannelFuture>(size());
            Iterator<Channel> channelIterator = iterator();

            if (channelIterator.hasNext())
            {
                Channel channel = channelIterator.next();
                if (message instanceof ChannelBuffer)
                {
                    ChannelBuffer buf = (ChannelBuffer) message;
                    futures.put(channel.getId(), channel.write(buf.duplicate()));
                }
                else
                {
                    futures.put(channel.getId(), channel.write(message));
                }
            }
            return new DefaultChannelGroupFuture(this, futures.values());
        }
    }
}
