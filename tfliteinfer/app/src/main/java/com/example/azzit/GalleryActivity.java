package com.example.azzit;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.azzit.stickermaker.StickerMaker;
import com.example.azzit.tflite.ClassifierWithModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = "[IC]GalleryActivity";
    public static final int GALLERY_IMAGE_REQUEST_CODE = 1;

    private ClassifierWithModel cls;
    private StickerMaker skm;
    private ImageView imageView;
    private TextView textView;
    private ImageView backimageView;
    public String[] ids;
    public List<Integer> Rids = new ArrayList<>();
    public Bitmap glasses;
    public float[] output;
    public Canvas tempCanvas;
    public Paint paint;
    public Bitmap bitmap_canvas;
    public Canvas stikerCanvas;
    public Bitmap targetBmp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        getImageFromGallery();
        Button selectBtn = findViewById(R.id.selectBtn);
        selectBtn.setOnClickListener(v -> getImageFromGallery());
        findViewById(R.id.backbtn).setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(i);
        });
        findViewById(R.id.railensunglass).setOnClickListener(this);
        findViewById(R.id.bitsunglass).setOnClickListener(this);
        findViewById(R.id.bdaysunglass).setOnClickListener(this);
        findViewById(R.id.aliensunglass).setOnClickListener(this);
        findViewById(R.id.leonsunglass).setOnClickListener(this);
        imageView = findViewById(R.id.imageView);
        ids = getResources().getStringArray(R.array.sticker_id);
        Button savebtn = findViewById(R.id.savebtn);
        savebtn.setOnClickListener(v -> skm.saveBitmaptoJpeg(bitmap_canvas, this));
        findViewById(R.id.sharebtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bitmap_canvas != null) {
                    String bitmapPath = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap_canvas, "title", null); //이미지를 insert하고
                    Uri bitmapUri = Uri.parse(bitmapPath);//경로를 통해서 Uri를 만들어서
                    Intent intent = new Intent(Intent.ACTION_SEND); //전송 인텐트를 만들고
                    intent.setType("image/*");//image형태로
                    intent.putExtra(Intent.EXTRA_STREAM, bitmapUri);
                    startActivity(Intent.createChooser(intent, "스티커 사진 보내기"));
                }
            }
        });
        cls = new ClassifierWithModel(this);
        try {
            cls.init();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }

    private void getImageFromGallery(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
//        Intent intent = new Intent(Intent.ACTION_PICK,
//                MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(intent, GALLERY_IMAGE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK &&
                requestCode == GALLERY_IMAGE_REQUEST_CODE) {
            if (data == null) {
                return;
            }

            Uri selectedImage = data.getData();
            Bitmap bitmap = null;

            try {
                if(Build.VERSION.SDK_INT >= 29) {
                    ImageDecoder.Source src =
                            ImageDecoder.createSource(getContentResolver(), selectedImage);
                    bitmap = ImageDecoder.decodeBitmap(src);
                } else {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                }
            } catch (IOException ioe) {
                Log.e(TAG, "Failed to read Image", ioe);
            }

            if(bitmap != null) {

                long start, end; // 서버에서 가져오는 시간 측정
                start = System.currentTimeMillis();
                output = cls.classify(bitmap); //모델 추론코드
                end = System.currentTimeMillis();
                System.out.println("get JSONData Time :" + (end - start)/1000.0);


//                backimageView.setImageBitmap(bitmap);
//                내가 수정한 부분
//                Bitmap bitmap_canvas = Bitmap.createBitmap(imageView.getWidth(), imageView.getHeight(), Bitmap.Config.ARGB_8888);
//                Canvas tempCanvas = new Canvas(bitmap_canvas); //그림 넣을 캔버스 만들기

//                int imageSize = 256;  // imageSize to rescale landmark
                int imageSize = 224;  // imageSize to rescale landmark

                //입력 이미지의 사이즈가 크기 때문에 이미지 뷰 영역에 맞춰줌
                float newWidth = bitmap.getWidth();
                float newHeight = bitmap.getHeight();
                if (bitmap.getWidth() >= imageView.getWidth()) {
                    newWidth = imageView.getWidth();
                    float tempWidth = bitmap.getWidth();
                    float tempHeight = bitmap.getHeight();
                    newHeight = ((float)(newWidth / tempWidth))*tempHeight;
                    Log.d(TAG, "onActivityResult: "+ newHeight);
                    if (newHeight >= imageView.getHeight()) {
                        tempHeight = newHeight;
                        newHeight = imageView.getHeight();
                        newWidth = (newHeight/tempHeight)*newWidth;
                    }
                }
                bitmap_canvas = Bitmap.createBitmap((int) newWidth,(int) newHeight, Bitmap.Config.ARGB_8888);
                tempCanvas = new Canvas(bitmap_canvas); //그림 넣을 캔버스 만들기
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap,(int) newWidth, (int) newHeight, false); //이미지 리사이징 실행코드
                targetBmp = resizedBitmap.copy(Bitmap.Config.ARGB_8888, false); //위 비트맵 이미지를 그냥 넣으면 오류떠서 오류 해결코드
                tempCanvas.drawBitmap(targetBmp, 0, 0, null); //캔버스에 입력 이미지를 넣음
                paint = new Paint(Paint.ANTI_ALIAS_FLAG); //그림을 그릴 페인트 생성
//                paint.setColor(Color.CYAN); //점의 색 설정
                for (int index = 0; index <= 15;) { //점 찍는 반복문
//                    tempCanvas.drawCircle(output[index]*newWidth/imageSize, output[index+1]*newHeight/imageSize, 8, paint);
                    output[index] = output[index]*newWidth/imageSize;
                    output[index+1] = output[index+1]*newHeight/imageSize;
                    index = index + 2;
                }
//                for (int i = 0; i < ids.length; i++) {
//                    Rids.add(Integer.parseInt("R.id."+ids[i]));
//                    ImageButton btn = (ImageButton) findViewById(Rids.get(i));
//                    btn.setOnClickListener(this);
//
                skm = new StickerMaker();
                glasses = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.bitsunglass);
                skm.make_sticker(tempCanvas, glasses, output, paint, 0.6);
                imageView.setImageDrawable(new BitmapDrawable(getResources(), bitmap_canvas)); //입력이미지와 점을 이미지 뷰에 그려줌
