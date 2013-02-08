package uk.co.b2esoftware.time;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * User: art
 * Date: 16/01/2013
 * Time: 12:40
 */
public class TimeServer
{
    private final int port;

    public TimeServer(final int port)
    {
        this.port = port;
    }

    public void run()
    {
        ServerBootstrap bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool()));

        bootstrap.setPipelineFactory(new ChannelPipelineFactory()
        {
            @Override
            public ChannelPipeline getPipeline() throws Exception
            {
                return Channels.pipeline(new TimeServerHandler());
            }
        });

        bootstrap.bind(new InetSocketAddress(port));
    }

    public static void main(String[] args)
    {
        int port;
        if (args.length > 0)
        {
            port = Integer.parseInt(args[0]);
        }
        else
        {
            port = 9797;
        }
        new TimeServer(port).run();
    }
}
