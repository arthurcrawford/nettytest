package uk.co.b2esoftware.time;


import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicLong;

import static org.jboss.netty.buffer.ChannelBuffers.dynamicBuffer;

/**
 * User: art
 * Date: 16/01/2013
 * Time: 12:45
 */
public class TimeServerHandler extends SimpleChannelUpstreamHandler
{
    Logger log = LoggerFactory.getLogger(TimeServerHandler.class);

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent e) throws Exception
    {
        log.warn("Unexpected exception from downstream", e.getCause());
        e.getChannel().close();
    }

    @Override
    public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception
    {
        Channel ch = e.getChannel();
        ChannelBuffer time = dynamicBuffer(ByteOrder.BIG_ENDIAN, 4);
        final int theTime = (int) (System.currentTimeMillis() / 1000L);
        time.writeInt(theTime);

        ChannelFuture f = ch.write(time);

        f.addListener(ChannelFutureListener.CLOSE);
    }

    private final AtomicLong transferredBytes = new AtomicLong();

    public AtomicLong getTransferredBytes()
    {
        return transferredBytes;
    }
}
