package top.yqingyu.qymsg.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import top.yqingyu.common.utils.ThreadUtil;
import top.yqingyu.qymsg.MsgTransfer;

public class MsgServer {

    private ServerBootstrap serverBootstrap;
    private int pool = Runtime.getRuntime().availableProcessors() * 2;
    private int radix = 32;
    private long clearTime = 30 * 60 * 1000;
    private int bodyLengthMax = 1400;
    private String serverName = "QyMsg";
    private String threadName = "handle";
    private Class<? extends QyMsgServerHandler> handler;
    private ServerExceptionHandler exceptionHandler;
    private MsgTransfer msgTransfer;
    private ChannelFuture future;

    private MsgServer() {
    }

    public static class Builder {
        public final MsgServer msgServer;

        public Builder() {
            msgServer = new MsgServer();
        }


        public Builder pool(int pool) {
            msgServer.pool = pool;
            return this;
        }

        public Builder radix(int radix) {
            msgServer.radix = radix;
            return this;
        }

        public Builder clearTime(int clearTime) {
            msgServer.clearTime = clearTime;
            return this;
        }

        public Builder serverName(String serverName) {
            msgServer.serverName = serverName;
            return this;
        }

        public Builder threadName(String threadName) {
            msgServer.threadName = threadName;
            return this;
        }

        public Builder handler(Class<? extends QyMsgServerHandler> handler) {
            msgServer.handler = handler;
            return this;
        }

        public Builder exceptionHandler(ServerExceptionHandler exceptionHandler) {
            msgServer.exceptionHandler = exceptionHandler;
            return this;
        }

        public Builder transfer(MsgTransfer msgTransfer) {
            msgServer.msgTransfer = msgTransfer;
            return this;
        }

        public Builder bodyLengthMax(int bodyLengthMax) {
            msgServer.bodyLengthMax = bodyLengthMax;
            return this;
        }

        public MsgServer build() throws Exception {
            NioEventLoopGroup serverGroup = new NioEventLoopGroup(1, ThreadUtil.createThFactoryC("BOSS", "Th"));
            NioEventLoopGroup clientGroup = new NioEventLoopGroup(msgServer.pool, ThreadUtil.createThFactoryC(msgServer.serverName, msgServer.threadName));
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(serverGroup, clientGroup);
            serverBootstrap.channel(NioServerSocketChannel.class);
            msgServer.msgTransfer = MsgTransfer.init(msgServer.radix, msgServer.bodyLengthMax, msgServer.clearTime);
            QyMsgServerInitializer initializer = new QyMsgServerInitializer(msgServer.handler, msgServer.msgTransfer);
            initializer.setQyMsgExceptionHandler(msgServer.exceptionHandler == null ? new ServerExceptionHandler() {
            } : msgServer.exceptionHandler);
            serverBootstrap.childHandler(initializer);
            msgServer.serverBootstrap = serverBootstrap;
            return msgServer;
        }
    }

    public void start(int port) throws Exception {
        future = serverBootstrap.bind(port).sync();
    }

    public void block() throws InterruptedException {
        future.channel().closeFuture().sync();
    }

    public ServerBootstrap getServerBootstrap() {
        return serverBootstrap;
    }

    public int getPool() {
        return pool;
    }

    public int getRadix() {
        return radix;
    }

    public long getClearTime() {
        return clearTime;
    }

    public String getServerName() {
        return serverName;
    }

    public String getThreadName() {
        return threadName;
    }

    public Class<? extends QyMsgServerHandler> getHandler() {
        return handler;
    }

    public ServerExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    public MsgTransfer getMsgTransfer() {
        return msgTransfer;
    }

    public ChannelFuture getFuture() {
        return future;
    }

    public int getBodyLengthMax() {
        return bodyLengthMax;
    }
}
