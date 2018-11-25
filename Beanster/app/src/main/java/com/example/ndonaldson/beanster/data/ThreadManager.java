package com.example.ndonaldson.beanster.data;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.ndonaldson.beanster.activities.main;

/**
 * Utility class to efficiently use background threads, report
 * all background exceptions, and provide easy access to the UI thread.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
public class ThreadManager {

    private static final int MAX_POOL_SIZE = 16;
    private static Context context;
    private ScheduledFuture<?> future = null;

    /**
     * Exception handler for mThreadFactory
     */
    private static UncaughtExceptionHandler mExceptionHandler = new UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {

                    Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                Log.i("ThreadManager", ex.getLocalizedMessage());
                Intent mStartActivity = new Intent(context, main.class);
                mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
                        | Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent mPendingIntent = PendingIntent.getActivity(context, 0, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, mPendingIntent);
                System.exit(0);
            }
        });
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
     * Constructor
     */
    public ThreadManager(Context context){
        this.context = context;
    }


    /**
     * Run bits on a background thread
     * @param runnable bits to run in the background
     */
    public void runInBackground(Runnable runnable, long timer) {
        if(future != null) future.cancel(true);
        future = mExecutor.scheduleAtFixedRate(runnable, timer, timer, TimeUnit.MILLISECONDS);
    }

    public void clearThreads(){
        future.cancel(true);
        mExecutor.shutdownNow();
    }

}
