package com.taobao.metamorphosis.server.filter;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import com.taobao.gecko.core.util.StringUtils;
import com.taobao.metamorphosis.consumer.ConsumerMessageFilter;
import com.taobao.metamorphosis.server.Service;
import com.taobao.metamorphosis.server.utils.MetaConfig;
import com.taobao.metamorphosis.utils.ThreadUtils;


/**
 * Consumer filter manager.
 * 
 * @author dennis<killme2008@gmail.com>
 * 
 */
public class ConsumerFilterManager implements Service {
    private ClassLoader filterClassLoader;
    private final ConcurrentHashMap<String/* class name */, FutureTask<ConsumerMessageFilter>> filters =
            new ConcurrentHashMap<String, FutureTask<ConsumerMessageFilter>>();


    public ConsumerFilterManager(MetaConfig metaConfig) throws Exception {
        String path = "/tmp";
        this.filterClassLoader =
                new URLClassLoader(new URL[] { new URL(path) }, Thread.currentThread().getContextClassLoader());
    }


    public ConsumerMessageFilter findFilter(String group) {
        final String className = null;
        if (StringUtils.isBlank(className)) {
            return null;
        }
        FutureTask<ConsumerMessageFilter> task = this.filters.get(className);
        if (task == null) {
            task = new FutureTask<ConsumerMessageFilter>(new Callable<ConsumerMessageFilter>() {

                @Override
                public ConsumerMessageFilter call() throws Exception {
                    return ConsumerFilterManager.this.intanceFilter(className);
                }

            });
            FutureTask<ConsumerMessageFilter> existsTask = this.filters.putIfAbsent(className, task);
            if (existsTask != null) {
                task = existsTask;
            }
        }
        return this.getFilter0(task);
    }


    @SuppressWarnings("unchecked")
    private ConsumerMessageFilter intanceFilter(String className) throws Exception {
        Class<ConsumerMessageFilter> clazz =
                (Class<ConsumerMessageFilter>) Class.forName(className, true, this.filterClassLoader);
        if (clazz != null) {
            return clazz.newInstance();
        }
        else {
            return null;
        }
    }


    private ConsumerMessageFilter getFilter0(FutureTask<ConsumerMessageFilter> task) {
        try {
            return task.get();
        }
        catch (ExecutionException e) {
            throw ThreadUtils.launderThrowable(e.getCause());
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }


    @Override
    public void init() {

    }


    @Override
    public void dispose() {
        this.filterClassLoader = null;
        this.filters.clear();

    }

}
