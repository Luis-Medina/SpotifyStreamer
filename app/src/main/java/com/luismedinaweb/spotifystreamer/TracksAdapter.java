package com.luismedinaweb.spotifystreamer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Track;

/**
 * Created by Luis on 6/11/2015.
 */
public class TracksAdapter extends ArrayAdapter<Track> {

    public TracksAdapter(Context context, ArrayList<Track> artists) {
        super(context, 0, artists);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Track track = getItem(position);
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_track, parent, false);
            holder = new ViewHolder();
            holder.trackTextView = (TextView) convertView.findViewById(R.id.track_textview);
            holder.albumTextView = (TextView) convertView.findViewById(R.id.album_textview);
            holder.trackImageView = (ImageView) convertView.findViewById(R.id.track_imageview);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.trackTextView.setText(track.name);
        holder.albumTextView.setText(track.album.name);

        String url;
        if (track.album.images.size() > 0) {
            url = track.album.images.get(0).url;
            if (url != null && !url.isEmpty()) {
                Picasso.with(getContext()).load(url)
                        .error(R.drawable.error)
                        .placeholder(R.drawable.placeholder)
                        .into(holder.trackImageView);
            }
        }

        return convertView;

    }

    class ViewHolder {
        TextView trackTextView;
        TextView albumTextView;
        ImageView trackImageView;
    }
}

