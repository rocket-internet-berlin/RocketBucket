package de.rocketinternet.android.bucket.core;

import android.content.Context;
import android.support.annotation.IntDef;

import java.util.Map;

import de.rocketinternet.android.bucket.models.Bucket;

/**
 * @author Sameh Gerges
 */
public interface BucketsContainer {

    static public final int SOURCE_INTERNAL_CACHE = 1;
    static public final int SOURCE_HTTP_CACHE = 2;
    static public final int SOURCE_NETWORK = 3;

    @IntDef ({SOURCE_HTTP_CACHE, SOURCE_INTERNAL_CACHE, SOURCE_NETWORK})
    @interface BucketsSource {}

    void onBucketsRetrieved(Context context, Map<String, Bucket> buckets, Throwable error, @BucketsSource  int source);
    void updateBucket(Context context, String experimentName, Bucket bucket);
}
