package de.rocketinternet.android.bucket;

import android.app.Application;
import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Map;

import de.rocketinternet.android.bucket.models.Bucket;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test for the Base Class {@link RocketBucket} which consider a main class
 */
@RunWith(MockitoJUnitRunner.class)
public class RocketBucketTest {
    private static final boolean DEBUG_ENABLED = true;
    @Mock Context mContext;
    @Mock Application mApplication;
    @Captor
    private ArgumentCaptor<RocketBucketContainer> mContainerCaptor;
    private static final String DEFAULT_APIKEY = "298492849283932";
    private static final String DEFAULT_ENDPONT = "http://bucket.com";
    Map<String, String> mResultCallbackExpirment;
    Throwable mResultCallbackThrowable;
    /*
        main callback for client to return the
     */

    RocketBucket mRocketBucket;

    @Before
    public void initialize() {
        BucketsProvider mockData = new BucketsProvider() {
            @Override
            public void loadBuckets(Context context, BucketsContainer container) {
                container.onBucketsRetrieved(context, MockBuilder.getMockedDefaultLatestBucket(), null);
            }
        };
        mRocketBucket = new RocketBucket(DEFAULT_ENDPONT, DEFAULT_APIKEY, mockData, null);
        mRocketBucket.updateLatestBuckets(mContext);
        RocketBucket.setInstance(mRocketBucket);//for setting provider manually !
        mResultCallbackThrowable = null;
        mResultCallbackExpirment = null;
    }

    @After
    public void tearDown() {
        RocketBucket.killTheBucket();
    }


    @Test
    public void testGetVariant_withExistingExperiment_shouldReturnTheVariant() throws Exception {
        assertEquals(MockBuilder.getDefaultBucketName(), RocketBucket.getBucketName(MockBuilder.getDefaultExpName()));
    }


    @Test()
    public void initialize_WithValidUrlAndAPIKey_ShouldnotThrowException() {
        RocketBucket.initialize(mContext, DEFAULT_ENDPONT, DEFAULT_APIKEY, null, false);
        assertNotNull(RocketBucket.getInstance());
    }

    @Test(expected = RuntimeException.class)
    public void initialize_withNullEndpoint_ShouldThrowException()
    {
        RocketBucket.killTheBucket();
        RocketBucket.initialize(mApplication, "", DEFAULT_APIKEY, null, DEBUG_ENABLED);
    }

    @Test(expected = RuntimeException.class)
    public void initialize_withNullAPIKey_ShouldThrowException() {
        RocketBucket.killTheBucket();
        RocketBucket.initialize(mApplication, DEFAULT_ENDPONT, "", null, DEBUG_ENABLED);

    }

    @Test
    public void testIsDebug_initializeWithDebugFalse_shouldReturnFalse() throws Exception {
        RocketBucket.killTheBucket();
        RocketBucket.initialize(mContext, DEFAULT_ENDPONT, DEFAULT_APIKEY, null, false);
        assertFalse(RocketBucket.isDebug());
    }

    @Test
    public void testIsDebug_initializeWithDebugTrue_shouldReturnTrue() throws Exception {
        RocketBucket.initialize(mApplication, DEFAULT_ENDPONT, DEFAULT_APIKEY, null, true);
        assertTrue(RocketBucket.isDebug());
    }


    @Test
    public void testGetVariant_whileExpirmentDoesntExist_shouldReturnTheDefaultVarient() throws Exception {
        RocketBucket.getInstance().updateLatestBuckets(mContext);
        assertEquals(RocketBucket.getBucketName("SomeExperiment"), RocketBucket.VARIANT_NAME_DEFAULT);
    }

    @Test
    public void testOnUnexpectedError() throws Exception {

    }

    @Test
    public void testGetCurrentExperiments() throws Exception {

    }

    @Test
    public void testGetCustomExperments() throws Exception {

    }

    @Test(expected = RuntimeException.class)
    public void testUpdateLatestBuckets_withoutInitialization_shouldThrowException() throws Exception {
        RocketBucket.killTheBucket();
        RocketBucket.getInstance().updateLatestBuckets(mContext);
    }



    @Test
    public void testGetBucketName_givingVarientInSameExperimentIsNotManullySellected_shouldReturnAutomaticBucket() throws Exception {
        assertEquals(MockBuilder.getDefaultBucketName(), RocketBucket.getBucketName(MockBuilder.getDefaultExpName()));
    }

    @Test
    public void testGetBucketName_givingVarientInSameExperimentISManullySellected_shouldReturnTheMannuallySelectedOne() throws Exception {
        RocketBucket.getInstance().updateBucket(mContext, MockBuilder.getDefaultExpName(), new Bucket("CUSTOM_VARIENT_NAME", Collections.EMPTY_MAP));
        assertEquals("CUSTOM_VARIENT_NAME", RocketBucket.getBucketName(MockBuilder.getDefaultExpName()));
    }

    @Test
    public void testGetBucketsProvider_onDebugingEnabled_shouldReturnDebuggingProvoider() {
        RocketBucket.killTheBucket();//to simulate what happening in first initialize
        RocketBucket.initialize(mApplication, DEFAULT_ENDPONT, DEFAULT_APIKEY, null, DEBUG_ENABLED);
        assertThat(RocketBucket.getInstance().getBucketsProvider(), instanceOf(EditableBucketsProvider.class));
    }

