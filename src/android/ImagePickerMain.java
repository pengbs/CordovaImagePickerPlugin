package com.rxxb.imagepicker;

import android.content.Intent;

import com.rxxb.imagepicker.bean.ImageItem;
import com.rxxb.imagepicker.crop.AspectRatio;
import com.rxxb.imagepicker.loader.GlideImageLoader;
import com.rxxb.imagepicker.ui.ImageGridActivity;
import com.rxxb.imagepicker.view.CropImageView;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by 彭保生 on 2018/2/28.
 */

public class ImagePickerMain extends CordovaPlugin {
    private CallbackContext mCallbackContext;
    private int outputType = 0;//输出格式，0表示输出路径，1表示base64字符串
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.mCallbackContext = callbackContext;
        if("getPictures".equals(action)){
            JSONObject params = args.getJSONObject(0);
            ImagePicker imagePicker = ImagePicker.getInstance();
            ImagePicker.getInstance().setImageLoader(new GlideImageLoader());
            int cutType = 2, maximumImagesCount = 1;
            if (params != null) {
                outputType = params.optInt("outputType", 0);
                maximumImagesCount = params.optInt("maximumImagesCount");
                imagePicker.setSelectLimit(maximumImagesCount);
                if (params.has("cutType")) {
                    cutType = params.optInt("cutType", 2);
                }
                if (params.has("width")) {
                    imagePicker.setOutPutX(params.optInt("width"));
                }
                if (params.has("height")) {
                    imagePicker.setOutPutY(params.optInt("height"));
                }
                if (cutType == 0) {
                    //圆形单选
                    imagePicker.setStyle(CropImageView.Style.CIRCLE);
                    imagePicker.setAspectRatio(new AspectRatio(1, 1));
                    imagePicker.setMultiMode(false);
                    imagePicker.setDynamicCrop(false);
                } else if (cutType == 1) {
                    //矩形
                    imagePicker.setStyle(CropImageView.Style.RECTANGLE);
                    imagePicker.setAspectRatio(new AspectRatio(1, 1));
                    imagePicker.setDynamicCrop(false);
                    if (maximumImagesCount == 1) {
                        imagePicker.setMultiMode(false);
                    }
                } else {
                    imagePicker.setStyle(CropImageView.Style.RECTANGLE);
                    int cutWidth = params.optInt("cutWidth", 1);
                    int cutHeight = params.optInt("cutHeight", 1);
                    imagePicker.setAspectRatio(new AspectRatio(cutWidth, cutHeight));
                    imagePicker.setDynamicCrop(true);
                    imagePicker.setMultiMode(true);
                }
            }
            Intent intent = new Intent(cordova.getActivity(), ImageGridActivity.class);
            cordova.startActivityForResult(this, intent, 100);
            return true;
        }
        mCallbackContext.error("error");
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == ImagePicker.RESULT_CODE_ITEMS) {
            if (intent != null && requestCode == 100) {
                ArrayList<ImageItem> images = (ArrayList<ImageItem>) intent.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
                ArrayList<String> imagePath = new ArrayList();
                if (images != null && !images.isEmpty()) {
                    for (ImageItem image : images) {
                        imagePath.add(image.path);
                    }
                }
                JSONArray res = new JSONArray(imagePath);
                mCallbackContext.success(res);
            } else {
                mCallbackContext.error("获取获取到图片数据");
            }
        } else {
            mCallbackContext.error("获取失败");
        }
    }
}
