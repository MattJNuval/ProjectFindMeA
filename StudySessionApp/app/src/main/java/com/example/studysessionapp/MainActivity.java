package com.example.studysessionapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.mapping.AndroidXMapFragment;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapLabeledMarker;
import com.here.android.mpa.mapping.MapObject;
import com.here.android.mpa.mapping.PositionIndicator;

import java.io.File;
import java.lang.ref.WeakReference;
import com.yelp.fusion.client.connection.YelpFusionApi;
import com.yelp.fusion.client.connection.YelpFusionApiFactory;
import com.yelp.fusion.client.models.Business;
import com.yelp.fusion.client.models.SearchResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {


    // region INSTANCE VARIABLES

    //region CONSTANTS
    // permissions request code
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;

    private final static String TAG = "NOGA";
    /**
     * Permissions that need to be explicitly requested from end user.
     */
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE };
    //endregion



    private PositioningManager positioningManager;
    private PositionIndicator positionIndicator;
    private boolean paused = false;
    private MapLabeledMarker mapLabeledMarker;
    private List<MapObject> mapObjectsList;
    private Image image;
    private EditText searchBarET;
    private boolean onCreateTrigger = true;
    private Map map = null;
    private AndroidXMapFragment mapFragment = null;
    //endregion

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissions();
        // all permissions were granted
        initialize();
        // Initializes mapObject List
        mapObjectsList = new ArrayList<>();
        // Initializes EditText for the search bar
        searchBarET = (EditText) findViewById(R.id.searchBar);
    }

    /***
     * onClick button that removes all existing markers on the map
     * @param view
     */
    public void removeAllMarkers(View view) {
        Toast.makeText(getApplicationContext(),"Marker removed", Toast.LENGTH_LONG).show();
        map.removeMapObjects(mapObjectsList);
    }

    /***
     * Sets markers based on Yelp's latitude and longitude results
     * @param name
     * @param latitude
     * @param longitude
     */
    public void setMarker(String name, double latitude, double longitude) {
        try {
            image = new Image();
            // Establish a bitmap version of a drawable item
            Bitmap icon = BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.mapmarker);
            // Set the image with the bipmap
            image.setBitmap(icon);
            //mapMarker = new MapMarker(new GeoCoordinate(latitude,longitude),image);
            mapLabeledMarker = new MapLabeledMarker(new GeoCoordinate(latitude,longitude),image);
            mapLabeledMarker.setLabelText(map.getMapDisplayLanguage(),name);
            mapObjectsList.add(mapLabeledMarker);
            map.addMapObject(mapLabeledMarker);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /***
     * Uses user input term and current position to find the nearest results in Yelps API
     * @param topic
     * @param latitude
     * @param longitude
     */
    public void searchYelp(String topic, String latitude, String longitude) {
        try {
            String apiKey = "hkkJIKWdwY1z1mnqBkp711ceL7Gx14oUa9Z7brHqklFM9fHbjeOWU_6NmWNGKqUYPrE0ilZIWMvzF4R87eGXAZ14dWHFzqYgRscBE7jX6TByS9fAYGSPKEQe5qAwXnYx";
            YelpFusionApiFactory yelpFusionApiFactory = new YelpFusionApiFactory();
            YelpFusionApi yelpFusionApi = yelpFusionApiFactory.createAPI(apiKey);
            Log.d(TAG, "Yelp Authentication Complete");


            HashMap<String, String> params = new HashMap<>();

            params.put("term", topic);
            params.put("latitude", latitude);
            params.put("longitude", longitude);

            Call<SearchResponse> call = yelpFusionApi.getBusinessSearch(params);

            Callback<SearchResponse> callback = new Callback<SearchResponse>() {
                @Override
                public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                    try {
                        SearchResponse searchResponse = response.body();
                        ArrayList<Business> businesses = searchResponse.getBusinesses();
                        for (int i = 0; i < 20; i++) {
                            String name = businesses.get(i).getName();
                            double latitude = businesses.get(i).getCoordinates().getLatitude();
                            double longitude = businesses.get(i).getCoordinates().getLongitude();
                            Log.d(TAG, businesses.get(i).getName() + "\n"
                                    + "(" + businesses.get(i).getCoordinates().getLatitude() + "," + businesses.get(i).getCoordinates().getLongitude() + ")");
                            setMarker(name, latitude, longitude);
                        }
                    } catch (IndexOutOfBoundsException e) {
                        Toast.makeText(getApplicationContext(),searchBarET.getText().toString() + " not found",Toast.LENGTH_LONG).show();
                    }


                    // Update UI text with the searchResponse.
                }
                @Override
                public void onFailure(Call<SearchResponse> call, Throwable t) {
                    Log.d(TAG,"UH OH! SUMTING WENT WONG!");
                    // HTTP error happened, do something to handle it.
                }
            };

            call.enqueue(callback);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /***
     * onClick button method to search user-inputted term
     * @param view
     */
    public void toSearch(View view) {
        try {
            if (!mapObjectsList.isEmpty()) {
                map.removeMapObjects(mapObjectsList);
            }
            Toast.makeText(getApplicationContext(), "Searching", Toast.LENGTH_LONG).show();
            String topic = searchBarET.getText().toString();
            Log.d(TAG, "Searching " + topic);
            searchYelp(topic, positioningManager.getPosition().getCoordinate().getLatitude() + "", positioningManager.getPosition().getCoordinate().getLongitude() + "");
        } catch (IllegalStateException e) {
            Toast.makeText(getApplicationContext(),searchBarET.getText().toString() + " not found",Toast.LENGTH_LONG).show();
        }
    }

    // Gets updates on the users current position on the map.
    private PositioningManager.OnPositionChangedListener positionListener =
            new PositioningManager.OnPositionChangedListener() {
                @Override
                public void onPositionUpdated(PositioningManager.LocationMethod locationMethod,
                                              GeoPosition geoPosition, boolean b) {

                    if(onCreateTrigger) {
                        map.setCenter(positioningManager.getPosition().getCoordinate(),
                                Map.Animation.BOW);
                        onCreateTrigger = false;
                    }

                    if (positioningManager != null) {
                        positioningManager.start(
                                PositioningManager.LocationMethod.GPS_NETWORK);
                    }
                }

                @Override
                public void onPositionFixChanged(PositioningManager.LocationMethod locationMethod,
                                                 PositioningManager.LocationStatus locationStatus) {

                }
            };

    /***
     * onClick button Centers map on the current position of the user
     * @param view
     */
    public void toCurrentLocation(View view) {
        map.setCenter(positioningManager.getPosition().getCoordinate(),
                Map.Animation.BOW);
        map.setZoomLevel(((map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2)*1.5);
    }

    private AndroidXMapFragment getMapFragment() {
        return (AndroidXMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapfragment);
    }

    @SuppressWarnings("deprecation")
    private void initialize() {
        setContentView(R.layout.activity_main);

        // Search for the map fragment to finish setup by calling init().
        mapFragment = getMapFragment();

        // Set up disk cache path for the map service for this application
        boolean success = com.here.android.mpa.common.MapSettings.setIsolatedDiskCacheRootPath(
                getApplicationContext().getExternalFilesDir(null) + File.separator + ".here-maps",
                "com.example.studysessionapp.MapService");

        if (!success) {
            Toast.makeText(getApplicationContext(), "Unable to set isolated disk cache path.", Toast.LENGTH_LONG);
        } else {
            mapFragment.init(new OnEngineInitListener() {
                @Override
                public void onEngineInitializationCompleted(
                        final OnEngineInitListener.Error error) {
                    if (error == OnEngineInitListener.Error.NONE) {
                        // retrieve a reference of the map from the map fragment
                        map = mapFragment.getMap();

                        map.setCenter(new GeoCoordinate(34.0589578,-118.3027765,0),
                                Map.Animation.NONE);
                        map.setZoomLevel((map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2);


                        if(positioningManager == null) {
                            positioningManager = PositioningManager.getInstance();
                            positioningManager.addListener(new WeakReference<>(positionListener));
                            positioningManager.start(PositioningManager.LocationMethod.GPS_NETWORK);
                        }

                        positionIndicator = map.getPositionIndicator();
                        positionIndicator.setVisible(true);
                        positionIndicator.setAccuracyIndicatorVisible(true);



                    } else {
                        System.out.println("ERROR: Cannot initialize Map Fragment");

                        runOnUiThread(new Runnable() {
                            @Override public void run() {
                                new AlertDialog.Builder(MainActivity.this).setMessage(
                                        "Error : " + error.name() + "\n\n" + error.getDetails())
                                        .setTitle(R.string.engine_init_error)
                                        .setNegativeButton(android.R.string.cancel,
                                                new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(
                                                            DialogInterface dialog,
                                                            int which) {
                                                        finishAffinity();
                                                    }
                                                }).create().show();
                            }
                        });
                    }
                }
            });
        }
    }

    /***
     * Menu button on the top of the Android App
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }


    /**
     * Checks the dynamically controlled permissions and requests missing permissions from end user.
     */
    protected void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<String>();
        // check all required dynamic permissions
        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            // request all missing permissions
            final String[] permissions = missingPermissions
                    .toArray(new String[missingPermissions.size()]);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                    grantResults);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int index = permissions.length - 1; index >= 0; --index) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        // exit the app if one permission is not granted
                        Toast.makeText(this, "Required permission '" + permissions[index]
                                + "' not granted, exiting", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
                break;
        }
    }

    // Resume current positioning listener on wake up
    public void onResume() {
        super.onResume();
        paused = false;
        if (positioningManager != null) {
            positioningManager.start(
                    PositioningManager.LocationMethod.GPS_NETWORK);
        }
    }

    // To pause current positioning listener
    public void onPause() {
        if (positioningManager != null) {
            positioningManager.stop();
        }
        super.onPause();
        paused = true;
    }

    // To remove the current positioning listener
    public void onDestroy() {
        if (positioningManager != null) {
            // Cleanup
            positioningManager.removeListener(
                    positionListener);
        }
        map = null;
        super.onDestroy();
    }
}