    @Test
    public void testGetBucketsProvider_onDebugingDisabled_shouldReturnDefaultProvoider() {
        RocketBucket.killTheBucket();//to simulate what happening in first initialize
        RocketBucket.initialize(mApplication, DEFAULT_ENDPONT, DEFAULT_APIKEY, null, !DEBUG_ENABLED);
        assertThat(RocketBucket.getInstance().getBucketsProvider(), instanceOf(BucketProviderImpl.class));
    }

    /*
        @Test
        public void testGetOriginalVariantName() throws Exception {

        }

        @Test
        public void testGetVariantValue() throws Exception {

        }
    */
    @Test
    public void testUpdateBucket_withCustomBucket_shouldUpdateCustomBucket() throws Exception {
        RocketBucket.getInstance().updateBucket(mContext, MockBuilder.getDefaultExpName(), new Bucket("CUSTOM_VARIENT_NAME_SET", Collections.EMPTY_MAP));
        assertEquals("CUSTOM_VARIENT_NAME_SET", RocketBucket.getBucketName(MockBuilder.getDefaultExpName()));
    }

    @Test
    public void testGetExtraByName_givenAnExtraName_shouldReturnExtraName() throws Exception {
        //assuming we have color extra initalized
        assertEquals(MockBuilder.COLOR_VALUE, RocketBucket.getExtraByName(MockBuilder.getDefaultExpName(), MockBuilder.COLOR_KEY, "default"));
    }

    @Test
    public void callbackContainer_givenSomeDataReturned_returnSameData() throws Exception {
        RocketBucket.killTheBucket();
        BucketsProvider mockData = new BucketsProvider() {
            @Override
            public void loadBuckets(Context context, BucketsContainer container) {
                container.onBucketsRetrieved(context, MockBuilder.getMockedDefaultLatestBucket(), null);
            }
        };

        mResultCallbackExpirment = null;
        mRocketBucket = new RocketBucket(DEFAULT_ENDPONT, DEFAULT_APIKEY, mockData, new RocketBucketContainer() {
            @Override
            public void onUnexpectedError(Throwable t) {

            }

            @Override
            public void onExperimentDataReady(Map<String, String> experiments) {
                mResultCallbackExpirment = experiments;
            }
        });
        RocketBucket.setInstance(mRocketBucket);//for setting provider manually !
        mRocketBucket.updateLatestBuckets(mContext);
        assertNotNull(mResultCallbackExpirment);
        assertEquals(1, mResultCallbackExpirment.size());
        assertEquals(mResultCallbackExpirment.get(MockBuilder.getDefaultExpName()), MockBuilder.getDefaultBucket().getName());
    }

    @Test
    public void callbackContainer_givenExceptionHappend_CallbackOnUnExpectedError() throws Exception {
        RocketBucket.killTheBucket();
        BucketsProvider mockData = new BucketsProvider() {
            @Override
            public void loadBuckets(Context context, BucketsContainer container) {

                container.onBucketsRetrieved(context, null, new Exception());
            }
        };

        mRocketBucket = new RocketBucket(DEFAULT_ENDPONT, DEFAULT_APIKEY, mockData, new RocketBucketContainer() {
            @Override
            public void onUnexpectedError(Throwable t) {
                mResultCallbackThrowable = t;
            }

            @Override
            public void onExperimentDataReady(Map<String, String> experiments) {
                mResultCallbackExpirment = experiments;
            }
        });
        RocketBucket.setInstance(mRocketBucket);//for setting provider manually !
        mRocketBucket.updateLatestBuckets(mContext);
        assertNotNull(mResultCallbackThrowable);
        assertNull(mResultCallbackExpirment);
    }

    @Test
    public void testUpdateExperimentList_givenEmptyBucketList_shouldNotReturnAnything() throws Exception {
        RocketBucket.killTheBucket();
        BucketsProvider mockData = new BucketsProvider() {
            @Override
            public void loadBuckets(Context context, BucketsContainer container) {
                container.onBucketsRetrieved(context, Collections.EMPTY_MAP, null);
            }
        };

        mResultCallbackExpirment = null;
        mRocketBucket = new RocketBucket(DEFAULT_ENDPONT, DEFAULT_APIKEY, mockData, new RocketBucketContainer() {
            @Override
            public void onUnexpectedError(Throwable t) {

            }

            @Override
            public void onExperimentDataReady(Map<String, String> experiments) {
                mResultCallbackExpirment = experiments;
            }
        });
        RocketBucket.setInstance(mRocketBucket);//for setting provider manually !
        mRocketBucket.updateLatestBuckets(mContext);
        assertNotNull(mResultCallbackExpirment);
        assertEquals(0, mResultCallbackExpirment.size());
    }

    @Test(expected = IllegalStateException.class)
    public void getInstance_whenNotInitalized_shoulThrowIllegalStateException() {
        RocketBucket.killTheBucket();
        assertNull(RocketBucket.getInstance());
    }
/*
    @Test
    public void testExperimentsLoaded() {
        initialize();
        assertEquals(RocketBucket.getInstance().getBucket(MockBuilder.getDefaultExpName()).getName(), MockBuilder.getDefaultBucketName());
    }*/

}