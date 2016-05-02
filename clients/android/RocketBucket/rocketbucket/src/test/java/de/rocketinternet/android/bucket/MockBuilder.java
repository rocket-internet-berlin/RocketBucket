package de.rocketinternet.android.bucket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.rocketinternet.android.bucket.models.Bucket;
import de.rocketinternet.android.bucket.models.Experiment;

/**
 * Created by mohamed.elawadi on 21/04/16.
 */
public class MockBuilder {

    private static final String DFAULT_EXP_NAME = "expirment1";
    private static final String DEFAULT_BUCKET_NAME = "experiment1bucket1";
    private static final int DEFAULT_PERCENTAGE = 50;

    public static List<Experiment> getAllExpirment() {
        Experiment experiment1 = buildExperiment(DFAULT_EXP_NAME, false, buildBuckets(buildBucket(DEFAULT_BUCKET_NAME, 50, null), buildBucket("experiment1bucket2", DEFAULT_PERCENTAGE,
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


    public static BucketsActivity.BucketPJO getBucketPJO(String name, String bucketName, boolean isAutomatic) {
        //return new BucketsActivity.BucketPJO(name == null ? DFAULT_EXP_NAME : name, bucketName == null ? DEFAULT_BUCKET_NAME : bucketName, isAutomatic);
        return null;
    }

    public static Map<String, Bucket> getMockedDefaultLatestBucket() {
        Map<String, Bucket> variantMap = new HashMap<>();
        variantMap.put(DFAULT_EXP_NAME, new Bucket(DEFAULT_BUCKET_NAME, Collections.EMPTY_MAP));//TODO: add more map data activate this feature
        return variantMap;
    }

    public static String getDefaultExpName() {
        return DFAULT_EXP_NAME;
    }

    public static String getDefaultBucketName() {
        return DEFAULT_BUCKET_NAME;
    }
}