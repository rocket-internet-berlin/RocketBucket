package de.rocketinternet.android.bucket.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.rocketinternet.android.bucket.models.Bucket;
import de.rocketinternet.android.bucket.models.Experiment;
import de.rocketinternet.android.bucket.ui.BucketsActivity;

/**
 * Created by mohamed.elawadi on 21/04/16.
 */
public final class MockBuilder {

    private static final String DEFAULT_APIKEY = "298492849283932";
    private static final String DEFAULT_ENDPONT = "http://bucket.com";
    private static final String DFAULT_EXP_NAME = "expirment1";
    private static final String DEFAULT_BUCKET_NAME = "experiment1bucket1";
    private static final int DEFAULT_PERCENTAGE = 50;

    public static final String COLOR_KEY = "color";
    public static final String COLOR_VALUE = "#FFFFF";
    public static final String COLOR_BUCKET_NAME = "COLOR_BUCKET";
    private static Bucket bucket = null;

    public static List<Experiment> getAllExpirment() {
        Experiment experiment1 = buildExperiment(DFAULT_EXP_NAME, false, buildBuckets(getDefaultBucket(), buildBucket("experiment1bucket2", DEFAULT_PERCENTAGE,
                null)));
        Experiment experiment2 = buildExperiment("expirment2", false, buildBuckets(buildBucket("expirment2bucket1", 50, null), buildBucket("expirment2bucket2", 50,
                null)));
        return Arrays.asList(experiment1, experiment2);
    }

    private static List<Bucket> buildBuckets(Bucket... buckets) {
        List<Bucket> bucketList = new ArrayList<>();
        for (Bucket bucket :
                buckets) {
            bucketList.add(bucket);
        }
        return bucketList;
    }

    private static List<Bucket> buildVariants(Bucket... variants) {
        return null;
    }

    private static Bucket buildBucket(String bucketName, int percentage, List<Bucket> varients) {

        return new Bucket(bucketName, percentage, null);//we more interested on bucket name for now
    }

    private static Experiment buildExperiment(String name, boolean isEnabled, List<Bucket> buckets) {
        return new Experiment(name, isEnabled, buckets);
    }


    public static BucketsActivity.BucketPJO getBucketPJO(Experiment experiment, Bucket bucket, boolean isAutomatic) {
        return new BucketsActivity.BucketPJO(experiment == null ? getAllExpirment().get(0) : experiment, bucket == null ? getDefaultBucket() : bucket, isAutomatic);
    }

    public static Map<String, Bucket> getMockedDefaultLatestBucket() {
        Map<String, Bucket> variantMap = new HashMap<>();
        variantMap.put(DFAULT_EXP_NAME, getDefaultBucket());
        return variantMap;
    }

    public static String getDefaultExpName() {
        return DFAULT_EXP_NAME;
    }

    public static String getDefaultBucketName() {
        return DEFAULT_BUCKET_NAME;
    }

    public static Bucket getDefaultBucket() {
        if (bucket == null) {
            Map<String, String> extraMap = new HashMap<>();
            extraMap.put(COLOR_KEY, COLOR_VALUE);
            bucket = new Bucket(DEFAULT_BUCKET_NAME, 100, extraMap);
        }
        return bucket;
    }

    public static String getApiKey(){
        return DEFAULT_APIKEY;
    }

    public static String getEndpoint(){
        return DEFAULT_ENDPONT;
    }
}