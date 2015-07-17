package com.luismedinaweb.spotifystreamer.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashMap;

import kaaes.spotify.webapi.android.models.AlbumSimple;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;

/**
 * Created by Luis on 6/18/2015.
 */
public class ParcelableTrack extends Track implements Parcelable{

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<ParcelableTrack> CREATOR = new Parcelable.Creator<ParcelableTrack>() {
        public ParcelableTrack createFromParcel(Parcel in) {
            return new ParcelableTrack(in);
        }

        public ParcelableTrack[] newArray(int size) {
            return new ParcelableTrack[size];
        }
    };
    private ParcelableImage trackImage;
    private String albumName;
    private String artistName;
    private String externalURL;

    public ParcelableTrack(Parcel in){
        name = in.readString();
        albumName = in.readString();
        trackImage = in.readParcelable(ParcelableImage.class.getClassLoader());
        artistName = in.readString();
        preview_url = in.readString();
        externalURL = in.readString();
    }

    public ParcelableTrack(Track track){
        name = track.name;
        albumName = track.album.name;
        artistName = track.artists.get(0).name;
        preview_url = track.preview_url;
        if(track.album.images.size() > 0){
            Image albumImage = track.album.images.get(0);
            trackImage = new ParcelableImage(albumImage.height, albumImage.width, albumImage.url);
        }
        if (track.external_urls.size() > 0) {
            externalURL = track.external_urls.get("spotify");
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(albumName);
        dest.writeParcelable(trackImage, flags);
        dest.writeString(artistName);
        dest.writeString(preview_url);
        dest.writeString(externalURL);
    }

    public Track getSpotifyTrack(){
        Track toReturn = new Track();
        toReturn.name = name;
        toReturn.album = new AlbumSimple();
        toReturn.album.name = albumName;
        toReturn.album.images = new ArrayList<>();
        toReturn.artists = new ArrayList<>();
        ArtistSimple artist = new ArtistSimple();
        artist.name = artistName;
        toReturn.artists.add(artist);
        toReturn.preview_url = preview_url;
        if(trackImage != null) toReturn.album.images.add(trackImage);
        if (externalURL != null) {
            if (toReturn.external_urls == null) toReturn.external_urls = new HashMap<>();
            toReturn.external_urls.put("spotify", externalURL);
        }
        return toReturn;
    }

    public ParcelableImage getTrackImage() {
        return trackImage;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getAlbumName() {
        return albumName;
    }

    public String getExternalURL() {
        return externalURL;
    }
}
