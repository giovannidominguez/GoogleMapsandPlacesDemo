package edu.ucsb.cs.cs190i.giovannidominguez;

import android.Manifest;
import android.content.Intent;
import android.location.LocationListener;
import android.net.Uri;
import android.provider.SyncStateContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import android.location.LocationManager;
import android.location.Criteria;
import android.app.PendingIntent;
import android.location.Location;
import android.content.Context;

import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import android.support.v4.content.ContextCompat;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker GPS_ME;
    private static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
    private double myLat;
    private double myLng;
    private List<Geofence> mGeofenceList =	new ArrayList<>();
    PendingIntent mGeofencePendingIntent;
    private GoogleApiClient mGoogleApiClient;
    public static final String TAG = "Activity";




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        //use for geofences
         //GEOFENCE_RADIUS_IN_METERS = 25;

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSION_ACCESS_COARSE_LOCATION);
        }
        LocationManager locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);

        Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (loc == null) {
// fall back to network if GPS is not available
            loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        if (loc != null) {
            myLat = loc.getLatitude();
            myLng = loc.getLongitude();
        }



        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                myLat = location.getLatitude();
                myLng = location.getLongitude();
                LatLng me = new LatLng(myLat, myLng);
                GPS_ME.remove();
                GPS_ME = mMap.addMarker(new MarkerOptions().position(me).title("ME").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        });


        Ion.with(this)
                .load("https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="+ myLat + "," + myLng + "&radius=500&key=AIzaSyCpcuzNWxTTvwSH-r1o4atdomyVZHaIhWo")
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        try {
                            JSONObject myJson = new JSONObject(result);
                            JSONArray arr = myJson.getJSONArray("results");

                            for (int i = 0; i < arr.length(); i++ ) {
                                JSONObject placeInfo = arr.getJSONObject(i);

                                // ADD THE MARKERS
                                Marker mark = mMap.addMarker(new MarkerOptions().position(
                                        new LatLng(
                                                Double.parseDouble(placeInfo.getJSONObject("geometry").getJSONObject("location").getString("lat")),
                                                Double.parseDouble(placeInfo.getJSONObject("geometry").getJSONObject("location").getString("lng"))
                                                )).title(placeInfo.getString("name")));
                                String placeid = placeInfo.getString("place_id");
                                mark.setTag(placeid);
                                // Instantiates a new CircleOptions object and defines the center and radius
                                CircleOptions circleOptions = new CircleOptions()
                                        .center(new LatLng(Double.parseDouble(placeInfo.getJSONObject("geometry").getJSONObject("location").getString("lat")),
                                                Double.parseDouble(placeInfo.getJSONObject("geometry").getJSONObject("location").getString("lng"))))
                                        .radius(25)
                                        .strokeWidth(2); // In meters
                                // Get back the mutable Circle
                                Circle circle = mMap.addCircle(circleOptions);



                                // add the geofences

                               mGeofenceList.add(new Geofence.Builder()
                                       .setRequestId(placeInfo.getString("place_id"))
                                       .setCircularRegion(Double.parseDouble(placeInfo.getJSONObject("geometry").getJSONObject("location").getString("lat")),
                                               Double.parseDouble(placeInfo.getJSONObject("geometry").getJSONObject("location").getString("lng")),
                                               25)
                                       .setExpirationDuration(10000000)
                                       .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                                               Geofence.GEOFENCE_TRANSITION_EXIT)
                                       .build());

                            }


                            mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                                @Override
                                public void onInfoWindowClick(Marker marker) {

                                    System.out.println(marker.getTag());
                                    Ion.with(MapsActivity.this).load("https://maps.googleapis.com/maps/api/place/details/json?placeid=" + marker.getTag() + "&key=AIzaSyCcOAodE6QxShOGd5o9LOaZdqZFhlxYMqE").asString().setCallback(new FutureCallback<String>() {
                                        @Override
                                        public void onCompleted(Exception e, String result) {

                                            try {
                                                JSONObject json = new JSONObject(result);
                                                JSONObject list = json.getJSONObject("result");

                                                String url = list.getString("url");
                                                Intent i = new Intent(Intent.ACTION_VIEW);
                                                i.setData(Uri.parse(url));
                                                startActivity(i);



                                            } catch (JSONException e1) {
                                                e1.printStackTrace();
                                            }


                                        }
                                    });




                                }
                            });

                        }


                 catch (JSONException jsone){

                        }
                    }

                });
    }
    private void initGoogleAPIClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(connectionAddListener)
                .addOnConnectionFailedListener(connectionFailedListener)
                .build();
        mGoogleApiClient.connect();
    }

    private GoogleApiClient.ConnectionCallbacks connectionAddListener = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Log.i(TAG, "onConnected");

            if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            myLng = location.getLatitude();
            myLat = location.getLongitude();



            try {
                LocationServices.GeofencingApi.addGeofences(
                        mGoogleApiClient,
                        getGeofencingRequest(),
                        getGeofencePendingIntent());

            } catch (SecurityException securityException) {

                Log.e(TAG, "Error");
            }



        }

        @Override
        public void onConnectionSuspended(int i) {

            Log.e(TAG, "onConnectionSuspended");

        }
    };

    private GoogleApiClient.OnConnectionFailedListener connectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        }
    };




    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //LatLng UCSB = new LatLng(34.4140, -119.8489);
        LatLng Current_Location = new LatLng(myLat, myLng);
        GPS_ME = mMap.addMarker(new MarkerOptions().position(Current_Location).title("ME").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        mMap.moveCamera( CameraUpdateFactory.newLatLngZoom(new LatLng(myLat, myLng) , 17f) );
    }


    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);

    }

}
