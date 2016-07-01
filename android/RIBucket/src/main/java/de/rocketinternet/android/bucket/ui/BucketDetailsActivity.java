package de.rocketinternet.android.bucket.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;

import de.rocketinternet.android.bucket.R;
import de.rocketinternet.android.bucket.core.EditableBucketsProvider;

/**
 * Created by mohamed.elawadi on 12/04/16.
 */
public class BucketDetailsActivity extends AppCompatActivity implements View.OnClickListener, BucketDetailsContract.View {

    public static final String KEY_BUCKET_CONTENT = "KEY_BUCKET_CONTENT";

    TextView experimentNameTextView;
    RadioGroup selectionTypeRadioGroup;
    Spinner bucketsListSpinner;
    Button saveButton;

    EditableBucketsProvider bucketsProvider;
    BucketDetailsPresenter presenter;

    public static Intent newIntent(Context context, BucketsActivity.BucketPJO bucket) {
        Intent intent = new Intent(context, BucketDetailsActivity.class);
        intent.putExtra(KEY_BUCKET_CONTENT, bucket);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bucket_details);
        initViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
        BucketsActivity.BucketPJO bucketPJO = getIntent().getParcelableExtra(BucketDetailsActivity.KEY_BUCKET_CONTENT);
        presenter = new BucketDetailsPresenter(this, bucketPJO, this, bucketsProvider);
    }

    private void initViews() {
        experimentNameTextView = (TextView) findViewById(R.id.bucketDetails_experimentName);
        selectionTypeRadioGroup = (RadioGroup) findViewById(R.id.bucketDetails_selectionMethod);
        bucketsListSpinner = (Spinner) findViewById(R.id.bucketDetails_variant);
        saveButton = (Button) findViewById(R.id.bucketDetails_save);
        saveButton.setOnClickListener(this);
    }

    @Override
    public void updateUI(final List<String> buckets, final int selectedExperimentPosition, final String experimentName) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ArrayAdapter<String> bucketsSinnerAdapter = new ArrayAdapter<>(BucketDetailsActivity.this,
                        android.R.layout.simple_spinner_item, buckets);
                bucketsSinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                bucketsListSpinner.setAdapter(bucketsSinnerAdapter);
                bucketsListSpinner.setSelection(selectedExperimentPosition);
                experimentNameTextView.setText(getString(R.string.bucket_item_experimentName, experimentName));
                bucketsSinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            }
        });
    }

    @Override
    public void updateSelectionMethod(final boolean isManual, final String automaticBucketName) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                selectionTypeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        if (checkedId == R.id.bucket_selection_automatic) {
                            bucketsListSpinner.setEnabled(false);
                        } else {
                            bucketsListSpinner.setEnabled(true);
                        }
                    }
                });
                selectionTypeRadioGroup.check(isManual ? R.id.bucket_selection_manual : R.id.bucket_selection_automatic);
                RadioButton automatic = (RadioButton) findViewById(R.id.bucket_selection_automatic);
                automatic.setText(getString(R.string.experiment_automatic_withValue, automaticBucketName));
            }
        });

    }

    @Override
    public void onClick(View v) {
        presenter.onSaveClicked(selectionTypeRadioGroup.getCheckedRadioButtonId() == R.id.bucket_selection_automatic, (String) bucketsListSpinner.getSelectedItem());
        setResult(RESULT_OK);
        finish();
    }

    public void setBucketsProvider(EditableBucketsProvider bucketsProvider) {
        this.bucketsProvider = bucketsProvider;
    }
}
