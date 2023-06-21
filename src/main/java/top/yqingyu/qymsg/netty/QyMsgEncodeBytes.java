package top.yqingyu.qymsg.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import top.yqingyu.qymsg.MsgTransfer;
import top.yqingyu.qymsg.QyMsg;

import java.util.ArrayList;

/**
 * QyMsg 转Byte发送出去。
 */
public class QyMsgEncodeBytes extends MessageToByteEncoder<QyMsg> {
    private final MsgTransfer transfer;

    public QyMsgEncodeBytes(MsgTransfer transfer) {
        this.transfer = transfer;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, QyMsg msg, ByteBuf out) throws Exception {
        ArrayList<byte[]> MsgBytes = transfer.msgEncoder.encode(msg);
        for (byte[] bytes : MsgBytes) {
            out.writeBytes(bytes);
        }

    }
}
