package com.example.alanabundis.thieftrakerv2;


import android.Manifest;
import android.app.NotificationManager;
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
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Sergio on 08/04/2017.
 */

public class ServicioThief extends Service
{
    //Variables de comunicacion con la actividad principal
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_UBICACION_VALUE = 4;
    static final int MSG_AUMENTO_TIEMPO = 5;
    static final int MSG_FINAL_SERVICE = 6;
    //para comprobar que el proceso esta corriendo
    public static boolean isRunning = false;
    //TAG del servicio
    public final String TAG = "ASL-TT";
    //Variables de Ubicacion
    Double latitud = 0.0;
    Double longitud = 0.0;

    //Variables de control de tiempo de ejecucion de servicio
    public int tiempoTotal = 60, aumentoTiempo = 0, tiempoTranscurrido = 0;

    public int cantidadIncidentes = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            //Extraemos la ubicacion
            LocationManager myLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            MyLocationListener myll = new MyLocationListener();
            myLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) myll);
        }
        isRunning = true;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("MyService", "Received start id " + startId + ": " + intent);
        AsyncTask envioUbicacion = new AsyncTask();
        envioUbicacion.execute();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "Service Stopped.");
        Toast.makeText(this, "Servicio Detenido", Toast.LENGTH_SHORT).show();
        isRunning = false;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        //Comunicacion con el proceso
        return mMessenger.getBinder();
    }

    //Clase que servira de Listener para la Ubicacion
    private class MyLocationListener implements LocationListener
    {
        @Override
        public void onLocationChanged(Location location) {
            //Toast.makeText(ServicioThief.this, "Ubicacion Nueva", Toast.LENGTH_SHORT).show();
            //Cada que la ubicacion cambie renovamos latitud y longitud
            latitud = location.getLatitude();
            longitud = location.getLongitude();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider)
        {
            Log.e(TAG, "GPS Activo");
        }

        @Override
        public void onProviderDisabled(String provider)
        {
            Log.e(TAG, "GPS Desactivado");
        }
    }

    class IncomingHandler extends Handler {
        // Clase que maneja la comunicacion con el servicio
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_AUMENTO_TIEMPO:
                    aumentoTiempo = msg.arg1;
                    if(aumentoTiempo == 0)
                    {
                        tiempoTranscurrido = 0;
                    }
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void sendUbicacionaIU(Double latitud, Double longitud)
    {
        //Metodo para enviar la ubicacion
        for (int i=mClients.size()-1; i>=0; i--) {
            try {
                //Enviamos los Datos como un String
                Bundle b = new Bundle();
                b.putString("str1", latitud + "+" + longitud);
                Message msg = Message.obtain(null, MSG_UBICACION_VALUE);
                msg.setData(b);
                mClients.get(i).send(msg);
            }
            catch (RemoteException e) {
                //Si el clientes esta muerto o no responde lo quitamos de la Lista
                mClients.remove(i);
            }
        }
    }

    public static boolean isRunning()
    {
        return isRunning;
    }

    private class AsyncTask extends android.os.AsyncTask
    {
        @Override
        protected Object doInBackground(Object[] params) {
            try
            {
                while(tiempoTranscurrido < tiempoTotal)
                {
                    sendUbicacionaIU(latitud, longitud);
                    calcularIncidentesCercanos(latitud, longitud);
                    Thread.sleep(5000);
                    Log.e(TAG, "" + tiempoTranscurrido);
                    tiempoTranscurrido += aumentoTiempo;
                }
                this.cancel(true);
            }
            catch(InterruptedException ie)
            {
                Log.e(TAG, "No se ha podido enviar la ubicacion");
            }
            return null;
        }

        @Override
        protected void onCancelled(Object objeto) {
            ServicioThief.this.stopSelf();
        }
    }

    public void calcularIncidentesCercanos(Double latitud, Double Longitud)
    {
        final Gson gson = new Gson();
        JsonObjectRequest request;
        VolleySingleton.getInstance(ServicioThief.this).
                addToRequestQueue(
                        request = new JsonObjectRequest(
                                Request.Method.GET,
                                "http://nmrapp.hol.es/contar_cercanos.php?lat=" + Double.toString(latitud) + "&lon=" + Double.toString(longitud),
                                null,
                                new Response.Listener<JSONObject>(){
                                    @Override
                                    public void onResponse(JSONObject response){
                                        try{
                                            String estado = response.getString("estado");
                                            switch (estado){
                                                case "1":
                                                    JSONArray jArrayIncidentes = response.getJSONArray("registros");
                                                    Incidentes[] arrayIncidentes = gson.fromJson(jArrayIncidentes.toString(), Incidentes[].class);
                                                    for(int i = 0; i < arrayIncidentes.length; i++)
                                                    {
                                                        cantidadIncidentes += arrayIncidentes[i].getCantidad();
                                                    }
                                                    if(cantidadIncidentes > 0)
                                                    {
                                                        NotificationManager nManager = (NotificationManager) getSystemService(getApplicationContext().NOTIFICATION_SERVICE);
                                                        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                                                                getBaseContext())
                                                                .setSmallIcon(R.drawable.ic_notification)
                                                                .setContentTitle("Alerta!")
                                                                .setContentText("Estas en una zona Peligrosa")
                                                                .setWhen(System.currentTimeMillis());
                                                        nManager.notify(12345, builder.build());
                                                        cantidadIncidentes = 0;
                                                    }
                                                    break;
                                                case "0":
                                                    break;
                                            }
                                            return;
                                        }catch(JSONException json){
                                            json.printStackTrace();
                                        }
                                    }
                                },
                                new Response.ErrorListener(){
                                    @Override
                                    public void onErrorResponse(VolleyError error){
                                        Log.e(TAG, error.toString());
                                        //Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                }
                        )
                );
        request.setRetryPolicy(new RetryPolicy() {
            @Override
            public int getCurrentTimeout() {
                return 50000;
            }

            @Override
            public int getCurrentRetryCount() {
                return 50000;
            }

            @Override
            public void retry(VolleyError error) throws VolleyError {

            }
        });
    }

    private class Incidentes
    {
        private int Tipo, Cantidad;

        public Incidentes(int Tipo, int Cantidad)
        {
            this.Tipo = Tipo;
            this.Cantidad = Cantidad;
        }

        public int getTipo() {
            return Tipo;
        }

        public void setTipo(int tipo) {
            Tipo = tipo;
        }

        public int getCantidad() {
            return Cantidad;
        }

        public void setCantidad(int cantidad) {
            Cantidad = cantidad;
        }
    }
}
