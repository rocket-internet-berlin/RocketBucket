package de.rocketinternet.android.bucket.ui;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import de.rocketinternet.android.bucket.BucketsActivity;

/**
 * Created by mohamed.elawadi on 12/04/16.
 */
public class BucketsAdapter extends RecyclerView.Adapter<BucketsViewHolder> {
    List<BucketsActivity.BucketPJO> mBucketList = new ArrayList<>();
    final OnItemClickListener onItemClickListener;

    public BucketsAdapter(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;

    }

    @Override
    public BucketsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new BucketsViewHolder(LayoutInflater.from(parent.getContext()), parent);
    }

    @Override
    public void onBindViewHolder(BucketsViewHolder holder, int position) {
        holder.onBindViewHolder(mBucketList.get(position), onItemClickListener);

    }

    @Override
    public int getItemCount() {
        return mBucketList == null ? 0 : mBucketList.size();
    }

    public void setData(@NonNull List<BucketsActivity.BucketPJO> data) {
        mBucketList.clear();
        mBucketList.addAll(data);


        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onItemClick(BucketsActivity.BucketPJO item);
    }
}
