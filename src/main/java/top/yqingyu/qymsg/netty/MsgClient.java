package top.yqingyu.qymsg.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class MsgClient {
    Bootstrap bootstrap;
    ConnectionPool pool;
    ConnectionConfig config;

    private EventLoopGroup group;

    private MsgClient() {
    }

    public static MsgClient create(ConnectionConfig config) {
        MsgClient client = new MsgClient();
        client.config = config;
        client.pool = new ConnectionPool(client);

        EventLoopGroup group = new NioEventLoopGroup(config.poolMax);
        client.group = group;
        Bootstrap bootstrap = new Bootstrap();
        client.bootstrap = bootstrap;

        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new QyMsgClientInitializer(client));
        return client;
    }

    public void shutdown() throws InterruptedException {
        group.shutdownGracefully().sync();
    }
}
