package de.rocketinternet.android.bucket;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

/**
 * Unit test for the Base Class {@link RocketBucket} which consider a main class
 */
@RunWith(MockitoJUnitRunner.class)
public class RocketBucketTest {
    private static final boolean DEBUG_ENABLED = true;
    @Mock Context mContext;

    private static final String DEFAULT_APIKEY = "298492849283932";
    private static final String DEFAULT_ENDPONT = "http://bucket.com";
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
    }

    @After
    public void tearDown() {
        RocketBucket.killTheBucket();
    }

//    @Test(expected = RuntimeException.class)
//    public void testGetInstance_withoutInitialization_shouldThrowanException() {
//        RocketBucket.getInstance();
//    }

    @Test
    public void testGetVariant_withExistingExperiment_shouldReturnTheVariant() throws Exception {
        assertEquals(MockBuilder.getDefaultBucketName(), RocketBucket.getBucketName(MockBuilder.getDefaultExpName()));
    }


//    @Test()
//    public void initialize_WithValidUrlAndAPIKey_ShouldnotThrowException() {
//        RocketBucket.initialize(mContext, DEFAULT_ENDPONT, DEFAULT_APIKEY, mCallback);
//        assertNotNull(RocketBucket.getInstance());
//    }

    @Test(expected = RuntimeException.class)
    public void initialize_withNullEndpoint_ShouldThrowException() {
        RocketBucket.initialize(mContext, null, DEFAULT_APIKEY, null,DEBUG_ENABLED);
    }

    @Test(expected = RuntimeException.class)
    public void initialize_withNullAPIKey_ShouldThrowException() {
        RocketBucket.initialize(mContext, DEFAULT_ENDPONT, null, null,DEBUG_ENABLED);

    }

    @Test
    public void testIsDebug_initializeWithDebugFalse_shouldReturnFalse() throws Exception {
        RocketBucket.initialize(mContext, DEFAULT_ENDPONT, DEFAULT_APIKEY, null,false);
        assertFalse(RocketBucket.isDebug());
    }

    @Test
    public void testIsDebug_initializeWithDebugTrue_shouldReturnTrue() throws Exception {
        RocketBucket.initialize(mContext, DEFAULT_ENDPONT, DEFAULT_APIKEY, null,true);
        assertFalse(RocketBucket.isDebug());
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

//    @Test(expected = RuntimeException.class)
//    public void testUpdateLatestBuckets_withoutInitialization_shouldThrowException() throws Exception {
//        RocketBucket.getInstance().updateLatestBuckets(mContext);
//    }
 /*
    @Test()
    public void testUpdateLatestBuckets_withDefaultData_shouldUpdateCurrentExperimentData() throws Exception {
        BucketsProvider mockData = new BucketsProvider() {
            @Override
            public void loadBuckets(Context context, BucketsContainer container) {

            }
        };
        mRocketBucket = new RocketBucket(DEFAULT_ENDPONT, DEFAULT_APIKEY, mockData, null);
        mRocketBucket.updateLatestBuckets(mContext);
        RocketBucket.setInstance(mRocketBucket);
        assertTrue(RocketBucket.getInstance().getCurrentExperiments().isEmpty());
        RocketBucket.getInstance().updateLatestBuckets(mContext);
        assertEquals(RocketBucket.getInstance().getCurrentExperiments().size(), 1);
    }

    @Test(expected = RuntimeException.class)
    public void testGetAllExperiments_WithoutInitialization_shouldThrowAnExeption() throws Exception {
        RocketBucket.getInstance().getAllExperiments(mContext, mAllExperimentsContainer);
    }

    @Test()
    public void testGetAllExperiments_WithoutInitialization_shouldReturnMockedAllExperiment() {
        RocketBucket.initialize(mContext, DEFAULT_ENDPONT, DEFAULT_APIKEY, null);
        RocketBucket.getInstance().getAllExperiments(mContext, mAllExperimentsContainer);
        verify(mAllExperimentsContainer).onExperimentDataReady(anyList());
    }


    @Test
    public void testGetVariantName_givingVarientInSameExperimentIsNotManullySellected_shouldReturnAutomatic() throws Exception {
        RocketBucket.initialize(mContext, DEFAULT_ENDPONT, DEFAULT_APIKEY, null);
        RocketBucket.getInstance().updateLatestBuckets(mContext);
        assertEquals(MockBuilder.getDefaultExpName(), RocketBucket.getBucketName(MockBuilder.getDefaultExpName()));
    }

    @Test
    public void testGetVariantName_givingVarientInSameExperimentISManullySellected_shouldReturnTheMannuallySelectedOne() throws Exception {
        RocketBucket.initialize(mContext, DEFAULT_ENDPONT, DEFAULT_APIKEY, null);
        RocketBucket.getInstance().updateLatestBuckets(mContext);
        RocketBucket.getInstance().setCustomBucket(mContext, MockBuilder.getDefaultExpName(), new BucketBase("CUSTOM_VARIENT_NAME", Collections.EMPTY_MAP));
        assertEquals("CUSTOM_VARIENT_NAME", RocketBucket.getBucketName(MockBuilder.getDefaultExpName()));
    }

    @Test
    public void testGetOriginalVariantName() throws Exception {

    }

    @Test
    public void testGetVariantValue() throws Exception {

    }

    @Test
    public void testSetCustomVarient_withCustomVariant_shouldUpdateCustomVarient() throws Exception {
        RocketBucket.initialize(mContext, DEFAULT_ENDPONT, DEFAULT_APIKEY, null);
        RocketBucket.getInstance().setCustomBucket(mContext, MockBuilder.getDefaultExpName(), new BucketBase(MockBuilder.getDefaultExpName(), Collections.EMPTY_MAP));
        assertTrue(RocketBucket.getInstance().isCustomBucketAvailable(MockBuilder.getDefaultExpName()));
    }

    @Test
    public void testExperimentsLoaded() {
        initialize();
        assertEquals(RocketBucket.getInstance().getVariant(MockBuilder.getDefaultExpName()).getName(), MockBuilder.getDefaultBucketName());
    }*/

}