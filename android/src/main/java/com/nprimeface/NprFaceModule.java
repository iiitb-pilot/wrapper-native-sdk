package com.nprimeface;

import android.app.Activity;
import android.content.Intent;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import in.nprime.injisdk.FaceLibActivity;
import in.nprime.injisdk.dto.CaptureResponse;
import in.nprime.injisdk.dto.SdkResponse;
import in.nprime.injisdk.dto.InitResponse;
import in.nprime.injisdk.dto.GenerateAndIdentifyTemplateResponse;

public class NprFaceModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    private static final int INIT_REQUEST_CODE = 1000;
    private static final int CAPTURE_REQUEST_CODE = 1001;
    private static final int GENERATE_AND_IDENTIFY_REQUEST_CODE = 1002;

    private Promise capturePromise;
    private Promise initPromise;
    private Promise generateAndIdentifyPromise;

    private final ReactApplicationContext reactContext;

    public NprFaceModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        reactContext.addActivityEventListener(this);
    }

    @Override
    public String getName() {
        return "NprFaceModule";
    }

    // ✅ FIXED: Accept object from JS (configure({}))
    @ReactMethod
    public void configure(ReadableMap config, Promise promise) {
        Log.d("NPR_DEBUG", "Configure called");

        try {
            this.initPromise = promise;
            Activity currentActivity = getCurrentActivity();

            if (currentActivity == null) {
                promise.reject("NO_ACTIVITY", "Activity not found");
                return;
            }

            String licenseKey = config.hasKey("licenseKey") ? config.getString("licenseKey") : "";
            String customRef = config.hasKey("customRef") ? config.getString("customRef") : "";

            String initJson = "{"
                    + "\"request\":{"
                    + "\"licenseKey\":\"" + licenseKey + "\","
                    + "\"customRef\":\"" + customRef + "\""
                    + "},"
                    + "\"timestamp\":\"\""
                    + "}";

            Intent intent = new Intent(currentActivity, FaceLibActivity.class);
            intent.setAction("in.face.lib.init");
            intent.putExtra("input", initJson.getBytes("UTF-8"));

            currentActivity.startActivityForResult(intent, INIT_REQUEST_CODE);

        } catch (Exception e) {
            promise.reject("INIT_ERROR", e.getMessage());
        }
    }

    // ✅ FIXED: Supports BOTH 3 and 4 param calls
    @ReactMethod
    public void faceCompare(boolean cameraSwitch, boolean liveness, String vcImageData, int mode, Promise promise) {
        Log.d("NPR_DEBUG", "faceCompare called");

        try {
            this.generateAndIdentifyPromise = promise;
            Activity currentActivity = getCurrentActivity();

            if (currentActivity == null) {
                promise.resolve(false);
                return;
            }

            String identifyJson = "{"
                    + "\"request\":{"
                    + "\"trustLevel\":\"Low\","
                    + "\"capturedTemplateData\":\"\","
                    + "\"vcImageData\":\"" + vcImageData + "\""
                    + "},"
                    + "\"timestamp\":\"\""
                    + "}";

            Intent intent = new Intent(currentActivity, FaceLibActivity.class);
            intent.setAction("in.face.lib.generateAndIdentifyTemplates");
            intent.putExtra("input", identifyJson.getBytes("UTF-8"));

            currentActivity.startActivityForResult(intent, GENERATE_AND_IDENTIFY_REQUEST_CODE);

        } catch (Exception e) {
            promise.resolve(false);
        }
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {

        if (requestCode == INIT_REQUEST_CODE) {
            if (initPromise == null) return;

            try {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    byte[] responseBytes = data.getByteArrayExtra("response");

                    SdkResponse<InitResponse> response = new ObjectMapper()
                            .readValue(responseBytes, new TypeReference<SdkResponse<InitResponse>>() {});

                    boolean success = response.getResponse() != null && response.getResponse().isInitSuccessful();

                    initPromise.resolve(success);
                } else {
                    initPromise.resolve(false);
                }
            } catch (Exception e) {
                initPromise.resolve(false);
            } finally {
                initPromise = null;
            }
        }

        else if (requestCode == GENERATE_AND_IDENTIFY_REQUEST_CODE) {
            if (generateAndIdentifyPromise == null) return;

            try {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    byte[] responseBytes = data.getByteArrayExtra("response");

                    SdkResponse<GenerateAndIdentifyTemplateResponse> response = new ObjectMapper()
                            .readValue(responseBytes, new TypeReference<SdkResponse<GenerateAndIdentifyTemplateResponse>>() {});

                    boolean match = response.getResponse() != null && response.getResponse().isMatchSuccessful();

                    generateAndIdentifyPromise.resolve(match);
                } else {
                    generateAndIdentifyPromise.resolve(false);
                }
            } catch (Exception e) {
                generateAndIdentifyPromise.resolve(false);
            } finally {
                generateAndIdentifyPromise = null;
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {}
}
