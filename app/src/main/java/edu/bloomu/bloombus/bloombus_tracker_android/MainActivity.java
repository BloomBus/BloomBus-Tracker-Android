package edu.bloomu.bloombus.bloombus_tracker_android;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.midi.MidiOutputPort;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    // UI Components
    private SupportMapFragment mMapFragment;
    private GoogleMap mMap;
    private FloatingActionButton mFab;
    private Spinner mLoopSpinner;

    // Private fields
    private FusedLocationProviderClient mFusedLocationClient;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mShuttlesReference;
    private UUID mUUID;
    private boolean mTrackingPaused;
    private List<Double> mPrevCoordinates;

    // Static fields
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 42;
    private static final float ZOOM_LEVEL = 14;
    private DatabaseReference mNewShuttleRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTrackingPaused = false;
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTrackingPaused = !mTrackingPaused;
                Drawable icon = ContextCompat.getDrawable(
                        getApplicationContext(),
                        mTrackingPaused ? R.drawable.ic_play_arrow_white_24dp
                                : R.drawable.ic_pause_white_24dp
                );
                mFab.setImageDrawable(icon);
                mNewShuttleRef.removeValue();
            }
        });

        mLoopSpinner = findViewById(R.id.loopSpinner);
        mLoopSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                randomizeUUID();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mPrevCoordinates = new ArrayList<>(Arrays.asList(41.012101, -76.4478475));
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mShuttlesReference = mFirebaseDatabase.getReference("shuttles");
        randomizeUUID();
        mMapFragment = (SupportMapFragment) getSupportFragmentManager()
            .findFragmentById(R.id.map);

        initLocationService();
    }

    private void randomizeUUID() {
        mUUID = UUID.randomUUID();
        mNewShuttleRef = mShuttlesReference.child(mUUID.toString());
        mNewShuttleRef.onDisconnect().removeValue();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initLocationService() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMapFragment.getMapAsync(this);
            LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (!mTrackingPaused) {
                        Log.d("LatLng", location.getLatitude() + " " + location.getLongitude());
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(latLng)
                            .zoom(ZOOM_LEVEL)
                            .build();
                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                        // Construct ShuttleInformation object
                        String loopKey = getResources().getStringArray(R.array.loop_names_encoded)[mLoopSpinner.getSelectedItemPosition()];
                        String loopKeyDisplayName = (String) mLoopSpinner.getSelectedItem();

                        ShuttleGeoJSONProperties shuttleProps = new ShuttleGeoJSONProperties(
                            loopKey,
                            loopKeyDisplayName,
                            System.currentTimeMillis(),
                            location.getSpeed(),
                            location.getAltitude(),
                            mPrevCoordinates
                        );
                        List<Double> shuttleCoords = new ArrayList<>(Arrays.asList(location.getLatitude(), location.getLongitude()));
                        GeoJSONGeometry geometry = new GeoJSONGeometry("Point", shuttleCoords);
                        ShuttleInformation shuttle = new ShuttleInformation("Feature", geometry, shuttleProps);

                        // Push to "/shuttles"
                        mNewShuttleRef.setValue(shuttle);

                        mPrevCoordinates = new ArrayList<>(Arrays.asList(location.getLatitude(), location.getLongitude()));
                    }
                }
                @Override
                public void onProviderDisabled(String provider) {
                    // TODO Auto-generated method stub
                }
                @Override
                public void onProviderEnabled(String provider) {
                    // TODO Auto-generated method stub
                }
                @Override
                public void onStatusChanged(String provider, int status,
                                            Bundle extras) {
                    // TODO Auto-generated method stub
                }
            });

        } else {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_FINE_LOCATION
            );
        }
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;
        try {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        } catch (SecurityException se) {
            se.printStackTrace();
        }
        // mPrevCoordinates was initialized to center of campus
        LatLng campusCenter = new LatLng(mPrevCoordinates.get(0), mPrevCoordinates.get(0));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(campusCenter, ZOOM_LEVEL));
    }

}
