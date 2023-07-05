package top.yqingyu.qymsg.netty;

import io.netty.channel.ChannelHandlerContext;
import top.yqingyu.qymsg.QyMsg;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class Connection {
    final ChannelHandlerContext ctx;
    private final int hash;
    final LinkedBlockingQueue<QyMsg> MSG_QUEUE = new LinkedBlockingQueue<>();
    private final ReentrantLock getLock = new ReentrantLock();
    Connection(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        hash = ctx.hashCode();
    }


    public void write(QyMsg msg) {
        ctx.writeAndFlush(msg);
    }

    public QyMsg take() throws InterruptedException {
        return MSG_QUEUE.take();
    }

    public QyMsg take(long timeout) throws InterruptedException {
        return MSG_QUEUE.poll(timeout, TimeUnit.MILLISECONDS);
    }

    public QyMsg get(QyMsg msg) throws InterruptedException {
        try {
            getLock.lock();
            write(msg);
            return take();
        } finally {
            getLock.unlock();
        }
    }

    public QyMsg get(QyMsg msg, long timeout) throws InterruptedException {
        try {
            getLock.lock();
            write(msg);
            return take(timeout);
        } finally {
            getLock.unlock();
        }
    }


    void put(QyMsg msg) throws InterruptedException {
        MSG_QUEUE.put(msg);
    }

    int getHash() {
        return hash;
    }
}
