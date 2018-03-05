package com.rxxb.imagepicker;

import android.content.Intent;

import com.rxxb.imagepicker.bean.ImageItem;
import com.rxxb.imagepicker.crop.AspectRatio;
import com.rxxb.imagepicker.loader.GlideImageLoader;
import com.rxxb.imagepicker.ui.ImageGridActivity;
import com.rxxb.imagepicker.util.BitmapUtil;
import com.rxxb.imagepicker.view.CropImageView;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
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
                imagePicker.setOutPutX(params.optInt("width", 0));
                imagePicker.setOutPutY(params.optInt("height", 0));

                //颜色相关设置
                if (params.has("oKButtonTitleColorNormal")) {
                    imagePicker.getViewColor().setoKButtonTitleColorNormal(params.getString("oKButtonTitleColorNormal"));
                }
                if (params.has("oKButtonTitleColorDisabled")) {
                    imagePicker.getViewColor().setoKButtonTitleColorDisabled(params.getString("oKButtonTitleColorDisabled"));
                }
                if (params.has("naviBgColor")) {
                    imagePicker.getViewColor().setNaviBgColor(params.getString("naviBgColor"));
                }
                if (params.has("naviTitleColor")) {
                    imagePicker.getViewColor().setNaviTitleColor(params.getString("naviTitleColor"));
                }
                if (params.has("barItemTextColor")) {
                    imagePicker.getViewColor().setBarItemTextColor(params.getString("barItemTextColor"));
                }
                if (params.has("toolbarBgColor")) {
                    imagePicker.getViewColor().setToolbarBgColor(params.getString("toolbarBgColor"));
                }
                if (params.has("toolbarTitleColorNormal")) {
                    imagePicker.getViewColor().setToolbarTitleColorNormal(params.getString("toolbarTitleColorNormal"));
                }
                if (params.has("toolbarTitleColorDisabled")) {
                    imagePicker.getViewColor().setToolbarTitleColorDisabled(params.getString("toolbarTitleColorDisabled"));
                }

                if (cutType == 0) {
                    //圆形单选
                    imagePicker.setCutType(0);
                    imagePicker.setStyle(CropImageView.Style.CIRCLE);
                    imagePicker.setAspectRatio(new AspectRatio(1, 1));
                    imagePicker.setMultiMode(false);
                    imagePicker.setDynamicCrop(false);
                } else if (cutType == 1) {
                    //矩形
                    imagePicker.setCutType(1);
                    imagePicker.setStyle(CropImageView.Style.RECTANGLE);
                    imagePicker.setAspectRatio(new AspectRatio(1, 1));
                    imagePicker.setDynamicCrop(false);
                    if (maximumImagesCount == 1) {
                        imagePicker.setMultiMode(false);
                    }
                } else {
                    imagePicker.setCutType(2);
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
        if (resultCode == ImagePicker.RESULT_CODE_ITEMS && intent != null && requestCode == 100) {
            final ArrayList<ImageItem> images = (ArrayList<ImageItem>) intent.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
            if (images == null || images.isEmpty()) {
                mCallbackContext.error("未获取到图片数据");
                return;
            }
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    ArrayList<String> imagePath = new ArrayList();
                    for (ImageItem image : images) {
                        String result = image.path;
                        boolean saved = false;
                        String destPath = ImagePicker.createFile(ImagePicker.getInstance().getCropCacheFolder(cordova.getContext()), "IMG_"+System.currentTimeMillis(), ".png").getAbsolutePath();
                        if (ImagePicker.getInstance().isOrigin() || ImagePicker.getInstance().getOutPutX() == 0 || ImagePicker.getInstance().getOutPutY() == 0) {
                            //原图按图片原始尺寸压缩, size小于150kb的不压缩
                            if (isNeedCompress(150, result)) {
                                saved = BitmapUtil.saveBitmap2File(BitmapUtil.compress(result), destPath);
                            }
                        } else {
                            //按给定的宽高压缩
                            saved = BitmapUtil.saveBitmap2File(BitmapUtil.getScaledBitmap(result, ImagePicker.getInstance().getOutPutX(), ImagePicker.getInstance().getOutPutY()), destPath);
                        }
                        if (outputType == 0) {
                            imagePath.add(saved ? destPath : result);
                        } else {
                            imagePath.add(BitmapUtil.base64Image(saved ? destPath : result));
                        }
                    }
                    JSONArray res = new JSONArray(imagePath);
                    mCallbackContext.success(res);
                }
            });
        } else {
            mCallbackContext.error("没有选择任何图片");
        }
    }

    private boolean isNeedCompress(int leastCompressSize, String path) {
        if (leastCompressSize > 0) {
            File source = new File(path);
            if (!source.exists()) {
                return false;
            }

            if (source.length() <= (leastCompressSize << 10)) {
                return false;
            }
        }
        return true;
    }
}
