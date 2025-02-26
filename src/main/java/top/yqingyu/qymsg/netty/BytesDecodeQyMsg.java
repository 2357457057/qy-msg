package top.yqingyu.qymsg.netty;

import com.alibaba.fastjson2.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yqingyu.common.qydata.ConcurrentQyMap;
import top.yqingyu.common.qydata.DataMap;
import top.yqingyu.common.utils.ArrayUtil;
import top.yqingyu.common.utils.IoUtil;
import top.yqingyu.qymsg.*;
import top.yqingyu.qymsg.serialize.KryoSerializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static top.yqingyu.qymsg.Dict.HEADER_LENGTH;
import static top.yqingyu.qymsg.Dict.SEGMENTATION_INFO_LENGTH;

/**
 * 配合netty的QyMsg解析器
 **/
public class BytesDecodeQyMsg extends ByteToMessageDecoder {

    private static final Logger log = LoggerFactory.getLogger(BytesDecodeQyMsg.class);
    private final ConcurrentQyMap<Integer, ConcurrentQyMap<String, Object>> DECODE_TEMP_CACHE = new ConcurrentQyMap<>();
    private final MsgConnector connector;
    private final MsgDecoder decoder;

    public BytesDecodeQyMsg(MsgTransfer transfer) {
        connector = transfer.connector;
        decoder = new MsgDecoder(transfer);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        try {
            decode0(ctx, in);
        } catch (Throwable w) {
            log.error("QyMsg decode error", w);
            throw w;
        }
    }

    protected void decode0(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        int ctxHashCode = ctx.hashCode();
        ConcurrentQyMap<String, Object> ctxData = DECODE_TEMP_CACHE.computeIfAbsent(ctxHashCode, k -> new ConcurrentQyMap<>());
        byte[] header = ctxData.get("header", byte[].class);
        byte[] segmentationInfo = ctxData.get("segmentationInfo", byte[].class);
        QyMsg msg = ctxData.get("obj", QyMsg.class);

        if (msg == null) {
            if (header == null) {
                int readLength = in.readableBytes();
                if (readLength >= HEADER_LENGTH) {
                    readLength = HEADER_LENGTH;
                }
                header = updateHeader(ctxData, readBytes(in, readLength));
                if (readLength < HEADER_LENGTH) {
                    return;
                }
            } else if (header.length != HEADER_LENGTH) {
                int remainingHeaderLength = HEADER_LENGTH - header.length;
                int readLength = in.readableBytes();
                if (readLength >= remainingHeaderLength) {
                    readLength = remainingHeaderLength;
                }
                header = updateHeader(ctxData, readBytes(in, readLength));
                if (header.length != HEADER_LENGTH) {
                    return;
                }
            }
            msg = decoder.createMsg(header);
            ctxData.put("obj", msg);
        }


        if (!msg.isSegmentation()) {
            QyMsg qyMsg;
            switch (msg.getMsgType()) {
                case NORM_MSG -> qyMsg = NORM_MSG_Disassembly(in, ctxData);
                case AC -> qyMsg = AC_Disassembly(in, ctxData);
                case HEART_BEAT -> qyMsg = msg;
                default -> qyMsg = ERR_MSG_Disassembly(in, ctxData);
            }
            if (qyMsg != null) {
                DECODE_TEMP_CACHE.remove(ctxHashCode);
                ctx.fireChannelRead(qyMsg);
            }
            return;
        }

        if (segmentationInfo == null) {
            int readLength = in.readableBytes();
            if (readLength >= SEGMENTATION_INFO_LENGTH) {
                readLength = SEGMENTATION_INFO_LENGTH;
            }
            segmentationInfo = updateSegmentationInfo(ctxData, readBytes(in, readLength));
            if (readLength < SEGMENTATION_INFO_LENGTH) {
                return;
            }
        } else if (segmentationInfo.length != SEGMENTATION_INFO_LENGTH) {
            int remainingHeaderLength = SEGMENTATION_INFO_LENGTH - segmentationInfo.length;
            int readLength = in.readableBytes();
            if (readLength >= remainingHeaderLength) {
                readLength = remainingHeaderLength;
            }
            segmentationInfo = updateSegmentationInfo(ctxData, readBytes(in, readLength));
            if (segmentationInfo.length != SEGMENTATION_INFO_LENGTH) {
                return;
            }
        }

        decoder.setSegmentInfo(msg, segmentationInfo);
        log.debug("PartMsgId: {} the part {} of {}", msg.getPartition_id(), msg.getNumerator(), msg.getDenominator());

        int bodySize = msg.getBodySize();
        int readableSize = in.readableBytes();
        byte[] msgBody = ctxData.get("body", byte[].class);
        if (msgBody == null) {
            if (readableSize >= bodySize) {
                DECODE_TEMP_CACHE.remove(ctxHashCode);
                msg.putMsg(readBytes(in, bodySize));
                QyMsg merger = connector.merger(msg);
                if (merger != null) {
                    ctx.fireChannelRead(merger);
                }
                return;
            }
            updateBody(ctxData, readBytes(in, readableSize));
            return;
        }
        bodySize -= msgBody.length;
        if (readableSize >= bodySize) {
            DECODE_TEMP_CACHE.remove(ctxHashCode);
            msg.putMsg(ArrayUtil.addAll(msgBody, readBytes(in, bodySize)));
            QyMsg merger = connector.merger(msg);
            if (merger != null) {
                ctx.fireChannelRead(merger);
            }
            return;
        }
        updateBody(ctxData, readBytes(in, readableSize));
    }

