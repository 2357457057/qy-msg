package top.yqingyu.qymsg;

import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yqingyu.common.utils.ArrayUtil;
import top.yqingyu.common.utils.IoUtil;
import top.yqingyu.common.utils.RadixUtil;
import top.yqingyu.common.utils.RandomStringUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;

import static top.yqingyu.qymsg.Dict.*;

/**
 * QyMsg 转为字节数据
 *
 * @author YYJ
 * @version 1.0.0
 */

public class MsgEncoder {
    private static final Logger log = LoggerFactory.getLogger(MsgEncoder.class);

    private final MsgTransfer transfer;

    public MsgEncoder(MsgTransfer transfer) {
        this.transfer = transfer;
    }

    /**
     * @param qyMsg 消息
     * @author YYJ
     * @description 消息组装
     */
    public ArrayList<byte[]> encode(QyMsg qyMsg) throws IOException {
        ArrayList<byte[]> list = new ArrayList<>();
        byte[] header = new byte[HEADER_LENGTH - BODY_LENGTH_LENGTH];
        header[MSG_TYPE_IDX] = MsgTransfer.MSG_TYPE_2_CHAR(qyMsg.getMsgType());
        header[DATA_TYPE_IDX] = MsgTransfer.DATA_TYPE_2_CHAR(qyMsg.getDataType());
        header[SEGMENTATION_IDX] = MsgTransfer.BOOLEAN_2_SEGMENTATION(false);
        String msgFrom = qyMsg.getFrom();
        if (msgFrom == null) {
            msgFrom = "";
        }
        byte[] from = msgFrom.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(from, 0, header, MSG_FROM_IDX_START, from.length);
        switch (qyMsg.getMsgType()) {
            case AC -> AC_Encode(header, qyMsg, list);
            case HEART_BEAT -> HEART_BEAT_Encode(header, list);
            case ERR_MSG -> ERR_MSG_Encode(header, qyMsg, list);
            default -> NORM_MSG_Encode(header, qyMsg, list);
        }
        //将信息长度与信息组合
        return list;
    }


    /**
     * 认证消息组装
     *
     * @param qyMsg  消息体对象
     * @param header 消息头
     * @param list   返回的消息集合
     */
    private void AC_Encode(byte[] header, QyMsg qyMsg, ArrayList<byte[]> list) throws IOException {
        byte[] body;
        if (Objects.requireNonNull(qyMsg.getDataType()) == DataType.OBJECT) {
            body = IoUtil.objToSerializBytes(qyMsg.Data());
        } else {
            body = JSON.toJSONBytes(qyMsg.Data());
        }
        OUT_OF_LENGTH_MSG_Encode(body, header, list);
    }

    /**
     * 心跳消息组装
     *
     * @param header 消息头
     * @param list   返回的消息集合
     */
    private void HEART_BEAT_Encode(byte[] header, ArrayList<byte[]> list) {
        header[DATA_TYPE_IDX] = MsgTransfer.DATA_TYPE_2_CHAR(DataType.STRING);
        list.add(ArrayUtil.addAll(header, transfer.getLength(ArrayUtil.EMPTY_BYTE_ARRAY)));
    }

    /**
     * 常规消息组装
     *
     * @param qyMsg  消息体对象
     * @param header 消息头
     * @param list   返回的消息集合
     */
    private void NORM_MSG_Encode(byte[] header, QyMsg qyMsg, ArrayList<byte[]> list) throws IOException {

        byte[] body;
        switch (qyMsg.getDataType()) {
            case OBJECT -> body = IoUtil.objToSerializBytes(qyMsg.Data());

            case STRING -> body = MsgHelper.gainMsg(qyMsg).getBytes(StandardCharsets.UTF_8);

            case STREAM -> body = (byte[]) MsgHelper.gainObjMsg(qyMsg);
            // JSON FILE
            default -> body = JSON.toJSONBytes(qyMsg.Data());

        }
        OUT_OF_LENGTH_MSG_Encode(body, header, list);
    }

    /**
     * 异常消息组装
     *
     * @param qyMsg  消息体对象
     * @param header 消息头
     * @param list   返回的消息集合
     */
    private void ERR_MSG_Encode(byte[] header, QyMsg qyMsg, ArrayList<byte[]> list) throws IOException {
        byte[] body;
        if (DataType.JSON.equals(qyMsg.getDataType())) {
            body = JSON.toJSONBytes(qyMsg.Data());
        } else if (DataType.OBJECT.equals(qyMsg.getDataType())) {
            body = IoUtil.objToSerializBytes(qyMsg.Data());
        } else {
            header[DATA_TYPE_IDX] = MsgTransfer.DATA_TYPE_2_CHAR(DataType.STRING);
            body = MsgHelper.gainMsg(qyMsg).getBytes(StandardCharsets.UTF_8);
        }
        OUT_OF_LENGTH_MSG_Encode(body, header, list);
    }

    /**
     * 对消息长度的判断  拆分并组装
     *
     * @param body   原消息体byte
     * @param header 消息头
     * @param list   返回的消息集合
     */
    private void OUT_OF_LENGTH_MSG_Encode(byte[] body, byte[] header, ArrayList<byte[]> list) {
        ArrayList<byte[]> bodyList = ArrayUtil.checkArrayLength(body, transfer.BODY_LENGTH_MAX);
        if (bodyList.size() == 1) {
            list.add(ArrayUtil.addAll(header, transfer.getLength(body), body));
            return;
        }
        header[SEGMENTATION_IDX] = MsgTransfer.BOOLEAN_2_SEGMENTATION(true);
        String part_trade_id = RandomStringUtil.random(PARTITION_ID_LENGTH, MsgTransfer.DICT);
        for (int i = 1; i <= bodyList.size(); i++) {
            byte[] cBody = bodyList.get(i - 1);
            byte[] length = transfer.getLength(cBody);
            byte[] partTradeIdBytes = part_trade_id.getBytes(StandardCharsets.UTF_8);
            byte[] NUMERATOR = ArrayUtil.leftPad(RadixUtil.radix2Byte(i, transfer.MSG_LENGTH_RADIX), NUMERATOR_LENGTH, RadixUtil.BYTE_DICT[0]);
            byte[] DENOMINATOR = ArrayUtil.leftPad(RadixUtil.radix2Byte(bodyList.size(), transfer.MSG_LENGTH_RADIX), DENOMINATOR_LENGTH, RadixUtil.BYTE_DICT[0]);
            byte[] bytes = ArrayUtil.addAll(header, length, partTradeIdBytes, NUMERATOR, DENOMINATOR, cBody);
            list.add(bytes);
        }
    }

}
