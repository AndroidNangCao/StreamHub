package com.nhom02.streamwire.cloud;

import android.content.Context;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.nhom02.streamwire.model.User;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Invoked by WorkManager off the UI thread. Reads must reach the server before marking sync successful. */
public final class CloudSyncEngine {
    private final SyncStore local;
    private final FirebaseFirestore cloud;
    private final String uid;
    public CloudSyncEngine(Context context) {
        local=new SyncStore(context);cloud=FirebaseRuntime.store();
        FirebaseUser user=FirebaseRuntime.auth().getCurrentUser();uid=user==null?null:user.getUid();
    }
    private void checkAccount() throws InterruptedException {
        FirebaseUser user=FirebaseRuntime.auth().getCurrentUser();String now=user==null?null:user.getUid();
        if(Thread.currentThread().isInterrupted()||!Objects.equals(uid,now))throw new InterruptedException("Account changed");
    }
    private <T>T await(com.google.android.gms.tasks.Task<T> task)throws Exception {
        checkAccount();T value=Tasks.await(task,20,TimeUnit.SECONDS);checkAccount();return value;
    }

    public void sync()throws Exception {
        boolean admin=false;
        if(uid!=null) {
            DocumentSnapshot profile=await(cloud.collection("users").document(uid).get(Source.SERVER));
            if(!profile.exists())throw new IllegalStateException("Hãy đăng nhập lại để tạo hồ sơ Firebase");
            User user=CloudAuth.profile(uid,profile.getData());local.cacheUser(user,false);
            if(user.isLocked())throw new IllegalStateException("Tài khoản đã bị khóa");
            admin=user.isAdmin();
            if(admin)local.enqueueUnpublishedVideos(uid);
            for(SyncStore.Change change:local.pending(uid)) {
                checkAccount();
                if(("video".equals(change.entity)||"profile".equals(change.entity))&&!admin)
                    throw new IllegalStateException("Không còn quyền gửi thay đổi quản trị");
                Map<String,Object> payload=new HashMap<>(change.data);
                payload.put("_server_updated_at",FieldValue.serverTimestamp());
                DocumentReference reference;
                if("video".equals(change.entity)) {
                    payload.put("id",change.id);reference=cloud.collection("videos").document(change.id);
                } else if("profile".equals(change.entity)) {
                    Map<String,Object> permissions=new HashMap<>();permissions.put("role",payload.get("role"));permissions.put("is_locked",payload.get("is_locked"));
                    permissions.put("_server_updated_at",FieldValue.serverTimestamp());
                    await(cloud.collection("users").document(change.id).update(permissions));local.acknowledge(change);continue;
                } else {
                    payload.put("user_id",uid);
                    reference=cloud.collection("users").document(uid).collection(collection(change.entity)).document(change.id);
                }
                await(reference.set(payload));local.acknowledge(change);
            }
        }

        Query query=cloud.collection("videos");
        if(!admin)query=query.whereEqualTo("status","ACTIVE").whereEqualTo("_deleted",false);
        QuerySnapshot videos=await(query.get(Source.SERVER));Set<String> visible=new HashSet<>();
        for(DocumentSnapshot video:videos.getDocuments()) {
            checkAccount();visible.add(video.getId());local.importRecord(uid,"video",video.getId(),video.getData());
        }
        if(!admin)local.hideMissingVideos(visible);
        if(uid!=null) {
            for(String entity:new String[]{"history","favorite","setting"}) {
                QuerySnapshot documents=await(cloud.collection("users").document(uid).collection(collection(entity)).get(Source.SERVER));
                for(DocumentSnapshot doc:documents.getDocuments()){checkAccount();local.importRecord(uid,entity,doc.getId(),doc.getData());}
            }
            if(admin) {
                QuerySnapshot users=await(cloud.collection("users").get(Source.SERVER));
                for(DocumentSnapshot doc:users.getDocuments()) {
                    checkAccount();if(!local.hasPending(uid,"profile",doc.getId()))local.cacheUser(CloudAuth.profile(doc.getId(),doc.getData()),false);
                }
            }
        }
        local.state("last_success:"+(uid==null?"guest":uid),String.valueOf(System.currentTimeMillis()));
    }
    private static String collection(String entity) {
        switch(entity){case "history":return "history";case "favorite":return "favorites";case "setting":return "settings";default:throw new IllegalArgumentException(entity);}
    }
}
