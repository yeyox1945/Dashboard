package com.corpalabs.zubut.dashboard;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.ArrayList;

public class DetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        Intent intent = getIntent();
        int item = intent.getIntExtra("Position", -1);
        ArrayList<String> lstPlaces = intent.getStringArrayListExtra("Places");
        ArrayList<String> lstLocations = intent.getStringArrayListExtra("Locations");
        TextView txtDetails = findViewById(R.id.txtDetails);

        if (item != -1) {
            txtDetails.setText(lstPlaces.get(item).toString());
            txtDetails.append(lstLocations.get(item).toString());
        } else
            txtDetails.setText("Hubo un error :(");
    }
}
