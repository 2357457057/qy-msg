package top.yqingyu.qymsg.socket;

import top.yqingyu.qymsg.MsgTransfer;
import top.yqingyu.qymsg.QyMsg;
import top.yqingyu.qymsg.netty.Constants;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class Connection {
    Socket socket;
    volatile boolean closed = false;
    final ReentrantLock getLock = new ReentrantLock();
    private volatile long activeTime = System.nanoTime();

    ConnectionConfig connectionConfig;

    MsgTransfer msgTransfer;

    public Connection(ConnectionConfig connectionConfig) throws IOException {
        this.connectionConfig = connectionConfig;
        msgTransfer = connectionConfig.msgTransfer;
        this.socket = new Socket(connectionConfig.host, connectionConfig.port);
    }

    void write(QyMsg msg) throws Exception {
        activeTime = System.nanoTime();
        msgTransfer.writeQyMsg(socket, msg);
    }

    QyMsg read() throws IOException, ClassNotFoundException, InterruptedException {
        QyMsg rtn;
        do {
            rtn = msgTransfer.readQyMsg(socket, new AtomicBoolean(true));
        } while (rtn == null);
        return rtn;
    }

    QyMsg read(long timeout) throws Exception {
        FutureTask<QyMsg> futureTask = new FutureTask<>(this::read);
        Thread thread = new Thread(futureTask);
        thread.setDaemon(true);
        thread.setName("connection" + hashCode());
        thread.start();
        try {
            return futureTask.get(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            close();
            thread.stop();
            throw e;
        }
    }

    public QyMsg get(QyMsg msg) throws Exception {
        try {
            getLock.lock();
            write(msg);
            return read();
        } finally {
            activeTime = System.nanoTime();
            getLock.unlock();
        }
    }

    public QyMsg get(QyMsg msg, long timeout) throws Exception {
        try {
            getLock.lock();
            write(msg);
            return read(timeout);
        } finally {
            activeTime = System.nanoTime();
            getLock.unlock();
        }
    }

    public void close() throws IOException {
        closed = true;
        socket.close();
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean needKeep() {
        return System.nanoTime() - activeTime > Constants.noOpMaxTime;
    }
}
