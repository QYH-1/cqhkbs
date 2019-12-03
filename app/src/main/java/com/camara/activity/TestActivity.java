package com.camara.activity;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.annotation.RequiresApi;

import com.camara.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

/**
 * @Author：qyh 版本：1.0
 * 创建日期：2019/12/3
 * 描述：
 * 修订历史：
 */
public class TestActivity extends Activity implements Camera.PreviewCallback {
    private final static String TAG_CAMERA_ACTIVITY = "MainActivity";
    private Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder cameraSurfaceHolder;
    private Button button;
    private Button video;
    private ImageButton photo, video_view;
    private MediaRecorder mediaRecorder;     //录制视频类
    protected boolean isPreview = false;     //摄像区域是否准备良好
    private int Sign = 1; //代表当前出去未录像状态
    private boolean isRecording = true;     // true表示没有录像，点击开始；false表示正在录像，点击暂停
    private File mRecVideoPath;
    private String videoPath;
    private File mRecAudioFile;
    private Button take_video;
    private static final int MAX_RECORD_TIME = 15 * 1000;
    private static final int PLUSH_PROGRESS = 100;
    private final int max = MAX_RECORD_TIME / PLUSH_PROGRESS;
    int cameraType = 1;
    private Chronometer ch; //记录时间
    private int cnt = 0;
    private float[] pulse_raw = new float[400];
    boolean saved = false; //标记是否已保存数据
    boolean flag = true; //标记是否点击了确认
    int fs;
    float fr;
    int duration;
    byte[] mPreBuffer = new byte[400];
    public SharedPreferences sp; //用来保存数据

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);// 去掉标题栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);// 设置全屏
        setContentView(R.layout.test);
        sp = PreferenceManager.getDefaultSharedPreferences(this);//获取了SharePreferences对象

        ch = findViewById(R.id.chronometer); //时间显示
        surfaceView = findViewById(R.id.VideoView);
        photo = findViewById(R.id.btn_photo);
        video = findViewById(R.id.take_video);
        video_view = findViewById(R.id.video_view);

