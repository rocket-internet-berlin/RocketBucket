package de.rocketinternet.android.bucket;

import android.content.Context;

import java.util.Map;

import de.rocketinternet.android.bucket.models.Bucket;

/**
 * @author Sameh Gerges
 */
interface BucketsContainer {
    void onBucketsRetrieved(Context context, Map<String, Bucket> buckets, Throwable error);
    void updateBucket(Context context, String experimentName, Bucket bucket);
}
