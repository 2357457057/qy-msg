package top.yqingyu.qymsg.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import top.yqingyu.qymsg.MsgConnector;
import top.yqingyu.qymsg.MsgTransfer;


/**
 * 初始化工具
 */
public class QyMsgServerInitializer extends ChannelInitializer<SocketChannel> {

    private final QyMsgServerHandler qyMsgServerHandler;
    private ServerExceptionHandler serverExceptionHandler;
    private final MsgTransfer transfer;

    public QyMsgServerInitializer(QyMsgServerHandler qyMsgServerHandler, MsgTransfer transfer) {
        //TEST CODE   MsgTransfer.init(32, 99999);
        this.qyMsgServerHandler = qyMsgServerHandler;
        this.transfer = transfer;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new BytesDecodeQyMsg(transfer));
        pipeline.addLast(qyMsgServerHandler);
        pipeline.addLast(new QyMsgEncodeBytes(transfer));
        pipeline.addLast(serverExceptionHandler == null ? new QyMsgExceptionHandler() : new QyMsgExceptionHandler(serverExceptionHandler));

    }

    public void setQyMsgExceptionHandler(ServerExceptionHandler qyMsgExceptionHandler) {
        this.serverExceptionHandler = qyMsgExceptionHandler;
    }
}
