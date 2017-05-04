package com.claresti.tt.thieftraker;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;

/**
 * Created by smp_3 on 24/04/2017.
 */

public class TTSingletonUbicacion implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener
{
    private static  TTSingletonUbicacion instancia = null;
    private Context context;

    private double latitud = 0, longitud = 0;

    private static final String LOGTAG = "SU-TT";

    private Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;

    protected TTSingletonUbicacion(Context context)
    {
        this.context = context;
        buildGoogleApiClient();
        createLocationRequest();
        buildLocationSettingsRequest();
    }

    public static TTSingletonUbicacion obtenerInstancia(Context context)
    {
        if(instancia == null)
        {
            instancia = new TTSingletonUbicacion(context);
        }

        return instancia;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        processLastLocation();
        // Iniciamos las actualizaciones de ubicación
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        Log.d(LOGTAG, "Conexión suspendida");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        Log.e(LOGTAG, "Error de conexión, Codigo: " + connectionResult.getErrorCode());
    }

    @Override
    public void onLocationChanged(Location location)
    {
        mLastLocation = location;
        updateLocations();
    }

    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(ActivityRecognition.API)
                .build();
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest()
                .setInterval(1000)
                .setFastestInterval(500)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest)
                .setAlwaysShow(true);
        mLocationSettingsRequest = builder.build();
    }

    private void processLastLocation() {
        getLastLocation();
        if (mLastLocation != null) {
            updateLocations();
        }
    }

    private void getLastLocation() {
        try
        {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }
        catch(SecurityException se)
        {
            Log.e(LOGTAG, se.getMessage());
        }
    }

    private void updateLocations() {
        latitud = mLastLocation.getLatitude();
        longitud = mLastLocation.getLongitude();
    }

    private void startLocationUpdates() {
        try
        {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, TTSingletonUbicacion.this);
        }
        catch(SecurityException se)
        {
            Log.e(LOGTAG, se.getMessage());
        }
    }
}
