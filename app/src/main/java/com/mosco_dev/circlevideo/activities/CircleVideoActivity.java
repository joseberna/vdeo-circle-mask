package com.mosco_dev.circlevideo.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.FileObserver;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.mosco_dev.circlevideo.R;
import com.mosco_dev.circlevideo.base.BaseVideo;
import com.mosco_dev.circlevideo.utils.GeneralUtils;
import com.mosco_dev.circlevideo.utils.Prefs;
import com.mosco_dev.circlevideo.view.CircleSurface;

import java.io.IOException;

public class CircleVideoActivity extends BaseVideo implements SurfaceHolder.Callback, MediaRecorder.OnInfoListener, View.OnClickListener {
    private final int MY_PERMISSIONS_REQUEST_CAMERA = 25;
    private CircleSurface mCircleSurface;
    private SurfaceHolder mSurfaceHolder;
    private TextView mLabRecording;
    private Camera mCamera;
    private MediaRecorder mMediaRecorder;
    private MediaPlayer mMediaPlayer;
    private Button mRecord;
    private Button mBtnSwitch;

    private int mCurrentId = Camera.CameraInfo.CAMERA_FACING_BACK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GeneralUtils.copyMaskFromAssetsToSDIfNeeded(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) && (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO))) {
            } else
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_CAMERA);
        } else {
            initViews();
        }
    }

    private void initViews() {
        setContentView(R.layout.activity_circle_video);
        mCircleSurface = (CircleSurface) findViewById(R.id.surface_camera);
        mLabRecording = (TextView) findViewById(R.id.lab_recording);


        mRecord = (Button) findViewById(R.id.btn_record);
        mRecord.setOnClickListener(this);
        mRecord.setText("Record");

        mBtnSwitch = (Button) findViewById(R.id.btn_switch);
        mBtnSwitch.setOnClickListener(this);

        if (mCircleSurface != null) {
            mSurfaceHolder = mCircleSurface.getHolder();
            mSurfaceHolder.addCallback(this);
            mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
    }

    private void initCamera() {
        try {
            mCamera = Camera.open(mCurrentId);

            if (mCamera != null) {
                Camera.Parameters params = mCamera.getParameters();
                mCamera.setParameters(params);
                mCamera.setDisplayOrientation(Prefs.ROTATION);
                try {
                    mCamera.setPreviewDisplay(mSurfaceHolder);
                    mCamera.startPreview();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                showToast("Camera not available!");
                finish();
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

    }

    public void switchCamera() {
        mCamera.stopPreview();
        mCamera.release();
        if (mCurrentId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            mCurrentId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            mCurrentId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        mCamera = Camera.open(mCurrentId);

        GeneralUtils.setCameraDisplayOrientation(this, mCurrentId, mCamera);
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
    }

    protected void startRecording() throws IOException {
        try {
            mMediaRecorder = new MediaRecorder();

            mCamera.startPreview();
            mCamera.unlock();

            mMediaRecorder.setCamera(mCamera);

            mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
            mMediaRecorder.setOrientationHint(Prefs.ROTATION);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

            try {
                CamcorderProfile camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
                if (camcorderProfile != null)
                    mMediaRecorder.setProfile(camcorderProfile);
            } catch (RuntimeException e) {
                e.printStackTrace();
            }

            String path = GeneralUtils.getVideoFolder() + Prefs.VIDEO_IN;

            mMediaRecorder.setOutputFile(path);
            mMediaRecorder.setMaxDuration(Prefs.DURATION);
            mMediaRecorder.setOnInfoListener(this);

            new FileObserver(path, FileObserver.CLOSE_WRITE) {
                @Override
                public void onEvent(int event, String path) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!isFinishing()) {
                                mLabRecording.setText("Editing...");
                                mLabRecording.setTextColor(Color.GREEN);
                                executeCmd(getVideoRecorderCmd(), Prefs.CODE_DECODING);
                            }

                        }
                    });
                }
            }.startWatching();

            mMediaRecorder.prepare();
            mMediaRecorder.start();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

    }

    private void stopCamera() {
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
                mCamera.release();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopPlayer() {
        if (mMediaPlayer != null) {
            try {
                if (mMediaPlayer.isPlaying())
                    mMediaPlayer.stop();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }

        }
    }

    private void stopRecording() {
        if (mMediaRecorder != null) {
            try {
                mMediaRecorder.stop();
                mMediaRecorder.release();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    private void initPlayBack() {
        String path = GeneralUtils.getVideoFolder() + Prefs.VIDEO_IN;
        try {
            mCamera.release();

            mLabRecording.setText("Playback");
            mLabRecording.setTextColor(Color.BLUE);

            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(path);
            mMediaPlayer.setDisplay(mSurfaceHolder);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.setLooping(true);
            mMediaPlayer.prepare();
            mMediaPlayer.start();

            mRecord.setText("Reset");

            if (mCircleSurface != null) {
                mCircleSurface.setColor(Color.BLACK);
            }

        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onFinishDecoding(int code) {
        if (!isFinishing()) {
            if (code == Prefs.CODE_DECODING)
                executeCmd(getVideoMaskCmd(), Prefs.CODE_MASK);
            else if (code == Prefs.CODE_MASK) {
                dismissDialog();
                initPlayBack();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        initCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopCamera();
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (!isFinishing()) {
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                mr.stop();
                mr.release();
                if (mCamera != null)
                    mCamera.stopPreview();
            }
        }

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_record) {
            if (mRecord.getText().toString().equals("Record")) {
                try {
                    startRecording();
                    mLabRecording.setText("Recording...");
                    mLabRecording.setTextColor(Color.RED);
                    mBtnSwitch.setEnabled(false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                mRecord.setText("Record");
                mBtnSwitch.setEnabled(true);
                mLabRecording.setText("Waiting for record");
                mLabRecording.setTextColor(Color.BLACK);
                stopPlayer();

                if (mCircleSurface != null) {
                    mCircleSurface.setColor(Color.WHITE);
                }

                if (mCircleSurface != null) {
                    initCamera();
                }
            }

        } else if (id == R.id.btn_switch) {
            switchCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPlayer();
    }

    @Override
    protected void onStop() {
        //stopCamera();
        //stopPlayer();
        //stopRecording();
        //stopProcess();
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initViews();
                }
            }
        }

    }
}