//                textView.setText(Arrays.toString(output)); //모델 추론 결과값 확인을 위한 텍스트 출력
            }
        }
        else if (resultCode == RESULT_CANCELED){
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            GalleryActivity.this.finish();
        }
    }

    @Override
    protected void onDestroy() {
        cls.finish();
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.railensunglass:
                glasses = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.raliensunglass);
                tempCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                tempCanvas.drawBitmap(targetBmp, 0, 0, null); //캔버스에 입력 이미지를 넣음
                skm.make_sticker(tempCanvas, glasses, output, paint, 1.6);
                imageView.setImageDrawable(new BitmapDrawable(getResources(), bitmap_canvas)); //입력이미지와 점을 이미지 뷰에 그려줌
                break;
            case R.id.bitsunglass:
                glasses = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.bitsunglass);
                tempCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                tempCanvas.drawBitmap(targetBmp, 0, 0, null); //캔버스에 입력 이미지를 넣음
                skm.make_sticker(tempCanvas, glasses, output, paint, 0.6);
                imageView.setImageDrawable(new BitmapDrawable(getResources(), bitmap_canvas)); //입력이미지와 점을 이미지 뷰에 그려줌
                break;
            case R.id.bdaysunglass:
                glasses = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.bdaysunglass);
                tempCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                tempCanvas.drawBitmap(targetBmp, 0, 0, null); //캔버스에 입력 이미지를 넣음
                skm.make_sticker(tempCanvas, glasses, output, paint,2.0);
                imageView.setImageDrawable(new BitmapDrawable(getResources(), bitmap_canvas)); //입력이미지와 점을 이미지 뷰에 그려줌
                break;
            case R.id.aliensunglass:
                glasses = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.aliensunglass);
                tempCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                tempCanvas.drawBitmap(targetBmp, 0, 0, null); //캔버스에 입력 이미지를 넣음
                skm.make_sticker(tempCanvas, glasses, output, paint, 1.4);
                imageView.setImageDrawable(new BitmapDrawable(getResources(), bitmap_canvas)); //입력이미지와 점을 이미지 뷰에 그려줌
                break;
            case R.id.leonsunglass:
                glasses = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.leonsunglass);
                tempCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                tempCanvas.drawBitmap(targetBmp, 0, 0, null); //캔버스에 입력 이미지를 넣음
                skm.make_sticker(tempCanvas, glasses, output, paint, 0.8);
                imageView.setImageDrawable(new BitmapDrawable(getResources(), bitmap_canvas)); //입력이미지와 점을 이미지 뷰에 그려줌
                break;
            default:
                break;
        }
    }
}