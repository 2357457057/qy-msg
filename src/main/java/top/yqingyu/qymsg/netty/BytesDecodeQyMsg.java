package top.yqingyu.qymsg.netty;

import com.alibaba.fastjson2.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yqingyu.common.qydata.ConcurrentQyMap;
import top.yqingyu.common.utils.ArrayUtil;
import top.yqingyu.common.utils.IoUtil;
import top.yqingyu.common.utils.StringUtil;
import top.yqingyu.qymsg.*;
import top.yqingyu.qymsg.exception.IllegalQyMsgException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static top.yqingyu.qymsg.Dict.*;

/**
 * 配合netty的QyMsg解析器
 **/
public class BytesDecodeQyMsg extends ByteToMessageDecoder {

    private static final Logger log = LoggerFactory.getLogger(BytesDecodeQyMsg.class);
    private final ConcurrentQyMap<Integer, ConcurrentQyMap<String, Object>> DECODE_TEMP_CACHE = new ConcurrentQyMap<>();
    private final MsgConnector connector;
    private final MsgTransfer transfer;

    public BytesDecodeQyMsg(MsgTransfer transfer) {
        connector = transfer.connector;
        this.transfer = transfer;
    }


    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int ctxHashCode = ctx.hashCode();
        String header = null;
        String segmentationInfo = null;
        byte[] readBytes;
        byte[] segBody = null;
        byte[] singleBody = null;
        ConcurrentQyMap<String, Object> ctxData = DECODE_TEMP_CACHE.get(ctxHashCode);
        if (ctxData != null) {
            header = ctxData.get("header", String.class);
            segmentationInfo = ctxData.get("segmentationInfo", String.class);
            segBody = ctxData.get("segBody", byte[].class);
            singleBody = ctxData.get("singleBody", byte[].class);
        }

        if (header == null) {
            readBytes = readBytes(in, HEADER_LENGTH);
            header = new String(readBytes, StandardCharsets.UTF_8);
        }
        if (StringUtil.isNotBlank(header) && header.length() != HEADER_LENGTH) {
            throw new IllegalQyMsgException("消息头长度非法 消息头为：" + header);
        }
        char $0 = header.charAt(MSG_TYPE_IDX);//msg  type
        char $1 = header.charAt(DATA_TYPE_IDX);//data type
        char $2 = header.charAt(SEGMENTATION_IDX);//是否分片
        String $3 = header.substring(MSG_LENGTH_IDX_START, MSG_LENGTH_IDX_END);     //后五位

        boolean segmentation;
        try {
            segmentation = MsgTransfer.SEGMENTATION_2_BOOLEAN($2);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法分片字符: " + header, e);
        }

