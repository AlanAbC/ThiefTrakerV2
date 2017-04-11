package com.example.alanabundis.thieftrakerv2;


import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

/**
 * Created by Sergio on 08/04/2017.
 */

public class ServicioThief extends Service
{
    //Variables de comunicacion
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_UBICACION_VALUE = 4;
    static boolean isRunning = false;
    public final String TAG = "ASL-TT";
    Double latitud = 0.0;
    Double longitud = 0.0;


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
        isRunning = true;
        AsyncTask envioUbicacion = new AsyncTask();
        envioUbicacion.execute();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("MyService", "Received start id " + startId + ": " + intent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("MyService", "Service Stopped.");
        isRunning = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    private class MyLocationListener implements LocationListener
    {
        @Override
        public void onLocationChanged(Location location) {
            //Toast.makeText(ServicioThief.this, "Ubicacion Nueva", Toast.LENGTH_SHORT).show();
            latitud = location.getLatitude();
            longitud = location.getLongitude();
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

    class IncomingHandler extends Handler { // Handler of incoming messages from clients.
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void sendUbicacionaIU(Double latitud, Double longitud) {
        for (int i=mClients.size()-1; i>=0; i--) {
            try {
                // Send data as an Integer
                Bundle b = new Bundle();
                b.putString("str1", latitud + "+" + longitud);
                Message msg = Message.obtain(null, MSG_UBICACION_VALUE);
                msg.setData(b);
                mClients.get(i).send(msg);
            }
            catch (RemoteException e) {
                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }

    public static boolean isRunning()
    {
        return isRunning;
    }

    private class AsyncTask extends android.os.AsyncTask implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
    {
        GoogleApiClient googleApi;
        @Override
        protected void onPreExecute()
        {
            googleApi = new GoogleApiClient.Builder(ServicioThief.this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            googleApi.connect();
        }

        @Override
        protected Object doInBackground(Object[] params) {
            try
            {
                while(isRunning)
                {
                    if (googleApi.isConnected()) {
                        if (ActivityCompat.checkSelfPermission(ServicioThief.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(ServicioThief.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                        {
                            Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApi);
                            if (mLastLocation != null) {

                                Log.d(TAG, "getLastLocation: " + mLastLocation.toString());
                                latitud = mLastLocation.getLatitude();
                                longitud = mLastLocation.getLongitude();
                                //A alan le gusta la musica de banda :v
                            }
                        }
                    }
                    sendUbicacionaIU(latitud, longitud);
                    Thread.sleep(10000);
                }
                googleApi.disconnect();
            }
            catch(InterruptedException ie)
            {
                Log.e(TAG, "No se ha podido enviar la ubicacion");
            }
            return null;
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {

        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        }
    }
}
