package com.corpalabs.zubut.dashboard;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;

import com.ebanx.swipebtn.OnStateChangeListener;
import com.ebanx.swipebtn.SwipeButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    LocationManager locationManager;
    LocationListener locationListener;
    ArrayList<String> lstPlaces = new ArrayList<>();
    ArrayList<String> lstLocations = new ArrayList<>();
    SwipeButton swpAddLocation;
    ArrayAdapter<String> adapter;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        adapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, lstPlaces);
        lstvPlaces.setAdapter(adapter);
        lstvPlaces.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), DetailsActivity.class);
                startActivity(intent);
            }
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        MapStyleOptions nightStyle = MapStyleOptions.loadRawResourceStyle(MainActivity.this, R.raw.night_map);
        mMap.setMapStyle(nightStyle);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                // Añadir marcador en mi ubicacion.
                mMap.clear();
                final LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.addMarker(new MarkerOptions().position(myLocation).title("Mi ubicacion"));


                swpAddLocation.setOnStateChangeListener(new OnStateChangeListener() {
                    @Override
                    public void onStateChange(boolean active) {

                        //Guardar la ubicacion
                        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                        String address = "";

                        try {
                            List<Address> addressList = geocoder.getFromLocation(myLocation.latitude, myLocation.longitude, 1);
                            Log.i("Addresses", addressList.get(0).toString());

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
                            Log.i("Address", address);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        adapter.notifyDataSetChanged();
                        Toast.makeText(MainActivity.this, "Ubicacion guardada", Toast.LENGTH_SHORT).show();
                    }
                });
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
        };

        // Checar si hay permisos de utilizar la ubicacion del dispositivo.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            LatLng lastLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            mMap.addMarker(new MarkerOptions().position(lastLocation)
                    .title("Last location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLocation, 12));
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

}
