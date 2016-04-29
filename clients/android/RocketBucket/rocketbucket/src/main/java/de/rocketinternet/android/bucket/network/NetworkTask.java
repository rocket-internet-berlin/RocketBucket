package de.rocketinternet.android.bucket.network;

import android.content.Context;
import android.net.Uri;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import de.rocketinternet.android.bucket.RocketBucket;
import de.rocketinternet.android.bucket.models.Bucket;
import de.rocketinternet.android.bucket.models.Experiment;

/**
 * @author Sameh Gerges
 */
public class NetworkTask implements com.squareup.okhttp.Callback {


    private static String formSplitUrl(String endpoint, String deviceId) {
        return endpoint + "?user_id=" + deviceId;
    }

    private final String HTTP_HEADER_API_KEYE = "X-Api-Key";
    private final String DIR_NAME_CACHING = "rocket_bucket";

    private final int CACHE_SIZE = 1 * 1024 * 1024; // 1 MiB
    private final int MAX_RETRY_COUNT = 5;

    private static OkHttpClient client;
    private final NetworkTaskCallback callBack;
    private int trailsCount;


    public NetworkTask(Context context, String apiKey, String url, NetworkTaskCallback callBack) {
        this.callBack = callBack;

        if (client == null) {
            this.client = new OkHttpClient();
            if (!RocketBucket.isDebug()) {//so we do not stuck with cached response while developing
                File cachingDir = new File(context.getCacheDir(), DIR_NAME_CACHING);
                if (cachingDir.exists() || cachingDir.mkdirs()) {
                    this.client.setCache(new Cache(cachingDir, CACHE_SIZE));
                } else {
                    callBack.onFailure(new RuntimeException("RocketBucket: failed to create caching dir " + cachingDir.getAbsolutePath()));
                }
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

     try {
         Request request = new Request.Builder()
                 .url(url)
                 .header(HTTP_HEADER_API_KEYE, apiKey).build();
         client.newCall(request).enqueue(this);
     }catch (Exception e){
         e.printStackTrace();
     }
    }

    @Override
    public void onFailure(Request request, IOException e) {
        trailsCount++;
        if (trailsCount < MAX_RETRY_COUNT) {
            client.newCall(request).enqueue(this);
        }

        callBack.onFailure(e);
    }

    @Override
    public void onResponse(com.squareup.okhttp.Response response) throws IOException {
        if (response.body() != null && response.isSuccessful()) {
            InputStream inputStream = null;

            try {

                inputStream = response.body().byteStream();
                callBack.onSuccess(inputStream);
            } catch (Throwable t) {
                callBack.onFailure(t);
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        }
    }

    private interface NetworkTaskCallback {
        void onSuccess(InputStream inputStream) throws IOException, JSONException;
        void onFailure(Throwable t);
    }

    public interface Callback<T> {
        void onCompletion(T response, Throwable error);
    }

    public static void updateLatestBucket(Context context, String endpoint, String apiKey, String deviceId, final Callback<Map<String, Bucket>>
            experimentMap) {
        new NetworkTask(context, apiKey, formSplitUrl(endpoint, deviceId), new NetworkTaskCallback() {
            @Override
            public void onSuccess(InputStream inputStream) throws IOException {
                experimentMap.onCompletion(JsonParser.parseExperiments(inputStream), null);
            }

            @Override
            public void onFailure(Throwable t) {
                experimentMap.onCompletion(null, t);
            }
        });
    }

    public static void getAllBuckets(Context context, String endpoint, String apiKey, final Callback<List<Experiment>>
            experimentMap) {
        new NetworkTask(context, apiKey, buildAllBucketsEndUrl(endpoint), new
                NetworkTaskCallback() {
                    @Override
                    public void onSuccess(InputStream inputStream) throws IOException, JSONException {
                        experimentMap.onCompletion(JsonParser.parseAllExperiments(inputStream), null);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        experimentMap.onCompletion(null, t);
                    }
                });
    }

    private static String buildAllBucketsEndUrl(String endpoint) {
        return Uri.parse(endpoint).buildUpon().appendPath("all").toString();
    }
}
