package de.rocketinternet.android.bucket.ui;

import android.support.annotation.UiThread;

import java.util.List;

/**
 * Created by mohamed.elawadi on 19/04/16.
 */
public interface BucketDetailsContract {
    interface View {
        @UiThread
        void updateSelectionMethod(final boolean isManual, String originalVariantName);

        @UiThread
        void updateUI(List<String> buckets, int selectedBucketPosition, String experimentName);
    }

    interface UserActionsListener {
        void onSaveClicked(boolean isAutomatic, String selectedItem);
    }
}
