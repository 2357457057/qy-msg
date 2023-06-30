package top.yqingyu.qymsg.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import top.yqingyu.qymsg.MsgTransfer;

public class QyMsgClientInitializer extends ChannelInitializer<SocketChannel> {
    private final MsgTransfer transfer;

    public QyMsgClientInitializer(MsgTransfer transfer) {
        this.transfer = transfer;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new QyMsgEncodeBytes(transfer));
        pipeline.addLast(new BytesDecodeQyMsg(transfer));
        pipeline.addLast(new QyMsgClientHandler());
    }
}
