package com.example.aveg;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class WeatherOptionsActivity extends AppCompatActivity {

    /* BEGIN config textboxes */
    EditText ipEditText;
    EditText sampleTimeEditText;
    /* END config textboxes */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_options);

        // get the Intent that started this Activity
        Intent intent = getIntent();

        // get the Bundle that stores the data of this Activity
        Bundle configBundle = intent.getExtras();

        ipEditText = findViewById(R.id.userInputIP);
        String ip = configBundle.getString(CommonData.CONFIG_IP_ADDRESS, CommonData.DEFAULT_IP_ADDRESS);
        ipEditText.setText(ip);

        sampleTimeEditText = findViewById(R.id.userInputTp);
        int tp = configBundle.getInt(CommonData.CONFIG_SAMPLE_TIME, CommonData.DEFAULT_SAMPLE_TIME);
        sampleTimeEditText.setText(Integer.toString(tp));
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra(CommonData.CONFIG_IP_ADDRESS, ipEditText.getText().toString());
        intent.putExtra(CommonData.CONFIG_SAMPLE_TIME, sampleTimeEditText.getText().toString());
        setResult(RESULT_OK, intent);
        finish();
    }

}
