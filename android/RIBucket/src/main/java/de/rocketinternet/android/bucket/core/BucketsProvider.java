package de.rocketinternet.android.bucket.core;

import android.content.Context;

/**
 * @author Sameh Gerges
 */
public interface BucketsProvider {
    void loadBuckets(Context context, Config config, BucketsContainer container);
}
