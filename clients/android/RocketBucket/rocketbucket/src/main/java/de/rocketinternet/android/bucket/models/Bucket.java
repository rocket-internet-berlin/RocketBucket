package de.rocketinternet.android.bucket.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mohamed.elawadi on 12/04/16.
 */
public class Bucket implements Parcelable {

    protected int mPercent = 0;
    protected String mName;
    protected Map<String, String> mData = null;

    public Bucket(String variantName, int percentage, Map<String, String> data) {
        this.mName = variantName;
        this.mPercent = percentage;
        this.mData = data;
    }

    protected Bucket(Parcel in) {
        mPercent = in.readInt();
        mName = in.readString();
    }

    public Bucket(String bucketName) {
        this (bucketName, new HashMap());
    }

    public Bucket(String bucketName, Map map) {
        this.mName = bucketName;
        this.mData = map;
    }
    public int getPercent() {
        return mPercent;
    }
    public Map<String, String> getData() {
        return mData;
    }

    public static final Creator<Bucket> CREATOR = new Creator<Bucket>() {
        @Override
        public Bucket createFromParcel(Parcel in) {
            return new Bucket(in);
        }

        @Override
        public Bucket[] newArray(int size) {
            return new Bucket[size];
        }
    };

    public String getName() {
        return mName;
    }

    public String getExtraByName(String key, String defaultValue) {
        return mData.containsKey(key) ? mData.get(key) : defaultValue;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mPercent);
        dest.writeString(mName);
    }

}
