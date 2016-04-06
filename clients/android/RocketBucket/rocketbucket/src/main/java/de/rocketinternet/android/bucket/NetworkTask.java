package de.rocketinternet.android.bucket;

import android.content.Context;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * @author Sameh Gerges
 */
class NetworkTask implements Callback {


    private static  String formSplitUrl(String endpoint, String deviceId){
        return endpoint + "?user_id=" + deviceId;
    }

    private final String HTTP_HEADER_API_KEYE = "X-Api-Key";
    private final String DIR_NAME_CACHING = "rocket_bucket";

    private final int CACHE_SIZE = 1 * 1024 * 1024; // 1 MiB
    private final int MAX_RETRY_COUNT = 5;

    private static OkHttpClient client;
    private final Request request;
    private final NetworkTaskCallback callBack;
    private int trailsCount;


    public NetworkTask(Context context, String apiKey, String url, NetworkTaskCallback callBack){
        if (client == null) {
            this.client = new OkHttpClient();
            File cachingDir = new File(context.getCacheDir(), DIR_NAME_CACHING);
            if (cachingDir.exists() || cachingDir.mkdirs()) {
                this.client.setCache(new Cache(cachingDir , CACHE_SIZE));
            } else {
                RocketBucket.getInstance().onUnexpectedError(new RuntimeException("RocketBucket: failed to create caching dir " + cachingDir.getAbsolutePath()));
            }

            /*if (RocketBucket.isDebug()) {
                com.squareup.okhttp.logging.HttpLoggingInterceptor loggingInterceptor = new com.squareup.okhttp.logging.HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                    @Override
                    public void log(String s) {
                        Log.d(RocketBucket.TAG, s);
                    }
                });
                loggingInterceptor.setLevel(com.squareup.okhttp.logging.HttpLoggingInterceptor.Level.BASIC);
                this.client.networkInterceptors().add(loggingInterceptor);
            }*/
        }

        this.callBack = callBack;
        this.request = new Request.Builder()
                .url(url)
                .header(HTTP_HEADER_API_KEYE, apiKey)
                .build();
        client.newCall(this.request).enqueue(this);
    }

    @Override
    public void onFailure(Request request, IOException e) {
        trailsCount ++;
        if (trailsCount < MAX_RETRY_COUNT) {
            client.newCall(request).enqueue(this);
        }

        RocketBucket.getInstance().onUnexpectedError(e);
    }

    @Override
    public void onResponse(com.squareup.okhttp.Response response) throws IOException {
        if (response.body() != null && response.isSuccessful()) {
            InputStream inputStream = null;

            try {
                inputStream = response.body().byteStream();
                callBack.onSuccess(inputStream);
            } catch (Throwable t) {
                RocketBucket.getInstance().onUnexpectedError(t);
            } finally {
                if (inputStream != null){
                    inputStream.close();
                }
            }
        }
    }

    private interface NetworkTaskCallback {
        void onSuccess(InputStream inputStream) throws IOException ;
    }

    public interface SuccessCallback <T>{
        void onSuccess(T response);
    }

    public static void updateLatestBucket(Context context, String endpoint, String apiKey, String deviceId, final SuccessCallback<Map<String, RocketBucket.Variant>>
            experimentMap) {
        new NetworkTask(context, apiKey, formSplitUrl(endpoint, deviceId),  new NetworkTaskCallback(){
            @Override
            public void onSuccess(InputStream inputStream) throws IOException {
                experimentMap.onSuccess( JsonParser.parseExperiments(inputStream));
            }
        });
    }
}