//        //获取上次操作的图片的路径
        try {
            String file = sp.getString("fileVideo", "");
            synchronized (file) {
                //通过getVideoThumbnail方法取得视频中的第一帧图片，该图片是一个bitmap对象
                Bitmap bitmap = getVideoThumbnail(file);
                //将bitmap对象转换成drawable对象
                Drawable drawable = new BitmapDrawable(bitmap);
                //将drawable对象设置给视频播放窗口surfaceView控件作为背景图片
                video_view.setBackgroundDrawable(drawable);
            }
        } catch (NullPointerException e) {
            e.fillInStackTrace();
        }
        video_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, 66);
            }
        });
        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(TestActivity.this, CaptureActivity.class);
                //录像的关闭和资源释放
                //camera.setPreviewCallback(null);
                camera.release();
                camera = null;
                startActivity(intent);
                finish();
            }
        });
        cameraSurfaceHolder = surfaceView.getHolder();
        cameraSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                cameraSurfaceHolder = holder;
                initView();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                cameraSurfaceHolder = holder;
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                releaseCamera();
            }
        });

        video.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public void onClick(View v) {
                if (Sign == 1) {
                    Sign = 2;
                    //此处摄像头的准备工作为mediaRecorder的前置操作，开启录像
                    if (isRecording) {
                        if (isPreview) {
                            camera.stopPreview();
                            camera.release();
                            camera = null;
                        }
                        if (null == mediaRecorder) {
                            mediaRecorder = new MediaRecorder();
                        } else {
                            mediaRecorder.reset();
                        }
                        video.setBackgroundResource(R.drawable.videostop);
                        ch.setVisibility(View.VISIBLE);
                        ch.setBase(SystemClock.elapsedRealtime());//计时器打开
                        cnt = 0;
                        camera.setPreviewCallback(TestActivity.this);
                        camera.setDisplayOrientation(90);
                        // camera = Camera.open();
                        camera.lock();
                        Camera.Parameters params = camera.getParameters();
                        if (cameraType == 0)
                            params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        camera.setDisplayOrientation(90);
                        // camera.enableShutterSound(false);
                        params.setPreviewFrameRate(25);
                        camera.setParameters(params);
                        //由MediaRecorder对象使用的Camera对象解锁
                        camera.unlock();
                        mediaRecorder.setCamera(camera);
                        // mediaRecorder.setOrientationHint(270);
                        //预览布局
                        mediaRecorder.setPreviewDisplay(cameraSurfaceHolder.getSurface());
                        //设置音频源
                        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
                        mediaRecorder.setOrientationHint(90);
                        //设置视频源
                        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                        //视频输出格式
                        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                        //音频输出格式
                        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
//                        //设置声音的编码类型
//                        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                        //设置视频的编码类型
                        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                        mediaRecorder.setVideoSize(640, 480);
                        mediaRecorder.setVideoEncodingBitRate(8 * 1024 * 1024);
                        //视频文件保存
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Date date = new Date(System.currentTimeMillis());
                        String fileName = format.format(date);
                        videoPath = "/sdcard/DCIM/Camera/" + fileName + ".mp4";
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("fileVideo", videoPath);
                        editor.commit();
                        mRecAudioFile = new File(videoPath);
                        mediaRecorder.setOutputFile(mRecAudioFile.getAbsolutePath());
                        try {
                            mediaRecorder.prepare();
                            mediaRecorder.start();
                            ch.start();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        isRecording = !isRecording;
//                        //在app build 中compile一下，是一个第三方的库
//                        Observable.interval(100,
//                                TimeUnit.MILLISECONDS,
//                                AndroidSchedulers.mainThread()).take(max).subscribe(new Subscriber<Long>() {
//                            @Override
//                            public void onCompleted() {
//
//                                Camera.Parameters parameters = camera.getParameters();
//                                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
//                                camera.setParameters(parameters);
//                                //录像的关闭和资源释放
//                                mediaRecorder.stop();
//                                mediaRecorder.reset();
//                                mediaRecorder.release();
//                                mediaRecorder = null;
//                                isRecording = !isRecording;
//                            }
//
//                            @Override
//                            public void onError(Throwable e) {
//                            }
//
//                            @Override
//                            public void onNext(Long aLong) {
//                            }
//                        });
                    }
                } else {
                    video.setBackgroundResource(R.drawable.videoc);
                    isRecording = false;
                    ch.stop();
                    ch.setVisibility(View.GONE);

                    //录像的关闭和资源释放
                    mediaRecorder.stop();
                    mediaRecorder.reset();
                    mediaRecorder.release();
                    mediaRecorder = null;
                    isRecording = !isRecording;
                    Sign = 1;

                    synchronized (videoPath) {
                        //通过getVideoThumbnail方法取得视频中的第一帧图片，该图片是一个bitmap对象
                        Bitmap bitmap = getVideoThumbnail(videoPath);
                        //将bitmap对象转换成drawable对象
                        Drawable drawable = new BitmapDrawable(bitmap);
                        //将drawable对象设置给视频播放窗口surfaceView控件作为背景图片
                        video_view.setBackgroundDrawable(drawable);
                    }
                }
            }
        });

    }

    //用于surfaceCreated执行，获取相机
    private void initView() {
        try {
            // 获得Camera对象
            camera = Camera.open();
            Camera.Parameters parameters = camera.getParameters();
            parameters.setPreviewFrameRate(30);
            camera.setParameters(parameters);
            camera.setDisplayOrientation(90);
            camera.addCallbackBuffer(mPreBuffer);
            camera.setPreviewDisplay(cameraSurfaceHolder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 释放摄像头资源
     */
    private void releaseCamera() {
        try {
            if (camera != null) {
                camera.setPreviewCallback(null);
                camera.stopPreview();
                camera.lock();
                camera.release();
                camera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //对视频帧的处理
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        System.out.println("调用帧。。。");

        //返回一帧图像的G通道信号
        float[] single_channel_data = getSingleChannelData(data, 2);
        //点击事件中将flag置为false，挡位true的时候什么都不做
        if (flag) {
            //DO NOTHING
            System.out.println("DO NOTHING。。。 ");
        } else if (cnt < 400) {
            //取400帧
            //System.out.println("取帧。。。 " );
            Camera.Parameters params = camera.getParameters();
            //获取帧率，算了400次，取的是最后一次的值
            fs = params.getPreviewFrameRate();
            //将一帧图像像素平均成一个值，形成一个长度为400的一维数组
            pulse_raw[cnt] = calculateAvg(single_channel_data);
            cnt++;
        } else if (saved == false) {
            //cnt 达到400后跳出上一个进入这里计算
            ch.stop();
            System.out.println("计算。。。 ");
            String content = ch.getText().toString();
            String[] split = content.split(":");
            duration = Integer.parseInt(split[1]);  //计时器获取秒数
            System.out.println("duration = " + duration);
            fr = (float) (400.0 / duration);
            System.out.println("fr = " + fr);
            Camera.Parameters params = camera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            surfaceView.setClickable(true);
        }


    }

    //转成Bitmap获取G通道数据
    float[] getSingleChannelData(byte[] data, int channel) {

        //转NV21格式的byte数组为Bitmap图
        YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, 960, 720, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, 960, 720), 80, baos);
        byte[] jdata = baos.toByteArray();
        Bitmap mBitmap = BitmapFactory.decodeByteArray(jdata, 0, jdata.length);
        //获取长宽
        int width = mBitmap.getWidth();
        int height = mBitmap.getHeight();
        //像素点个数，输出数组长度
        int[] pixels = new int[width * height];
        float[] output = new float[width * height];
        //获取像素值
        mBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            int clr = pixels[i];
            //与操作 取相应位
            int red = (clr & 0x00ff0000) >> 16;
            int green = (clr & 0x0000ff00) >> 8;
            int blue = clr & 0x000000ff;
            if (channel == 1) { //red
                output[i] = red; // 取高两位
            }
            if (channel == 2) { //green
                output[i] = green; // 取中两位
            }
            if (channel == 3) { //blue
                output[i] = blue; // 取低两位
            }
            if (channel == 4) {    //亮度Y
                output[i] = (float) (0.229 * red + 0.587 * green + 0.114 * blue);
            }
            if (channel == 5) {    //色度U
                output[i] = (float) (-0.169 * red - 0.331 * green + 0.5 * blue);
            }
            if (channel == 6) {    //饱和度V
                output[i] = (float) (0.5 * red - 0.419 * green - 0.081 * blue);
            }
        }
        return output;
    }

    float calculateAvg(float[] array) {
        float sum = 0;
        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        float avg = sum / array.length;
        return avg;
    }

    /**
     * 通过url路径获得视频的第一帧图片
     *
     * @param url
     * @return
     */
    public Bitmap getVideoThumbnail(String url) {
        Bitmap bitmap = null;
        //MediaMetadataRetriever 是android中定义好的一个类，提供了统一
        //的接口，用于从输入的媒体文件中取得帧和元数据；
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            //（）根据文件路径获取缩略图
            //retriever.setDataSource(filePath);
            retriever.setDataSource(url, new HashMap());
            //获得第一帧图片
            bitmap = retriever.getFrameAtTime();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        Log.v("bitmap", "bitmap=" + bitmap);
        return bitmap;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 66:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    setImage(uri);
                }
        }
    }

    private void setImage(Uri uri) {
        LayoutInflater inflater = LayoutInflater.from(getApplication());
        View view = inflater.inflate(R.layout.showvideo, null);
        VideoView videoView = view.findViewById(R.id.videoShow);

//        //设置视频控制器
        videoView.setMediaController(new MediaController(this));
        //设置视频路径
        videoView.setVideoURI(uri);
        //开始播放视频
        videoView.start();
//        Intent intent = new Intent(Intent.ACTION_VIEW);
//        intent.setDataAndType(uri, "video/mp4");
//        startActivity(intent);
        new AlertDialog.Builder(this)
                .setView(view)
                .create()
                .show();
    }
}
