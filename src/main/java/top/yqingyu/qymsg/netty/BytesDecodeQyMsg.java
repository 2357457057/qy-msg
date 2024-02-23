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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static top.yqingyu.qymsg.Dict.*;

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
            decode0(ctx, in, out);
        } catch (Throwable w) {
            log.error("msg decode error", w);
            throw w;
        }
    }

    protected void decode0(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int ctxHashCode = ctx.hashCode();
        byte[] header = null;
        byte[] segmentationInfo = null;
        byte[] readBytes;
        byte[] segBody = null;
        byte[] singleBody = null;
        ConcurrentQyMap<String, Object> ctxData = DECODE_TEMP_CACHE.get(ctxHashCode);
        if (ctxData != null) {
            header = ctxData.get("header", byte[].class);
            segmentationInfo = ctxData.get("segmentationInfo", byte[].class);
            segBody = ctxData.get("segBody", byte[].class);
            singleBody = ctxData.get("singleBody", byte[].class);
        }


        if (header == null) {
            int readLength = in.readableBytes();
            if (readLength >= HEADER_LENGTH) {
                readLength = HEADER_LENGTH;
            }
            header = readBytes(in, readLength);
            log.warn(new String(header, StandardCharsets.UTF_8));
            if (readLength < HEADER_LENGTH) {
                updateSingleCtxInfo(ctxHashCode, new ConcurrentQyMap<>(), header, null);
                return;
            }
        } else if (header.length != HEADER_LENGTH) {
            int remainingHeaderLength = HEADER_LENGTH - header.length;
            int readLength = in.readableBytes();
            if (readLength >= remainingHeaderLength) {
                readLength = remainingHeaderLength;
            }
            header = ArrayUtil.addAll(header, readBytes(in, readLength));
            log.warn(new String(header, StandardCharsets.UTF_8));
            if (header.length < HEADER_LENGTH) {
                updateSingleCtxInfo(ctxHashCode, ctxData, header, null);
                return;
            }
        }


        boolean segmentation;
        try {
            segmentation = MsgTransfer.SEGMENTATION_2_BOOLEAN(header[SEGMENTATION_IDX]);
        } catch (Exception e) {
            throw decoder.handleException(e, "非法分片标识", header);
        }

        if (!segmentation) {
            MsgType msgType;
            try {
                msgType = MsgTransfer.CHAR_2_MSG_TYPE(header[MSG_TYPE_IDX]);
            } catch (Exception e) {
                throw decoder.handleException(e, "非法的消息标识", header);
            }
            QyMsg qyMsg;
            switch (msgType) {
                case AC -> qyMsg = AC_Disassembly(in, ctxHashCode, ctxData, header, singleBody);
                case HEART_BEAT -> qyMsg = HEART_BEAT_Disassembly(in, ctxHashCode, ctxData, header, singleBody);
                case ERR_MSG -> qyMsg = ERR_MSG_Disassembly(in, ctxHashCode, ctxData, header, singleBody);
                default -> qyMsg = NORM_MSG_Disassembly(in, ctxHashCode, ctxData, header, singleBody);
            }
            if (qyMsg != null) {
                DECODE_TEMP_CACHE.remove(ctxHashCode);
                ctx.fireChannelRead(qyMsg);
            }
            return;
        }
        QyMsg parse = decoder.createMsg(header);
        if (segmentationInfo == null) {
            int readLength = in.readableBytes();
            if (readLength >= SEGMENTATION_INFO_LENGTH) {
                readLength = SEGMENTATION_INFO_LENGTH;
            }
            segmentationInfo = readBytes(in, readLength);
            if (readLength < SEGMENTATION_INFO_LENGTH) {
                updateSegCtxInfo(ctxHashCode, ctxData == null ? new ConcurrentQyMap<>() : ctxData, header, segmentationInfo, null);
                return;
            }
        } else if (segmentationInfo.length != SEGMENTATION_INFO_LENGTH) {
            int remainingHeaderLength = HEADER_LENGTH - segmentationInfo.length;
            int readLength = in.readableBytes();
            if (readLength >= remainingHeaderLength) {
                readLength = remainingHeaderLength;
            }
            segmentationInfo = ArrayUtil.addAll(segmentationInfo, readBytes(in, readLength));
            if (segmentationInfo.length < SEGMENTATION_INFO_LENGTH) {
                updateSegCtxInfo(ctxHashCode, ctxData, header, segmentationInfo, null);
                return;
            }
        }
        decoder.setSegmentInfo(parse, segmentationInfo);
        int bodySize = decoder.getMsgLength(header);
        int readableSize = in.readableBytes();
        if (segBody == null) {
            if (readableSize >= bodySize) {
                readBytes = readBytes(in, bodySize);
                parse.putMsg(readBytes);
                log.debug("PartMsgId: {} the part {} of {}", parse.getPartition_id(), parse.getNumerator(), parse.getDenominator());
                QyMsg merger = connector.merger(parse);
                if (merger != null) {
                    DECODE_TEMP_CACHE.remove(ctxHashCode);
                    ctx.fireChannelRead(merger);
                }
            } else {
                updateSegCtxInfo(ctxHashCode, ctxData == null ? new ConcurrentQyMap<>() : ctxData, header, segmentationInfo, readBytes(in, readableSize));
            }
        } else {
            bodySize -= segBody.length;
            if (readableSize >= bodySize) {
                DECODE_TEMP_CACHE.remove(ctxHashCode);
                readBytes = ArrayUtil.addAll(segBody, readBytes(in, bodySize));
                parse.putMsg(readBytes);
                log.debug("PartMsgId: {} the part {} of {}", parse.getPartition_id(), parse.getNumerator(), parse.getDenominator());
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
    }

    private byte[] readBytes(ByteBuf buf, int length) {
        byte[] readBytes = new byte[length];
        buf.readBytes(readBytes);
        return readBytes;
    }

    private byte[] readBytes2(ByteBuf in, int ctxHashCode, ConcurrentQyMap<String, Object> ctxInfo, byte[] header, byte[] body) {
        int bodySize = decoder.getMsgLength(header);
        int readableSize = in.readableBytes();
        if (body == null) {
            if (readableSize >= bodySize) {
                return readBytes(in, bodySize);
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

    private void updateSegCtxInfo(int ctxHashCode, ConcurrentQyMap<String, Object> ctxInfo, byte[] header, byte[] segmentationInfo, byte[] body) {
        DECODE_TEMP_CACHE.put(ctxHashCode, ctxInfo);
        ctxInfo.put("header", header);
        ctxInfo.put("segmentationInfo", segmentationInfo);
        ctxInfo.put("segBody", body);
    }

    private void updateSingleCtxInfo(int ctxHashCode, ConcurrentQyMap<String, Object> ctxInfo, byte[] header, byte[] body) {
        DECODE_TEMP_CACHE.put(ctxHashCode, ctxInfo);
        ctxInfo.put("header", header);
        ctxInfo.put("singleBody", body);
    }


    /**
     * 认证消息解析
     *
     * @param in 流
     */
    private QyMsg AC_Disassembly(ByteBuf in, int ctxHashCode, ConcurrentQyMap<String, Object> ctxInfo, byte[] header, byte[] body) throws IOException, ClassNotFoundException {
        byte[] bytes = readBytes2(in, ctxHashCode, ctxInfo, header, body);
        if (bytes == null) return null;
        QyMsg qyMsg = decoder.createMsg(header);
        if (Objects.requireNonNull(qyMsg.getDataType()) == DataType.OBJECT) {
            return qyMsg.setDataMap(IoUtil.deserializationObj(bytes, DataMap.class));
        }
        return qyMsg.setDataMap(JSON.parseObject(bytes, DataMap.class));
    }

    /**
     * 心跳消息组装
     */
    private QyMsg HEART_BEAT_Disassembly(ByteBuf in, int ctxHashCode, ConcurrentQyMap<String, Object> ctxInfo, byte[] header, byte[] body) {
        QyMsg qyMsg = decoder.createMsg(header);
        byte[] bytes = readBytes2(in, ctxHashCode, ctxInfo, header, body);
        if (bytes == null) return null;
        qyMsg.setFrom(new String(bytes, StandardCharsets.UTF_8));
        return qyMsg;
    }

    /**
     * 常规消息组装
     */
    private QyMsg NORM_MSG_Disassembly(ByteBuf in, int ctxHashCode, ConcurrentQyMap<String, Object> ctxInfo, byte[] header, byte[] body) throws IOException, ClassNotFoundException {
        QyMsg qyMsg = decoder.createMsg(header);
        switch (qyMsg.getDataType()) {
            case STRING -> {
                byte[] bytes = readBytes2(in, ctxHashCode, ctxInfo, header, body);
                if (bytes == null) return null;
                String s = new String(bytes, StandardCharsets.UTF_8);
                String from = s.substring(0, CLIENT_ID_LENGTH);
                String msg = s.substring(CLIENT_ID_LENGTH);
                qyMsg.setFrom(from);
                qyMsg.putMsg(msg);
                return qyMsg;
            }
            case OBJECT -> {
                byte[] bytes = readBytes2(in, ctxHashCode, ctxInfo, header, body);
                if (bytes == null) return null;
                return qyMsg.setDataMap(IoUtil.deserializationObj(bytes, DataMap.class));
            }
            case STREAM -> {
                return streamDeal(in, ctxHashCode, ctxInfo, header, body);
            }
            default -> { //JSON FILE
                byte[] bytes = readBytes2(in, ctxHashCode, ctxInfo, header, body);
                if (bytes == null) return null;
                return qyMsg.setDataMap(JSON.parseObject(bytes, DataMap.class));
            }
        }
    }

    /**
     * 异常消息组装
     */
    private QyMsg ERR_MSG_Disassembly(ByteBuf in, int ctxHashCode, ConcurrentQyMap<String, Object> ctxInfo, byte[] header, byte[] body) throws IOException, ClassNotFoundException {
        QyMsg qyMsg = decoder.createMsg(header);
        if (DataType.JSON.equals(qyMsg.getDataType())) {
            byte[] bytes = readBytes2(in, ctxHashCode, ctxInfo, header, body);
            if (bytes == null) return null;
            return qyMsg.setDataMap(JSON.parseObject(bytes, DataMap.class));
        } else if (DataType.OBJECT.equals(qyMsg.getDataType())) {
            byte[] bytes = readBytes2(in, ctxHashCode, ctxInfo, header, body);
            if (bytes == null) return null;
            return qyMsg.setDataMap(IoUtil.deserializationObj(bytes, DataMap.class));
        } else {
            return streamDeal(in, ctxHashCode, ctxInfo, header, body);
        }
    }

    /**
     * 流类型数据处理
     *
     * @author YYJ
     */
    private QyMsg streamDeal(ByteBuf in, int ctxHashCode, ConcurrentQyMap<String, Object> ctxInfo, byte[] header, byte[] body) {
        QyMsg qyMsg = decoder.createMsg(header);

        byte[] bytes = readBytes2(in, ctxHashCode, ctxInfo, header, body);
        if (bytes == null) return null;

        byte[] from = new byte[CLIENT_ID_LENGTH];
        System.arraycopy(bytes, 0, from, 0, CLIENT_ID_LENGTH);

        body = new byte[decoder.getMsgLength(header) - CLIENT_ID_LENGTH];
        System.arraycopy(bytes, CLIENT_ID_LENGTH, body, 0, CLIENT_ID_LENGTH);

        qyMsg.setFrom(new String(from, StandardCharsets.UTF_8));
        qyMsg.putMsg(body);
        return qyMsg;
    }

}
