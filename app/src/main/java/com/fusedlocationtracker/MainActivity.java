package com.fusedlocationtracker;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


/**
 * Created by Darshna Desai on 27/12/17
 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener, CurrentLocationListener {

    private Button btnStart, btnStop;
    private TextView tvLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init() {
        getViewIds();
        setListener();
    }

    private void getViewIds() {
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        tvLocation = findViewById(R.id.tvLocation);
    }

    private void setListener() {
        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnStart:
                startLocationUpdates();
                break;

            case R.id.btnStop:
                stopLocationUpdates();
                break;
        }
    }

    private void startLocationUpdates() {
        FusedLocationProviderUtil.getInstance(MainActivity.this, this);
    }

    private void stopLocationUpdates() {
        FusedLocationProviderUtil.getInstance().stopLocationUpdates();
    }

    @Override
    public void onLocationUpdate(Location location) {
        tvLocation.setText("Latitude: " + location.getLatitude() + " & Longitude: " + location.getLongitude());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case FusedLocationProviderUtil.REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.e("Success", "User agreed to make required location settings changes.");
                        FusedLocationProviderUtil gpsUtil = FusedLocationProviderUtil.getInstance();
                        gpsUtil.getLocation();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.e("Failure", "User chose not to make required location settings changes.");
                        break;
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == FusedLocationProviderUtil.REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                Log.e("ERROR", "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.e("Success", "Permission granted, updates requested, starting location updates");
                FusedLocationProviderUtil gpsUtil = FusedLocationProviderUtil.getInstance();
                gpsUtil.getLocation();
            } else {
                showSnackbar("Permission was denied, but is needed for core functionality.",
                        R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }

    private void showSnackbar(final String mainTextString, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                mainTextString,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

}
