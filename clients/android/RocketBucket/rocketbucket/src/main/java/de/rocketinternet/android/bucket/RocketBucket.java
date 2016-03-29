package de.rocketinternet.android.bucket;

import android.content.Context;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Sameh Gerges
 */
public class RocketBucket {

    static final String TAG = RocketBucket.class.getSimpleName();

    private static boolean isDebug;

    public static final String VARIANT_NAME_DEFAULT = "default_variant";
    private static final Variant VARIANT_DEFAULT = new Variant(VARIANT_NAME_DEFAULT);
    private static RocketBucket self;

    private final Map<String, Variant> experimentMap;

    @Nullable private final RocketBucketContainer container;

    private RocketBucket(@Nullable RocketBucketContainer container){
        this.experimentMap = new HashMap<>();
        this.container = container;
    }

    static RocketBucket getInstance(){
        if (self == null) {
            throw new RuntimeException("Rocket Bucket is not initialized, please make sure to call initialize function");
        }
        return self;
    }

    public static boolean isDebug() {
        return isDebug;
    }

    public static void setIsDebug(boolean isDebug) {
        RocketBucket.isDebug = isDebug;
    }

    private void updateExperimentList(Map<String, Variant> results){
        experimentMap.clear();
        experimentMap.putAll(results);

        if (container != null){
            Map<String, String> experiments = new HashMap<>();
            for (String experimentName : results.keySet()) {
                experiments.put(experimentName, getVariantName(experimentName));
            }

            container.onExperimentDataReady(experiments);
        }
    }

    private Variant getVariant(@NonNull String experimentName){
        return experimentMap.containsKey(experimentName)? experimentMap.get(experimentName) : VARIANT_DEFAULT;
    }

    void onUnexpectedError(Throwable t){
        Log.w(TAG, t.getMessage(), t);

        if (container != null) {
            container.onUnexpectedError(t);
        }
    }

    private static String getDeviceId(Context context){

        /*String deviceId = null;

        try {
            String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (androidId != null & androidId.length() > 1) {
                Long.valueOf(androidId.substring(0, 1), 16);
                deviceId = androidId;
            }
        } catch (NumberFormatException ex) {
            //Exception ignored as it can happen as result of known bug on some devices that have android id as string represents model name or something else.
        }

        if (deviceId == null) {
            TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELECOM_SERVICE);
            deviceId = telephony.getDeviceId();
        }*/
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static void initialize(@NonNull Context context, @NonNull String endpoint, @NonNull String apiKey, @Nullable RocketBucketContainer container) {
        self = new RocketBucket(container);

        NetworkTask.updateLatestBucket(context, endpoint, apiKey, getDeviceId(context), new NetworkTask.SuccessCallback<Map<String, Variant>>() {
            @Override
            public void onSuccess(Map<String, Variant> response) {
                getInstance().updateExperimentList(response);
            }
        });
    }

    public static String getVariantName(@NonNull String experimentName){
        return getInstance().getVariant(experimentName).getName();
    }

    public static String getVariantValue(@NonNull String experimentName, String key, String defaultValue){
        return getInstance().getVariant(experimentName).getValue(key, defaultValue);
    }

    static class Variant{
        private String name;
        private Map<String, String> data;

        public Variant(String name) {
            this.data = new HashMap<>();
            this.name = name;
        }

        public Variant(String name, @NonNull Map<String, String> data) {
            this.data = data;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public String getValue(String key, String defaultValue){
            return data.containsKey(key)? data.get(key) : defaultValue;
        }
    }
}
