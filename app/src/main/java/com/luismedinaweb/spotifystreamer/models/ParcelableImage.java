package com.luismedinaweb.spotifystreamer.models;

import android.os.Parcel;
import android.os.Parcelable;

import kaaes.spotify.webapi.android.models.Image;

/**
 * Created by Luis on 6/18/2015.
 */
public class ParcelableImage extends Image implements Parcelable {

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<ParcelableImage> CREATOR = new Parcelable.Creator<ParcelableImage>() {
        public ParcelableImage createFromParcel(Parcel in) {
            return new ParcelableImage(in);
        }

        public ParcelableImage[] newArray(int size) {
            return new ParcelableImage[size];
        }
    };

    public ParcelableImage(int height, int width, String url){
        this.height = height;
        this.width = width;
        this.url = url;
    }

    public ParcelableImage(Parcel in){
        height = in.readInt();
        width = in.readInt();
        url = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(height);
        dest.writeInt(width);
        dest.writeString(url);
    }
}
