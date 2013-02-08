package uk.co.b2esoftware.time;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * User: art
 * Date: 16/01/2013
 * Time: 13:17
 */
public class TimeClientHandler extends SimpleChannelUpstreamHandler
{
    Logger log = LoggerFactory.getLogger(TimeClientHandler.class);

    public TimeClientHandler()
    {
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception
    {
        log.info("message received");
        ChannelBuffer buf = (ChannelBuffer) e.getMessage();
        long serverTimeMillis = buf.readInt() * 1000L;
        System.out.println("new Date(serverTimeMillis) = " + new Date(serverTimeMillis));
        e.getChannel().close();
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent e) throws Exception
    {
        log.warn("Unexpected error from downstream", e.getCause());
        e.getChannel().close();
    }
}
