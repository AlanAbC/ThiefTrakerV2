package com.claresti.tt.thieftrakerv2;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
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
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
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
import java.util.StringTokenizer;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    public static final int REQUEST_LOCATION = 1;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private Location mLastLocation;
    AlertDialog alert = null;
    LocationManager locationManager;

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
    private static final String LOGTAG = "ALM-TT";
    private List<Address> address;

    //Comunicacion con el Servicio
    Messenger mService = null;
    boolean mIsBound;
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        processLastLocation();
        // Iniciamos las actualizaciones de ubicación
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(LOGTAG, "Conexión suspendida");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(
                this,
                "Error de conexión con el código:" + connectionResult.getErrorCode(),
                Toast.LENGTH_LONG)
                .show();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        updateLocationUI();
    }

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ServicioThief.MSG_UBICACION_VALUE:
                    String str1 = msg.getData().getString("str1");
                    StringTokenizer st = new StringTokenizer(str1, "+");
                    String lati = st.nextToken();
                    String longi = st.nextToken();
                    lat = Double.parseDouble(lati);
                    lon = Double.parseDouble(longi);
                    if(lat != 0.0 && lon != 0.0)
                    {
                        mGoogleApiClient.disconnect();
                        actualizarMapa();
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            try {
                Message msg = Message.obtain(null, ServicioThief.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            }
            catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mIsBound = false;
            mService = null;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Establecer punto de entrada para la API de ubicación
        buildGoogleApiClient();

        // Crear configuración de peticiones
        createLocationRequest();

        // Crear opciones de peticiones
        buildLocationSettingsRequest();

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if ( !locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            checkLocationSettings();
        }

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


        doBindService();
        sendMessageToService(0);
    }

    void doBindService() {
        if(!mIsBound)
        {
            bindService(new Intent(this, ServicioThief.class), mConnection, Context.BIND_AUTO_CREATE);
            mIsBound = true;
        }

    }

    public void actualizarMapa()
    {
        if(flag2 == 0){
            LatLng coordenadas = new LatLng(lat, lon);
            CameraUpdate camara = CameraUpdateFactory.newLatLngZoom(coordenadas, 15);
            mMap.animateCamera(camara);
            actualizarMarcadores();
            flag2 ++;
        }else {
            if (lat > lat + 0.00058 && lat < lat - 0.00058 && lon > lon + 0.00058 && lon < lon - 0.00058) {
                LatLng coordenadas = new LatLng(lat, lon);
                CameraUpdate camara = CameraUpdateFactory.newLatLngZoom(coordenadas, 15);
                mMap.animateCamera(camara);
                actualizarMarcadores();
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
        LatLng posUsu = new LatLng(lat, lon);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(posUsu));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15.0f));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                startLocationUpdates();

            } else {
                Log.e(LOGTAG, "Permisos no otorgados");
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

    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(ActivityRecognition.API)
                .enableAutoManage(this, this)
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

    private void checkLocationSettings() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("El sistema GPS esta desactivado, ¿Desea activarlo?")
                .setCancelable(false)
                .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        alert = builder.create();
        alert.show();
    }

    private void startLocationUpdates() {
        if (isLocationPermissionGranted()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, MapsActivity.this);
        } else {
            manageDeniedPermission();
        }
    }

    private boolean isLocationPermissionGranted() {
        int permission = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    private void manageDeniedPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Aquí muestras confirmación explicativa al usuario
            // por si rechazó los permisos anteriormente
        } else {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        }
    }

    private void processLastLocation() {
        getLastLocation();
        if (mLastLocation != null) {
            updateLocationUI();
        }
    }

    private void getLastLocation() {
        if (isLocationPermissionGranted()) {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        } else {
            manageDeniedPermission();
        }
    }

    private void updateLocationUI() {
        lat = mLastLocation.getLatitude();
        lon = mLastLocation.getLongitude();
        actualizarMapa();
    }

    private void sendMessageToService(int intvaluetosend) {
        if (mIsBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, ServicioThief.MSG_AUMENTO_TIEMPO, intvaluetosend, 0);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                }
            }
        }
    }

    //La aplicacion pase a primer plano
    @Override
    public void onResume()
    {
        super.onResume();
        startService(new Intent(MapsActivity.this, ServicioThief.class));
        doBindService();
        sendMessageToService(0);
    }

    //La aplicacion pase a segundo plano
    @Override
    public void onPause()
    {
        super.onPause();
        sendMessageToService(5);
    }



}
