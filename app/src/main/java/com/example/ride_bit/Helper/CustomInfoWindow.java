package com.example.ride_bit.Helper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.example.ride_bit.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;


public class CustomInfoWindow implements GoogleMap.InfoWindowAdapter {

    View myView;

    public CustomInfoWindow(Context context){
        myView = LayoutInflater.from(context)
                .inflate(R.layout.custom_user_info, null);

    }

    @Override
    public View getInfoWindow(Marker marker) {
        TextView txtPickUpTitle=(myView.findViewById(R.id.txtPickUpInfo));
        txtPickUpTitle.setText(marker.getTitle());

        TextView txtPickupSinniper=(myView.findViewById(R.id.txtSniped));
        txtPickupSinniper.setText(marker.getSnippet());

        return  myView;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }
}

