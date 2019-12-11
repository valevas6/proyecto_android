package com.example.ride_bit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.example.ride_bit.Common.Common;
import com.example.ride_bit.Remote.IGoogleAPI;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverMap extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    private GoogleMap mMap;
    //Play services
    private static  final int MY_PERMISSION_REQUEST_CODE = 7000;
    private static  final int PLAY_SERVICE_RES_REQUEST = 7001;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;

    private static int UPDATE_INTERVAL = 5000;
    private static int FASTEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;

    //Present System
    DatabaseReference onlineRef, currentUserRef;

    Runnable drawPathRunnable = new Runnable() {
        @Override
        public void run() {
            if(index<polyLineList.size()-1){
                index++;
                next=index+1;
            }
            if(index<polyLineList.size()-1){
                startPosition=polyLineList.get(index);
                endPosition=polyLineList.get(next);
            }

            ValueAnimator valueAnimator= ValueAnimator.ofFloat(0,1);
            valueAnimator.setDuration(3000);
            valueAnimator.setInterpolator(new LinearInterpolator());
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    v=animation.getAnimatedFraction();
                    lon=v*endPosition.longitude+(1-v)*startPosition.longitude;
                    lat=v*endPosition.latitude+(1-v)*startPosition.latitude;
                    LatLng newPos = new LatLng(lat, lon);
                    carMarker.setPosition(newPos);
                    carMarker.setAnchor(0.5f, 0.5f);
                    carMarker.setRotation(getBearing(startPosition, newPos));
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(newPos)
                                    .zoom(15.5f)
                                    .build()
                    ));
                }
            });
            valueAnimator.start();
            handler.postDelayed(this, 3000);
        }
    };

    private float getBearing(LatLng startPosition, LatLng endPosition) {
        double lat = Math.abs(startPosition.latitude - endPosition.latitude);
        double lon = Math.abs(startPosition.longitude - endPosition.longitude);

        if(startPosition.latitude< endPosition.latitude && startPosition.longitude<endPosition.longitude)
            return (float)(Math.toDegrees(Math.atan(lon/lat)));
        else if(startPosition.latitude>= endPosition.latitude && startPosition.longitude<endPosition.longitude)
            return (float)((90-Math.toDegrees(Math.atan(lon/lat)))+90);
        else if(startPosition.latitude>= endPosition.latitude && startPosition.longitude>=endPosition.longitude)
            return (float)(90-Math.toDegrees(Math.atan(lon/lat))+180);
        else if(startPosition.latitude< endPosition.latitude && startPosition.longitude>=endPosition.longitude)
            return(float)((90-Math.toDegrees(Math.atan(lon/lat)))+270);

        return -1;
    }

    DatabaseReference drivers;
    GeoFire geoFire;

    Marker mCurrent;

    SwitchCompat location_switch;
    SupportMapFragment mapFragment;

    //Car animation
    private List<LatLng> polyLineList;
    private Marker carMarker;
    private float v;
    private  double lat, lon;
    private Handler handler;
    private  LatLng startPosition, endPosition, currentPosition;
    private  int index, next;
    //private Button btnGo;
    //private EditText edtPlace;
    PlacesClient placesClient;
    private String destination;
    private PolylineOptions polylineOptions, blackPolyLineOptions;
    private Polyline blackPolyLine, grayPolyLine;

    private IGoogleAPI mService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.driver_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Present System
        onlineRef = FirebaseDatabase.getInstance().getReference().child(".info/connected");
        currentUserRef = FirebaseDatabase.getInstance().getReference(Common.driver_tb1)
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        onlineRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                currentUserRef.onDisconnect().removeValue();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        //Init View
        location_switch =  findViewById(R.id.locationSwitch);
        location_switch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(location_switch.isChecked()){
                    FirebaseDatabase.getInstance().goOnline();
                    startLocationUpdates();
                    displayLocation();
                    Snackbar.make(mapFragment.getView(), "You are online", Snackbar.LENGTH_SHORT)
                            .show();
                }else{
                    FirebaseDatabase.getInstance().goOffline();
                    stopLocationUpdates();
                    mCurrent.remove();
                    mMap.clear();
                    //handler.removeCallbacks(drawPathRunnable);
                    Snackbar.make(mapFragment.getView(), "You are offline", Snackbar.LENGTH_SHORT)
                            .show();
                }
            }
        });

        drivers = FirebaseDatabase.getInstance().getReference(Common.driver_tb1);
        geoFire = new GeoFire(drivers);

        polyLineList = new ArrayList<>();

        String apiKey= "AIzaSyDI_TIXG4EUtjn1BIBthczgKpPgGRi3-_M";

        if(!Places.isInitialized()){
            Places.initialize(getApplicationContext(), apiKey);
        }

        placesClient = Places.createClient(this);

        final AutocompleteSupportFragment places;
        places = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        places.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME));

        places.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                final LatLng prueba= place.getLatLng();
                Log.i("PlacesApi", "onPaceSelected: "+prueba.longitude+"\n"+prueba.latitude);
                if(location_switch.isChecked()){
                    destination=place.getName();
                    destination=destination.replace("", "+");
                    Toast.makeText(DriverMap.this, ""+destination, Toast.LENGTH_SHORT).show();

                    getDirection();
                }
                else{
                    Toast.makeText(DriverMap.this, "Enable location", Toast.LENGTH_SHORT).show();

                }
            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(DriverMap.this, ""+status.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        setUpLocation();
        mService = Common.getGoogleAPI();
    }

    private void getDirection() {
        currentPosition = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

        String requestApi = null;
        try{
            requestApi= "https://maps.googleapis.com/maps/api/directions/json?"+
                    "mode-driving&"+
                    "transit_routing_preference=less_driving&"+
                    "origin="+currentPosition.latitude+","+currentPosition.longitude+"&"+
                    "destination="+destination+"&key=AIzaSyB8FsVuAhkYl5Am9--4Va9MMSI9rCbnnUI";

            Log.d("EDMTDEV", requestApi);
            mService.getPath(requestApi)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            try {
                                JSONObject jsonObject = new JSONObject(response.body().toString());
                                JSONArray jsonArray = jsonObject.getJSONArray("routes");
                                for (int i = 0;i<jsonArray.length(); i++ ){
                                    JSONObject route = jsonArray.getJSONObject(i);
                                    JSONObject poly = route.getJSONObject("overview_polyline");
                                    String polyline = poly.getString("points");
                                    polyLineList=decodePoly(polyline);

                                    //Adjust bounds
                                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                    for(LatLng latLng:polyLineList)
                                        builder.include(latLng);
                                    LatLngBounds bounds = builder.build();
                                    CameraUpdate mCameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 2);
                                    mMap.animateCamera(mCameraUpdate);
                                    polylineOptions= new PolylineOptions();
                                    polylineOptions.color(Color.GRAY);
                                    polylineOptions.width(5);
                                    polylineOptions.startCap(new SquareCap());
                                    polylineOptions.endCap(new SquareCap());
                                    polylineOptions.jointType(JointType.ROUND);
                                    polylineOptions.addAll(polyLineList);
                                    grayPolyLine= mMap.addPolyline(polylineOptions);

                                    blackPolyLineOptions= new PolylineOptions();
                                    blackPolyLineOptions.color(Color.BLACK);
                                    blackPolyLineOptions.width(5);
                                    blackPolyLineOptions.startCap(new SquareCap());
                                    blackPolyLineOptions.endCap(new SquareCap());
                                    blackPolyLineOptions.jointType(JointType.ROUND);
                                    blackPolyLine= mMap.addPolyline(blackPolyLineOptions);

                                    mMap.addMarker(new MarkerOptions()
                                            .position(polyLineList.get(polyLineList.size()-1))
                                            .title("Pickup Location"));

                                    //Animation
                                    ValueAnimator polyLineAnimator = ValueAnimator.ofInt(0,100);
                                    polyLineAnimator.setDuration(2000);
                                    polyLineAnimator.setInterpolator(new LinearInterpolator());
                                    polyLineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                        @Override
                                        public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                            List<LatLng> points = grayPolyLine.getPoints();
                                            int percentValue = (int)valueAnimator.getAnimatedValue();
                                            int size= points.size();
                                            int newPoints= (int)(size*(percentValue/100.0f));
                                            List<LatLng> p = points.subList(0, newPoints);
                                            blackPolyLine.setPoints(p);
                                        }
                                    });
                                    polyLineAnimator.start();
                                    carMarker = mMap.addMarker(new MarkerOptions().position(currentPosition)
                                            .flat(true)
                                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_car)));

                                    handler = new Handler();
                                    index=-1;
                                    next=1;
                                    handler.postDelayed(drawPathRunnable, 3000);

                                }
                            }catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(DriverMap.this, ""+t.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setTrafficEnabled(false);
        mMap.setIndoorEnabled(false);
        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }

    private void setUpLocation() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            //Request runTime permission
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, MY_PERMISSION_REQUEST_CODE);
        }else{
            if(checkPlayServices()){
                buildGoogleApiClient();
                createdLocationRequest();
                if(location_switch.isChecked()) displayLocation();
            }
        }
    }

    private void createdLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICE_RES_REQUEST).show();
            else {
                Toast.makeText(this, "This device is not supported", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
    }

    private void stopLocationUpdates() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            return;
        }
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    private void displayLocation() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(mLastLocation != null){
            if(location_switch.isChecked()){
                final double latitude = mLastLocation.getLatitude();
                final double longitude = mLastLocation.getLongitude();

                //Update To Firebase
                geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(), new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        //Add Marker
                        if(mCurrent != null) mCurrent.remove();
                        mCurrent = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(latitude, longitude))
                                .title("Your Location"));

                        //Move camera to this position
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15.0f));
                    }
                });
            }
        }
        else{
            Log.d("ERROR", "Cannot get your location");
        }
    }

    private void startLocationUpdates() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case MY_PERMISSION_REQUEST_CODE:
                if(grantResults.length>0&& grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if(checkPlayServices()){
                        buildGoogleApiClient();
                        createdLocationRequest();
                        if(location_switch.isChecked()) displayLocation();
                    }
                }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        displayLocation();
    }
}
