package com.cyberus2603.navigation;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMapLoadedCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapLongClickListener,
        SensorEventListener{

    private static final int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 101;
    private final String POINTS_JSON_FILE = "points.json";
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback locationCallback;
    TextView accelerometerText;
    FloatingActionButton recordData;
    FloatingActionButton deletePoint;
    Button clearMemory;
    Marker gpsMarker = null;

    public class marker_data {
        private double latitude;
        private double longtitude;
        public marker_data(double latitude,double longtitude) {
            this.latitude = latitude;
            this.longtitude = longtitude;
        }
        public double getLatitude() {
            return this.latitude;
        }
        public double getLongtitude() {
            return this.longtitude;
        }
    }

    private boolean record_data;
    SensorManager sensorManager;
    Sensor sensor;

    List<Marker> markerList;
    List<marker_data> markerData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(MapsActivity.this,sensor,SensorManager.SENSOR_DELAY_NORMAL);

        accelerometerText = findViewById(R.id.accelerometr_data);
        recordData = findViewById(R.id.record_button);
        deletePoint = findViewById(R.id.delete_button);
        clearMemory = findViewById(R.id.clear_memory_button);
        record_data = false;

        accelerometerText.setEnabled(false);
        accelerometerText.setVisibility(View.INVISIBLE);
        recordData.setEnabled(false);
        recordData.setVisibility(View.INVISIBLE);
        deletePoint.setEnabled(false);
        deletePoint.setVisibility(View.INVISIBLE);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        markerList = new ArrayList<>();
        markerData = new ArrayList<>();
    }
    @Override
    protected void onDestroy() {
        saveToJson();
        super.onDestroy();
    }



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
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
        restoreFromJason();
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void saveToJson() {
        Gson gson = new Gson();
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(POINTS_JSON_FILE,MODE_PRIVATE);
            FileWriter writer = new FileWriter(outputStream.getFD());
            gson.toJson(markerData,writer);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void restoreFromJason() {
        FileInputStream inputStream;
        int DEFAULT_BUFFER_SIZE = 10000;
        Gson gson = new Gson();
        String readJson;

        try {
            inputStream = openFileInput(POINTS_JSON_FILE);
            FileReader reader = new FileReader(inputStream.getFD());
            char[] buf = new char[DEFAULT_BUFFER_SIZE];
            int n;
            StringBuilder builder = new StringBuilder();
            while ((n = reader.read(buf)) >= 0) {
                String tmp = String.valueOf(buf);
                String substring = (n<DEFAULT_BUFFER_SIZE) ? tmp.substring(0,n) : tmp;
                builder.append(substring);
            }
            reader.close();
            readJson = builder.toString();

            Type to_restore_type = new TypeToken<List<marker_data>>(){}.getType();
            List<marker_data> to_restore = gson.fromJson(readJson,to_restore_type);

            if (to_restore != null) {
                for (int i = 0; i < to_restore.size(); i++ ){
                    Marker marker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(to_restore.get(i).getLatitude(),to_restore.get(i).getLongtitude()))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                            .alpha(0.8f)
                            .title(String.format("Position: (%.2f, %.2f", to_restore.get(i).getLatitude(),to_restore.get(i).getLongtitude())));
                    markerData.add(new marker_data(to_restore.get(i).getLatitude(),to_restore.get(i).getLongtitude()));
                    markerList.add(marker);
                }
            }



        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, null);
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if(locationResult != null) {
                    if (gpsMarker != null)
                        gpsMarker.remove();
                    Location location = locationResult.getLastLocation();
                    gpsMarker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(location.getLatitude(),location.getLongitude()))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                            .alpha(0.8f)
                            .title("Current Location"));
                }
            }
        };
    }

    @Override
    public void onMapLoaded() {
        Log.i(MapsActivity.class.getSimpleName(), "MapLoaded");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }


        Task<Location> lastLocation = fusedLocationClient.getLastLocation();

        lastLocation.addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null && mMap != null) {
                    mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(),location.getLongitude()))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                            .title(getString(R.string.last_known_loc_msg)));
                }
            }
        });

        createLocationRequest();
        createLocationCallback();
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        if (locationCallback != null)
            fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    public void zoomInClick(View v) {
        mMap.moveCamera(CameraUpdateFactory.zoomIn());
    }

    public void zoomOutClick(View v) {
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }

    @Override
    public void onMapLongClick(LatLng latLng) {

        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(latLng.latitude,latLng.longitude))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                .alpha(0.8f)
                .title(String.format("Position: (%.2f, %.2f", latLng.latitude,latLng.longitude)));
        markerData.add(new marker_data(latLng.latitude,latLng.longitude));

        markerList.add(marker);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        CameraPosition cameraPos = mMap.getCameraPosition();
        if (cameraPos.zoom < 14f)
            mMap.moveCamera(CameraUpdateFactory.zoomTo(14f));

        recordData.setEnabled(true);
        recordData.setVisibility(View.VISIBLE);
        recordData.animate();
        deletePoint.setEnabled(true);
        deletePoint.setVisibility(View.VISIBLE);
        deletePoint.animate();

        return false;
    }

    public void recordData(View view) {
        record_data = !record_data;
        if (record_data) {
            accelerometerText.setEnabled(true);
            accelerometerText.setVisibility(View.VISIBLE);
            accelerometerText.animate();
        } else {
            accelerometerText.animate();
            accelerometerText.setEnabled(false);
            accelerometerText.setVisibility(View.INVISIBLE);
        }
    }

    public void hideButtons(View view) {
        accelerometerText.setEnabled(false);
        accelerometerText.setVisibility(View.INVISIBLE);
        recordData.setEnabled(false);
        recordData.setVisibility(View.INVISIBLE);
        deletePoint.setEnabled(false);
        deletePoint.setVisibility(View.INVISIBLE);
    }

    public void onSensorChanged(SensorEvent event){
        if (record_data) {
            accelerometerText.setText("Accelerometr: x=" + event.values[0] + " y=" + event.values[1]);
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void eraseMemory(View view) {
        markerList.clear();
        markerData.clear();
        mMap.clear();
        gpsMarker.remove();

        File json = new File(POINTS_JSON_FILE);
        json.delete();
    }
}
