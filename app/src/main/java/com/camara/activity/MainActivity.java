package com.camara.activity;

/**
 * @Author：qyh 版本：1.0
 * 创建日期：2019/11/28
 * 描述：
 * 修订历史：
 */
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.camara.R;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    public static final int TAKE_PHOTO =102;  //  TAKE_PHOTO来作为case处理图片的标识
    private ImageView imgIcon;  //  显示拍照后的图片
    private Button btnPhoto,btnVideo;    //  拍照
    private Uri imageUri;   //  通用资源标志符

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camara);
       //btnPhoto = findViewById(R.id.btn_photo);
        //btnVideo = findViewById(R.id.btn_video);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Intent intent = new Intent(MainActivity.this,CaptureActivity.class);
                startActivity(intent);
                finish();
            }
        },0);
//        btnPhoto.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//            }
//        });
    }
}
