package de.rocketinternet.android.bucket;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import de.rocketinternet.android.bucket.core.BucketsContainer;
import de.rocketinternet.android.bucket.core.BucketsProvider;
import de.rocketinternet.android.bucket.core.BucketsProviderImpl;
import de.rocketinternet.android.bucket.core.Config;
import de.rocketinternet.android.bucket.core.EditableBucketsProvider;
import de.rocketinternet.android.bucket.models.Bucket;
import de.rocketinternet.android.bucket.network.BucketService;
import de.rocketinternet.android.bucket.ui.BucketDetailsActivity;
import de.rocketinternet.android.bucket.ui.BucketTrackerUi;
import de.rocketinternet.android.bucket.ui.BucketsActivity;

/**
 * @author Sameh Gerges
 */
public class RocketBucket implements BucketsContainer {

    public static final String TAG = RocketBucket.class.getSimpleName();


    public static final String VARIANT_NAME_DEFAULT = "default_variant";
    private static final Bucket BUCKET_BASE_DEFAULT = new Bucket(VARIANT_NAME_DEFAULT);


    private static RocketBucket sSelf;

    private final Map<String, Bucket> mExperimentMap = new HashMap<>();
    private final BucketsProvider bucketsProvider;
    private final Config config;

    @Nullable private final RocketBucketContainer mContainer;


    protected RocketBucket(@NonNull Context context, @NonNull Config config, @NonNull BucketsProvider bucketsProvider, @Nullable RocketBucketContainer container) {
        this.config = config;
        this.bucketsProvider = bucketsProvider;
        this.mContainer = container;
    }

    private void updateExperimentList(Map<String, Bucket> results) {
        mExperimentMap.clear();
        mExperimentMap.putAll(results);
    }

    private void notifyContainer(){
        if (mContainer != null && mExperimentMap.size() > 0) {
            mContainer.onExperimentDataReady(getActiveExperiments());
        }
    }

    public Map<String, String> getActiveExperiments(){
        Map<String, String> experiments = new HashMap<>();
        for (String experimentName : mExperimentMap.keySet()) {
            experiments.put(experimentName, getBucketName(experimentName));
        }
        return  experiments;
    }

    Bucket getBucket(@NonNull String experimentName) {
        return mExperimentMap.containsKey(experimentName) ? mExperimentMap.get(experimentName) : BUCKET_BASE_DEFAULT;
    }

    public void onUnexpectedError(Throwable t) {
        Log.w(TAG, t.getMessage(), t);

        if (mContainer != null) {
            mContainer.onUnexpectedError(t);
        }
    }

    public static void onError(Throwable t){
        getInstance().onUnexpectedError(t);
    }
    /**
     * initialize RocketBucket which retrieve this device's buckets and make it available for future use. this is nessessary before using any of the functionality of
     * RocketBucket otherwise it will throw an exception it is not initialized !
     * @param context application context
     * @param config configuration object that control library behavior for more details check {@link Config}
     * @param container (optional) call back to be notified when request successfully served by backend
     */
    public static void initialize(@NonNull Context context, @NonNull Config config, @Nullable RocketBucketContainer container) {

        if (config.isDebug() && !(context instanceof Application)) {
            throw new IllegalArgumentException("Context has to be instance of Application! in debug version");
        }

        if (sSelf == null) {
            BucketService bucketService = BucketService.initialize(context, config);
            BucketsProvider bucketsProvider;
            if (config.isDebug()) {
                EditableBucketsProvider editableBucketsProvider = new EditableBucketsProvider(bucketService);
                Application application = (Application) context;
                injectDebugViewIntoLifecycle(application, editableBucketsProvider);
                bucketsProvider = editableBucketsProvider;
            } else {
                bucketsProvider = new BucketsProviderImpl(bucketService);
            }

            synchronized (RocketBucket.class) {
                if (sSelf == null) {
                    sSelf = new RocketBucket(context, config, bucketsProvider, container);
                }
            }

            bucketsProvider.loadBuckets(context, config, sSelf);
        }
    }

    private static void injectDebugViewIntoLifecycle(Application application, final EditableBucketsProvider bucketsProvider) {

        application.registerActivityLifecycleCallbacks(new  Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                if (activity instanceof BucketsActivity) {
                    ((BucketsActivity) activity).setBucketsProvider(bucketsProvider);
                    return;
                } else if (activity instanceof BucketDetailsActivity) {
                    ((BucketDetailsActivity) activity).setBucketsProvider(bucketsProvider);
                    return;
                }
                BucketTrackerUi.inject(activity);
            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {

            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
    }

    /**
     * provide bucket assigned by server for current device to make discession or decide what to display/behaviour user should expect
     * @param experimentName Experiment name which we want to inquiry it's bucket for. e.g: AwesomeTabExperiment
     * @return bucket name in which server assign for this device e.g TabOld or TabAwesome
     */
    public static String getBucketName(@NonNull String experimentName) {
        return getInstance().getBucket(experimentName).getName();
    }

    /**
     * get extra attributes stored in bucket, this handy when we we have specific value returned from server
     * @param experimentName experiment name which this bucket are contained
     * @param key name of extra value inside this bucket for example : buttonColor or buttonTitle
     * @param defaultValue in case this bucket doesn't contains this key this value are returned, this is handy in case response still didn't retrieved for any reason.
     * @return extra value in a form of String, then you have to parse it depend on expected value is
     */
    public static String getExtraByName(@NonNull String experimentName, String key, String defaultValue) {
        return getInstance().getBucket(experimentName).getExtraByName(key, defaultValue);
    }

    public static Map<String, String> getExperiments(){
        return getInstance().getActiveExperiments();
    }

    @VisibleForTesting
    protected static void killTheBucket() {
        sSelf = null;
    }

    static RocketBucket getInstance() {
        if (sSelf == null) {
            throw new IllegalStateException("Rocket Bucket is not initialized, please make sure to call initialize function");
        }
        return sSelf;
    }

    @VisibleForTesting
    static void setInstance(RocketBucket bucket) {
        sSelf = bucket;
    }

    @Override
    public void onBucketsRetrieved(Context context, Map<String, Bucket> buckets, Throwable error, @BucketsSource int source) {
        if (buckets != null) {
            updateExperimentList(buckets);
        } else if (error != null) {
            onUnexpectedError(error);
        }

        if (source != SOURCE_INTERNAL_CACHE) {
            /**
             * Notifying container will happen only trail to update cached data is done.
             * So in case of failure due to network error, container will be notified even if cached data is expired
             */
            notifyContainer();
        }
    }

    @Override
    public void updateBucket(Context context, String experimentName, Bucket bucket) {
        mExperimentMap.put(experimentName, bucket);
    }

    BucketsProvider getBucketsProvider() {
        return bucketsProvider;
    }

}
