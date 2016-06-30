package de.rocketinternet.android.bucket.network;

import android.content.Context;
import android.net.Uri;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.rocketinternet.android.bucket.core.Config;
import de.rocketinternet.android.bucket.RocketBucket;
import de.rocketinternet.android.bucket.models.Experiment;
import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author Sameh Gerges
 */
public class BucketService {
    private static final String HTTP_HEADER_API_KEY = "X-Api-Key";

    Config config;
    OkHttpClient httpClient;

    private BucketService(Context context, Config config) {
        this.config = config;

        File cachingDir = config.getCachingDir(context);

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS);

        if (cachingDir.exists() || cachingDir.mkdirs()) {
            clientBuilder.cache(new Cache(cachingDir, Config.CACHE_SIZE));
        } else {
            RocketBucket.onError(new RuntimeException("RocketBucket: failed to create caching dir " + cachingDir.getAbsolutePath()));
        }

        if (config.isDebug()) {
            clientBuilder.addNetworkInterceptor(new HttpLoggingInterceptor(HttpLoggingInterceptor.Level.BODY));
        }

        this.httpClient = clientBuilder.build();
    }

    private Request.Builder createRequestBuilder(){
        return new Request.Builder()
                .header(HTTP_HEADER_API_KEY, config.getApiKey());
    }

    private static String buildAllBucketsEndUrl(String endpoint) {
        return Uri.parse(endpoint).buildUpon().appendPath("all").toString();
    }

    private static String buildSplitUrl(String endpoint, String deviceId) {
        return endpoint + "?user_id=" + deviceId;
    }

    public static BucketService initialize(Context context, Config config){
        return new BucketService(context, config);
    }

    public void getBuckets(String deviceId, final Callback<InputStream> callback){
        Request request = createRequestBuilder()
                .url(buildSplitUrl(config.getEndpoint(), deviceId))
                .get()
                .build();

        httpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body().byteStream(), response.cacheResponse() != null);
                } else {
                    callback.onFailure(new RuntimeException(response.code() + " " +response.message()));
                }
            }
        });
    }

    public InputStream getBuckets(String deviceId, long timeout) throws IOException {
        OkHttpClient tempHttpClient = httpClient.newBuilder()
            .readTimeout(timeout, TimeUnit.SECONDS)
            .writeTimeout(timeout, TimeUnit.SECONDS)
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .build();

        Request request = createRequestBuilder()
                .url(buildSplitUrl(config.getEndpoint(), deviceId))
                .get()
                .build();

        return tempHttpClient.newCall(request).execute().body().byteStream();
    }

    public void getBuckets(final Callback<List<Experiment>> callback){
        Request request = createRequestBuilder()
                .url(buildAllBucketsEndUrl(config.getEndpoint()))
                .build();

        httpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        callback.onSuccess(JsonParser.parseAllExperiments(response.body().byteStream()), response.cacheResponse() != null);
                    } catch (JSONException e) {
                        callback.onFailure(e);
                    }

                } else {
                    callback.onFailure(new RuntimeException(response.code() + " " +response.message()));
                }
            }
        });
    }

    public interface Callback<T>{
        void onSuccess(T data, boolean fromCache) throws IOException;
        void onFailure(Throwable t);
    }
}
