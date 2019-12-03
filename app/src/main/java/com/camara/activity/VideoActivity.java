package com.camara.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;

import com.camara.R;

import java.io.File;

/**
 * @Author：qyh 版本：1.0
 * 创建日期：2019/12/2
 * 描述：
 * 修订历史：
 */
public class VideoActivity extends Activity {

    private static final String TAG = "VideoActivity";
    private int Sign = 1; //代表当前出去未录像状态
    // 程序中的两个按钮
    private Button video;
    // 系统的视频文件
    File videoFile;
    MediaRecorder mRecorder;
    private ImageButton photo,video_view;
    public SharedPreferences sp; //用来保存数据
    // 显示视频预览的SurfaceView
    SurfaceView sView;
    // 记录是否正在进行录制
    private boolean isRecording = false;
    private String file;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //去掉标题栏（ActionBar实际上是设置在标题栏上的）
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //去掉状态栏(顶部显示时间、电量的部分)，设置全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.video);
        sp = PreferenceManager.getDefaultSharedPreferences(this);//获取了SharePreferences对象
        // 获取程序界面中的两个按钮
        photo = findViewById(R.id.btn_photo);
        video = findViewById(R.id.take_video);
        // 获取程序界面中的SurfaceView
        sView = (SurfaceView) findViewById(R.id.videoCameraView);
        video_view = findViewById(R.id.video_view);
        //获取上次操作的图片的路径
        try {
            MediaMetadataRetriever media = new MediaMetadataRetriever();
            String fileV = sp.getString("filePhoto","");
            media.setDataSource(fileV);
            Bitmap bitmap = media.getFrameAtTime();
            video_view.setImageBitmap(bitmap);
        }catch (NullPointerException e){
            e.fillInStackTrace();
        }
        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(VideoActivity.this, CaptureActivity.class);
                startActivity(intent);
                finish();
            }
        });
        video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //点击进入录像状态
                if (Sign == 1 && !isRecording) {
                    video.setBackgroundResource(R.drawable.videostop);
                    try {
                        // 创建保存录制视频的视频文件
                        videoFile = new File("/sdcard/DCIM/Camera/" + System.currentTimeMillis() + ".mp4");
                        file = "/sdcard/DCIM/Camera/" + System.currentTimeMillis() + ".mp4";
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("fileVideo",file);
                        editor.commit();
                        // 创建MediaPlayer对象
                        mRecorder = new MediaRecorder();
                        mRecorder.reset();
                        //设置从麦克风采集声音
                        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        // 设置从摄像头采集图像
                        mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                        // 设置视频文件的输出格式
                        // 必须在设置声音编码格式、图像编码格式之前设置
                        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                        // 设置声音编码的格式
                        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
                        // 设置图像编码的格式
                        mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
                        //mRecorder.setVideoSize(1920, 1080);  // 1080P
                        // 每秒16帧
                        //mRecorder.setVideoFrameRate(16);
                        mRecorder.setOutputFile(videoFile.getAbsolutePath());
                        // 指定使用SurfaceView来预览视频
                        mRecorder.setPreviewDisplay(sView.getHolder().getSurface());  // ①
                        mRecorder.prepare();
                        // 开始录制
                        mRecorder.start();
                        Log.d(TAG, "---recording---");
                        isRecording = true;
                        Sign = 2;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    // 如果正在进行录制
                    if (isRecording) {
                        video.setBackgroundResource(R.drawable.videoc);
                        // 停止录制
                        mRecorder.stop();
                        MediaMetadataRetriever media = new MediaMetadataRetriever();
                        media.setDataSource(file);
                        Bitmap bitmap = media.getFrameAtTime();
                        video_view.setImageBitmap(bitmap);
                        // 释放资源
                        mRecorder.release();
                        mRecorder = null;
                        isRecording = false;
                        Sign = 1;
                    }
                }
            }
        });
        // 设置分辨率
        // sView.getHolder().setFixedSize(1920, 1080);   // 1080P
        // 设置该组件让屏幕不会自动关闭
        sView.getHolder().setKeepScreenOn(true);
    }
}