    private byte[] readBytes(ByteBuf buf, int length) {
        byte[] readBytes = new byte[length];
        buf.readBytes(readBytes);
        return readBytes;
    }

    private byte[] readBytes2(ByteBuf in, ConcurrentQyMap<String, Object> ctxData) {
        int bodySize = ctxData.get("obj", QyMsg.class).getBodySize();
        byte[] body = ctxData.get("body", byte[].class);
        int readableSize = in.readableBytes();
        if (body == null) {
            if (readableSize >= bodySize) {
                return readBytes(in, bodySize);
            }
            updateBody(ctxData, readBytes(in, readableSize));
            return null;
        }
        bodySize -= body.length;
        if (readableSize >= bodySize) {
            return ArrayUtil.addAll(body, readBytes(in, bodySize));
        }
        updateBody(ctxData, readBytes(in, readableSize));
        return null;
    }

    private byte[] updateHeader(ConcurrentQyMap<String, Object> ctxInfo, byte[] data) {
        return updateData(ctxInfo, "header", data);
    }

    private byte[] updateSegmentationInfo(ConcurrentQyMap<String, Object> ctxInfo, byte[] data) {
        return updateData(ctxInfo, "segmentationInfo", data);
    }

    private void updateBody(ConcurrentQyMap<String, Object> ctxInfo, byte[] data) {
        updateData(ctxInfo, "body", data);
    }

    private byte[] updateData(ConcurrentQyMap<String, Object> ctxInfo, String key, byte[] data) {
        if (data != null) {
            byte[] bytes = ctxInfo.get(key, byte[].class);
            if (bytes != null) {
                data = ArrayUtil.addAll(bytes, data);
            }
            ctxInfo.put(key, data);
        }
        return data;
    }


    /**
     * 认证消息解析
     *
     * @param in 流
     */
    private QyMsg AC_Disassembly(ByteBuf in, ConcurrentQyMap<String, Object> ctxData) throws IOException, ClassNotFoundException {
        byte[] bytes = readBytes2(in, ctxData);
        if (bytes == null) return null;
        QyMsg qyMsg = ctxData.get("obj", QyMsg.class);
        if (Objects.requireNonNull(qyMsg.getDataType()) == DataType.OBJECT) {
            return qyMsg.setDataMap(IoUtil.deserializationObj(bytes, DataMap.class));
        }
        return qyMsg.setDataMap(JSON.parseObject(bytes, DataMap.class));
    }


    /**
     * 常规消息组装
     */
    private QyMsg NORM_MSG_Disassembly(ByteBuf in, ConcurrentQyMap<String, Object> ctxData) throws IOException, ClassNotFoundException {
        QyMsg qyMsg = ctxData.get("obj", QyMsg.class);
        DataType dataType = qyMsg.getDataType();
        return getMsgFromByteBuf(in, ctxData, qyMsg, dataType);
    }

    private QyMsg getMsgFromByteBuf(ByteBuf in, ConcurrentQyMap<String, Object> ctxData, QyMsg qyMsg, DataType dataType) throws IOException, ClassNotFoundException {
        byte[] bytes = readBytes2(in, ctxData);
        if (bytes == null) return null;
        log.warn("bytes.length:{}", bytes.length);
        switch (dataType) {
            case KRYO5 -> {
                return qyMsg.setDataMap(KryoSerializer.INSTANCE.decode(bytes));
            }
            case JSON -> {
                return qyMsg.setDataMap(JSON.parseObject(bytes, DataMap.class));
            }
            case OBJECT -> {
                return qyMsg.setDataMap(IoUtil.deserializationObj(bytes, DataMap.class));
            }
            case STREAM -> {
                qyMsg.putMsg(bytes);
                return qyMsg;
            }
            case STRING -> {
                String s = new String(bytes, StandardCharsets.UTF_8);
                qyMsg.putMsg(s);
                return qyMsg;
            }
            default -> {
                return qyMsg.setDataMap(KryoSerializer.INSTANCE.decode(bytes));
            }
        }
    }

    /**
     * 异常消息组装
     */
    private QyMsg ERR_MSG_Disassembly(ByteBuf in, ConcurrentQyMap<String, Object> ctxData) throws IOException, ClassNotFoundException {
        QyMsg qyMsg = ctxData.get("obj", QyMsg.class);
        DataType dataType = qyMsg.getDataType();
        return getMsgFromByteBuf(in, ctxData, qyMsg, dataType);
    }

}
