package de.rocketinternet.android.bucket;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import de.rocketinternet.android.bucket.models.Bucket;

/**
 * @author Sameh Gerges
 */
public class RocketBucket implements BucketsContainer{

    static final String TAG = RocketBucket.class.getSimpleName();

    private static boolean sIsDebug;
    public static Config CONFIG;
    public static final String VARIANT_NAME_DEFAULT = "default_variant";
    private static final Bucket BUCKET_BASE_DEFAULT = new Bucket(VARIANT_NAME_DEFAULT);

    private static RocketBucket sSelf;

    private final Map<String, Bucket> mExperimentMap = new HashMap<>();
    private final BucketsProvider bucketsProvider;
    @Nullable private final RocketBucketContainer mContainer;


    RocketBucket(@NonNull String endpoint, @NonNull String apiKey, @NonNull BucketsProvider bucketsProvider, @Nullable RocketBucketContainer container) {
        CONFIG = new Config(apiKey, endpoint);

        this.bucketsProvider = bucketsProvider;
        this.mContainer = container;
    }

    private void updateExperimentList(Map<String, Bucket> results) {
        mExperimentMap.clear();
        mExperimentMap.putAll(results);

        if (mContainer != null) {
            Map<String, String> experiments = new HashMap<>();
            for (String experimentName : results.keySet()) {
                experiments.put(experimentName, getVariantName(experimentName));
            }
            mContainer.onExperimentDataReady(experiments);
        }
    }

    Bucket getVariant(@NonNull String experimentName) {
        return mExperimentMap.containsKey(experimentName) ? mExperimentMap.get(experimentName) : BUCKET_BASE_DEFAULT;
    }

    void onUnexpectedError(Throwable t) {
        Log.w(TAG, t.getMessage(), t);

        if (mContainer != null) {
            mContainer.onUnexpectedError(t);
        }
    }

    public Map<String, Bucket> getCurrentExperiments() {
        return mExperimentMap;
    }

    public static void initialize(@NonNull Context context, @NonNull String endpoint, @NonNull String apiKey, @Nullable RocketBucketContainer container, boolean
            isDebug) {
        if ((apiKey == null || apiKey.isEmpty()) || (endpoint == null || endpoint.isEmpty())) {
            throw new IllegalStateException("endpoint and api key cannot be null! or empty");
        }
        if (sSelf == null) {
            RocketBucket.sIsDebug = isDebug;
            BucketsProvider bucketsProvider;
            if (isDebug) {
                bucketsProvider = new EditableBucketsProvider();
            } else {
                bucketsProvider = new BucketProviderImpl();
            }
            synchronized (RocketBucket.class) {
                if (sSelf == null) {
                    sSelf = new RocketBucket(endpoint, apiKey, bucketsProvider, container);
                }
            }
        }
        sSelf.updateLatestBuckets(context);


    }

    void updateLatestBuckets(final Context context) {
        bucketsProvider.loadBuckets(context, this);
    }

    public static String getVariantName(@NonNull String experimentName) {
        return getInstance().getVariant(experimentName).getName();
    }

    public String getVariantValue(@NonNull String experimentName, String key, String defaultValue) {
        return getInstance().getVariant(experimentName).getValue(key, defaultValue);
    }

    @VisibleForTesting
    public static void killTheBucket() {
        sSelf = null;
    }

    static RocketBucket getInstance() {
        if (sSelf == null) {
            throw new IllegalStateException("Rocket BucketBase is not initialized, please make sure to call initialize function");
        }
        return sSelf;
    }

    public static boolean isDebug() {
        return sIsDebug;
    }

    public static void setIsDebug(boolean isDebug) {
        RocketBucket.sIsDebug = isDebug;
    }

    @Override
    public void onBucketsRetrieved(Context context, Map<String, Bucket> buckets, Throwable error) {
        if (buckets != null) {
            updateExperimentList(buckets);
        } else if (error != null) {
            onUnexpectedError(error);
        }
    }

    @Override
    public void updateBucket(Context context, String experimentName, Bucket bucket) {
        mExperimentMap.put(experimentName, bucket);
    }

    public BucketsProvider getBucketsProvider() {
        return bucketsProvider;
    }


    public static class Config {
        private String mEndpoint;
        private String mApiKey;

        public Config(String apiKey, String endpoint) {
            this.mApiKey = apiKey;
            this.mEndpoint = endpoint;
        }

        public String getApiKey() {
            return mApiKey;
        }

        public String getEndpoint() {
            return mEndpoint;
        }
    }
}
