package com.example.alanabundis.thieftrakerv2;


import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Sergio on 08/04/2017.
 */

public class ServicioThief extends Service
{
    private Double latitud;
    private Double longitud;
    public final String TAG = "ASL-TT";

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(ServicioThief.this, "Servicio Iniciado", Toast.LENGTH_SHORT).show();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            LocationManager myLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            MyLocationListener myll = new MyLocationListener();
            myLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) myll);
            Toast.makeText(ServicioThief.this, "Actualizaciones Activadas", Toast.LENGTH_SHORT).show();
        }


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Servicio destruído!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private class MyLocationListener implements LocationListener
    {

        @Override
        public void onLocationChanged(Location location) {
            Toast.makeText(ServicioThief.this, "Latitud: " + location.getLatitude(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e(TAG, "GPS Activo");
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, "GPS Desactivado");
        }
    }
}
