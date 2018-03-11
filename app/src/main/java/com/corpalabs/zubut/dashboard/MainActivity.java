package com.corpalabs.zubut.dashboard;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.ebanx.swipebtn.OnStateChangeListener;
import com.ebanx.swipebtn.SwipeButton;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SwipeButton swpAddLocation = findViewById(R.id.swpAddLocation);

        swpAddLocation.setOnStateChangeListener(new OnStateChangeListener() {
            @Override
            public void onStateChange(boolean active) {
                Toast.makeText(MainActivity.this, "Ubicacion guardada", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
