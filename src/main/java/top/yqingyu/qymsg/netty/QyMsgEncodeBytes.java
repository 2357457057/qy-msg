package top.yqingyu.qymsg.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import top.yqingyu.qymsg.Dict;
import top.yqingyu.qymsg.MsgTransfer;
import top.yqingyu.qymsg.MsgType;
import top.yqingyu.qymsg.QyMsg;

import java.util.ArrayList;

/**
 * QyMsg 转Byte发送出去。
 */
public class QyMsgEncodeBytes extends MessageToByteEncoder<QyMsg> {
    private final MsgTransfer transfer;

    private final QyMsgExceptionHandler exceptionHandler;

    public QyMsgEncodeBytes(MsgTransfer transfer, QyMsgExceptionHandler exceptionHandler) {
        this.transfer = transfer;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, QyMsg msg, ByteBuf out) throws Exception {
        ArrayList<byte[]> MsgBytes = null;
        try {
            MsgBytes = transfer.msgEncoder.encode(msg);
        } catch (Exception e) {
            if (exceptionHandler != null)
                exceptionHandler.exceptionCaught(ctx, e);
            QyMsg clone = msg.clone();
            clone.setMsgType(MsgType.ERR_MSG);
            clone.putMsg("msg encodeException");
            clone.putMsgData(Dict.ERR_MSG_EXCEPTION, e);
            MsgBytes = transfer.msgEncoder.encode(clone);
        }
        for (byte[] bytes : MsgBytes) {
            out.writeBytes(bytes);
        }

    }
}
