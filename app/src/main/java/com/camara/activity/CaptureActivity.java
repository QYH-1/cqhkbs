package com.camara.activity;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.SharedPreferencesKt;

import com.camara.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


/**
 * @Author：qyh 版本：1.0
 * 创建日期：2019/11/28
 * 描述：
 * 修订历史：
 */
public class CaptureActivity extends Activity {
    public static final int MAX_WIDTH = 200;
    public static final int MAX_HEIGHT = 200;
    private Uri photoUri;
    private String fileName;
    private String SD = "/sdcard/DCIM/Camera/";
    private File photoFile;
    private OutputStream outStream;
    private int mWidth = -1;
    private int mHeight = -1;
    private SurfaceView surfaceView;
    /**
     * 拍照
     */
    private Button takePic;
    private ImageButton preViewPic, photo, video;
    private Camera camera; //这个是hardare的Camera对象
    public SharedPreferences sp; //用来保存数据

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);// 去掉标题栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);// 设置全屏
        setContentView(R.layout.activity_capture);
        sp = PreferenceManager.getDefaultSharedPreferences(this);//获取了SharePreferences对象

        initView();
        initListener();
    }

    /**
     * 旋转图片
     *
     * @param angle  被旋转角度
     * @param bitmap 图片对象
     * @return 旋转后的图片
     */
    public static Bitmap rotaingImageView(int angle, Bitmap bitmap) {
        Log.e("TAG", "angle===" + angle);
        Bitmap returnBm = null;
        // 根据旋转角度，生成旋转矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        try {
            // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
            returnBm = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
        }
        if (returnBm == null) {
            returnBm = bitmap;
        }
        if (bitmap != returnBm) {
            bitmap.recycle();
        }
        return returnBm;
    }


    /**
     * 初始化View
     */
    private void initView() {
        surfaceView = (SurfaceView) findViewById(R.id.myCameraView);
        takePic = (Button) findViewById(R.id.take_pic);
        preViewPic = (ImageButton) findViewById(R.id.pic_pre_view);
        photo = findViewById(R.id.btn_photo);
        video = findViewById(R.id.btn_video);

        //获取上次操作的图片的路径
        try {
            String file = sp.getString("filePhoto", "");
            Bitmap bm = BitmapFactory.decodeFile(file);
            BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bm);
            preViewPic.setBackgroundDrawable(bitmapDrawable);
        } catch (NullPointerException e) {
            e.fillInStackTrace();
        }
    }

    /**
     * 初始化监听器
     */
    private void initListener() {
        SurfaceHolder holder = surfaceView.getHolder();
        holder.setFixedSize(1000, 720);
        holder.setKeepScreenOn(true);//对焦
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); //绘制的类型
        holder.addCallback(new TakePictureSurfaceCallback());// 为SurfaceView的句柄添加一个回数调函

        takePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (camera != null) {
                    camera.takePicture(null, null, new TakePictureCallback());
                }
            }
        });
        preViewPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, 2);
            }
        });
        video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CaptureActivity.this, TestActivity.class);
                camera.setPreviewCallback(null);
                camera.release();
                camera = null;
                startActivity(intent);
                finish();
            }
        });
    }

    private final class TakePictureSurfaceCallback implements SurfaceHolder.Callback {

        @SuppressLint("LongLogTag")
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                // 获得Camera对象
                camera = Camera.open();
                // 设置用于显示拍照摄像的SurfaceHolder对象
                if (camera == null) {
                    SurfaceHolder mSurfaceHolder = surfaceView.getHolder();
                    camera.setPreviewDisplay(mSurfaceHolder);
                    camera.startPreview();
                    int cametacount = Camera.getNumberOfCameras();
                    camera = Camera.open(cametacount - 1);
                }

                Camera.Parameters params = camera.getParameters();
                params.setPreviewSize(640, 480);
                params.setJpegQuality(100);//照片质量
                params.setPictureSize(640, 480);//图片分辨率
                params.setPreviewFrameRate(5);//预览帧率

                camera.setDisplayOrientation(90);
                /**
                 * 设置预显示
                 */
                camera.setPreviewDisplay(holder);
                camera.setPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {

                    }
                });
                /**
                 * 开启预览
                 */
                camera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Camera.Parameters parameters = camera.getParameters();//获取camera的parameter实例
            List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();//获取所有支持的camera尺寸
            Camera.Size optionSize = getOptimalPreviewSize(sizeList, surfaceView.getWidth(), surfaceView.getHeight());//获取一个最为适配的屏幕尺寸
            parameters.setPreviewSize(640,480);//把只存设置给parameters
            parameters.setJpegQuality(100);//照片质量
            parameters.setPictureSize(640, 480);//图片分辨率
            parameters.setPreviewFrameRate(5);//预览帧率

           // camera.setParameters(parameters);//把parameters设置给camera上
            camera.setDisplayOrientation(90);
            camera.startPreview();//开始预览
            Log.i("111", "surfaceChanged: " + width + "  " + height);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (camera != null) {
                camera.setPreviewCallback(null);
                camera.release();
                camera = null;
            }
        }
    }

    private final class TakePictureCallback implements Camera.PictureCallback {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            Bitmap mBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);//此bitmap作为预览时候使用，用完一定注意回收
            Bitmap bBitmap = rotaingImageView(90, mBitmap);
            BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bBitmap);
            preViewPic.setVisibility(View.VISIBLE);
            preViewPic.setBackgroundDrawable(bitmapDrawable);

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = new Date(System.currentTimeMillis());
            fileName = format.format(date);
            saveBmp2Gallery(getBaseContext(), bBitmap, fileName);
            if (camera != null) {
                camera.startPreview();//重置相机
                camera = null;
            }
        }
    }

    /**
     * @param bmp     获取的bitmap数据
     * @param picName 自定义的图片名
     */
    public void saveBmp2Gallery(Context context, Bitmap bmp, String picName) {
        String fileName = null;
        //系统相册目录
        String galleryPath = Environment.getExternalStorageDirectory()
                + File.separator + Environment.DIRECTORY_DCIM
                + File.separator + "Camera" + File.separator;

        // 声明文件对象
        File file = null;
        // 声明输出流
        FileOutputStream outStream = null;
        try {
            // 如果有目标文件，直接获得文件对象，否则创建一个以filename为名称的文件
            file = new File(galleryPath, picName + ".jpg");
            // 获得文件相对路径
            fileName = file.toString();
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("filePhoto", fileName);
            editor.commit();
            // 获得输出流，如果文件中有内容，追加内容
            outStream = new FileOutputStream(fileName);
            if (null != outStream) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, outStream);
            }
        } catch (Exception e) {
            e.getStackTrace();
        } finally {
            try {
                if (outStream != null) {
                    outStream.close();
                    //setPhotoFile(file);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            MediaStore.Images.Media.insertImage(context.getContentResolver(), bmp, fileName, null);
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri uri = Uri.fromFile(file);
            intent.setData(uri);
            context.sendBroadcast(intent);
            Log.d("0111", "图片保存成功");
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("0112", "图片保存失败");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 2:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    setImage(uri);
                }
        }
    }

    private void setImage(Uri uri) {
        LayoutInflater inflater = LayoutInflater.from(getApplication());
        View view = inflater.inflate(R.layout.showphoto, null);
        ImageView picture = view.findViewById(R.id.photoShow);
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
            picture.setImageBitmap(bitmap);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        new AlertDialog.Builder(this)
                .setView(view)
                .create()
                .show();
    }

    /**
     * 解决预览变形问题
     *
     * @param sizes
     * @param w
     * @param h
     * @return
     */
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
}
