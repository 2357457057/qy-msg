package top.yqingyu.qymsg.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import top.yqingyu.common.utils.ThreadUtil;
import top.yqingyu.qymsg.MsgTransfer;

/**
 * netty Qymsg 的服务器demo
 */
public class DemoServer {

    public static void main(String[] args) throws InterruptedException {
        NioEventLoopGroup serverGroup = new NioEventLoopGroup(1, ThreadUtil.createThFactoryC("BOSS", "Th"));
        NioEventLoopGroup clientGroup = new NioEventLoopGroup(2, ThreadUtil.createThFactoryC("Main", "handler"));
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(serverGroup, clientGroup);
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.childHandler(new QyMsgServerInitializer(new DemoMsgHandler(), MsgTransfer.init(32, 60 * 30 * 1000)));
            ChannelFuture channelFuture = serverBootstrap.bind(4729).sync();
            channelFuture.channel().closeFuture().sync();
        } finally {
            serverGroup.shutdownGracefully();
            clientGroup.shutdownGracefully();
        }
    }
}
