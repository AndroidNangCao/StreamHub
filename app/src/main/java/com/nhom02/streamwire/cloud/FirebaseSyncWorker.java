package com.nhom02.streamwire.cloud;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class FirebaseSyncWorker extends Worker {
    public static final String ACTION="com.nhom02.streamwire.SYNC_CHANGED";
    private String uid;
    private static final Object LOCK=new Object();
    public FirebaseSyncWorker(@NonNull Context context,@NonNull WorkerParameters params){super(context,params);}
    @NonNull @Override public Result doWork() {
        if(!FirebaseRuntime.configured(getApplicationContext()))return Result.success();
        synchronized(LOCK) {
            com.google.firebase.auth.FirebaseUser user=FirebaseRuntime.auth().getCurrentUser();uid=user==null?"guest":user.getUid();
            SyncStore local=new SyncStore(getApplicationContext());
            try {
                status(local,"Đang đồng bộ…");new CloudSyncEngine(getApplicationContext()).sync();
                status(local,"Đã đồng bộ Firebase");return Result.success();
            } catch(Exception error) {
                Throwable cause=error;while(cause.getCause()!=null)cause=cause.getCause();
                if(cause instanceof InterruptedException){Thread.currentThread().interrupt();return Result.retry();}
                boolean denied=cause instanceof FirebaseFirestoreException&&((FirebaseFirestoreException)cause).getCode()==FirebaseFirestoreException.Code.PERMISSION_DENIED;
                status(local,denied?"Chưa đồng bộ: kiểm tra quyền Firebase":"Chưa đồng bộ. Dữ liệu vẫn được lưu trên máy.");
                android.util.Log.w("FirebaseSync","Synchronization incomplete",error);
                return denied||cause instanceof IllegalStateException?Result.failure():Result.retry();
            }
        }
    }
    private void status(SyncStore local,String message) {
        local.state("status:"+uid,message);
        getApplicationContext().sendBroadcast(new Intent(ACTION).setPackage(getApplicationContext().getPackageName()));
    }
}
