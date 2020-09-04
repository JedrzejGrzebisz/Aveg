package com.example.aveg;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

public class WeatherOptionsActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener{

    /* BEGIN config widgets */
    EditText ipEditText;
    EditText sampleTimeEditText;
    private String temperatureUnit;
    private String pressureUnit;
    private String humidityUnit;
    Spinner temperatureUnitPicked;
    Spinner pressureUnitPicked;
    Spinner humidityUnitPicked;
    /* END config widgets */

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

        //Initialize spinners
        temperatureUnitPicked = findViewById(R.id.temperatureUnitPicked);
        pressureUnitPicked = findViewById(R.id.pressureUnitPicekd);
        humidityUnitPicked = findViewById(R.id.humidityUnitPicked);

        ArrayAdapter<CharSequence> temperatureAdapter = ArrayAdapter.createFromResource(this, R.array.temperatureUnit, android.R.layout.simple_spinner_item);
        ArrayAdapter<CharSequence> pressureAdapter = ArrayAdapter.createFromResource(this, R.array.pressureUnit, android.R.layout.simple_spinner_item);
        ArrayAdapter<CharSequence> humidityAdapter = ArrayAdapter.createFromResource(this, R.array.humidityUnit, android.R.layout.simple_spinner_item);

        temperatureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pressureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        humidityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        temperatureUnitPicked.setAdapter(temperatureAdapter);
        temperatureUnitPicked.setOnItemSelectedListener(this);
        pressureUnitPicked.setAdapter(pressureAdapter);
        pressureUnitPicked.setOnItemSelectedListener(this);
        humidityUnitPicked.setAdapter(humidityAdapter);
        humidityUnitPicked.setOnItemSelectedListener(this);

    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra(CommonData.CONFIG_IP_ADDRESS, ipEditText.getText().toString());
        intent.putExtra(CommonData.CONFIG_SAMPLE_TIME, sampleTimeEditText.getText().toString());
        intent.putExtra(CommonData.CONFIG_TEMPERATURE_UNIT, temperatureUnit);
        intent.putExtra(CommonData.CONFIG_PRESSURE_UNIT, pressureUnit);
        intent.putExtra(CommonData.CONFIG_HUMIDITY_UNIT, humidityUnit);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == temperatureUnitPicked.getId())
        {
            temperatureUnit = parent.getItemAtPosition(position).toString();
        }
        if (parent.getId() == pressureUnitPicked.getId())
        {
            pressureUnit = parent.getItemAtPosition(position).toString();
        }
        if (parent.getId() == humidityUnitPicked.getId())
        {
            humidityUnit = parent.getItemAtPosition(position).toString();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        //nothing
    }
}
