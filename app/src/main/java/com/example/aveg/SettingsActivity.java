package com.example.aveg;

import android.os.Bundle;
import android.os.Handler;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    TextView textViewIP;
    Handler handler = new Handler();
    Runnable runnable;
    Integer counter = 0;
    int delay = 3000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }

    //Testing calling function every 2 sec in one activity
    public void forTest(TextView textView) {
        Toast.makeText(SettingsActivity.this, "This method is run every 10 seconds",
                Toast.LENGTH_SHORT).show();
        counter = counter + 1;
        textView.setText("Licznik: " + counter);
    }

    @Override
    protected void onResume() {
        textViewIP = (TextView) findViewById(R.id.textViewIP);
        handler.postDelayed(runnable = new Runnable() {
            public void run() {
                handler.postDelayed(runnable, delay);
                forTest(textViewIP);
            }
        }, delay);
        super.onResume();
    }

    @Override
    protected void onStop() {
        handler.removeCallbacks(runnable); //stop handler when activity not visible
        super.onStop();
    }

}
