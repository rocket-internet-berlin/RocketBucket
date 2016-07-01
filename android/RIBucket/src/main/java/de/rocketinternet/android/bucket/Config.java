package de.rocketinternet.android.bucket;

import android.content.Context;
import android.text.TextUtils;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * @author Sameh Gerges
 */

public class Config {
    static private final String DIR_NAME_CACHING = "rocket_bucket_cache";
    static public final int CACHE_SIZE = 1 * 1024 * 1024; // 1 MiB

    private String mEndpoint;
    private String mApiKey;
    private long mReadTimeout;
    private boolean mDebug;

    private Config() {

    }

    public String getApiKey() {
        return mApiKey;
    }

    public String getEndpoint() {
        return mEndpoint;
    }

    public long getReadTimeout() {
        return mReadTimeout;
    }

    public boolean isBlockAppTillExperimentsLoaded(){
        return  mReadTimeout > 0;
    }

    public boolean isDebug(){
        return mDebug;
    }

    public File getCachingDir(Context context) {
        return new File(context.getCacheDir(), DIR_NAME_CACHING);
    }

    public File getBucketsCachingFile(Context context){
        return new File(getCachingDir(context), "rocket_bucket.local");
    }


    public static final class Builder {
        private Config config;

        public Builder() {
            config = new Config();
        }

        /**
         * @param apiKey provided by RocketBucket server for more info https://github.com/rocket-internet-berlin/RocketBucket
         */
        public Builder apiKey(String apiKey) {
            config.mApiKey = apiKey;
            return this;
        }

        public Builder endpoint(String val) {
            config.mEndpoint = val;
            return this;
        }

        /**
         * If it value greater than 0, it will block {@code RocketBucket.initialize} caller thread till loading experiments data.
         * This blocking behaviour will happen only at first time loading experiments. Otherwise will use local cached experiments till loading fresh copy.
         */
        public Builder blockAppTillExperimentsLoaded(int timeout, TimeUnit unit) {
            if (timeout < 0) throw new IllegalArgumentException("timeout < 0");
            if (unit == null) throw new NullPointerException("unit == null");
            long millis = unit.toMillis(timeout);
            if (millis > Integer.MAX_VALUE) throw new IllegalArgumentException("Timeout too large.");
            if (millis == 0 && timeout > 0) throw new IllegalArgumentException("Timeout too small.");

            config.mReadTimeout = millis;

            return this;
        }

        /**
         * @param isDebug value to indicate whither or not to show debugging view on different activities in order to mannually test different buckets while
         *                running the app without server code change
         * @return
         */
        public Builder debugMode(boolean isDebug){
            config.mDebug = isDebug;
            return this;
        }

        public Config build() {
            if (TextUtils.isEmpty(config.mApiKey) || TextUtils.isEmpty(config.mEndpoint) ) {
                throw new IllegalStateException("endpoint and api key cannot be null! or empty");
            }

            return config;
        }
    }
}
