package com.example.ride_bit;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;


import com.example.ride_bit.Common.Common;
import com.example.ride_bit.Helper.CustomInfoWindow;
import com.example.ride_bit.Model.User;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.LocationListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;

import android.view.MenuItem;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import android.view.Menu;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

public class Home extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    PlacesClient placesClient;
    SupportMapFragment mapFragment;

    //Location
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

    DatabaseReference mDatabase;
    GeoFire geoFire;

    private static final int REQUEST_CALL = 1;

    Marker mUserMarker;

    TextView main_name, main_email;

    DatabaseReference driverAvailable;
    DatabaseReference infoUser;

    //BottomSheet
    ImageView imgExpanded;
    BottomSheetUserfragment bottomSheet;
    Button btnPickUp;

    boolean isDriverFound= false;
    String driverId="";
    int radius = 1;
    int distance = 1;
    private static final int LIMIT = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        //Maps

        mapFragment= (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapHome);
        mapFragment.getMapAsync(this);


        placesSearch();

        //Init view
        imgExpanded = findViewById(R.id.imgExpandable);
        bottomSheet= BottomSheetUserfragment.newInstance("Rider bottom sheet");
        imgExpanded.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheet.show(getSupportFragmentManager(), bottomSheet.getTag());
            }
        });

        btnPickUp = findViewById(R.id.pickup);
        btnPickUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPickupHere(FirebaseAuth.getInstance().getCurrentUser().getUid());
            }
        });

        View navigationHeaderView = navigationView.getHeaderView(0);

        main_name = navigationHeaderView.findViewById(R.id.main_name);
        main_email = navigationHeaderView.findViewById(R.id.main_email);

        setUpLocation();

    }

    private void requestPickupHere(String uid) {
        DatabaseReference dbRequeest = FirebaseDatabase.getInstance().getReference(Common.pickup_tb1);
        GeoFire mGeoFire = new GeoFire(dbRequeest);
        mGeoFire.setLocation(uid, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

        if(mUserMarker.isVisible())
            mUserMarker.remove();

        mUserMarker = mMap.addMarker(new MarkerOptions()
        .title("Pickup here")
        .snippet("")
        .position(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        mUserMarker.showInfoWindow();

        btnPickUp.setText("Getting your driver...");

        findDriver();
    }

    private void findDriver() {
        DatabaseReference drivers = FirebaseDatabase.getInstance().getReference(Common.driver_tb1);
        GeoFire gfDrivers = new GeoFire(drivers);

        GeoQuery geoQuery = gfDrivers.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, final GeoLocation location) {
                if(!isDriverFound){
                    isDriverFound = true;
                    driverId = key;
                    btnPickUp.setText("Call driver");



                    Toast.makeText(Home.this, ""+key, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                //no driver
                if(!isDriverFound){
                    radius++;
                    findDriver();
                }


            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void loadAllAvailableDriver() {

        mMap.clear();
        //Current location
        mMap.addMarker(new MarkerOptions().position(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
                                            .title("You"));

        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference(Common.driver_tb1);
        GeoFire gfDrivers = new GeoFire(driverLocation);

        GeoQuery geoQuery = gfDrivers.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), distance);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, final GeoLocation location) {
                FirebaseDatabase.getInstance().getReference(Common.user_driver_tb1)
                        .child(key)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                User rider = dataSnapshot.getValue(User.class);

                                mMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(location.latitude, location.longitude))
                                        .flat(true)
                                        .title(rider.getName())
                                        .snippet("Phone: "+rider.getPhone())
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_car)));


                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (distance<=LIMIT){
                    distance++;
                    loadAllAvailableDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
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
                displayLocation();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case MY_PERMISSION_REQUEST_CODE:
                if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    buildGoogleApiClient();
                    createdLocationRequest();
                    displayLocation();
                }
        }
    }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {

            //Present System
            driverAvailable = FirebaseDatabase.getInstance().getReference(Common.driver_tb1);
            driverAvailable.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    loadAllAvailableDriver();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            final double latitude = mLastLocation.getLatitude();
            final double longitude = mLastLocation.getLongitude();


            if (mUserMarker != null) mUserMarker.remove();
            mUserMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(latitude, longitude))
                    .title("Your Location"));

            //Move camera to this position
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15.0f));

            loadAllAvailableDriver();

            Log.d("EDMDEV", String.format("Yout location was changed: %f / %f", latitude, longitude));

        }else{
            Log.d("EDMDEV", "Can not get your location");
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

    private void placesSearch() {

        //PlacesSearch

        String apiKey= "AIzaSyDI_TIXG4EUtjn1BIBthczgKpPgGRi3-_M";

        if(!Places.isInitialized()){
            Places.initialize(getApplicationContext(), apiKey);
        }

        placesClient = Places.createClient(this);

        //location
        final AutocompleteSupportFragment location;
        location = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.place_location);

        location.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME));

        location.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                final LatLng prueba= place.getLatLng();
                Log.i("PlacesApi", "onPaceSelected: "+prueba.longitude+"\n"+prueba.latitude);

                String destination=place.getName();

                Toast.makeText(Home.this, ""+destination, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(Home.this, ""+status.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        final AutocompleteSupportFragment destination;
        destination = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.place_destination);

        destination.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME));

        destination.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                final LatLng prueba= place.getLatLng();
                Log.i("PlacesApi", "onPaceSelected: "+prueba.longitude+"\n"+prueba.latitude);

                String destination=place.getName();

                Toast.makeText(Home.this, ""+destination, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(Home.this, ""+status.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        FragmentManager fragmentManager = getSupportFragmentManager();

        if (id == R.id.profile) {
            // Handle the camera action
        } else if (id == R.id.changePass) {
            Intent intent = new Intent(Home.this, ChangePass.class);
            startActivity(intent);
            finish();

        } else if (id == R.id.driver) {
            Intent intent = new Intent(Home.this, MainDriver.class);
            startActivity(intent);
            finish();

        } else if (id == R.id.help) {
            Intent intent = new Intent(Home.this, HelpActivity.class);
            startActivity(intent);
            finish();

        } else if (id == R.id.about) {
            Intent intent = new Intent(Home.this, AboutActivity.class);
            startActivity(intent);
            finish();

        } else if (id == R.id.log_out) {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(Home.this, LogInMainUser.class);
            startActivity(intent);
            finish();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.setInfoWindowAdapter(new CustomInfoWindow(this));


    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
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