        if (segmentation) {
            MsgType msgType;
            try {
                msgType = MsgTransfer.CHAR_2_MSG_TYPE($0);
            } catch (Exception e) {
                throw new IllegalQyMsgException("非法的消息类型: " + header, e);
            }
            DataType dataType;
            try {
                dataType = MsgTransfer.CHAR_2_DATA_TYPE($1);
            } catch (Exception e) {
                throw new IllegalQyMsgException("非法的数据类型: " + header, e);
            }

            QyMsg parse = new QyMsg(msgType, dataType);
            parse.setSegmentation(true);

            if (segmentationInfo == null) {
                readBytes = readBytes(in, SEGMENTATION_INFO_LENGTH);
                segmentationInfo = new String(readBytes, StandardCharsets.UTF_8);
            }
            parse.setPartition_id(segmentationInfo.substring(PARTITION_ID_IDX_START, PARTITION_ID_IDX_END));
            parse.setNumerator(Integer.parseInt(segmentationInfo.substring(NUMERATOR_IDX_START, NUMERATOR_IDX_END), transfer.MSG_LENGTH_RADIX));
            parse.setDenominator(Integer.parseInt(segmentationInfo.substring(DENOMINATOR_IDX_START, DENOMINATOR_IDX_END), transfer.MSG_LENGTH_RADIX));
            int bodySize = Integer.parseInt($3, transfer.MSG_LENGTH_RADIX);
            int readableSize = in.readableBytes();
            if (segBody == null) {
                if (readableSize >= bodySize) {
                    readBytes = readBytes(in, bodySize);
                    parse.putMsg(readBytes);
                    QyMsg merger = connector.merger(parse);
                    if (merger != null) {
                        DECODE_TEMP_CACHE.remove(ctxHashCode);
                        ctx.fireChannelRead(merger);
                    }
                } else {
                    ConcurrentQyMap<String, Object> ctxInfo = new ConcurrentQyMap<>();
                    updateSegCtxInfo(ctxHashCode, ctxInfo, header, segmentationInfo, readBytes(in, readableSize));
                }
            } else {
                bodySize -= segBody.length;
                if (readableSize >= bodySize) {
                    DECODE_TEMP_CACHE.remove(ctxHashCode);
                    readBytes = ArrayUtil.addAll(segBody, readBytes(in, bodySize));
                    parse.putMsg(readBytes);
                    QyMsg merger = connector.merger(parse);
                    if (merger != null) {
                        DECODE_TEMP_CACHE.remove(ctxHashCode);
                        ctx.fireChannelRead(merger);
                    }
                } else {
                    segBody = ArrayUtil.addAll(segBody, readBytes(in, readableSize));
                    updateSegCtxInfo(ctxHashCode, ctxData, header, segmentationInfo, segBody);
                }
            }
        } else {
            MsgType msgType;
            try {
                msgType = MsgTransfer.CHAR_2_MSG_TYPE($0);
            } catch (Exception e) {
                throw new IllegalQyMsgException("非法的消息类型: " + header, e);
            }
            QyMsg qyMsg;
            switch (msgType) {
                case AC -> qyMsg = AC_Disassembly($3, in, ctxHashCode, ctxData, header, singleBody);
                case HEART_BEAT ->
                        qyMsg = HEART_BEAT_Disassembly($0, $1, $2, $3, in, ctxHashCode, ctxData, header, singleBody);
                case ERR_MSG ->
                        qyMsg = ERR_MSG_Disassembly($0, $1, $2, $3, in, ctxHashCode, ctxData, header, singleBody);
                default -> qyMsg = NORM_MSG_Disassembly($0, $1, $3, in, ctxHashCode, ctxData, header, singleBody);
            }
            if (qyMsg != null) {
                DECODE_TEMP_CACHE.remove(ctxHashCode);
                ctx.fireChannelRead(qyMsg);
            }
        }
    }

    private byte[] readBytes(ByteBuf buf, int length) {
        byte[] readBytes = new byte[length];
        buf.readBytes(readBytes);
        return readBytes;
    }

    private byte[] readBytes2(String msg_length, ByteBuf in, int ctxHashCode, ConcurrentQyMap<String, Object> ctxInfo, String header, byte[] body) {
        int bodySize = Integer.parseInt(msg_length, transfer.MSG_LENGTH_RADIX);
        int readableSize = in.readableBytes();
        if (body == null) {
            if (readableSize >= bodySize) {
                return readBytes(in, Integer.parseInt(msg_length, transfer.MSG_LENGTH_RADIX));
            } else {
                ctxInfo = new ConcurrentQyMap<>();
                updateSingleCtxInfo(ctxHashCode, ctxInfo, header, readBytes(in, readableSize));
                return null;
            }
        } else {
            bodySize -= body.length;
            if (readableSize >= bodySize) {
                return ArrayUtil.addAll(body, readBytes(in, bodySize));
            } else {
                body = ArrayUtil.addAll(body, readBytes(in, readableSize));
                updateSingleCtxInfo(ctxHashCode, ctxInfo, header, body);
                return null;
            }
        }

    }

    private void updateSegCtxInfo(int ctxHashCode, ConcurrentQyMap<String, Object> ctxInfo, String header, String segmentationInfo, byte[] body) {
        DECODE_TEMP_CACHE.put(ctxHashCode, ctxInfo);
        ctxInfo.put("header", header);
        ctxInfo.put("segmentationInfo", segmentationInfo);
        ctxInfo.put("segBody", body);
    }

    private void updateSingleCtxInfo(int ctxHashCode, ConcurrentQyMap<String, Object> ctxInfo, String header, byte[] body) {
        DECODE_TEMP_CACHE.put(ctxHashCode, ctxInfo);
        ctxInfo.put("header", header);
        ctxInfo.put("singleBody", body);
    }


    /**
     * 认证消息解析
     *
     * @param msg_length 消息长度
     * @param in         流
     */
    private QyMsg AC_Disassembly(String msg_length, ByteBuf in, int ctxHashCode, ConcurrentQyMap<String, Object> ctxInfo, String header, byte[] body) {
        byte[] bytes = readBytes2(msg_length, in, ctxHashCode, ctxInfo, header, body);
        if (bytes == null) return null;
        return JSON.parseObject(bytes, QyMsg.class);
    }

    /**
     * 心跳消息组装
     */
    private QyMsg HEART_BEAT_Disassembly(char msg_type, char data_type, char segmentationC, String msg_length, ByteBuf in, int ctxHashCode, ConcurrentQyMap<String, Object> ctxInfo, String header, byte[] body) {
        MsgType msgType;
        try {
            msgType = MsgTransfer.CHAR_2_MSG_TYPE(msg_type);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法的消息类型: " + header, e);
        }
        DataType dataType;
        try {
            dataType = MsgTransfer.CHAR_2_DATA_TYPE(data_type);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法的数据类型: " + header, e);
        }
        boolean segmentation;
        try {
            segmentation = MsgTransfer.SEGMENTATION_2_BOOLEAN(segmentationC);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法分片字符: " + header, e);
        }

        QyMsg qyMsg = new QyMsg(msgType, dataType);
        qyMsg.setSegmentation(segmentation);
        byte[] bytes = readBytes2(msg_length, in, ctxHashCode, ctxInfo, header, body);
        if (bytes == null) return null;
        qyMsg.setFrom(new String(bytes, StandardCharsets.UTF_8));
        return qyMsg;
    }

    /**
     * 常规消息组装
     */
    private QyMsg NORM_MSG_Disassembly(char msg_type, char data_type, String msg_length, ByteBuf in, int ctxHashCode, ConcurrentQyMap<String, Object> ctxInfo, String header, byte[] body) throws IOException, ClassNotFoundException {
        MsgType msgType;
        try {
            msgType = MsgTransfer.CHAR_2_MSG_TYPE(msg_type);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法的消息类型: " + header, e);
        }
        DataType dataType;
        try {
            dataType = MsgTransfer.CHAR_2_DATA_TYPE(data_type);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法的数据类型: " + header, e);
        }
        switch (dataType) {
            case STRING -> {
                byte[] bytes = readBytes2(msg_length, in, ctxHashCode, ctxInfo, header, body);
                if (bytes == null) return null;
                String s = new String(bytes, StandardCharsets.UTF_8);
                String from = s.substring(0, CLIENT_ID_LENGTH);
                String msg = s.substring(CLIENT_ID_LENGTH);
                QyMsg qyMsg = new QyMsg(msgType, dataType);
                qyMsg.setFrom(from);
                qyMsg.putMsg(msg);
                return qyMsg;
            }
            case OBJECT -> {
                byte[] bytes = readBytes2(msg_length, in, ctxHashCode, ctxInfo, header, body);
                if (bytes == null) return null;
                return IoUtil.deserializationObj(bytes, QyMsg.class);
            }
            case STREAM -> {
                return streamDeal(msg_type, data_type, msg_length, in, ctxHashCode, ctxInfo, header, body);
            }
            default -> { //JSON FILE
                byte[] bytes = readBytes2(msg_length, in, ctxHashCode, ctxInfo, header, body);
                if (bytes == null) return null;
                return JSON.parseObject(bytes, QyMsg.class);
            }
        }
    }

    /**
     * 异常消息组装
     */
    private QyMsg ERR_MSG_Disassembly(char msg_type, char data_type, char segmentation, String msg_length, ByteBuf in, int ctxHashCode, ConcurrentQyMap<String, Object> ctxInfo, String header, byte[] body) {
        DataType dataType;
        try {
            dataType = MsgTransfer.CHAR_2_DATA_TYPE(data_type);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法的数据类型: " + header, e);
        }
        if (DataType.JSON.equals(dataType)) {
            byte[] bytes = readBytes2(msg_length, in, ctxHashCode, ctxInfo, header, body);
            if (bytes == null) return null;
            return JSON.parseObject(bytes, QyMsg.class);
        } else {
            return streamDeal(msg_type, data_type, msg_length, in, ctxHashCode, ctxInfo, header, body);
        }
    }

    /**
     * @author YYJ
     * @description 流类型数据处理
     */
    private QyMsg streamDeal(char msg_type, char data_type, String msg_length, ByteBuf in, int ctxHashCode, ConcurrentQyMap<String, Object> ctxInfo, String header, byte[] body) {
        MsgType msgType;
        try {
            msgType = MsgTransfer.CHAR_2_MSG_TYPE(msg_type);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法的消息类型: " + header, e);
        }
        DataType dataType;
        try {
            dataType = MsgTransfer.CHAR_2_DATA_TYPE(data_type);
        } catch (Exception e) {
            throw new IllegalQyMsgException("非法的数据类型: " + header, e);
        }
        QyMsg qyMsg = new QyMsg(msgType, dataType);

        byte[] bytes = readBytes2(msg_length, in, ctxHashCode, ctxInfo, header, body);
        if (bytes == null) return null;

        byte[] from = new byte[CLIENT_ID_LENGTH];
        System.arraycopy(bytes, 0, from, 0, CLIENT_ID_LENGTH);

        body = new byte[Integer.parseInt(msg_length, transfer.MSG_LENGTH_RADIX) - CLIENT_ID_LENGTH];
        System.arraycopy(bytes, CLIENT_ID_LENGTH, body, 0, CLIENT_ID_LENGTH);

        qyMsg.setFrom(new String(from, StandardCharsets.UTF_8));
        qyMsg.putMsg(body);
        return qyMsg;
    }
}
