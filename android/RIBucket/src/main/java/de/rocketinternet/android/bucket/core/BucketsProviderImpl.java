package de.rocketinternet.android.bucket.core;

import android.content.Context;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import de.rocketinternet.android.bucket.Config;
import de.rocketinternet.android.bucket.network.BucketService;
import de.rocketinternet.android.bucket.network.JsonParser;

/**
 * This is the mean provider to download buckets from backend
 *
 * Implementation is providing to levels of caching
 * - HTTP caching which is done by http stack (OKHTTP) and follow standard caching rules
 * - Application level caching, which save download buckets to permanent file under application caching folder. This file never expire.
 *
 * Application level caching is used to have very quick access to experiment details
 *
 * Download behavior is
 * - in case of there is no cached data (first run or can't reach backend before or delete app data) application force calling thread to wait till first request is
 * completed. Blocker waiting behavior and timeout can be enabled / disabled from {@link Config}
 * -
 * @author Sameh Mikhail
 */
public class BucketsProviderImpl implements BucketsProvider {

    private BucketService bucketService;

    public BucketsProviderImpl(BucketService bucketService) {
        this.bucketService = bucketService;
    }

    @Override
    public void loadBuckets(final Context context, final Config config, final BucketsContainer container) {
        final BucketService.Callback callback = new BucketService.Callback<InputStream>() {
            @Override
            public void onSuccess(InputStream inputStream, boolean fromCache) throws IOException {
                onBucketsRetrieved(context, config, container, inputStream, fromCache);
            }
            @Override
            public void onFailure(Throwable t) {
                container.onBucketsRetrieved(context, null, t, BucketsContainer.SOURCE_NETWORK);
            }
        };

        boolean cachedData = loadCachedDataIfExists(context, config, container);

        if (!cachedData && config.isBlockAppTillExperimentsLoaded()) {
            callBucketServiceSync(context, config, callback);
        } else {
            bucketService.getBuckets(getDeviceId(context), callback);
        }
    }

    private void onBucketsRetrieved(Context context, Config config, BucketsContainer container, InputStream inputStream, boolean fromCache) throws IOException {
        Reader jsonReader = null;
        if (fromCache) { // no need to rewrite to disk, only in case of new data
            jsonReader = new InputStreamReader(inputStream, Charset.defaultCharset());
        } else {
            jsonReader = new InputStreamReaderWithPersistence(inputStream, Charset.defaultCharset(), config.getBucketsCachingFile(context));
        }
        container.onBucketsRetrieved(context, JsonParser.parseExperiments(jsonReader), null, fromCache? BucketsContainer.SOURCE_HTTP_CACHE : BucketsContainer.SOURCE_NETWORK);
        jsonReader.close();
    }

    private void callBucketServiceSync(final Context context, final Config config, final BucketService.Callback<InputStream> callback){
        //Null checking for unit testing
        if (Looper.getMainLooper() != null && Looper.getMainLooper().getThread() == Thread.currentThread()) { //Avoid network access on UI thread
            Thread loadingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    callBucketServiceSync(context, config, callback);
                }
            });
            loadingThread.start();
            try {
                loadingThread.join();
            } catch (InterruptedException e) {
                callback.onFailure(e);
            }
        } else {
            try {
                callback.onSuccess(bucketService.getBuckets(getDeviceId(context), config.getReadTimeout()), false);
            } catch (IOException e) {
                callback.onFailure(e);
            }
        }
    }

    @VisibleForTesting
    static private String getDeviceId(Context context){
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private boolean loadCachedDataIfExists(Context context, Config config, BucketsContainer container){
        File cachedData = config.getBucketsCachingFile(context);
        InputStream inputStream = null;
        if (cachedData.exists()) {
            try {
                inputStream = new FileInputStream(cachedData);
                container.onBucketsRetrieved(context, JsonParser.parseExperiments(new InputStreamReader(inputStream, Charset.defaultCharset())), null,
                        BucketsContainer.SOURCE_INTERNAL_CACHE);
                return true;
            } catch (Throwable t) {
                container.onBucketsRetrieved(context, null, t, BucketsContainer.SOURCE_INTERNAL_CACHE);
                return  false;
            } finally {
                try {
                    if (inputStream != null) inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return  false;
    }

    /**
     * This is simple StreamReader that delegate functionality to {@link java.io.InputStreamReader} with saving data from source input string to @writeTo file.
     * This is meant to be used in cases where input stream need to be read in memory and also persisted to desk. Normal implementation will read the while stream in
     * memory then save it to desk.
     *
     * So this implementation is much memory efficient but it may be less slower as it writes to disk. But since this will be a required functionality then we will
     * have this drawback anyway but now it has better memory consumption. And less iterations
     */
    private class InputStreamReaderWithPersistence extends InputStreamReader{
        private OutputStreamWriter outputStreamWriter;

        InputStreamReaderWithPersistence(InputStream inputStream, Charset encoding, File writeTo) throws UnsupportedEncodingException, FileNotFoundException {
            super(inputStream, encoding);
            this.outputStreamWriter = new OutputStreamWriter(new FileOutputStream(writeTo)) ;
        }

        @Override
        public int read() throws IOException {
            int read = super.read();
            outputStreamWriter.write(read);
            return read;
        }

        @Override
        public int read(char[] buffer, int offset, int count) throws IOException {
            int size = super.read(buffer, offset, count);
            outputStreamWriter.write(buffer, offset, count);
            return size;
        }

        @Override
        public void close() throws IOException {
            super.close();
            outputStreamWriter.close();
        }
    }
}


