package de.rocketinternet.android.bucket;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import de.rocketinternet.android.bucket.models.Bucket;

/**
 * @author Sameh Gerges
 */
public class EditableBucketsProvider implements BucketsProvider, BucketsContainer {

    private Map<String, Bucket> mExperimentMap;
    private Map<String, Bucket> mCustomExperimentMap;

    private BucketsContainer container;

    public EditableBucketsProvider() {
        this.mExperimentMap = new HashMap<>();
        this.mCustomExperimentMap = new HashMap<>();
    }

    @Override
    public void loadBuckets(Context context, BucketsContainer container) {
        this.container = container;
        new BucketProviderImpl().loadBuckets(context, this);
    }

    @Override
    public void onBucketsRetrieved(Context context, Map<String, Bucket> buckets, Throwable error) {

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
        this.container.onBucketsRetrieved(context, buckets, error);
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
}
