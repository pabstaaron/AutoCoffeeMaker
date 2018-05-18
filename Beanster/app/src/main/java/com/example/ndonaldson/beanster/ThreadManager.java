package com.example.ndonaldson.beanster;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

/**
 * Utility class to efficiently use background threads, report
 * all background exceptions, and provide easy access to the UI thread.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
public class ThreadManager {

    private static final int MAX_POOL_SIZE = 16;

    /**
     * Exception handler for mThreadFactory
     */
    private static UncaughtExceptionHandler mExceptionHandler = new UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            Log.i("ThreadManager", ex.getLocalizedMessage());
        }
    };

    /**
     * Logs which thread execution was rejected
     */
    private static RejectedExecutionHandler mRejectionHandler = new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            Log.i("ThreadManager", r.toString() + " failed was rejected");
        }
    };

    /**
     *Threadfactory with personalized uncaughtExceptionHandler within this class
     */
    private static final ThreadFactory mThreadFactory = new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setUncaughtExceptionHandler(mExceptionHandler);
            return t;
        }
    };

    //runs threads
    private static final ScheduledThreadPoolExecutor mExecutor = new ScheduledThreadPoolExecutor(MAX_POOL_SIZE, mThreadFactory, mRejectionHandler);

    /**
     * Empty constructor
     */
    public ThreadManager(){

    }


    /**
     * Run bits on a background thread
     * @param runnable bits to run in the background
     */
    public void runInBackground(Runnable runnable, long timer) {
        mExecutor.scheduleAtFixedRate(runnable, timer, timer, TimeUnit.MILLISECONDS);
    }

}
