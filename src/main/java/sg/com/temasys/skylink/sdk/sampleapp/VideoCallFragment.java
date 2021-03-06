package sg.com.temasys.skylink.sdk.sampleapp;

/**
 * Created by lavanyasudharsanam on 20/1/15.
 */

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.media.AudioManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.temasys.skylink.sampleapp.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import sg.com.temasys.skylink.sdk.config.SkylinkConfig;
import sg.com.temasys.skylink.sdk.listener.LifeCycleListener;
import sg.com.temasys.skylink.sdk.listener.MediaListener;
import sg.com.temasys.skylink.sdk.listener.RemotePeerListener;
import sg.com.temasys.skylink.sdk.rtc.SkylinkConnection;

/**
 * This class is used to demonstrate the VideoCall between two clients in WebRTC
 */
public class VideoCallFragment extends Fragment implements LifeCycleListener, MediaListener, RemotePeerListener {
    private static final String TAG = VideoCallFragment.class.getCanonicalName();
    public static final String MY_USER_NAME = "bob";
    private static final String ARG_SECTION_NUMBER = "section_number";
    //set height width for self-video when in call
    public static final int WIDTH = 350;
    public static final int HEIGHT = 350;
    private LinearLayout parentFragment;
    private Button toggleAudioButton;
    private Button toggleVideoButton;
    private Button btnEnterRoom;
    private EditText etRoomName;
    private SkylinkConnection skylinkConnection;
    private String peerId;
    private ViewGroup.LayoutParams selfLayoutParams;
    private boolean audioMuted;
    private boolean videoMuted;
    private boolean connected;
    private AudioRouter audioRouter;
    public static final JSONObject UserObject = new JSONObject();
    Vibrator c;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Initialize views
        View rootView = inflater.inflate(R.layout.fragment_video_call, container, false);
        parentFragment = (LinearLayout) rootView.findViewById(R.id.ll_video_call);
        btnEnterRoom = (Button) rootView.findViewById(R.id.btn_enter_room);
        etRoomName = (EditText) rootView.findViewById(R.id.et_room_name);
        etRoomName.setVisibility(View.GONE);
        toggleAudioButton = (Button) rootView.findViewById(R.id.toggle_audio);
        toggleVideoButton = (Button) rootView.findViewById(R.id.toggle_video);



        try {
            UserObject.put("id","bob");
            UserObject.put("name","Bob");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(getArguments().getString("RoomName")!=null){
            c = (Vibrator)getActivity().getSystemService(Context.VIBRATOR_SERVICE);
            c.vibrate(new long[] { 1000, 1000, 1000, 1000, 1000 }, 0);
        }
        isMyServiceRunning();
        btnEnterRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isMyServiceRunning()){
                    getActivity().stopService(new Intent(getActivity(),MyServiceVideo.class));

                }
                c.cancel();
                String roomName = getArguments().getString("RoomName");
                Log.d("Service",roomName);
                if (roomName.isEmpty()) {
                    Toast.makeText(getActivity(), "Please enter valid room name", Toast.LENGTH_SHORT).show();
                    return;
                }


                btnEnterRoom.setVisibility(View.GONE);

                String apiKey = getString(R.string.app_key);
                String apiSecret = getString(R.string.app_secret);

                // Initialize the skylink connection
                initializeSkylinkConnection();

                // Initialize the audio router
                initializeAudioRouter();

                // Obtaining the Skylink connection string done locally
                // In a production environment the connection string should be given
                // by an entity external to the App, such as an App server that holds the Skylink API secret
                // In order to avoid keeping the API secret within the application
                String skylinkConnectionString = Utils.
                        getSkylinkConnectionString(roomName, apiKey,
                                apiSecret, new Date(), SkylinkConnection.DEFAULT_DURATION);

