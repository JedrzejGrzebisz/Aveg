package com.example.aveg;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

public class WeatherOptionsActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener{

    //Deklaracja elementów interfejsu użytkownika
    EditText ipEditText;
    EditText sampleTimeEditText;
    Spinner temperatureUnitPicked;
    Spinner pressureUnitPicked;
    Spinner humidityUnitPicked;

    //Deklaracja wybranej jednostki
    private String temperatureUnit;
    private String pressureUnit;
    private String humidityUnit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_options);

        //Pobranie intencji oraz odczyt z niej paczki danych
        Intent intent = getIntent();
        Bundle configBundle = intent.getExtras();

        //Inicjalizacja pól editText umieszczenie w nich informacji pobranych z intencji
        //Definicja aktualnego IP oraz TP
        ipEditText = findViewById(R.id.userInputIP);
        String ipAddress = configBundle.getString(CommonData.CONFIG_IP_ADDRESS, CommonData.DEFAULT_IP_ADDRESS);
        ipEditText.setText(ipAddress);
        sampleTimeEditText = findViewById(R.id.userInputTp);
        int sampleTime = configBundle.getInt(CommonData.CONFIG_SAMPLE_TIME, CommonData.DEFAULT_SAMPLE_TIME);
        sampleTimeEditText.setText(Integer.toString(sampleTime));

        //Inicjalizacja spinnerów
        temperatureUnitPicked = findViewById(R.id.temperatureUnitPicked);
        pressureUnitPicked = findViewById(R.id.pressureUnitPicekd);
        humidityUnitPicked = findViewById(R.id.humidityUnitPicked);

        ArrayAdapter<CharSequence> temperatureAdapter = ArrayAdapter.createFromResource(this, R.array.temperatureUnit, android.R.layout.simple_spinner_item);
        ArrayAdapter<CharSequence> pressureAdapter = ArrayAdapter.createFromResource(this, R.array.pressureUnit, android.R.layout.simple_spinner_item);
        ArrayAdapter<CharSequence> humidityAdapter = ArrayAdapter.createFromResource(this, R.array.humidityUnit, android.R.layout.simple_spinner_item);

        temperatureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pressureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        humidityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        //Wybieranie jednostki z wykorzystaniem implementacji interfejsu
        temperatureUnitPicked.setAdapter(temperatureAdapter);
        temperatureUnitPicked.setOnItemSelectedListener(this);
        pressureUnitPicked.setAdapter(pressureAdapter);
        pressureUnitPicked.setOnItemSelectedListener(this);
        humidityUnitPicked.setAdapter(humidityAdapter);
        humidityUnitPicked.setOnItemSelectedListener(this);
    }

    /**
     * @brief Umieszczenie w intencji informacji o wybranym IP, TP oraz jednostce
     * @note Jeśli pola IP oraz TP są puste to do intencji przekazane są domyślne wartości
     */
    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        if (!ipEditText.getText().toString().equals("") && !sampleTimeEditText.getText().toString().equals(""))
        {
            intent.putExtra(CommonData.CONFIG_IP_ADDRESS, ipEditText.getText().toString());
            intent.putExtra(CommonData.CONFIG_SAMPLE_TIME, sampleTimeEditText.getText().toString());
        }
        else
        {
            intent.putExtra(CommonData.CONFIG_IP_ADDRESS, CommonData.DEFAULT_IP_ADDRESS);
            intent.putExtra(CommonData.CONFIG_SAMPLE_TIME, "500");
        }
        intent.putExtra(CommonData.CONFIG_TEMPERATURE_UNIT, temperatureUnit);
        intent.putExtra(CommonData.CONFIG_PRESSURE_UNIT, pressureUnit);
        intent.putExtra(CommonData.CONFIG_HUMIDITY_UNIT, humidityUnit);
        setResult(RESULT_OK, intent);
        finish();
    }

    //Funkcja ustawia jednostki w zależnośi od aktualnie wybranych wartości ze spinnerów
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

    //Pusta funckja, która musi być zdefiniowana, ze względu na implementację interfejsu OnItemSelectedListener
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        //nothing
    }
}
