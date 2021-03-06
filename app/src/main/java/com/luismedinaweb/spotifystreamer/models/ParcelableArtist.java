package com.luismedinaweb.spotifystreamer.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Image;

/**
 * Created by Luis on 6/18/2015.
 */
public class ParcelableArtist extends Artist implements Parcelable{

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<ParcelableArtist> CREATOR = new Parcelable.Creator<ParcelableArtist>() {
        public ParcelableArtist createFromParcel(Parcel in) {
            return new ParcelableArtist(in);
        }

        public ParcelableArtist[] newArray(int size) {
            return new ParcelableArtist[size];
        }
    };
    private ParcelableImage artistImage;

    public ParcelableArtist(Parcel in){
        name = in.readString();
        id = in.readString();
        artistImage = in.readParcelable(ParcelableImage.class.getClassLoader());
    }

    public ParcelableArtist(Artist artist){
        name = artist.name;
        id = artist.id;
        if(artist.images.size() > 0){
            Image image = artist.images.get(0);
            artistImage = new ParcelableImage(image.height, image.width, image.url);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(id);
        dest.writeParcelable(artistImage, flags);
    }

    public Artist getSpotifyArtist(){
        Artist toReturn = new Artist();
        toReturn.name = name;
        toReturn.id = id;
        toReturn.images = new ArrayList<>();
        if(artistImage != null) toReturn.images.add(artistImage);
        return toReturn;
    }


}