                Boolean a = skylinkConnection.connectToRoom(skylinkConnectionString,
                        UserObject);
                Log.d("Service",a.toString());
                // Use the Audio router to switch between headphone and headset
                audioRouter.startAudioRouting(getActivity().getApplicationContext());
                connected = true;
            }
        });

        toggleAudioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // If audio is enabled, mute audio and if audio is enabled, mute it
                audioMuted = !audioMuted;

                if (audioMuted) {
                    Toast.makeText(getActivity(), getString(R.string.muted_audio),
                            Toast.LENGTH_SHORT).show();
                    toggleAudioButton.setText(getString(R.string.enable_audio));
                } else {
                    Toast.makeText(getActivity(), getString(R.string.enabled_audio),
                            Toast.LENGTH_SHORT).show();
                    toggleAudioButton.setText(getString(R.string.mute_audio));
                }

                skylinkConnection.muteLocalAudio(audioMuted);
            }
        });

        toggleVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // If video is enabled, mute video and if video is enabled, mute it
                videoMuted = !videoMuted;

                if (videoMuted) {
                    Toast.makeText(getActivity(), getString(R.string.muted_video),
                            Toast.LENGTH_SHORT).show();
                    toggleVideoButton.setText(getString(R.string.enable_video));
                } else {
                    Toast.makeText(getActivity(), getString(R.string.enabled_video),
                            Toast.LENGTH_SHORT).show();
                    toggleVideoButton.setText(getString(R.string.mute_video));
                }

                skylinkConnection.muteLocalVideo(videoMuted);
            }
        });

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Allow volume to be controlled using volume keys
        getActivity().setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    private void initializeAudioRouter() {
        if (audioRouter == null) {
            audioRouter = AudioRouter.getInstance();
            audioRouter.init(((AudioManager) getActivity().
                    getSystemService(android.content.Context.AUDIO_SERVICE)));
        }
    }

    private void initializeSkylinkConnection() {
        if (skylinkConnection == null) {
            skylinkConnection = SkylinkConnection.getInstance();
            //the app_key and app_secret is obtained from the temasys developer console.
            skylinkConnection.init(getString(R.string.app_key),
                    getSkylinkConfig(), this.getActivity().getApplicationContext());
            //set listeners to receive callbacks when events are triggered
            skylinkConnection.setLifeCycleListener(this);
            skylinkConnection.setMediaListener(this);
            skylinkConnection.setRemotePeerListener(this);
        }
    }

    private SkylinkConfig getSkylinkConfig() {
        SkylinkConfig config = new SkylinkConfig();
        //AudioVideo config options can be NO_AUDIO_NO_VIDEO, AUDIO_ONLY, VIDEO_ONLY, AUDIO_AND_VIDEO;
        config.setAudioVideoSendConfig(SkylinkConfig.AudioVideoConfig.AUDIO_AND_VIDEO);
        config.setHasPeerMessaging(true);
        config.setHasFileTransfer(true);
        config.setTimeout(Constants.TIME_OUT);
        return config;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }

    @Override
    public void onDetach() {
        //close the connection when the fragment is detached, so the streams are not open.
        super.onDetach();
        if (skylinkConnection != null && connected) {
            skylinkConnection.disconnectFromRoom();
            skylinkConnection.setLifeCycleListener(null);
            skylinkConnection.setMediaListener(null);
            skylinkConnection.setRemotePeerListener(null);
            connected = false;
            getActivity().startService(new Intent(getActivity(),MyServiceVideo.class));
            audioRouter.stopAudioRouting(getActivity().getApplicationContext());
        }
    }

    /***
     * Lifecycle Listener Callbacks -- triggered during events that happen during the SDK's lifecycle
     */

    /**
     * Triggered when connection is successful
     *
     * @param isSuccess
     * @param message
     */

    @Override
    public void onConnect(boolean isSuccess, String message) {
        if (isSuccess) {
            etRoomName.setEnabled(false);
            toggleAudioButton.setVisibility(View.VISIBLE);
            toggleVideoButton.setVisibility(View.VISIBLE);
            Toast.makeText(getActivity(), "Connected to room + " + etRoomName.getText().toString() + " as " + MY_USER_NAME, Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "Skylink Failed " + message);
            Toast.makeText(getActivity(), "Skylink Connection Failed\nReason : "
                    + message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLockRoomStatusChange(String remotePeerId, boolean lockStatus) {
        Toast.makeText(getActivity(), "Peer " + remotePeerId +
                " has changed Room locked status to " + lockStatus, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onWarning(int errorCode, String message) {
        Log.d(TAG, message + "warning");

        Toast.makeText(getActivity(), "Warning is errorCode" + errorCode, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisconnect(int errorCode, String message) {
        Log.d(TAG, message + " disconnected");
        Toast.makeText(getActivity(), "onDisconnect " + message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onReceiveLog(String message) {
        Log.d(TAG, message + " on receive log");
    }

    /**
     * Media Listeners Callbacks - triggered when receiving changes to Media Stream from the remote peer
     */

    /**
     * Triggered after the user's local media is captured.
     *
     * @param videoView
     * @param size
     */
    @Override
    public void onLocalMediaCapture(GLSurfaceView videoView, Point size) {
        if (videoView != null) {
            //show media on screen
            videoView.setTag("self");
            parentFragment.removeView(videoView);
            parentFragment.addView(videoView);
        }
    }

    @Override
    public void onVideoSizeChange(GLSurfaceView videoView, Point size) {
        Log.d(TAG, videoView + " got size");
    }

    @Override
    public void onRemotePeerAudioToggle(String remotePeerId, boolean isMuted) {
        String message = null;
        if (isMuted) {
            message = "Your peer muted their audio";
        } else {
            message = "Your peer unmuted their audio";
        }

        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRemotePeerVideoToggle(String peerId, boolean isMuted) {
        String message = null;
        if (isMuted)
            message = "Your peer muted video";
        else
            message = "Your peer unmuted their video";

        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Remote Peer Listener Callbacks - triggered during events that happen when data or connection
     * with remote peer changes
     */

    @Override
    public void onRemotePeerJoin(String remotePeerId, Object userData, boolean hasDataChannel) {
        Toast.makeText(getActivity(), "Your peer has just connected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRemotePeerMediaReceive(String remotePeerId, GLSurfaceView videoView, Point size) {
        if (videoView == null) {
            return;
        }

        if (!TextUtils.isEmpty(this.peerId) && !remotePeerId.equals(this.peerId)) {
            Toast.makeText(getActivity(), " You are already in connection with two peers",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Resize self view
        View self = parentFragment.findViewWithTag("self");
        if (this.selfLayoutParams == null) {
            // Get the original size of the layout
            this.selfLayoutParams = self.getLayoutParams();
        }

        self.setLayoutParams(new ViewGroup.LayoutParams(WIDTH, HEIGHT));
        parentFragment.removeView(self);
        parentFragment.addView(self);

        // Remove peer video if it exist
        View viewToRemove = parentFragment.findViewWithTag("peer");
        if (viewToRemove != null) {
            parentFragment.removeView(viewToRemove);
        }

        videoView.setTag("peer");
        parentFragment.addView(videoView);

        this.peerId = remotePeerId;
    }

    @Override
    public void onRemotePeerLeave(String remotePeerId, String message) {
        Toast.makeText(getActivity(), "Your peer has left the room", Toast.LENGTH_SHORT).show();
        if (remotePeerId != null && remotePeerId.equals(this.peerId)) {
            this.peerId = null;
            View peerView = parentFragment.findViewWithTag("peer");
            parentFragment.removeView(peerView);

            // Resize self view to original size
            if (this.selfLayoutParams != null) {
                View self = parentFragment.findViewWithTag("self");
                self.setLayoutParams(selfLayoutParams);
            }
        }
    }

    @Override
    public void onRemotePeerUserDataReceive(String remotePeerId, Object userData) {
        Log.d(TAG, "onRemotePeerUserDataReceive " + remotePeerId);
        Toast.makeText(getActivity(), "onRemotePeer", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onOpenDataConnection(String peerId) {
        Log.d(TAG, "onOpenDataConnection");
        Toast.makeText(getActivity(), "onOpenDataConnection", Toast.LENGTH_SHORT).show();
    }




        public boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            Log.d("Services",service.service.getClassName());
            if ("sg.com.temasys.skylink.sdk.sampleapp.MyServiceVideo".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
