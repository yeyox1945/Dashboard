package com.corpalabs.zubut.dashboard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.ebanx.swipebtn.OnStateChangeListener;
import com.ebanx.swipebtn.SwipeButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks {

    private GoogleMap mMap;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private ArrayList<String> lstPlaces = new ArrayList<>();
    private ArrayList<String> lstLocations = new ArrayList<>();
    private SwipeButton swpAddLocation;
    private ArrayAdapter<String> adapter;
    private SharedPreferences sharedPreferences;

    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Borrar las listas y actualizar el listView
        lstPlaces.clear();
        lstLocations.clear();
        sharedPreferences.edit().clear().apply();
        adapter.notifyDataSetChanged();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = initGoogleApi();
        }

        sharedPreferences = this.getSharedPreferences("com.corpalabs.zubut.dashboard", Context.MODE_PRIVATE);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Sacar las dimensiones de la pantalla del dispositivo.
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int height = metrics.heightPixels; // alto absoluto en pixeles

        // Ajustar la altura del fragment a la mitad de la pantalla.
        ViewGroup.LayoutParams params = mapFragment.getView().getLayoutParams();
        params.height = height / 2 - 100;
        mapFragment.getView().setLayoutParams(params);

        // Boton slide para agregar ubicacion a la lista.
        swpAddLocation = findViewById(R.id.swpAddLocation);

        // Llenado del listview.
        ListView lstvPlaces = findViewById(R.id.lstvPlaces);
        try {
            lstPlaces = (ArrayList<String>) ObjectSerializer.deserialize(sharedPreferences.getString("Places", ObjectSerializer.serialize(new ArrayList<>())));
            lstLocations = (ArrayList<String>) ObjectSerializer.deserialize(sharedPreferences.getString("Locations", ObjectSerializer.serialize(new ArrayList<>())));
        } catch (Exception e) {
            e.printStackTrace();
        }

        adapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, lstPlaces);
        lstvPlaces.setAdapter(adapter);
        lstvPlaces.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, DetailsActivity.class);
                intent.putExtra("Position", position);
                intent.putExtra("Places", lstPlaces);
                intent.putExtra("Locations", lstLocations);
                startActivity(intent);
            }
        });

        swpAddLocation.setOnStateChangeListener(new OnStateChangeListener() {
            @Override
            public void onStateChange(boolean active) {
                if (active && mLocation != null) {
                    saveAdress(mLocation);
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    private void alertNoGPS() {
        new AlertDialog.Builder(this).setTitle("GPS apagado")
                .setIcon(R.drawable.ic_location_off_black_24dp)
                .setMessage("Para el funcionamiento correcto de la app se necesita activar el GPS, deseas activarlo?")
                .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .show();
    }

    /**
     * Build google API
     */
    private GoogleApiClient initGoogleApi() {
        return new GoogleApiClient
                .Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mGoogleApiClient.connect();

        MapStyleOptions nightStyle = MapStyleOptions.loadRawResourceStyle(MainActivity.this, R.raw.night_map);

        mMap.setMapStyle(nightStyle);

        // Validar si el GPS del dispositivo esta activado.
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            alertNoGPS();
        }

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(final Location location) {

                // Añadir marcador en mi ubicacion.
                mMap.clear();

                // Añadimos el marcador al mapa
                addMarker(location, mMap);

                // Movemos la camara a la posición
                setupMap(location, mMap, false);

                // Guardamos la localización
                mLocation = location;
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
                //TODO
            }

            @Override
            public void onProviderEnabled(String s) {
                //TODO
            }

            @Override
            public void onProviderDisabled(String s) {
                //TODO
            }
        };

        // Checar si hay permisos de utilizar la ubicacion del dispositivo.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
    }

    // Pedir los permisos para acceder a la ubicacion del dispositivo.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }
        }
    }

    // Connection callback
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        try {
            Location location = getLastLocation(mGoogleApiClient);
            if (location != null) {
                mLocation = location;
                setupMap(location, mMap, true);
                addMarker(location, mMap);
            }
        } catch (Exception e) {
            Log.e(MainActivity.class.getSimpleName(), e.toString());
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "Connection fail", Toast.LENGTH_SHORT).show();
    }

    /**
     * Get last location
     *
     * @param mGoogleApiClient
     * @return Last location
     */
    @SuppressLint("MissingPermission")
    private Location getLastLocation(GoogleApiClient mGoogleApiClient) {
        return LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }

    /**
     * Add markers in map
     *
     * @param location
     * @param googleMap
     */
    private void addMarker(Location location, GoogleMap googleMap) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(String.format("%s, %s", location.getLatitude(), location.getLongitude()))
                .visible(true));
    }

    /**
     * Setup map on location
     *
     * @param location
     * @param googleMap
     * @param withZoom
     */
    private void setupMap(Location location, GoogleMap googleMap, boolean withZoom) {
        LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());

        CameraUpdate cameraUpdate = withZoom ?
                CameraUpdateFactory.newLatLngZoom(latlng, 15) :
                CameraUpdateFactory.newLatLng(latlng);

        googleMap.moveCamera(cameraUpdate);
    }

    private void saveAdress(Location location) {
        //Guardar la ubicacion
        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        String address = "";

        try {
            List<Address> addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

            if (addressList.size() > 0) {

                if (addressList.get(0).getThoroughfare() == null) {
                    Date currentTime = Calendar.getInstance().getTime();
                    address = currentTime.toString();
                } else {

                    if (addressList.get(0).getAddressLine(0) != null)
                        address += addressList.get(0).getAddressLine(0) + " ";

                    if (addressList.get(0).getAddressLine(1) != null)
                        address += addressList.get(0).getAddressLine(1) + " ";

                    if (addressList.get(0).getAddressLine(2) != null)
                        address += addressList.get(0).getAddressLine(2) + " ";

                    if (addressList.get(0).getAddressLine(3) != null)
                        address += addressList.get(0).getAddressLine(3);
                }
            }

            lstPlaces.add(address);
            lstLocations.add(location.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }

        adapter.notifyDataSetChanged();

        // Guardar en memoria para almacenamiento permanente.
        try {
            sharedPreferences.edit().putString("Places", ObjectSerializer.serialize(lstPlaces)).apply();
            sharedPreferences.edit().putString("Locations", ObjectSerializer.serialize(lstLocations)).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(MainActivity.this, "Ubicacion guardada", Toast.LENGTH_SHORT).show();
    }
}
