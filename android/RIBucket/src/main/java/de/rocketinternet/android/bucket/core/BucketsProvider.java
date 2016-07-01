package de.rocketinternet.android.bucket.core;

import android.content.Context;

import de.rocketinternet.android.bucket.Config;

/**
 * @author Sameh Gerges
 */
public interface BucketsProvider {
    void loadBuckets(Context context, Config config, BucketsContainer container);
}
