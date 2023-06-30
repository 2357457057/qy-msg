package top.yqingyu.qymsg.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import top.yqingyu.qymsg.MsgConnector;
import top.yqingyu.qymsg.MsgTransfer;

import java.lang.reflect.Constructor;


/**
 * 初始化工具
 */
public class QyMsgServerInitializer extends ChannelInitializer<SocketChannel> {

    private final Constructor<QyMsgServerHandler> constructor;
    private ServerExceptionHandler serverExceptionHandler;
    private final MsgTransfer transfer;

    @SuppressWarnings("unchecked")
    public QyMsgServerInitializer(Class<? extends QyMsgServerHandler> clazz, MsgTransfer transfer) throws Exception {
        this.constructor = (Constructor<QyMsgServerHandler>) clazz.getConstructor();
        this.transfer = transfer;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new BytesDecodeQyMsg(transfer));
        pipeline.addLast(constructor.newInstance());
        pipeline.addLast(new QyMsgEncodeBytes(transfer));
        pipeline.addLast(serverExceptionHandler == null ? new QyMsgExceptionHandler() : new QyMsgExceptionHandler(serverExceptionHandler));

    }

    public void setQyMsgExceptionHandler(ServerExceptionHandler qyMsgExceptionHandler) {
        this.serverExceptionHandler = qyMsgExceptionHandler;
    }
}
