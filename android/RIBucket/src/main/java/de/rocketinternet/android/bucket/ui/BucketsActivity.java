package de.rocketinternet.android.bucket.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.rocketinternet.android.bucket.R;
import de.rocketinternet.android.bucket.core.EditableBucketsProvider;
import de.rocketinternet.android.bucket.models.Bucket;
import de.rocketinternet.android.bucket.models.Experiment;
import de.rocketinternet.android.bucket.network.BucketService;


/**
 * Created by mohamed.elawadi on 12/04/16.
 */
public class BucketsActivity extends AppCompatActivity implements BucketsAdapter.OnItemClickListener {
    private static final String TAG = "BUCKETS ACTIVITY";
    private static final int VARIANT_REQUEST_CODE = 1;
    private BucketsAdapter adapter;

    /**
     * Attribute is inject just after onCreate being called
     */
    private EditableBucketsProvider bucketsProvider;

    public static Intent newIntent(Context context) {
        return new Intent(context, BucketsActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_bucket_list);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        adapter = new BucketsAdapter(this);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.bucketsRecyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateCurrentExperimentsAdapterData();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void updateCurrentExperimentsAdapterData() {
        bucketsProvider.getBucketService().getBuckets(new BucketService.Callback<List<Experiment>>() {
            @Override
            public void onSuccess(List<Experiment> data, boolean fromCache) throws IOException {
                final List<BucketPJO> currentBucketList = new ArrayList<>();
                for (Experiment experiment : data) {
                    currentBucketList.add(new BucketPJO(experiment, bucketsProvider.getBucket(experiment.getName()),
                            !bucketsProvider.isCustomBucketAvailable(experiment.getName())));
                }


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.setData(currentBucketList);
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                //TODO:: Display error message with retry button !
            }
        });
    }

    @Override
    public void onItemClick(BucketPJO item) {
        startActivityForResult(BucketDetailsActivity.newIntent(this, item), VARIANT_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            updateCurrentExperimentsAdapterData();
        }
    }

    public void setBucketsProvider(EditableBucketsProvider bucketsProvider) {
        this.bucketsProvider = bucketsProvider;
    }

    /**
     * PJO for adapter use
     */
    public static class BucketPJO implements Parcelable {
        Experiment experiment;
        Bucket bucket;
        boolean isAutomatic;

        public BucketPJO(Experiment exp, Bucket bucket, boolean isAutomatic) {
            this.experiment = exp;
            this.bucket = bucket;
            this.isAutomatic = isAutomatic;
        }

        protected BucketPJO(Parcel in) {
            experiment = in.readParcelable(Experiment.class.getClassLoader());
            bucket = in.readParcelable(Bucket.class.getClassLoader());
            isAutomatic = in.readByte() != 0;
        }

        public static final Creator<BucketPJO> CREATOR = new Creator<BucketPJO>() {
            @Override
            public BucketPJO createFromParcel(Parcel in) {
                return new BucketPJO(in);
            }

            @Override
            public BucketPJO[] newArray(int size) {
                return new BucketPJO[size];
            }
        };

        public String getExperimentName() {
            return experiment.getName();
        }

        public boolean isAutomatic() {
            return isAutomatic;
        }

        public String getVariantName() {
            return bucket.getName();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(experiment, flags);
            dest.writeParcelable(bucket, flags);
            dest.writeByte((byte) (isAutomatic ? 1 : 0));
        }

        public Experiment getExperiment() {
            return experiment;
        }
    }


}
