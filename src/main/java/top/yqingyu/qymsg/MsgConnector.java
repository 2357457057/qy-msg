package top.yqingyu.qymsg;

import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yqingyu.common.utils.ArrayUtil;
import top.yqingyu.common.utils.IoUtil;
import top.yqingyu.common.utils.LocalDateTimeUtil;
import top.yqingyu.common.utils.ThreadUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class MsgConnector implements Runnable {
    private final ConcurrentHashMap<String, ArrayList<QyMsg>> MSG_CONTAINER = new ConcurrentHashMap<>();
    public static final Logger log = LoggerFactory.getLogger(MsgConnector.class);

    private long clearTime;

    public MsgConnector(long clearTime) {
        this.clearTime = clearTime;
    }

    public QyMsg merger(QyMsg qyMsg) throws IOException, ClassNotFoundException {
        String partition_id = qyMsg.getPartition_id();
        Integer denominator = qyMsg.getDenominator();
        ArrayList<QyMsg> list = MSG_CONTAINER.get(partition_id);

        //最后一块拼图。
        if (list != null && list.size() + 1 == denominator) {
            list.add(qyMsg);
            byte[] b = new byte[0];
            AtomicReference<byte[]> buf = new AtomicReference<>();
            buf.set(b);
            list.stream()
                    .sorted(Comparator.comparingInt(QyMsg::getNumerator))
                    .forEach(a ->
                            buf.set(
                                    ArrayUtil.addAll(buf.get(), (byte[]) MsgHelper.gainObjMsg(a))
                            )
                    );

            MsgType msgType = qyMsg.getMsgType();
            DataType dataType = qyMsg.getDataType();

            QyMsg out = new QyMsg(msgType, dataType);
            out.setSegmentation(false);
            out.setFrom(qyMsg.getFrom());
            if (DataType.JSON.equals(dataType)) {
                out = JSON.parseObject(buf.get(), QyMsg.class);
            } else if (DataType.OBJECT.equals(dataType)) {
                out = IoUtil.deserializationObj(buf.get(), QyMsg.class);
            } else if (DataType.STRING.equals(dataType)) {
                String s = new String(buf.get(), StandardCharsets.UTF_8);
                out.putMsg(s);
            } else if (DataType.STREAM.equals(dataType)) {
                out.putMsg(buf.get());
            } else {
                out.putMsg(buf.get());
            }

            MSG_CONTAINER.remove(partition_id);
            log.debug("消息 {} piece {} 拼接完成", partition_id, out.getDenominator());
            return out;
        } else if (list != null && MSG_CONTAINER.get(partition_id).size() + 1 != denominator) {
            qyMsg.putMsgData("now", LocalDateTime.now());
            MSG_CONTAINER.get(partition_id).add(qyMsg);
        } else {
            ArrayList<QyMsg> qyMsgArrayList = new ArrayList<>();
            qyMsgArrayList.add(qyMsg);
            MSG_CONTAINER.put(partition_id, qyMsgArrayList);
        }
        return null;
    }

    @Override
    public void run() {
        while (true)
            try {
                Thread.sleep(clearTime);
                LocalDateTime now = LocalDateTime.now();
                MSG_CONTAINER.forEach((k, list) -> {
                    Optional<QyMsg> max =
                            list.stream().max((o1, o2) ->
                                    (int) LocalDateTimeUtil.between(
                                            (LocalDateTime) MsgHelper.gainMsgOBJ(o1, "now"),
                                            (LocalDateTime) MsgHelper.gainMsgOBJ(o2, "now"),
                                            ChronoUnit.SECONDS)
                            );

                    if (max.isPresent()) {

                        //当前分区的数据最老数据
                        QyMsg maxMsg = max.get();
                        long min = LocalDateTimeUtil.between(now, (LocalDateTime) MsgHelper.gainMsgOBJ(maxMsg, "now"), ChronoUnit.MINUTES);
                        if (min > clearTime) {
                            MSG_CONTAINER.remove(k);
                            log.debug("消息过期，已清除 {} ", maxMsg);
                        }

                    }
                });
            } catch (Exception e) {
                log.error("容器清除器异常", e);
            }
    }
}
