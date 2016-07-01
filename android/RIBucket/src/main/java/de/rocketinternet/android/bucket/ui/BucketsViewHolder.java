package de.rocketinternet.android.bucket.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.rocketinternet.android.bucket.R;

/**
 * Created by mohamed.elawadi on 12/04/16.
 */
public class BucketsViewHolder extends RecyclerView.ViewHolder {
    public BucketsViewHolder(View itemView) {
        super(itemView);
    }

    public BucketsViewHolder(LayoutInflater layoutInflater, ViewGroup parent) {
        super(layoutInflater.inflate(R.layout.item_bucket, parent, false));
    }

    public void onBindViewHolder(BucketsActivity.BucketPJO bucket, final BucketsAdapter.OnItemClickListener onItemClickListener) {
        itemView.setTag(bucket);
        Context context = itemView.getContext();
        ((TextView) itemView.findViewById(R.id.bucket_experiment_name)).setText(context.getString(R.string.bucket_item_experimentName, bucket.getExperimentName()));
        ((TextView) itemView.findViewById(R.id.bucket_variant_name)).setText(context.getString(R.string.bucket_item_selectedBucket, bucket.getVariantName()));
        ((TextView) itemView.findViewById(R.id.bucket_selection_method)).setText(context.getString(R.string.bucket_item_selectionMode, context.getString(bucket
                .isAutomatic() ? R.string.experiment_automatic : R
                .string.experiment_mannual)));
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClickListener.onItemClick((BucketsActivity.BucketPJO) v.getTag());
            }
        });
    }
}
