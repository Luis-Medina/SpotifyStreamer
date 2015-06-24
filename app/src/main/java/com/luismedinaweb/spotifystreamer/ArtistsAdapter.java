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

import kaaes.spotify.webapi.android.models.Artist;

/**
 * Created by Luis on 6/10/2015.
 */
public class ArtistsAdapter extends ArrayAdapter<Artist> {


    public ArtistsAdapter(Context context, ArrayList<Artist> artists) {
        super(context, 0, artists);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Artist artist = getItem(position);
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_artist, parent, false);
            holder = new ViewHolder();
            holder.artistTextView = (TextView) convertView.findViewById(R.id.artist_textview);
            holder.artistImageView = (ImageView) convertView.findViewById(R.id.artist_imageview);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.artistTextView.setText(artist.name);

        String url;
        if (artist.images.size() > 0) {
            url = artist.images.get(0).url;
            if (url != null && !url.isEmpty()) {
                Picasso.with(getContext()).load(url)
                        .error(R.drawable.error)
                        .placeholder(R.drawable.placeholder)
                        .into(holder.artistImageView);
            }
        }

        return convertView;

    }

    class ViewHolder {
        TextView artistTextView;
        ImageView artistImageView;
    }
}
