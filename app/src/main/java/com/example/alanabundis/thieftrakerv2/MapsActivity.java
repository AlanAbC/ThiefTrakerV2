package com.example.alanabundis.thieftrakerv2;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ImageButton reporte;
    private ImageButton acerca;
    private ImageButton buscar;
    private EditText direccion;
    private Double lat;
    private Double lon;
    private int flag = 0;
    private int flag2 = 0;
    private String d;
    private static final int myPermiso = 1;
    private static final String LOGTAG = "android-localizacion";
    private List<Address> address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        startService(new Intent(MapsActivity.this, ServicioThief.class));
        direccion = (EditText) findViewById(R.id.txtDireccion);
        reporte = (ImageButton) findViewById(R.id.btnRegistro);
        acerca = (ImageButton) findViewById(R.id.acercade);
        buscar = (ImageButton) findViewById(R.id.btnBuscar);
        reporte.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MapsActivity.this, reporte.class);
                startActivity(i);
            }
        });
        acerca.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MapsActivity.this, acerca.class);
                startActivity(i);
            }
        });
        buscar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d = direccion.getText().toString();
                if(d.equals("")){
                    Toast.makeText(getApplicationContext(), "No hay dirrección para buscar", Toast.LENGTH_LONG).show();
                }else{
                    try{
                        Geocoder coder = new Geocoder(getApplicationContext());
                        address = coder.getFromLocationName(d, 1);
                        Address location = address.get(0);
                        if(location != null){
                            lat = location.getLatitude();
                            lon = location.getLongitude();
                            LatLng coordenadas = new LatLng(lat, lon);
                            CameraUpdate camara = CameraUpdateFactory.newLatLngZoom(coordenadas, 18);
                            mMap.animateCamera(camara);
                            actualizarMarcadores();
                        }else{
                            Toast.makeText(getApplicationContext(), "No se encontró la dirección", Toast.LENGTH_LONG).show();
                        }
                    }catch(IOException e){
                        Toast.makeText(getApplicationContext(), "No se encontró la dirección", Toast.LENGTH_LONG).show();
                    }
                }
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(direccion.getWindowToken(), 0);
            }
        });
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, myPermiso);
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        lat = 20.5917044;
        lon = -100.3880343;
        //LatLng posUsu = new LatLng(lat, lon);
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(posUsu));
        //mMap.animateCamera(CameraUpdateFactory.zoomTo(15.0f));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
                @Override
                public void onMyLocationChange(Location location) {
                    if(flag2 == 0){
                        lat = location.getLatitude();
                        lon = location.getLongitude();
                        LatLng coordenadas = new LatLng(lat, lon);
                        CameraUpdate camara = CameraUpdateFactory.newLatLngZoom(coordenadas, 15);
                        mMap.animateCamera(camara);
                        actualizarMarcadores();
                        flag2 ++;
                    }else{
                        if(location.getLatitude() > lat + 0.00058 && location.getLatitude() < lat - 0.00058 && location.getLongitude() > lon + 0.00058 && location.getLongitude() < lon - 0.00058){
                            lat = location.getLatitude();
                            lon = location.getLongitude();
                            LatLng coordenadas = new LatLng(lat, lon);
                            CameraUpdate camara = CameraUpdateFactory.newLatLngZoom(coordenadas, 15);
                            mMap.animateCamera(camara);
                            actualizarMarcadores();
                        }
                    }
                }
            });
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        switch (requestCode) {
            case myPermiso: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mMap.setMyLocationEnabled(true);
                        mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
                            @Override
                            public void onMyLocationChange(Location location) {
                                lat = location.getLatitude();
                                lon = location.getLongitude();
                                LatLng coordenadas = new LatLng(lat, lon);
                                CameraUpdate camara = CameraUpdateFactory.newLatLngZoom(coordenadas, 15);
                                mMap.animateCamera(camara);
                            }
                        });
                    }
                } else {

                }
                return;
            }
        }
    }


    private void actualizarMarcadores(){
        final Gson gson = new Gson();
        JsonObjectRequest request;
        VolleySingleton.getInstance(MapsActivity.this).
                addToRequestQueue(
                        request = new JsonObjectRequest(
                                Request.Method.GET,
                                "http://nmrapp.hol.es/puntosCercanos.php?lat=" + Double.toString(lat) + "&lon=" + Double.toString(lon),
                                null,
                                new Response.Listener<JSONObject>(){
                                    @Override
                                    public void onResponse(JSONObject response){
                                        try{
                                            String estado = response.getString("estado");
                                            switch (estado){
                                                case "1":
                                                    JSONArray jArrayMarcadores = response.getJSONArray("registros");
                                                    objetoSucesos[] arrayMarcadores = gson.fromJson(jArrayMarcadores.toString(), objetoSucesos[].class);
                                                    for(int i = 0; i < arrayMarcadores.length; i++){
                                                        agregarMarcador(arrayMarcadores[i]);
                                                    }/*
                                                    if(flag > 2)
                                                    {
                                                        NotificationManager nManager = (NotificationManager) getSystemService(getApplicationContext().NOTIFICATION_SERVICE);
                                                        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                                                                getBaseContext())
                                                                .setSmallIcon(R.drawable.ic_notificacion)
                                                                .setContentTitle("Alerta!")
                                                                .setContentText("Estas en una zona Peligrosa")
                                                                .setWhen(System.currentTimeMillis());
                                                        nManager.notify(12345, builder.build());

                                                    }*/
                                                    flag = 0;
                                                    break;
                                                case "0":
                                                    //Toast.makeText(getApplicationContext(), response.toString(), Toast.LENGTH_LONG).show();
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
                                        Log.e(LOGTAG, error.toString());
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

    private void agregarMarcador(objetoSucesos sucesos){
        LatLng pos = new LatLng(Double.parseDouble(sucesos.getLATITUD()), Double.parseDouble(sucesos.getLONGITUD()));
        if(lat < Double.parseDouble(sucesos.getLATITUD()) + 0.00252 && lat > Double.parseDouble(sucesos.getLATITUD()) - 0.00252 && lon < Double.parseDouble(sucesos.getLONGITUD()) + 0.00252 && lon > Double.parseDouble(sucesos.getLONGITUD()) - 0.00252)
        {
            flag++;
        }
        switch (sucesos.getID_TIPO()){
            case "1":
                mMap.addMarker(new MarkerOptions().position(pos).title("Robo a casa").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                break;
            case "2":
                mMap.addMarker(new MarkerOptions().position(pos).title("Robo automovil").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                break;
            case "3":
                mMap.addMarker(new MarkerOptions().position(pos).title("Asalto").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                break;
            case "4":
                mMap.addMarker(new MarkerOptions().position(pos).title("Vandalismo").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
                break;
            case "5":
                mMap.addMarker(new MarkerOptions().position(pos).title("Violacion").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
                break;
            case "6":
                mMap.addMarker(new MarkerOptions().position(pos).title("Drogatictos/Borrachos").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE)));
                break;
            case "7":
                mMap.addMarker(new MarkerOptions().position(pos).title("Cristalazo").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));
                break;
        }
    }
}
