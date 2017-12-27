package com.fusedlocationtracker;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.fusedlocationtracker.receiver.LocationUpdatesBroadcastReceiver;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

/**
 * Created by Darshna Desai on 12/12/17
 */

public class FusedLocationProviderUtil {

    private Activity mActivity;
    private static FusedLocationProviderUtil gpsLocation;
    private CurrentLocationListener currentLocationListener;

    private SettingsClient mSettingsClient;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private LocationSettingsRequest mLocationSettingsRequest;

    private long UPDATE_INTERVAL = 10 * 1000;  /* 10 secs */
    private long FASTEST_INTERVAL = 2000; /* 2 sec */
    private float DISPLACEMENT = 500f; // 500f meters

    public static final int REQUEST_CHECK_SETTINGS = 0x1;
    public static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    public static FusedLocationProviderUtil getInstance(Activity activity, CurrentLocationListener currentLocationListener) {
        if (gpsLocation == null) {
            gpsLocation = new FusedLocationProviderUtil(activity, currentLocationListener);
        } else {
            gpsLocation.initData(activity, currentLocationListener);
        }
        return gpsLocation;
    }

    public static FusedLocationProviderUtil getInstance() {
        if (gpsLocation == null) {
            gpsLocation = new FusedLocationProviderUtil();
        }
        return gpsLocation;
    }

    private FusedLocationProviderUtil(Activity activity, CurrentLocationListener currentLocationListener) {
        initData(activity, currentLocationListener);
    }

    public FusedLocationProviderUtil() {
    }

    private void initData(Activity activity, CurrentLocationListener currentLocationListener) {
        this.mActivity = activity;
        this.currentLocationListener = currentLocationListener;
        init();
    }

    public void setActivity(Activity activity) {
        this.mActivity = activity;
    }

    public void setListener(CurrentLocationListener currentLocationListener) {
        this.currentLocationListener = currentLocationListener;
    }

    public CurrentLocationListener getListener() {
        return currentLocationListener;
    }

    private void init() {
        initializeVariables();
        checkPermissionAndGetLocation();
    }

    private void initializeVariables() {
        if (mSettingsClient == null && mFusedLocationClient == null) {
            mSettingsClient = LocationServices.getSettingsClient(mActivity);
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mActivity);

            createLocationCallback();
            createLocationRequest();
            buildLocationSettingsRequest();
        }
    }

    private void checkPermissionAndGetLocation() {
        if (checkPermissions()) {
            getLocation();
        } else if (!checkPermissions()) {
            requestPermissions();
        }
    }

    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (currentLocationListener != null) {
                    currentLocationListener.onLocationUpdate(locationResult.getLastLocation());
                }
            }
        };
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    @SuppressWarnings("MissingPermission")
    public void getLocation() {
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(mActivity, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.e("Success", "All location settings are satisfied.");
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, getPendingIntent());
                        /*mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());*/
                    }
                })
                .addOnFailureListener(mActivity, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.e("FAILURE", "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(mActivity, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.e("Error", "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e("Error", errorMessage);
                                Toast.makeText(mActivity, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }


    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(mActivity,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(mActivity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    private PendingIntent getPendingIntent() {
        // Note: for apps targeting API level 25 ("Nougat") or lower, either
        // PendingIntent.getService() or PendingIntent.getBroadcast() may be used when requesting
        // location updates. For apps targeting API level O, only
        // PendingIntent.getBroadcast() should be used. This is due to the limits placed on services
        // started in the background in "O".

        // TODO(developer): uncomment to use PendingIntent.getService().
//        Intent intent = new Intent(this, LocationUpdatesIntentService.class);
//        intent.setAction(LocationUpdatesIntentService.ACTION_PROCESS_UPDATES);
//        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intent = new Intent(mActivity, LocationUpdatesBroadcastReceiver.class);
        intent.setAction(LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES);
        return PendingIntent.getBroadcast(mActivity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(getPendingIntent()).addOnCompleteListener(mActivity
                , new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        currentLocationListener = null;
                    }
                });

        /*mFusedLocationClient.removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(mActivity, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        currentLocationListener = null;
                    }
                });*/
    }

    @SuppressWarnings("MissingPermission")
    private void getCurrentLocation() {

        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // GPS location can be null if GPS is switched off
                        if (location != null) {
                            Log.e("SUCCESS", "getLastLocation" + location);
                            if (currentLocationListener != null) {
                                currentLocationListener.onLocationUpdate(location);
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("MapDemoActivity", "Error trying to get last GPS location");
                        e.printStackTrace();
                    }
                });

        /*mFusedLocationClient.getLastLocation().addOnCompleteListener(mActivity, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            Log.e("SUCCESS", "getLastLocation" + task.getResult());
                            if (currentLocationListener != null) {
                                currentLocationListener.onLocationUpdate(task.getResult());
                            }
                        } else {
                            Log.e("Failure", "getLastLocation:exception", task.getException());
                        }
                    }
                });*/

    }

}
