package uk.co.b2esoftware.echo;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.jboss.netty.buffer.ChannelBuffers.dynamicBuffer;

/**
 * User: art
 * Date: 16/01/2013
 * Time: 13:07
 */
public class EchoClient implements Runnable
{
    private final String host;
    private final int port;
    private final int firstMessageSize;

    public EchoClient(final String host, final int port, final int firstMessageSize)
    {
        this.host = host;
        this.port = port;
        this.firstMessageSize = firstMessageSize;
    }

    public static void main(String[] args)
    {
        final Executor executor = Executors.newCachedThreadPool();

        final EchoClient echoClient1 = new EchoClient("localhost", 9797, 256);
        final EchoClient echoClient2 = new EchoClient("localhost", 9797, 256);

        executor.execute(echoClient1);
        executor.execute(echoClient2);
    }

    public void run()
    {
        ClientBootstrap bootstrap = new ClientBootstrap(
                new NioClientSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool()));

        final ChannelPipeline pipeline = Channels.pipeline(new EchoClientHandler(firstMessageSize));

        bootstrap.setPipelineFactory(new ChannelPipelineFactory()
        {
            @Override
            public ChannelPipeline getPipeline() throws Exception
            {
                return pipeline;
            }
        });

        ChannelFuture connectionFuture = bootstrap.connect(new InetSocketAddress(host, port));

        connectionFuture.addListener(new ChannelFutureListener()
        {
            @Override
            public void operationComplete(final ChannelFuture future) throws Exception
            {
                // Send messages through the channel
                final int poolSize = 2;
                final ScheduledExecutorService executor = Executors.newScheduledThreadPool(poolSize);

                Random generator = new Random();
                int randomDelay = generator.nextInt(10000);
                for (int i = 0; i < poolSize; i++)
                {
                    executor.scheduleAtFixedRate(new MessageSender(future.getChannel()), 0, randomDelay, TimeUnit.MILLISECONDS);
                }

            }
        });

        connectionFuture.getChannel().getCloseFuture().awaitUninterruptibly();

        bootstrap.releaseExternalResources();
    }

    private static AtomicInteger count = new AtomicInteger(0);

    class MessageSender implements Runnable
    {
        private final Channel channel;

        public MessageSender(final Channel channel)
        {
            //To change body of created methods use File | Settings | File Templates.
            this.channel = channel;
        }

        @Override
        public void run()
        {
            final ChannelBuffer buf = dynamicBuffer();
            buf.writeBytes(String.format(
                    Thread.currentThread().getName() + " - message %s\n", count.incrementAndGet()).getBytes());
            ChannelFuture future = channel.write(buf);
            future.addListener(new ChannelFutureListener()
            {
                @Override
                public void operationComplete(final ChannelFuture future) throws Exception
                {
                    buf.clear();
                }
            });
        }
    }
}


