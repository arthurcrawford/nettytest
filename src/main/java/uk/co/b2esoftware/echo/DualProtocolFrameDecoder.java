package uk.co.b2esoftware.echo;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import java.nio.ByteBuffer;

/**
 * User: art
 * Date: 01/02/2013
 * Time: 10:53
 * Frame decoder based on the netty javadoc examples modified to support our needs.  This decoder allows multiplexing
 * the binary protocol with the fallback delimiter based text protocol.  If it detects that the first 4 bytes in the
 * buffer are not according to the binary protocol it replaces itself with the delimiter frame decoder to which it
 * passes the whole buffer.
 *
 * The protocol is:
 *
 *    1st 2 bytes  = message length
 *    next 2 bytes = length bytes end marker (to validate that the preceding 2 bytes are the length)
 *    next length bytes = message
 *
 * The expected length bytes are:
 *
 *     Hex    16 bit unsigned  string
 *     3C3E   15422            <>
 */
public class DualProtocolFrameDecoder extends FrameDecoder
{
    private final int LENGTH_END_BYTES = 15422;

    @Override
    protected Object decode(final ChannelHandlerContext ctx, final Channel channel,
                            final ChannelBuffer buffer) throws Exception
    {
        // Check for the leading four byte sequence of our binary protocol
        if (buffer.readableBytes() < 4)
        {
            // The length field was not received yet - return null.
            // This method will be invoked again when more packets are
            // received and appended to the buffer.
            return null;
        }

        // Mark the current buffer position before reading the length field
        // because the whole frame might not be in the buffer yet.
        // We will reset the buffer position to the marked position if
        // there's not enough bytes in the buffer.
        buffer.markReaderIndex();

        // Read 2 bytes as the length field.
        final int length = buffer.readShort() - 2;
        // Read the 2 byte length endMarker
        final short lengthEndMarker = buffer.readShort();

        // If it's not a correct length end marker, just return the frame as all available bytes
        if (lengthEndMarker != LENGTH_END_BYTES)
        {
            // Binary protocol not detected - make the fallback delimiter decoder next in the chain instead
            ctx.getPipeline().addLast("delimiterdecoder", new DelimiterBasedFrameDecoder(256, true, Delimiters.lineDelimiter()));
            ctx.getPipeline().remove(this);
            // Push the whole buffer onto the next decoder
            buffer.resetReaderIndex();
            return buffer.readBytes(buffer.readableBytes());
        }
        // Make sure if there's enough bytes in the buffer.
        else if (buffer.readableBytes() < length)
        {
            // The whole bytes were not received yet - return null.
            // This method will be invoked again when more packets are
            // received and appended to the buffer.

            // Reset to the marked position to read the length field again
            // next time.
            buffer.resetReaderIndex();
            return null;
        }
        else
        {
            // There's enough bytes in the buffer. Read it.
            // Successfully decoded a frame.  Return the decoded frame, rewritten as HEX values.
            ChannelBuffer returnBuffer = ChannelBuffers.dynamicBuffer();
            byte[] bytes = buffer.readBytes(length).array();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes)
            {
                sb.append(String.format("%02X", b));
            }
            returnBuffer.writeBytes(sb.toString().getBytes());
            return returnBuffer;
        }
    }
}
