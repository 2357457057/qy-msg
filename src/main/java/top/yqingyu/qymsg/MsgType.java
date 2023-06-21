package top.yqingyu.qymsg;



/**
 * QyMsg消息类型
 * @author YYJ
 * @version 1.0.0
 * @ClassNameMsgType
 * @createTime 2022年09月02日 00:12:00
 */
public enum MsgType {
    AC,             // 认证消息
    HEART_BEAT,     //心跳
    NORM_MSG,       //普通消息
    ERR_MSG;        //普通消息
}