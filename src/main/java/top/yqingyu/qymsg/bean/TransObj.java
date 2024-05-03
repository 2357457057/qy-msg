package top.yqingyu.qymsg.bean;

import top.yqingyu.common.function.Progress;

import java.io.Serial;
import java.io.Serializable;

public class TransObj implements Serializable {
    @Serial
    private static final long serialVersionUID = 704921303243464801L;
    private String fileName;
    private  String fileId;
    private long size;
    private String sendPath;
    private String savePath;
    private String nextId;
    private boolean overwrite = false;
    private boolean rename = false;
    private String newName;
    private boolean isUpload = false;
    private boolean hasNext = false;
    private transient Progress progress;

    public TransObj() {
    }

    public TransObj(String fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isUpload() {
        return isUpload;
    }

    public void setUpload(boolean upload) {
        isUpload = upload;
    }

    public String getSendPath() {
        return sendPath;
    }

    public void setSendPath(String sendPath) {
        this.sendPath = sendPath;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public String getNextId() {
        return nextId;
    }


    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public boolean isRename() {
        return rename;
    }

    public void setRename(boolean rename) {
        this.rename = rename;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    public Progress getProgress() {
        return progress;
    }

    public void setProgress(Progress progress) {
        this.progress = progress;
    }

    public void setNextId(String nextId) {
        this.hasNext = true;
        this.nextId = nextId;
    }
}
