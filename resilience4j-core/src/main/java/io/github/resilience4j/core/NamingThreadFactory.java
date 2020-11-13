package io.github.resilience4j.core;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates threads using "$name-%d" pattern for naming. Is based on {@link Executors#defaultThreadFactory}
 */
public class NamingThreadFactory implements ThreadFactory {

    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String prefix;

    public NamingThreadFactory(String name) {
        this.group = getThreadGroup();
        this.prefix = String.join("-",name, "");
    }

    private ThreadGroup getThreadGroup() {
        SecurityManager security = System.getSecurityManager();
        return security != null ? security.getThreadGroup()
            : Thread.currentThread().getThreadGroup();
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(group, runnable, createName(), 0);
        if (thread.isDaemon()) {
            thread.setDaemon(false);
        }
        if (thread.getPriority() != Thread.NORM_PRIORITY) {
            thread.setPriority(Thread.NORM_PRIORITY);
        }
        return thread;
    }

    private String createName() {
        return prefix + threadNumber.getAndIncrement();
    }
}
