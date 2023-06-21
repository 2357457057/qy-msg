package top.yqingyu.qymsg.bean;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author YYJ
 * @version 1.0.0
 */
public class FileObj implements Serializable {
    @Serial
    private static final long serialVersionUID = -7186048204384950942L;
    private String name;
    private String current;
    private String parent;
    private long size;
    private boolean isDir;
    private List<FileObj> child;

    private long lastModified;

    public FileObj() {
    }

    public FileObj(File file) {
        this.name = file.getName();
        this.parent = file.getParent();
        this.current = file.getPath();
        this.isDir = file.isDirectory();
        this.child = new ArrayList<>();
        this.size = file.length();
        this.lastModified = file.lastModified();
    }

    public FileObj(String name, String parent, long size, boolean isDir, List<FileObj> child) {
        this.name = name;
        this.parent = parent;
        this.size = size;
        this.isDir = isDir;
        this.child = child;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isDir() {
        return isDir;
    }

    public void setDir(boolean dir) {
        isDir = dir;
    }

    public List<FileObj> getChild() {
        return child;
    }

    public void setChild(List<FileObj> child) {
        this.child = child;
    }

    public String getCurrent() {
        return current;
    }

    public void setCurrent(String current) {
        this.current = current;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
}
