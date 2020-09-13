package com.example.aveg;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity{

    //Deklaracja elementów interfejsu użytkownika
    EditText userInputSettingsIp;
    EditText userInputSettingsTp;
    Button setPrefBtn;

    //Deklaracja wybranego IP oraz czasu próbkowania
    String ipAddressPref;
    int sampleTimePref;

    //Deklaracja interfejsu z preferencjami użytkownika
    SharedPreferences userSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        //Inicjalizacja preferencji, pól tekstowych oraz przycisku
        userSettings = getSharedPreferences("userPref", Activity.MODE_PRIVATE);
        userInputSettingsIp = findViewById(R.id.userInputSettingsIp);
        userInputSettingsTp = findViewById(R.id.userInputSettingsTp);
        setPrefBtn = findViewById(R.id.setPrefBtn);

        //Wczytanie aktualnych preferencji
        loadPref();

        //Zapisanie preferencji wpisanych w polach tekstowych
        setPrefBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePref();
            }
        });
    }

    /**
     * @brief Wczytanie informacji o aktualnych preferencjach, ustawienie ich w polach editText
     */
    private void loadPref() {
        ipAddressPref = userSettings.getString(CommonData.CONFIG_IP_ADDRESS, CommonData.DEFAULT_IP_ADDRESS);
        sampleTimePref = userSettings.getInt(CommonData.CONFIG_SAMPLE_TIME, CommonData.DEFAULT_SAMPLE_TIME);
        userInputSettingsIp.setText(ipAddressPref);
        userInputSettingsTp.setText(Integer.toString(sampleTimePref));
    }

    /**
     * @brief Zapisanie nowych wpisanych przez użytkownika preferencji IP oraz TP
     * @note Jeśli użytkownik zostawił puste pole, to do preferencji zapisywane są wartości domyślne
     */
    private void savePref() {
        SharedPreferences.Editor editor = userSettings.edit();
        if (!userInputSettingsIp.getText().toString().equals("") && !userInputSettingsTp.getText().toString().equals(""))
        {
            editor.putString(CommonData.CONFIG_IP_ADDRESS, userInputSettingsIp.getText().toString());
            editor.putInt(CommonData.CONFIG_SAMPLE_TIME, Integer.parseInt(userInputSettingsTp.getText().toString()));
        }
        else
        {
            editor.putString(CommonData.CONFIG_IP_ADDRESS, CommonData.DEFAULT_IP_ADDRESS);
            editor.putInt(CommonData.CONFIG_SAMPLE_TIME, CommonData.DEFAULT_SAMPLE_TIME);
        }

        editor.apply();
    }

}
