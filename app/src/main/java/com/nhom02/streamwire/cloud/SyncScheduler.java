package com.nhom02.streamwire.cloud;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.work.*;
import java.util.concurrent.TimeUnit;

public final class SyncScheduler {
    private static final String NAME = "firebase-sync";
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static Runnable pending;
    private SyncScheduler() {}

    public static synchronized void request(Context context) {
        if (!FirebaseRuntime.configured(context)) return;
        if (pending != null) HANDLER.removeCallbacks(pending);
        Context app = context.getApplicationContext();
        pending = () -> enqueue(app);
        HANDLER.postDelayed(pending, 750);
    }

    private static Constraints network() { return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(); }

    public static void enqueue(Context context) {
        if (!FirebaseRuntime.configured(context)) return;
        WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.APPEND_OR_REPLACE,
                new OneTimeWorkRequest.Builder(FirebaseSyncWorker.class).setConstraints(network())
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).addTag(NAME).build());
    }

    public static void periodic(Context context) {
        if (!FirebaseRuntime.configured(context)) return;
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME + "-periodic", ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(FirebaseSyncWorker.class, 15, TimeUnit.MINUTES).setConstraints(network()).addTag(NAME).build());
    }

    public static synchronized void cancel(Context context) {
        if (pending != null) HANDLER.removeCallbacks(pending);
        if (FirebaseRuntime.configured(context)) WorkManager.getInstance(context).cancelAllWorkByTag(NAME);
    }
}
