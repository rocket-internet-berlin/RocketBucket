package de.rocketinternet.android.bucket.core;

import android.content.Context;
import android.os.Looper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.nio.charset.Charset;

import de.rocketinternet.android.bucket.RocketBucket;
import de.rocketinternet.android.bucket.models.Bucket;
import de.rocketinternet.android.bucket.network.BucketService;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http.RealResponseBody;
import okio.Buffer;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit testing for {@link BucketsProviderImpl}
 * @author Sameh Gerges
 */
@RunWith(MockitoJUnitRunner.class)
public class BucketsProviderImplTest {
    private static final String DEFAULT_APIKEY = "298492849283932";
    private static final String DEFAULT_ENDPONT = "http://bucket.com";

    @Mock Context context;
    @Mock OkHttpClient httpClient;
    @Mock BucketService bucketService;
    @Mock RocketBucket rocketBucket;

    @Mock Call httpCall;Response httpResponse;
    @Captor ArgumentCaptor<Callback> captor;

    @Mock Config config;
    File cachingDir;
    File cachingFile;

    Buffer rawRespone;
    InputStream responseStream;
    BucketsProviderImpl bucketsProvider;

    OkHttpClient.Builder httpClientBuilder;

    @Before
    public void initialize() throws IOException {
        cachingDir = new File("./RocketBucketCache");
        cachingDir.mkdirs();

        cachingFile = new File(cachingDir, "buckets.json");

        httpClientBuilder = new OkHttpClient().newBuilder();
        rawRespone = new Buffer();
        rawRespone.writeString("{\"experiments\":[{\"name\":\"customer_support_tool_integration\",\"description\":\"\"," +
                "\"bucket\":{\"name\":\"intercom\"}}," +
                "{\"name\":\"catalog_filters\",\"description\":\"\",\"bucket\":{\"name\":\"disabled\"}},{\"name\":\"browse_tab_design_background_image\"," +
                "\"description\":\"\",\"bucket\":{\"name\":\"on\"}},{\"name\":\"rate_us_app\",\"description\":\"\",\"bucket\":{\"name\":\"challenger_stars\"}}]}",
                Charset.defaultCharset());

        responseStream = new StringBufferInputStream("{\"experiments\":[{\"name\":\"customer_support_tool_integration\",\"description\":\"\"," +
                "\"bucket\":{\"name\":\"intercom\"}}," +
                "{\"name\":\"catalog_filters\",\"description\":\"\",\"bucket\":{\"name\":\"disabled\"}},{\"name\":\"browse_tab_design_background_image\"," +
                "\"description\":\"\",\"bucket\":{\"name\":\"on\"}},{\"name\":\"rate_us_app\",\"description\":\"\",\"bucket\":{\"name\":\"challenger_stars\"}}]}");
        Request httpRequest = new Request.Builder()
                .url("http://lyke.co.id")
                .get()
                .build();
        httpResponse = new Response.Builder()
                .body(new RealResponseBody(null, rawRespone))
                .request(httpRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("Okay")
                .build();

        bucketsProvider = new BucketsProviderImpl(bucketService);
        when(httpClient.newCall(any(Request.class))).thenReturn(httpCall);
        when(httpCall.execute()).thenReturn(httpResponse);
        when(context.getCacheDir()).thenReturn(cachingDir);
        when(config.getBucketsCachingFile(context)).thenReturn(cachingFile);
        when(httpClient.newBuilder()).thenReturn(httpClientBuilder);

        when(config.getApiKey()).thenReturn(DEFAULT_APIKEY);
        when(config.getEndpoint()).thenReturn(DEFAULT_ENDPONT);
        //when(httpClientBuilder.build()).thenReturn(httpClient);
        doNothing().when(httpCall).enqueue(captor.capture());
    }

    /*@Test
    public void loadingWithNoCacheOld() throws IOException {
        Looper.prepareMainLooper();

        when(cachingFile.exists()).thenReturn(false);

        when(config.getReadTimeout()).thenReturn(20000l);
        when(config.getCachingDir(context)).thenReturn(cachingDir);
        when(config.getBucketsCachingFile(context)).thenReturn(cachingFile);


        RocketBucket rocketBucket = createBucket(config);
        bucketsProvider.loadBuckets(context, config, rocketBucket);

        verify(httpCall, times(1)).execute();
        //verify(rocketBucket).onBucketsRetrieved();
    }
*/
    @Test
    public void loadingWithNoCache() throws IOException {
        Looper.prepareMainLooper();

        cachingFile.delete();

        when(config.isBlockAppTillExperimentsLoaded()).thenReturn(true);
        when(bucketService.getBuckets(anyString(), anyLong())).thenReturn(responseStream);

        bucketsProvider.loadBuckets(context, config,  rocketBucket);

        assertTrue(cachingFile.exists()); //verify caching file created
        verify(bucketService, times(1)).getBuckets(anyString(), anyLong()); //verify sync loading
        //noinspection WrongConstant
        verify(rocketBucket, times(1)).onBucketsRetrieved(eq(context), anyMapOf(String.class, Bucket.class), (Throwable) eq(null), eq(BucketsContainer.SOURCE_NETWORK)); // verify buckets loaded from correct source
    }

    @Test
    public void loadingWithWithCache() throws IOException {

    }

}
