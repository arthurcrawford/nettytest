package uk.co.b2esoftware.echo;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

import static org.jboss.netty.buffer.ChannelBuffers.dynamicBuffer;

/**
 * User: art
 * Date: 16/01/2013
 * Time: 13:17
 */
public class EchoClientHandler extends SimpleChannelUpstreamHandler
{
    Logger log = LoggerFactory.getLogger(EchoClientHandler.class);
    private final ChannelBuffer firstMessage;
    private final AtomicLong transferredBytes = new AtomicLong();

    public EchoClientHandler(final int firstMessageSize)
    {
        if (firstMessageSize <= 0)
        {
            throw new IllegalArgumentException("firstMessageSize: " + firstMessageSize);
        }
        firstMessage = ChannelBuffers.buffer(firstMessageSize);
        for (int i = 0; i < firstMessage.capacity(); i++)
        {
            firstMessage.writeByte((byte) i);
        }
    }

    public long getTransferredBytes()
    {
        return transferredBytes.get();
    }

    @Override
    public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception
    {
        ChannelBuffer outBuf = dynamicBuffer();
        outBuf.writeBytes("channel connected\n".getBytes());
        e.getChannel().write(outBuf);
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception
    {
        ChannelBuffer buf = (ChannelBuffer) e.getMessage();
        StringBuilder sb = new StringBuilder();
        while (buf.readable())
        {
            sb.append((char)buf.readByte());
        }
        log.info("Message received: " + sb.toString());
//        System.out.println(sb.toString());
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent e) throws Exception
    {
        log.warn("Unexpected error from downstream", e.getCause());
        e.getChannel().close();
    }
}
