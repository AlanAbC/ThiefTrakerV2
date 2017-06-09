package com.claresti.tt.thieftraker;


import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
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

    private LocationManager locationManager;
    Criteria criteria;


    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("MyService", "Received start id " + startId + ": " + intent);
        locationManager = (LocationManager) getSystemService(getApplicationContext().LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return START_STICKY;
        }
        criteria = new Criteria();
        locationManager.requestLocationUpdates(5000, 100, criteria, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                latitud = location.getLatitude();
                longitud = location.getLongitude();
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        }, null);
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

    private class AsyncTask extends android.os.AsyncTask
    {
        @Override
        protected Object doInBackground(Object[] params) {
            try
            {
                while(tiempoTranscurrido < tiempoTotal)
                {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return 0;
                    }

                    Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
                    if(location != null) {
                        latitud = location.getLatitude();
                        longitud = location.getLongitude();
                        sendUbicacionaIU(latitud, longitud);
                        calcularIncidentesCercanos(latitud, longitud);
                        Log.e(TAG, "" + tiempoTranscurrido + latitud + longitud);
                    }
                    Thread.sleep(5000);
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
                                "http://tt.claresti.com/contar_cercanos.php?lat=" + Double.toString(latitud) + "&lon=" + Double.toString(longitud),
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
                                                                .setSmallIcon(R.drawable.icono_not_pre)
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
