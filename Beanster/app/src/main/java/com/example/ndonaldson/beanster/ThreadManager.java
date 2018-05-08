package com.example.ndonaldson.beanster;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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
    private static final int KEEP_ALIVE = 1;

    private static UncaughtExceptionHandler mExceptionHandler = new UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            Log.i("ThreadManager", ex.getLocalizedMessage());
        }
    };

    private static final ThreadFactory mThreadFactory = new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setUncaughtExceptionHandler(mExceptionHandler);
            return t;
        }
    };

    private static final BlockingQueue<Runnable> mPoolWorkQueue = new LinkedBlockingQueue<Runnable>();
    private static final ThreadPoolExecutor mExecutor = new ThreadPoolExecutor(MAX_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, mPoolWorkQueue, mThreadFactory);


    /**
     * Empty constructor
     */
    public ThreadManager(){

    }


    /**
     * Run bits on a background thread
     * @param runnable bits to run in the background
     */
    public void runInBackground(Runnable runnable) {
        mExecutor.execute(runnable);
    }

}
