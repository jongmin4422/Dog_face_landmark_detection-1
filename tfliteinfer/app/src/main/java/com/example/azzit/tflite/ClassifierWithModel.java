package com.example.azzit.tflite;

import static com.example.azzit.GalleryActivity.TAG;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;
import android.util.Size;

import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.model.Model;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.tensorflow.lite.support.image.ops.ResizeOp.ResizeMethod.NEAREST_NEIGHBOR;

public class ClassifierWithModel{
//    private static final String MODEL_NAME = "mobilenet_imagenet_model.tflite";
//    private static final String MODEL_NAME = "dogmodel.tflite";
    private static final String MODEL_NAME = "converted_model_quant_224.tflite";

    //change model-> should change imagesize in CameraActivity and GalleryActivity

    private static final String LABEL_FILE = "labels.txt";

    Context context;
    Model model;
    int modelInputWidth, modelInputHeight, modelInputChannel;
    TensorImage inputImage;
    TensorBuffer outputBuffer;
//    private List<String> labels;

    private boolean isInitialized = false;

    public ClassifierWithModel(Context context) {
        this.context = context;
    }

    public void init() throws IOException {
        model = Model.createModel(context, MODEL_NAME);

        initModelShape();
//        labels = FileUtil.loadLabels(context, LABEL_FILE);
//        labels.remove(0);

        isInitialized = true;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    private void initModelShape() {
        Tensor inputTensor = model.getInputTensor(0);
        int[] shape = inputTensor.shape();
        modelInputChannel = shape[0];
        modelInputWidth = shape[1];
        modelInputHeight = shape[2];

        inputImage = new TensorImage(inputTensor.dataType());

        Tensor outputTensor = model.getOutputTensor(0);
        outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), outputTensor.dataType());
    }

    public Size getModelInputSize() {
        if(!isInitialized)
            return new Size(0, 0);
        return new Size(modelInputWidth, modelInputHeight);
    }

    private Bitmap convertBitmapToARGB8888(Bitmap bitmap) {
        return bitmap.copy(Bitmap.Config.ARGB_8888,true);
    }

    private TensorImage loadImage(final Bitmap bitmap, int sensorOrientation) {
        if(bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
            inputImage.load(convertBitmapToARGB8888(bitmap));
        } else {
            inputImage.load(bitmap);
        }

        int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
        int numRotation = sensorOrientation / 90;

        ImageProcessor imageProcessor = new ImageProcessor.Builder()
//                .add(new ResizeWithCropOrPadOp(cropSize, cropSize)) //좀 더 알아보고 넣을 것
                .add(new ResizeOp(modelInputWidth, modelInputHeight, NEAREST_NEIGHBOR))
//                .add(new Rot90Op(numRotation)) //좀 더 알아보고 넣을 것
                .add(new NormalizeOp(0.0f, 255.0f))
                .build();

        return imageProcessor.process(inputImage);
    }

    public float[] classify(Bitmap image, int sensorOrientation) {
        inputImage = loadImage(image, sensorOrientation);

        Object[] inputs = new Object[]{inputImage.getBuffer()};
        Log.d(TAG, "classify: " + inputs);
        Map<Integer, Object> outputs = new HashMap();
        outputs.put(0, outputBuffer.getBuffer().rewind());
        Log.d(TAG, "classify: "+ outputs);
        model.run(inputs, outputs);
        Log.d(TAG, "classify: "+ outputBuffer.getFloatArray());
//        Map<String, Float> output =
//                new TensorLabel(labels, outputBuffer).getMapWithFloatValue(); 클래스 분류에서 사용함(클래스 값과 해당 종을 매핑하는 것 같음)
        float[] res = outputBuffer.getFloatArray(); //결과값을 배열에 넣어줌
        res = Arrays.copyOfRange(res, 133, res.length); //133이후의 좌표에 해당하는 값을 가져옴
        return res; //반환
    }

    public float[] classify(Bitmap image) {
        return classify(image, 0);
    }

    private Pair<String, Float> argmax(Map<String, Float> map) {
        String maxKey = "";
        float maxVal = -1;

        for(Map.Entry<String, Float> entry : map.entrySet()) {
            float f = entry.getValue();
            if(f > maxVal) {
                maxKey = entry.getKey();
                maxVal = f;
            }
        }

        return new Pair<>(maxKey, maxVal);
    }

    public void finish() {
        if(model != null) {
            model.close();
        }
    }
}
