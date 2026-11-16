package com.nhom02.streamwire;

import android.app.Application;
import com.nhom02.streamwire.cloud.SyncScheduler;

public class StreamWireApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        SyncScheduler.periodic(this);
        SyncScheduler.request(this);
        if (com.nhom02.streamwire.cloud.FirebaseRuntime.configured(this)) {
            android.net.ConnectivityManager network = (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            network.registerDefaultNetworkCallback(new android.net.ConnectivityManager.NetworkCallback() {
                private void notifyScreens() {
                    sendBroadcast(new android.content.Intent(com.nhom02.streamwire.cloud.FirebaseSyncWorker.ACTION).setPackage(getPackageName()));
                }
                @Override public void onAvailable(android.net.Network available) { SyncScheduler.request(StreamWireApplication.this); notifyScreens(); }
                @Override public void onLost(android.net.Network lost) { notifyScreens(); }
            });
        }
    }
}
