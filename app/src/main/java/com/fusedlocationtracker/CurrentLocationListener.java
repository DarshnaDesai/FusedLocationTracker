package com.fusedlocationtracker;

import android.location.Location;

/**
 * Created by Darshna Desai on 27/12/17
 */

public interface CurrentLocationListener {
    void onLocationUpdate(Location location);
}
