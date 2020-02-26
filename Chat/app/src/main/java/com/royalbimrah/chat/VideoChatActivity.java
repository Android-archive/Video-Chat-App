package com.royalbimrah.chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class VideoChatActivity extends AppCompatActivity implements Session.SessionListener, PublisherKit.PublisherListener {

    private static String API_KEY ="46521322";
    private static String SESSION_ID ="2_MX40NjUyMTMyMn5-MTU4MjY0MTg3NjE5N35JcmFlZE82YnlDeU12RFljR0ExV2hldGx-fg";
    private static String TOKEN ="T1==cGFydG5lcl9pZD00NjUyMTMyMiZzaWc9YmE4NGE0NmVhOGM0MzcwNjg4NTgzZTAzYWVjMjRiNzI0ZTA0OTI5NzpzZXNzaW9uX2lkPTJfTVg0ME5qVXlNVE15TW41LU1UVTRNalkwTVRnM05qRTVOMzVKY21GbFpFODJZbmxEZVUxMlJGbGpSMEV4VjJobGRHeC1mZyZjcmVhdGVfdGltZT0xNTgyNjQxOTYyJm5vbmNlPTAuMjA5MTcwNjkxOTcyMjE1MTYmcm9sZT1wdWJsaXNoZXImZXhwaXJlX3RpbWU9MTU4NTIzMDM2MCZpbml0aWFsX2xheW91dF9jbGFzc19saXN0PQ==";
    private static final String LOG_TAG = VideoChatActivity.class.getSimpleName();
    private static final int RC_VIDEO_APP_PERM = 124;

    private FrameLayout mPublisherViewController;
    private FrameLayout mSubscriberViewController;
    private Session mSession;
    private Publisher mPublisher;
    private Subscriber msubscriber;


    private ImageView closeVideoChatBtn;
    private DatabaseReference userRef;
    private String userID="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chat);

        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference().child("Users");

        closeVideoChatBtn = findViewById(R.id.close_video_chat_btn);
        closeVideoChatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                userRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if (dataSnapshot.child(userID).hasChild("Ringing")){
                            userRef.child(userID).child("Ringing").removeValue();

                            if (mPublisher != null){
                                mPublisher.destroy();
                            }
                            if (msubscriber != null){
                                msubscriber.destroy();
                            }
                            startActivity(new Intent(VideoChatActivity.this, RegisterActivity.class));
                            finish();
                        }
                        if (dataSnapshot.child(userID).hasChild("Calling")){
                            userRef.child(userID).child("Calling").removeValue();

                            if (mPublisher != null){
                                mPublisher.destroy();
                            }
                            if (msubscriber != null){
                                msubscriber.destroy();
                            }

                            startActivity(new Intent(VideoChatActivity.this, RegisterActivity.class));
                            finish();
                        }
                        else {
                            if (mPublisher != null){
                                mPublisher.destroy();
                            }
                            if (msubscriber != null){
                                msubscriber.destroy();
                            }
                        startActivity(new Intent(VideoChatActivity.this, RegisterActivity.class));
                        finish();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        });

        requestPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, VideoChatActivity.this);
    }
    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions(){
        String [] perms = {Manifest.permission.INTERNET, Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};

        if (EasyPermissions.hasPermissions(this, perms)){

            mPublisherViewController = findViewById(R.id.publisher_container);
            mSubscriberViewController = findViewById(R.id.subscriber_container);


            //initialize and connect the session
            mSession = new Session.Builder(this, API_KEY, SESSION_ID).build();
            mSession.setSessionListener(VideoChatActivity.this);
            mSession.connect(TOKEN);
        }
        else {
            EasyPermissions.requestPermissions(this, "Hey! this app needs Mic and Camera, Please allow.", RC_VIDEO_APP_PERM, perms);

        }
    }

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {

    }

    @Override
    public void onConnected(Session session) {

        Log.i(LOG_TAG, "Session Connected");
        mPublisher =  new Publisher.Builder(this).build();
        mPublisher.setPublisherListener(VideoChatActivity.this);
        mPublisherViewController.addView(mPublisher.getView());

        if (mPublisher.getView() instanceof GLSurfaceView){
            ((GLSurfaceView) mPublisher.getView()).setZOrderOnTop(true);
        }
        mSession.publish(mPublisher);
    }

    @Override
    public void onDisconnected(Session session) {
        Log.i(LOG_TAG, "Stream Disconnected");
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {

        Log.i(LOG_TAG,"Stream Received");
        if (msubscriber == null){
            msubscriber = new Subscriber.Builder(this, stream).build();
            mSession.subscribe(msubscriber);
            mSubscriberViewController.addView(msubscriber.getView());
        }
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.i(LOG_TAG, "Stream Dropped");

        if (msubscriber != null){
            msubscriber = null;
            mSubscriberViewController.removeAllViews();
        }
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        Log.i(LOG_TAG, "Stream Error");
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
