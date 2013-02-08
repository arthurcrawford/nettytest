package uk.co.b2esoftware.time;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * User: art
 * Date: 16/01/2013
 * Time: 13:07
 */
public class TimeClient
{
    private final String host;
    private final int port;

    public TimeClient(final String host, final int port)
    {
        this.host = host;
        this.port = port;
    }

    public static void main(String[] args)
    {
        if (args.length < 2)
        {
            System.err.println(String.format("Usage: %s {host} {port}",
                    TimeClient.class.getName()));
        }

        final String host = args[0];
        final int port = Integer.parseInt(args[1]);
        new TimeClient(host, port).run();
    }

    public void run()
    {
        ClientBootstrap bootstrap = new ClientBootstrap(
            new NioClientSocketChannelFactory(
                    Executors.newCachedThreadPool(),
                    Executors.newCachedThreadPool()));

        bootstrap.setPipelineFactory(new ChannelPipelineFactory()
        {
            @Override
            public ChannelPipeline getPipeline() throws Exception
            {
                return Channels.pipeline(new TimeClientHandler());
            }
        });

        ChannelFuture future = bootstrap.connect(new InetSocketAddress(host, port));

        future.getChannel().getCloseFuture().awaitUninterruptibly();

        bootstrap.releaseExternalResources();
    }
}
