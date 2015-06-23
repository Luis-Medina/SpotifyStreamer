package com.luismedinaweb.spotifystreamer;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.AlbumSimple;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;

/**
 * Created by Luis on 6/18/2015.
 */
public class ParcelableTrack extends Track implements Parcelable{

    private ParcelableImage trackImage;
    private String albumName;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(albumName);
        dest.writeParcelable(trackImage, flags);
    }

    public ParcelableTrack(Parcel in){
        name = in.readString();
        albumName = in.readString();
        trackImage = in.readParcelable(ParcelableImage.class.getClassLoader());
    }

    public ParcelableTrack(Track track){
        name = track.name;
        albumName = track.album.name;
        if(track.album.images.size() > 0){
            Image albumImage = track.album.images.get(0);
            trackImage = new ParcelableImage(albumImage.height, albumImage.width, albumImage.url);
        }
    }

    public Track getSpotifyTrack(){
        Track toReturn = new Track();
        toReturn.name = name;
        toReturn.album = new AlbumSimple();
        toReturn.album.name = albumName;
        toReturn.album.images = new ArrayList<>();
        if(trackImage != null) toReturn.album.images.add(trackImage);
        return toReturn;
    }

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<ParcelableTrack> CREATOR = new Parcelable.Creator<ParcelableTrack>() {
        public ParcelableTrack createFromParcel(Parcel in) {
            return new ParcelableTrack(in);
        }

        public ParcelableTrack[] newArray(int size) {
            return new ParcelableTrack[size];
        }
    };
}
