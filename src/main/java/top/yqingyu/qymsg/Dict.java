package top.yqingyu.qymsg;

/**
 * QyMsg 的常量字段。
 *
 * @author YYJ
 * @version 1.0.0
 * @ClassNameDict
 * @description
 * @createTime 2023年01月05日 23:23:00
 */
public interface Dict {
    /*==========================QyMSG=============================*/
    Integer CLIENT_ID_LENGTH = 32;
    String QYMSG = "MSG";
    String MSG_ID = "Q_MSG_ID";
    String ERR_MSG_EXCEPTION = "msgException";
    /*==========================QyMSG=============================*/

    /*==========================HEADER=============================*/
    Integer HEADER_LENGTH = 8 + CLIENT_ID_LENGTH;
    Integer MSG_TYPE_IDX = 0;
    Integer DATA_TYPE_IDX = 1;
    Integer SEGMENTATION_IDX = 2;
    Integer MSG_FROM_IDX_START = 3;
    Integer BODY_LENGTH_LENGTH = 5;
    Integer MSG_LENGTH_IDX_START = HEADER_LENGTH - 5;
    Integer MSG_LENGTH_IDX_END = HEADER_LENGTH;
    /*==========================HEADER=============================*/


    /*==========================分片信息=============================*/
    Integer PARTITION_ID_IDX_START = 0;
    Integer PARTITION_ID_LENGTH = 16;
    Integer PARTITION_ID_IDX_END = PARTITION_ID_IDX_START + PARTITION_ID_LENGTH;
    Integer NUMERATOR_IDX_START = 16;
    Integer NUMERATOR_LENGTH = 2;
    Integer NUMERATOR_IDX_END = NUMERATOR_IDX_START + NUMERATOR_LENGTH;
    Integer DENOMINATOR_IDX_START = 18;
    Integer DENOMINATOR_LENGTH = 2;
    Integer DENOMINATOR_IDX_END = DENOMINATOR_IDX_START + DENOMINATOR_LENGTH;
    Integer SEGMENTATION_INFO_LENGTH = PARTITION_ID_LENGTH + NUMERATOR_LENGTH + DENOMINATOR_LENGTH;
    /*==========================分片信息=============================*/


    /*==========================FileMSG=============================*/
    String FILE_ID = "FILE_ID";
    String FILE_NAME = "FILE_NAME";
    String FILE_LENGTH = "FILE_LENGTH";
    String FILE_LOCAL_PATH = "LOCAL_PATH";
    String FILE_REMOTE_PATH = "REMOTE_PATH";
    String FILE_CUT_TIMES = "CUT_TIMES";
    String FILE_IDX = "IDX";
    String FILE_POSITION = "POSITION";
    String FILE_CUT_LENGTH = "CUT_LENGTH";
    Integer TRANS_THREAD_MAX = 6;

    /*==========================FileMSG=============================*/

    String KEY_CMD = "key";
}
