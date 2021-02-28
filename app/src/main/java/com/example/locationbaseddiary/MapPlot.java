package com.example.locationbaseddiary;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
//import android.widget.Toolbar;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;

public class MapPlot extends AppCompatActivity implements OnMapReadyCallback {

    private Toolbar toolbar;

    GoogleMap map;
    HashMap<String, ArrayList<String>> results;
    double[] currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_plot);

        toolbar = findViewById(R.id.toolbar2);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Location Based Diary");

        results = (HashMap<String, ArrayList<String>>) getIntent().getSerializableExtra("results");
        currentLocation = (double[]) getIntent().getSerializableExtra("currentLocation");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_back, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.menu_back){
            Toast.makeText(this, "Back button has been pressed", Toast.LENGTH_SHORT).show();
            finish();
            startActivity(new Intent(MapPlot.this, MainActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        LatLng taskLocation;
        Double xcoord, ycoord;
        String name;
        for (String task : results.keySet()) {
            xcoord = Double.parseDouble(results.get(task).get(2));
            ycoord = Double.parseDouble(results.get(task).get(3));
            name = results.get(task).get(0);
            taskLocation = new LatLng(ycoord, xcoord);
            map.addMarker(new MarkerOptions().position(taskLocation).title(name));
        }

        // Displaying user's current location
        taskLocation = new LatLng(currentLocation[0], currentLocation[1]);
        map.addMarker(new MarkerOptions().position(taskLocation).title("Your Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(taskLocation, 16.0f));
    }
}