package de.rocketinternet.android.bucket.core;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import de.rocketinternet.android.bucket.Config;
import de.rocketinternet.android.bucket.RocketBucket;
import de.rocketinternet.android.bucket.models.Bucket;
import de.rocketinternet.android.bucket.network.BucketService;

/**
 * @author Sameh Gerges
 */
public class EditableBucketsProvider implements BucketsProvider, BucketsContainer {

    private Map<String, Bucket> mExperimentMap;
    private Map<String, Bucket> mCustomExperimentMap;

    private BucketsContainer container;
    final private BucketService bucketService;

    public EditableBucketsProvider(BucketService bucketService) {
        this.bucketService = bucketService;
        this.mExperimentMap = new HashMap<>();
        this.mCustomExperimentMap = new HashMap<>();
    }

    @Override
    public void loadBuckets(Context context, Config config, BucketsContainer container) {
        this.container = container;
        new BucketsProviderImpl(bucketService).loadBuckets(context, config, this);
    }

    @Override
    public void onBucketsRetrieved(Context context, Map<String, Bucket> buckets, Throwable error, @BucketsSource int source) {

        if (buckets != null) {
            mExperimentMap.clear();
            mCustomExperimentMap.clear();

            for (String experiment : buckets.keySet()) {

                this.mExperimentMap.put(experiment, buckets.get(experiment));

                Bucket customBucket = SharedPrefManager.getCustomBucket(context, experiment);
                if (customBucket != null) {
                    buckets.put(experiment, customBucket);
                    this.mCustomExperimentMap.put(experiment, customBucket);
                } else {
                    this.mCustomExperimentMap.remove(experiment);
                }
            }
        }
        this.container.onBucketsRetrieved(context, buckets, error, source);
    }

    @Override
    public void updateBucket(Context context, String experimentName, Bucket bucket) {
        if (bucket != null) {
            this.mCustomExperimentMap.put(experimentName, bucket);
            SharedPrefManager.saveCustomBucket(context, experimentName, bucket);
            this.container.updateBucket(context, experimentName, bucket);
        } else {
            this.mCustomExperimentMap.remove(experimentName);
            SharedPrefManager.removeBucket(context, experimentName);
            this.container.updateBucket(context, experimentName, mExperimentMap.get(experimentName));
        }
    }

    public boolean isCustomBucketAvailable(@NonNull String experimentName) {
        return mCustomExperimentMap.containsKey(experimentName);
    }

    public String getAutomaticVariant(String experiment) {
        return mExperimentMap.get(experiment) != null ? mExperimentMap.get(experiment).getName() : RocketBucket.VARIANT_NAME_DEFAULT;
    }

    public BucketService getBucketService() {
        return bucketService;
    }

    public Bucket getBucket(@NonNull String experimentName) {
        return  mExperimentMap.get(experimentName);
    }
}
