package com.rxxb.imagepicker;

import android.widget.Toast;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by 彭保生 on 2018/2/28.
 */

public class ImagePickerMain extends CordovaPlugin {
    private CallbackContext mCallbackContext;
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.mCallbackContext = callbackContext;
        if("toast".equals(action)){
            String msg = args.getString(0);
            Toast.makeText(cordova.getActivity(),msg,Toast.LENGTH_SHORT).show();
            callbackContext.success("success");
            return true;
        }
        mCallbackContext.error("error");
        return false;
    }
}
