package edu.bloomu.bloombus.bloombus_tracker_android;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.geojson.Point;
import com.mapbox.turf.TurfMeasurement;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.time.StopWatch;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    // UI Components
    private SupportMapFragment mMapFragment;
    private GoogleMap mMap;
    private FloatingActionButton mFab;
    private Spinner mLoopSpinner;

    // Private fields
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mShuttlesReference;
    private DatabaseReference mNewShuttleRef;
    private DatabaseReference mStopsReference;
    private DatabaseReference mLoopsReference;
    private BidiMap<String, Point> mAllStopsDictionary;
    private HashMap<String, List<String>> mLoopsDictionary;
    private List<Point> mCurrentLoopStopsList;
    private UUID mUUID;
    private String mLoopKey;
    private String mLoopKeyDisplayName;
    private boolean mTrackingPaused;
    private LatLng mPrevCoordinates;
    private PackageInfo mPackageInfo;
    private boolean mIsDwelling;
    private double mStopProximityThresholdMeters;
    private double mShuttleSpeedThresholdMetersPerSec;
    private StopWatch mStopWatch;

    // Static fields
    private static final String TAG = MainActivity.class.getName();
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 42;
    private static final float MIN_ZOOM_LEVEL = 14F;
    private static final float DEFAULT_ZOOM_LEVEL = 17F;
    private static final float MAX_ZOOM_LEVEL = 20F;
    private static final Double DEFAULT_LAT = 41.012101;
    private static final Double DEFAULT_LNG = -76.4478475;
    private static final LatLngBounds MAP_BOUNDS = new LatLngBounds(
        new LatLng(40.989417, -76.493869),
        new LatLng(41.021290, -76.443038)
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTrackingPaused = false;
        mIsDwelling = false;
        mFab = findViewById(R.id.fab);
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

        LatLng defaultLatLng = new LatLng(DEFAULT_LAT, DEFAULT_LNG);
        mPrevCoordinates = defaultLatLng;
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mShuttlesReference = mFirebaseDatabase.getReference("shuttles");
        mStopsReference = mFirebaseDatabase.getReference("stops");
        mLoopsReference = mFirebaseDatabase.getReference("loops");
        mCurrentLoopStopsList = new LinkedList<>();
        mStopWatch = new StopWatch();

        // Retrieve defined constants from Firebase
        // Triggers callback chain: buildLoopsDictionary => buildStopsDictionary => onLoopSelectionChange => initLocationService
        mFirebaseDatabase.getReference("constants").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mShuttleSpeedThresholdMetersPerSec = dataSnapshot.child("shuttleSpeedThresholdMetersPerSec").getValue(Double.class);
                mStopProximityThresholdMeters = dataSnapshot.child("stopProximityThresholdMeters").getValue(Double.class);
                buildLoopsDictionary();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        mMapFragment = SupportMapFragment.newInstance(new GoogleMapOptions()
            .minZoomPreference(MIN_ZOOM_LEVEL)
            .maxZoomPreference(MAX_ZOOM_LEVEL)
            .camera(CameraPosition.fromLatLngZoom(defaultLatLng, DEFAULT_ZOOM_LEVEL)));
        ft.replace(R.id.map_container, mMapFragment);
        ft.commit();
        mMapFragment.getMapAsync(this);
        try {
            mPackageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not find PackageInfo", e);
        }
    }

    private void buildLoopsDictionary() {
        this.mLoopsDictionary = new HashMap<>();
        mLoopsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot loopSnapshot: dataSnapshot.child("features").getChildren()) {
                    DataSnapshot propertiesSnapshot = loopSnapshot.child("properties");
                    String loopKey = (String) propertiesSnapshot.child("key").getValue();
                    List<String> stopKeys = new LinkedList<>();
                    for (DataSnapshot stopKeySnapshot : propertiesSnapshot.child("stops").getChildren()) {
                        stopKeys.add(stopKeySnapshot.getValue(String.class));
                    }
                    mLoopsDictionary.put(loopKey, stopKeys);
                }

                mLoopSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        onLoopSelectionChange();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
                buildStopsDictionary();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void buildStopsDictionary() {
        mAllStopsDictionary = new DualHashBidiMap<>();
        mStopsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot stopSnapshot: dataSnapshot.getChildren()) {
                    DataSnapshot coordsSnapshot = stopSnapshot.child("geometry").child("coordinates");
                    mAllStopsDictionary.put(stopSnapshot.getKey(), Point.fromLngLat(
                        coordsSnapshot.child("0").getValue(Double.class),
                        coordsSnapshot.child("1").getValue(Double.class)
                    ));
                }
                onLoopSelectionChange();
                initLocationService();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void randomizeUUID() {
        if (mNewShuttleRef != null) {
            mNewShuttleRef.removeValue();
        }
        mUUID = UUID.randomUUID();
        mNewShuttleRef = mShuttlesReference.child(mUUID.toString());
        mNewShuttleRef.onDisconnect().removeValue();
    }

    private void onLoopSelectionChange() {
        mLoopKey = getResources().getStringArray(R.array.loop_names_encoded)[mLoopSpinner.getSelectedItemPosition()];
        mLoopKeyDisplayName = (String) mLoopSpinner.getSelectedItem();
        mCurrentLoopStopsList.clear();
        if (mMap != null) mMap.clear();
        System.out.println(mLoopKey);
        List<String> stopKeys = mLoopsDictionary.get(mLoopKey);
        for (String stopKey : stopKeys) {
            Point stopPoint = mAllStopsDictionary.get(stopKey);
            LatLng stopLatLng = new LatLng(stopPoint.latitude(), stopPoint.longitude());
            mCurrentLoopStopsList.add(stopPoint);
            mMap.addCircle(new CircleOptions()
                .center(stopLatLng)
                .radius(mStopProximityThresholdMeters)
                .strokeColor(0xffff0000)
                .strokeWidth(4)
                .fillColor(0x44ff0000)
            );
            mMap.addMarker(new MarkerOptions()
                .position(stopLatLng)
                .title(stopKey)
            );
        }
        this.randomizeUUID();
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
                        LatLng currentCoords = new LatLng(location.getLatitude(), location.getLongitude());
                        Point currentPoint = Point.fromLngLat(location.getLongitude(), location.getLatitude());
                        CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(currentCoords)
                            .zoom(DEFAULT_ZOOM_LEVEL)
                            .bearing(0)
                            .build();
                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                        double minDistance = Double.MAX_VALUE;
                        Point nearestPoint = null;
                        for (Point p : mCurrentLoopStopsList) {
                            double dist = TurfMeasurement.distance(currentPoint, p, "meters");
                            if (dist < minDistance) {
                                minDistance = dist;
                                nearestPoint = p;
                            }
                        }
                        if (mIsDwelling) {

                        } else {

                        }
                        if (nearestPoint != null) {
                            String stopKey = mAllStopsDictionary.getKey(nearestPoint);
                            if (mIsDwelling) {
                                if (minDistance > mStopProximityThresholdMeters) {
                                    if (location.getSpeed() > mShuttleSpeedThresholdMetersPerSec) {
                                        onShuttleDepartFromStop(stopKey);
                                    } else { // Shuttle exited shuttle proximity improperly, destroy dwelling record
                                        mIsDwelling = false;
                                        mStopWatch.reset();
                                        Snackbar.make(findViewById(R.id.coordinatorLayout), "Shuttle exited proximity improperly.", Snackbar.LENGTH_LONG).show();
                                    }
                                }
                            } else {
                                if (minDistance < mStopProximityThresholdMeters) {
                                    if (location.getSpeed() < mShuttleSpeedThresholdMetersPerSec) {
                                        onShuttleArriveAtStop(stopKey);
                                    } else { // Shuttle entered shuttle proximity improperly, destroy dwelling record
                                        mIsDwelling = true;
                                        mStopWatch.reset();
                                        Snackbar.make(findViewById(R.id.coordinatorLayout), "Shuttle entered proximity improperly.", Snackbar.LENGTH_LONG).show();
                                    }
                                }
                            }
                        }

                        // Construct ShuttleInformation object
                        ShuttleGeoJSONProperties shuttleProps = new ShuttleGeoJSONProperties(
                            mLoopKey,
                            mLoopKeyDisplayName,
                            System.currentTimeMillis(),
                            location.getSpeed(),
                            location.getAltitude(),
                            mPackageInfo.versionName,
                            mPrevCoordinates
                        );

                        GeoJSONGeometry geometry = new GeoJSONGeometry("Point", currentCoords);
                        ShuttleInformation shuttle = new ShuttleInformation("Feature", geometry, shuttleProps);

                        // Push to "/shuttles"
                        mNewShuttleRef.setValue(shuttle);

                        mPrevCoordinates = currentCoords;
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

    private void onShuttleArriveAtStop(String stopKey) {
        if (mStopWatch.isStopped()) {
            // Should only happen after app is started or if shuttle improperly entered stop
            // proximity, do not use for historical data
            mStopWatch.start();
        } else {
            mStopWatch.stop();
            float duration = mStopWatch.getTime() / 1000F;
            String message = String.format("Arrived at: %s after %f seconds.", stopKey, duration);
            Snackbar.make(findViewById(R.id.coordinatorLayout), message, Snackbar.LENGTH_INDEFINITE).show();
            mStopWatch.reset();
            mIsDwelling = true;
            mStopWatch.start();
        }
    }

    private void onShuttleDepartFromStop(String stopKey) {
        if (mStopWatch.isStopped()) {
            // Should only happen if shuttle improperly entered stop proximity, do not use for
            // historical data
            mStopWatch.start();
        } else {
            mStopWatch.stop();
            float duration = mStopWatch.getTime() / 1000F;
            String message = String.format("Departed from: %s after %f seconds.", stopKey, duration);
            mStopWatch.reset();
            mIsDwelling = false;
            mStopWatch.start();
            Snackbar.make(findViewById(R.id.coordinatorLayout), message, Snackbar.LENGTH_INDEFINITE).show();
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
    }
}
