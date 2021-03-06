package de.rocketinternet.android.bucket.ui;

import android.content.Context;
import android.support.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.rocketinternet.android.bucket.core.EditableBucketsProvider;
import de.rocketinternet.android.bucket.models.Bucket;
import de.rocketinternet.android.bucket.models.Experiment;


/**
 * Created by mohamed.elawadi on 19/04/16.
 */
 public final class BucketDetailsPresenter implements BucketDetailsContract.UserActionsListener {
    private final BucketDetailsContract.View mViewController;
    private final Context mContext;

    final BucketsActivity.BucketPJO mBucket;
    final EditableBucketsProvider mBucketsProvider;

    public BucketDetailsPresenter(Context context, BucketsActivity.BucketPJO bucketPJO, BucketDetailsContract.View viewController, EditableBucketsProvider bucketsProvider) {
        //add check if intent doesn't contain target variables throw exception if not
        this.mViewController = viewController;
        this.mBucket = bucketPJO;
        this.mBucketsProvider = bucketsProvider;
        this.mContext = context;

        onExperimentDataReady();
    }

    public String getCurrentExperimentName() {
        return mBucket.getExperimentName();
    }

    public void onExperimentDataReady() {
        Experiment experiment = mBucket.getExperiment();
        if (experiment == null) {
            throw new RuntimeException("Current experiment not found!, all experiments doesn't contain current experiment! API inconsistent!");
        }

        String experimentName = experiment.getName();
        boolean isManual = mBucketsProvider.isCustomBucketAvailable(experimentName);
        mViewController.updateSelectionMethod(isManual, mBucketsProvider.getAutomaticVariant(experimentName));
        Map<String, Map<String, String>> variants = getAllVariants(experiment.getBuckets());//for spinner initialization
        List<String> bucketListAsString = getVariantsAsStrings(variants);
        //throw exception in case no experiment found

        mViewController.updateUI(bucketListAsString, bucketListAsString.indexOf(mBucketsProvider.getBucket(experimentName).getName()), experimentName);
    }


    public List<String> getVariantsAsStrings(Map<String, Map<String, String>> variants) {
        List<String> list = new ArrayList<>();
        for (Map.Entry variant : variants.entrySet()) {
            list.add((String) variant.getKey());
        }
        return list;
    }

    public Bucket getVariantByName(String variantName) {
        if (mBucket.bucket == null) {
            throw new NullPointerException("experiment name cannot be null!");
        }
        for (Bucket bucket : getCurrentExperiment().getBuckets()) {
            if (bucket.getName().equals(variantName)) {
                return bucket;
            }
        }

        return null;
    }

    public Map<String, Map<String, String>> getAllVariants(List<Bucket> buckets) {
        Map<String, Map<String, String>> variants = new HashMap<>();
        for (Bucket bucket : buckets) {
            variants.put(bucket.getName(), bucket.getData());
        }
        return variants;
    }

    @Override
    public void onSaveClicked(boolean isAutomatic, String selectedItem) {
        if (isAutomatic) {
            mBucketsProvider.updateBucket(mContext, getCurrentExperimentName(), null);
        } else {
            Bucket selectedBucket = getVariantByName(selectedItem);
            mBucketsProvider.updateBucket(mContext, getCurrentExperimentName(), selectedBucket);
        }

        mBucket.isAutomatic = isAutomatic;
    }

    @VisibleForTesting
    protected Experiment getCurrentExperiment() {
        return mBucket.getExperiment();
    }

    @VisibleForTesting
    protected boolean isManualExpirment() {
        return mBucketsProvider.isCustomBucketAvailable(getCurrentExperimentName());
    }
}