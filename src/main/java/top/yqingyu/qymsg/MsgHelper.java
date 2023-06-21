package top.yqingyu.qymsg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yqingyu.common.utils.*;

import java.io.File;
import java.util.*;


import static top.yqingyu.qymsg.Dict.*;

/**
 * @author YYJ
 * @version 1.0.0
 * @ClassNameMsgHelper
 * @description
 * @createTime 2022年09月02日 00:31:00
 */

public class MsgHelper {

    private static final Logger log = LoggerFactory.getLogger(MsgHelper.class);

    public static String gainMsg(QyMsg msg) {
        return msg.getDataMap().getString(QYMSG, "");
    }

    public static Object gainObjMsg(QyMsg msg) {
        return msg.getDataMap().get(QYMSG);
    }

    public static String gainMsgValue(QyMsg msg, String key) {
        return msg.getDataMap().getString(key, "");
    }

    public static Object gainMsgOBJ(QyMsg msg, String key) {
        return msg.getDataMap().get(key);
    }

    //     * 文件唯一ID
//     * 文件名
//     * 文件总长度
//     * 切分次数
//     * 切分长度

    /**
     * 暂且不用
     */
    @Deprecated
    public static List<QyMsg> buildFileMsg(File file, int transThread, String remotePath) throws CloneNotSupportedException {
        if (!(transThread > 0 && transThread <= TRANS_THREAD_MAX))
            throw new IllegalArgumentException("线程数不对！");

        long fileLength = file.length();
        long CUT_LENGTH = fileLength / transThread;
        long yu = fileLength % transThread;

        QyMsg qyMsg = new QyMsg(MsgType.NORM_MSG, DataType.FILE);
        qyMsg.putMsgData(FILE_ID, UUIDUtil.randomUUID().toString());
        qyMsg.putMsgData(FILE_NAME, file.getName());
        qyMsg.putMsgData(FILE_LENGTH, fileLength);

        qyMsg.putMsgData(FILE_LOCAL_PATH, file.getPath());
        qyMsg.putMsgData(FILE_REMOTE_PATH, remotePath);
        qyMsg.putMsgData(FILE_CUT_TIMES, transThread);

        ArrayList<QyMsg> msg = new ArrayList<>();

        if (transThread != 1) {
            for (int i = 1; i <= transThread; i++) {
                QyMsg clone = qyMsg.clone();
                msg.add(clone);
                clone.putMsgData(FILE_IDX, i);
                clone.putMsgData(FILE_POSITION, Math.max((i - 1) * CUT_LENGTH - 1, 0));
                clone.putMsgData(FILE_CUT_LENGTH, transThread - 1 == i ? yu : CUT_LENGTH);
            }
            return msg;
        }

        qyMsg.putMsgData(FILE_POSITION, 0);
        msg.add(qyMsg);
        return msg;
    }
}
