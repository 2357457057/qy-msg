package top.yqingyu.qymsg;

import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yqingyu.common.utils.ArrayUtil;
import top.yqingyu.common.utils.IoUtil;
import top.yqingyu.common.utils.RandomStringUtil;
import top.yqingyu.common.utils.StringUtil;

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
 * @ClassNameEncodeMsg
 * @createTime 2022年09月06日 10:36:00
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
        StringBuilder sb = new StringBuilder();
        sb.append((char) MsgTransfer.MSG_TYPE_2_CHAR(qyMsg.getMsgType()));
        sb.append((char) MsgTransfer.DATA_TYPE_2_CHAR(qyMsg.getDataType()));

        switch (qyMsg.getMsgType()) {
            case AC -> AC_Encode(sb, qyMsg, list);
            case HEART_BEAT -> HEART_BEAT_Encode(sb, qyMsg, list);
            case ERR_MSG -> ERR_MSG_Encode(sb, qyMsg, list);
            default -> NORM_MSG_Encode(sb, qyMsg, list);
        }
        //将信息长度与信息组合
        return list;
    }


    /**
     * 认证消息组装
     *
     * @param qyMsg 消息体对象
     * @param sb    消息头SB
     * @param list  返回的消息集合
     */
    private void AC_Encode(StringBuilder sb, QyMsg qyMsg, ArrayList<byte[]> list) throws IOException {
        byte[] body;
        if (Objects.requireNonNull(qyMsg.getDataType()) == DataType.OBJECT) {
            body = IoUtil.objToSerializBytes(qyMsg);
        } else {
            body = JSON.toJSONString(qyMsg).getBytes(StandardCharsets.UTF_8);
        }
        OUT_OF_LENGTH_MSG_Encode(body, sb, list);
    }

    /**
     * 心跳消息组装
     *
     * @param qyMsg 消息体对象
     * @param sb    消息头SB
     * @param list  返回的消息集合
     */
    private void HEART_BEAT_Encode(StringBuilder sb, QyMsg qyMsg, ArrayList<byte[]> list) {
        sb.setCharAt(1, (char) MsgTransfer.DATA_TYPE_2_CHAR(DataType.STRING));
        sb.append(MsgTransfer.BOOLEAN_2_SEGMENTATION(false));
        byte[] body = qyMsg.getFrom().getBytes(StandardCharsets.UTF_8);
        sb.append(transfer.getLength(body));
        byte[] header = sb.toString().getBytes(StandardCharsets.UTF_8);
        list.add(ArrayUtil.addAll(header, body));
    }

    /**
     * 常规消息组装
     *
     * @param qyMsg 消息体对象
     * @param sb    消息头SB
     * @param list  返回的消息集合
     */
    private void NORM_MSG_Encode(StringBuilder sb, QyMsg qyMsg, ArrayList<byte[]> list) throws IOException {

        byte[] body;
        switch (qyMsg.getDataType()) {
            case OBJECT -> body = IoUtil.objToSerializBytes(qyMsg);

            case STRING -> body = (qyMsg.getFrom() + MsgHelper.gainMsg(qyMsg)).getBytes(StandardCharsets.UTF_8);

            case STREAM ->
                    body = ArrayUtil.addAll(qyMsg.getFrom().getBytes(StandardCharsets.UTF_8), (byte[]) MsgHelper.gainObjMsg(qyMsg));
            // JSON FILE
            default -> body = JSON.toJSONString(qyMsg).getBytes(StandardCharsets.UTF_8);

        }
        OUT_OF_LENGTH_MSG_Encode(body, sb, list);
    }

    /**
     * 异常消息组装
     *
     * @param qyMsg 消息体对象
     * @param sb    消息头SB
     * @param list  返回的消息集合
     */
    private void ERR_MSG_Encode(StringBuilder sb, QyMsg qyMsg, ArrayList<byte[]> list) {
        byte[] body;
        if (DataType.JSON.equals(qyMsg.getDataType())) {
            body = JSON.toJSONString(qyMsg).getBytes(StandardCharsets.UTF_8);
        } else {
            body = (qyMsg.getFrom() + MsgHelper.gainMsg(qyMsg)).getBytes(StandardCharsets.UTF_8);
        }
        OUT_OF_LENGTH_MSG_Encode(body, sb, list);
    }

    /**
     * 对消息长度的判断  拆分并组装
     *
     * @param body 原消息体byte
     * @param sb   消息头SB
     * @param list 返回的消息集合
     */
    private void OUT_OF_LENGTH_MSG_Encode(byte[] body, StringBuilder sb, ArrayList<byte[]> list) {
        ArrayList<byte[]> bodyList = ArrayUtil.checkArrayLength(body, transfer.BODY_LENGTH_MAX);
        if (bodyList.size() == 1) {
            sb.append(MsgTransfer.BOOLEAN_2_SEGMENTATION(false));
            sb.append(transfer.getLength(body));
            byte[] header = sb.toString().getBytes(StandardCharsets.UTF_8);
            list.add(ArrayUtil.addAll(header, body));
        } else {
            sb.append(MsgTransfer.BOOLEAN_2_SEGMENTATION(true));
            String part_trade_id = RandomStringUtil.random(PARTITION_ID_LENGTH, MsgTransfer.DICT);
            for (int i = 1; i <= bodyList.size(); i++) {
                StringBuilder builder = new StringBuilder(sb);
                byte[] cBody = bodyList.get(i - 1);
                builder.append(transfer.getLength(cBody));
                builder.append(part_trade_id);
                builder.append(StringUtil.leftPad(Integer.toUnsignedString(i, transfer.MSG_LENGTH_RADIX), NUMERATOR_LENGTH, '0'));
                builder.append(StringUtil.leftPad(Integer.toUnsignedString(bodyList.size(), transfer.MSG_LENGTH_RADIX), DENOMINATOR_LENGTH, '0'));
                byte[] cHeader = builder.toString().getBytes(StandardCharsets.UTF_8);
                byte[] bytes = ArrayUtil.addAll(cHeader, cBody);
                list.add(bytes);
            }
        }
    }

}
