package com.nhom02.streamwire.cloud;

import android.content.Context;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Source;
import com.nhom02.streamwire.model.User;
import java.util.*;

public final class CloudAuth {
    private CloudAuth() {}
    public static final class Failure extends Exception {
        public final String stage;
        public final boolean accountCreated;
        Failure(String stage, boolean accountCreated, Exception cause) {
            super("Firebase step failed: " + stage, cause);
            this.stage = stage;
            this.accountCreated = accountCreated;
        }
    }
    public static Task<User> authenticate(Context context,String email,String password,String fullName) {
        TaskCompletionSource<User> result=new TaskCompletionSource<>();
        Task<com.google.firebase.auth.AuthResult> login=fullName==null
                ?FirebaseRuntime.auth().signInWithEmailAndPassword(email,password)
                :FirebaseRuntime.auth().createUserWithEmailAndPassword(email,password);
        login.addOnSuccessListener(auth -> {
            FirebaseUser user=auth.getUser();
            FirebaseRuntime.store().collection("users").document(user.getUid()).get(Source.SERVER)
                    .addOnSuccessListener(doc -> {
                        if(doc.exists())finish(context,user,doc.getData(),result,fullName!=null);
                        else {
                            Map<String,Object> data=new HashMap<>();data.put("id",user.getUid());data.put("email",user.getEmail());
                            data.put("username",user.getEmail());data.put("full_name",fullName==null?user.getEmail():fullName);
                            data.put("role","USER");data.put("is_locked",0);data.put("avatar_url","");
                            FirebaseRuntime.store().collection("users").document(user.getUid()).set(data)
                                    .addOnSuccessListener(ignored -> finish(context,user,data,result,fullName!=null))
                                    .addOnFailureListener(error -> result.setException(new Failure("SAVE_PROFILE",fullName!=null,error)));
                        }
                    }).addOnFailureListener(error -> result.setException(new Failure("READ_PROFILE",fullName!=null,error)));
        }).addOnFailureListener(error -> result.setException(new Failure("AUTH",false,error)));
        return result.getTask();
    }

    private static void finish(Context context,FirebaseUser firebase,Map<String,Object> data,TaskCompletionSource<User> result,boolean accountCreated) {
        FirebaseUser current=FirebaseRuntime.auth().getCurrentUser();
        if(current==null||!current.getUid().equals(firebase.getUid())){result.setException(new IllegalStateException("Phiên đăng nhập đã thay đổi"));return;}
        User user=profile(firebase.getUid(),data);user.setEmail(firebase.getEmail());
        if(user.isLocked()){FirebaseRuntime.auth().signOut();result.setException(new IllegalStateException("Tài khoản đã bị khóa"));return;}
        try {new SyncStore(context).cacheUser(user,true);result.setResult(user);}
        catch(RuntimeException e){result.setException(new Failure("CACHE",accountCreated,e));}
    }

    public static User profile(String uid,Map<String,Object> data) {
        User user=new User();user.setId(uid);user.setEmail(text(data,"email"));user.setUsername(text(data,"username"));
        user.setFullName(text(data,"full_name"));user.setRole(text(data,"role"));user.setAvatarUrl(text(data,"avatar_url"));
        Object locked=data.get("is_locked");user.setLocked(Boolean.TRUE.equals(locked)||locked instanceof Number&&((Number)locked).intValue()!=0);
        return user;
    }
    private static String text(Map<String,Object> data,String key){Object value=data.get(key);return value==null?"":String.valueOf(value);}
}
