package de.rocketinternet.android.bucket;

import android.content.Context;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Unit test for the implementation of {@link BucketDetailsPresenter}
 */
@RunWith(MockitoJUnitRunner.class)
public class BucketDetailsPresenterTest{
    @Mock Context mContext;
  /*  BucketDetailsPresenter mPresenter;
    @Mock BucketDetailsContract.View mViewController;


    @Mock Intent mIntent;
    @Captor ArgumentCaptor<RocketBucketContainer> apiResultCaptor;

    @After
    public void tearDown() throws Exception {
        mPresenter = null;
    }

    @Test
    public void testOnUnexpectedError() throws Exception {

    }

    @Test
    public void testOnExperimentDataReady_givenExperimentName_PresenterShouldSetTheName() throws Exception {
        RocketBucket.initialize(mContext, "endpoint", "apiKey", null);
        BucketsActivity.BucketPJO defaultPjo = MockBuilder.getBucketPJO(null, null, true);
        mPresenter = new BucketDetailsPresenter(mContext, Injection.provideBucketsRepository(), defaultPjo, mViewController);
        mPresenter.onExperimentDataReady(MockBuilder.getAllExpirment());
        assertEquals(mPresenter.getCurrentExperimentName(), MockBuilder.getDefaultExpName());

    }

    @Test
    public void testOnExperimentDataReady_givenCurrentExpirment_PresenterShouldHaveCurrentExpirment() throws Exception {
        RocketBucket.initialize(mContext, "endpoint", "apiKey", null);
        BucketsActivity.BucketPJO defaultPjo = MockBuilder.getBucketPJO(null, null, true);
        mPresenter = new BucketDetailsPresenter(mContext, Injection.provideBucketsRepository(), defaultPjo, mViewController);
        mPresenter.onExperimentDataReady(MockBuilder.getAllExpirment());
        assertNotNull(mPresenter.getCurrentExperiment());
    }

    @Test
    public void testOnExperimentDataReady_firstInitialization_selectionModeShouldBeAutomaticByDefault() throws Exception {
        RocketBucket.initialize(mContext, "endpoint", "apiKey", null);
        BucketsActivity.BucketPJO defaultPjo = MockBuilder.getBucketPJO(null, null, true);
        mPresenter = new BucketDetailsPresenter(mContext, Injection.provideBucketsRepository(), defaultPjo, mViewController);
        mPresenter.onExperimentDataReady(MockBuilder.getAllExpirment());
        assertFalse(mPresenter.isManualExpirment());
    }

    @Test
    public void testOnExperimentDataReady_selectionModeAutomatic_shouldAskUIToMakeItAutomatic() throws Exception {
        RocketBucket.initialize(mContext, "endpoint", "apiKey", null);
        BucketsActivity.BucketPJO defaultPjo = MockBuilder.getBucketPJO(null, null, true);
        mPresenter = new BucketDetailsPresenter(mContext, Injection.provideBucketsRepository(), defaultPjo, mViewController);
        mPresenter.onExperimentDataReady(MockBuilder.getAllExpirment());
        verify(mViewController).updateSelectionMethod(eq(false), anyString());
    }

    @Test
    public void testOnExperimentDataReady_selectedVarientAreFrist_shouldLocatePositionZeroOnTheSpinner() throws Exception {
        RocketBucket.initialize(mContext, "endpoint", "apiKey", null);
        BucketsActivity.BucketPJO defaultPjo = MockBuilder.getBucketPJO(null, null, true);
        mPresenter = new BucketDetailsPresenter(mContext, Injection.provideBucketsRepository(), defaultPjo, mViewController);
        mPresenter.onExperimentDataReady(MockBuilder.getAllExpirment());
        verify(mViewController).updateUI(anyList(), eq(0), anyString());
    }


    @org.junit.Test
    public void testGetCurrentExperimentFromAllExperiments_containingExperiment_shouldReturnExperiment() throws Exception {
    }

    @org.junit.Test
    public void testGetVariantsAsStrings() throws Exception {

    }

    @org.junit.Test
    public void testGetVariantByName() throws Exception {

    }

    @org.junit.Test
    public void testGetAllVariants() throws Exception {

    }

    @org.junit.Test
    public void testOnSaveClicked() throws Exception {

    }*/
}