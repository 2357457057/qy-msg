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
    private final Object[] constructorParam;

    @SuppressWarnings("unchecked")
    public QyMsgServerInitializer(MsgTransfer transfer, Class<? extends QyMsgServerHandler> clazz, Object... constructorParam) throws Exception {
        if (constructorParam == null) {
            constructorParam = new Object[0];
        }
        this.constructorParam = constructorParam;
        Class<?>[] typeList = new Class[constructorParam.length];
        for (int i = 0; i < constructorParam.length; i++) {
            typeList[i] = constructorParam[i].getClass();

        }
        this.constructor = (Constructor<QyMsgServerHandler>) clazz.getConstructor(typeList);
        this.transfer = transfer;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        QyMsgExceptionHandler qyMsgExceptionHandler = serverExceptionHandler == null ? new QyMsgExceptionHandler() : new QyMsgExceptionHandler(serverExceptionHandler);
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new BytesDecodeQyMsg(transfer));
        pipeline.addLast(constructor.newInstance(constructorParam));
        pipeline.addLast(new QyMsgEncodeBytes(transfer, qyMsgExceptionHandler));
        pipeline.addLast(qyMsgExceptionHandler);

    }

    public void setQyMsgExceptionHandler(ServerExceptionHandler qyMsgExceptionHandler) {
        this.serverExceptionHandler = qyMsgExceptionHandler;
    }
}
