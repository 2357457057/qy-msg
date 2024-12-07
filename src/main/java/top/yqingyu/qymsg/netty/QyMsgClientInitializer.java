package top.yqingyu.qymsg.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import top.yqingyu.qymsg.MsgTransfer;

public class QyMsgClientInitializer extends ChannelInitializer<SocketChannel> {
    private final MsgTransfer transfer;
    private final ConnectionPool pool;

    public QyMsgClientInitializer(MsgClient client) {
        this.transfer = MsgTransfer.init(client.config.radix, client.config.bodyLengthMax, client.config.clearTime);
        this.pool = client.pool;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new QyMsgEncodeBytes(transfer, new QyMsgExceptionHandler()));
        pipeline.addLast(new BytesDecodeQyMsg(transfer));
        pipeline.addLast(new QyMsgClientHandler(pool));
    }
}
