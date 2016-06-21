package de.rocketinternet.android.bucket;

import android.content.Context;
import android.provider.Settings;

import java.util.Map;

import de.rocketinternet.android.bucket.models.Bucket;
import de.rocketinternet.android.bucket.network.NetworkTask;

/**
 * @author Sameh Gerges
 */
public class BucketProviderImpl implements BucketsProvider {

    @Override
    public void loadBuckets(final Context context, final BucketsContainer container) {
        NetworkTask.updateLatestBucket(context, RocketBucket.CONFIG.getEndpoint(), RocketBucket.CONFIG.getApiKey(),
            getDeviceId(context), new NetworkTask.Callback<Map<String, Bucket>>() {
                @Override
                public void onCompletion(Map<String, Bucket> response, Throwable error) {
                    container.onBucketsRetrieved(context, response, error);
                }

        });
    }

    static private String getDeviceId(Context context){
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}


