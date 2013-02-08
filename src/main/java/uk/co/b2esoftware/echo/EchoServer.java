package uk.co.b2esoftware.echo;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.MemoryAwareThreadPoolExecutor;
import org.jboss.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * User: art
 * Date: 16/01/2013
 * Time: 12:40
 */
public class EchoServer
{
    private final int port;
    private final EchoServerHandler serverHandler = new EchoServerHandler();
    private final Executor eventExecutor;

    public EchoServer(final int port, final int corePoolSize,
                      final int maxChannelMemorySize, final int maxTotalMemorySize)
    {
        this.port = port;
        this.eventExecutor = new MemoryAwareThreadPoolExecutor(
                corePoolSize, maxChannelMemorySize, maxTotalMemorySize, 100,
                TimeUnit.MILLISECONDS);
    }

    public void run()
    {

        ServerBootstrap bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool()));

        bootstrap.setPipelineFactory(new MyChannelPipelineFactory(new ExecutionHandler(eventExecutor)));

        bootstrap.bind(new InetSocketAddress(port));
    }

    private class MyChannelPipelineFactory implements ChannelPipelineFactory
    {
        private final ExecutionHandler executionHandler;
        private final StringEncoder stringEncoder = new StringEncoder(CharsetUtil.ISO_8859_1);
        private final StringDecoder stringDecoder = new StringDecoder(CharsetUtil.ISO_8859_1);

        MyChannelPipelineFactory(final ExecutionHandler executionHandler)
        {
            this.executionHandler = executionHandler;
        }

        @Override
        public ChannelPipeline getPipeline() throws Exception
        {
            // Construct the default pipeline
            ChannelPipeline pipeline = Channels.pipeline();
            // The DualProtocolFrameDecoder below will replace itself with a fallback delimiter-base decoder if the first 4
            // bytes don't match those in the binary protocol
            pipeline.addLast("binarydecoder", new DualProtocolFrameDecoder());
            pipeline.addLast("stringencoder", stringEncoder);
            pipeline.addLast("stringdecoder", stringDecoder);
            pipeline.addLast("pipelineExecutor", executionHandler);
            pipeline.addLast("handler", serverHandler);
            return pipeline;
        }
    }

    public static void main(String[] args)
    {
        int port = 9797;
        int corePoolSize = 2;
        final int maxChannelMemorySize = 100;
        final int maxTotalMemorySize = 1000;

        if (args.length > 0)
        {
            port = Integer.parseInt(args[0]);
        }

        new EchoServer(port, corePoolSize, maxChannelMemorySize, maxTotalMemorySize).run();
    }
}
