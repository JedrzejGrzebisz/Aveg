package com.example.aveg;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RpyOptionsActivity extends AppCompatActivity {

    //Deklaracja elementów interfejsu użytkownika
    private EditText ipEditText;
    private EditText sampleTimeEditText;

    //Deklarcja wybranej jednostki
    private String rpyUnit;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rpy_options);

        //Pobranie intencji oraz odczyt z niej paczki danych
        Intent intent = getIntent();
        Bundle configBundle = intent.getExtras();

        //Inicjalizacja pól editText umieszczenie w nich informacji pobranych z intencji
        //Definicja aktualnego IP oraz TP
        ipEditText = findViewById(R.id.userInputRpyIP);
        String ip = configBundle.getString(CommonData.CONFIG_IP_ADDRESS, CommonData.DEFAULT_IP_ADDRESS);
        ipEditText.setText(ip);
        sampleTimeEditText = findViewById(R.id.userInputRpyTp);
        int tp = configBundle.getInt(CommonData.CONFIG_SAMPLE_TIME, CommonData.DEFAULT_SAMPLE_TIME);
        sampleTimeEditText.setText(Integer.toString(tp));

        //Inicjalizacja spinnera
        Spinner rpyUnitPicked = findViewById(R.id.rpyUnitPicked);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.rpyUnit, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rpyUnitPicked.setAdapter(adapter);

        //Ustawienie wybranej ze spinnera jednostki
        rpyUnitPicked.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                rpyUnit = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //nothing
            }
        });
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
        intent.putExtra(CommonData.CONFIG_RPY_UNIT, rpyUnit);
        setResult(RESULT_OK, intent);
        finish();
    }

}
