package com.nhom02.streamwire.cloud;

import android.content.Context;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.nhom02.streamwire.R;

public final class FirebaseRuntime {
    private static FirebaseApp app;
    private static FirebaseFirestore store;
    private FirebaseRuntime() {}

    public static synchronized boolean configured(Context context) {
        if (app != null) return true;
        String key = context.getString(R.string.firebase_api_key).trim();
        String id = context.getString(R.string.firebase_app_id).trim();
        String project = context.getString(R.string.firebase_project_id).trim();
        if (key.isEmpty() || id.isEmpty() || project.isEmpty()) return false;
        if (FirebaseApp.getApps(context).isEmpty()) app = FirebaseApp.initializeApp(context,
                new FirebaseOptions.Builder().setApiKey(key).setApplicationId(id).setProjectId(project).build());
        else app = FirebaseApp.getInstance();
        return true;
    }

    public static FirebaseAuth auth() { return FirebaseAuth.getInstance(app); }

    public static synchronized FirebaseFirestore store() {
        if (store == null) {
            store = FirebaseFirestore.getInstance(app);
            // SQLite owns the durable outbox; avoid a second persistent write queue.
            store.setFirestoreSettings(new FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build());
        }
        return store;
    }
}
