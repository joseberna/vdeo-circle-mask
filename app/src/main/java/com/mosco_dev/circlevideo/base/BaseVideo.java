package com.mosco_dev.circlevideo.base;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.mosco_dev.circlevideo.R;
import com.mosco_dev.circlevideo.utils.GeneralUtils;
import com.mosco_dev.circlevideo.utils.Prefs;

import java.util.Locale;

public class BaseVideo extends AppCompatActivity {
    private ProgressDialog mProgressDecoding;
    private FFmpeg ffmpeg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        unlockScreen();
        ffmpeg = FFmpeg.getInstance(this);
        loadBinary();
        mProgressDecoding = new ProgressDialog(this);
        mProgressDecoding.setMessage(getResources().getString(R.string.lab_dialog_decoding));
        mProgressDecoding.setCancelable(false);
    }

    private void loadBinary() {
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    showUnsupportedExceptionDialog();
                }
            });
        } catch (FFmpegNotSupportedException e) {
            showUnsupportedExceptionDialog();
        }
    }

    private void showUnsupportedExceptionDialog() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getString(R.string.device_not_supported))
                .setMessage(getString(R.string.device_not_supported_message))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .create()
                .show();

    }

    private void unlockScreen() {
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        params.screenBrightness = 1;
        getWindow().setAttributes(params);
    }

    public String[] getVideoRecorderCmd() {
        String pathVideoIn = GeneralUtils.getVideoFolder() + Prefs.VIDEO_IN;
        String pathVideoTemp = GeneralUtils.getVideoFolder() + Prefs.VIDEO_TEMP;
        int widthVideo;
        try {
            widthVideo = GeneralUtils.getWithVideoResource(pathVideoIn);
        }catch (RuntimeException e){
            e.printStackTrace();
            widthVideo = 480;
        }

        int centerVideo = widthVideo / 2;

        String complex = String.format(Locale.getDefault(), "-y -i %s -strict -2 -filter:v crop=%d:%d:in_w/2-%d:in_h/2 %s", pathVideoIn, widthVideo, widthVideo, centerVideo, pathVideoTemp);
        return GeneralUtils.utilConvertToComplex(complex);
    }

    public String[] getVideoMaskCmd() {
        String pathVideoTemp = GeneralUtils.getVideoFolder() + Prefs.VIDEO_TEMP;
        String pathVideoOut = GeneralUtils.getVideoFolder() + Prefs.VIDEO_OUT;
        String pathVideoMask = GeneralUtils.getVideoFolder() + Prefs.VIDEO_MASK;

        return new String[]{"-y", "-i", pathVideoTemp, "-strict", "experimental", "-vf", String.format("movie=%s [watermark]; [in][watermark] overlay=0:0 [out]", pathVideoMask), pathVideoOut};
    }

    public void executeCmd(final String[] command, final int code) {
        try {
            ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    //showToast("FAILED with output : " + s);
                    Log.d(Prefs.TAG, s);
                }

                @Override
                public void onSuccess(String s) {
                    //showToast("SUCCESS with output : " + s);
                    Log.d(Prefs.TAG, s);
                }

                @Override
                public void onProgress(String s) {
                }

                @Override
                public void onStart() {
                    mProgressDecoding.show();
                }

                @Override
                public void onFinish() {
                    onFinishDecoding(code);
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
        }

    }

    public void onFinishDecoding(int code) {

    }

    public void dismissDialog() {
        mProgressDecoding.dismiss();
    }

    public void showToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }

    public void stopProcess() {
        if (ffmpeg != null)
            ffmpeg.killRunningProcesses();
    }
}
