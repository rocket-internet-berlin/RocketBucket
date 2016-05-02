package de.rocketinternet.android.bucket;

import android.content.Context;

/**
 * @author Sameh Gerges
 */
public interface BucketsProvider {
    void loadBuckets(Context context, BucketsContainer container);
}
