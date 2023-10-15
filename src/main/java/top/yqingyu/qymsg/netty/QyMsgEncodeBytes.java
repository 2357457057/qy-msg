package top.yqingyu.qymsg.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import top.yqingyu.qymsg.MsgTransfer;
import top.yqingyu.qymsg.MsgType;
import top.yqingyu.qymsg.QyMsg;

import java.io.NotSerializableException;
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
        try {
            ArrayList<byte[]> MsgBytes = transfer.msgEncoder.encode(msg);
            for (byte[] bytes : MsgBytes) {
                out.writeBytes(bytes);
            }
        } catch (NotSerializableException serializableException) {
            if (exceptionHandler != null)
                exceptionHandler.exceptionCaught(ctx, serializableException);
            QyMsg clone = msg.clone();
            clone.setMsgType(MsgType.ERR_MSG);
            clone.putMsg(serializableException);
            encode(ctx, clone, out);
        } catch (Exception e) {
            if (exceptionHandler != null)
                exceptionHandler.exceptionCaught(ctx, e);
        }

    }
}
