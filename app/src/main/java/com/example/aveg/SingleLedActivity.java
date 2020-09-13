package com.example.aveg;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

public class SingleLedActivity extends AppCompatActivity {

    //Deklaracja elementów interfejsu użytkownika
    EditText rowNb, colNb, ledColor;
    EditText ipAddressEditText;

    //Ustawienie domyślnej wartości IP
    String ipAddress = CommonData.DEFAULT_IP_ADDRESS;

    //Deklaracja interfejsu z preferencjami użytkownika
    SharedPreferences userSettings;

    //Deklarcja kolejki zapytań
    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_led);

        //Inicjalizacja elementów interfejsu użytkownika
        rowNb = findViewById(R.id.userInputRowNb);
        colNb = findViewById(R.id.userInputColNb);
        ledColor = findViewById(R.id.userInputSingleLedColor);
        ipAddressEditText = findViewById(R.id.ipSingleLed);

        //Inicjalizacja preferencji, ustawienie aktualnych preferencji IP
        userSettings = getSharedPreferences("userPref", Activity.MODE_PRIVATE);
        String ipAddressPref = userSettings.getString(CommonData.CONFIG_IP_ADDRESS, CommonData.DEFAULT_IP_ADDRESS);
        ipAddress = ipAddressPref;
        ipAddressEditText.setText(ipAddress);

        //Inicjalizacja kolejki
        queue = Volley.newRequestQueue(SingleLedActivity.this);
    }

    /**
     * @brief Funkcja startująca widok wyświetlania tekstu
     * @param v Kliknięty element wiodku(np. button, textView)
     */
    public void changeToTextLedActivity(View v) {
        if (v.getId() == R.id.goToTextLedBtn)
        {
            startActivity(new Intent(SingleLedActivity.this, TextLedActivity.class));
        }
    }

    /**
     * @param ip Adres IP serwera na którym znajduje się plik PHP
     * @brief Zwraca adres URL do pliku PHP z obsługą zapalania ledu
     * @retval Pełen adres URL do pliku PHP
     */
    private String getURL(String ip) {
        return ("http://" + ip + "/" + CommonData.SINGLE_LED_FILE_NAME);
    }

    /**
     * @brief Zapisuje parametry diody do zapalenia
     * @retval Parametry diody w postaci HashMapy
     */
    public Map<String, String> getLedDisplayParams() {
        Map<String, String> params = new HashMap<String, String>();

        String rowNbText = rowNb.getText().toString();
        params.put("row", rowNbText);

        String colNbText = colNb.getText().toString();
        params.put("column", colNbText);

        String ledColorText = ledColor.getText().toString();
        params.put("color", ledColorText);

        return params;
    }

    /**
     * @brief Wysłanie zapytania POST, aby zapalić diodę
     * @note Jeśli pole IP jest puste, to wybrany zostaje domyślny adres IP oraz wyświetlany
     * jest odpowiedni komunikat jak Toast
     * @param v widok - klknięty przycisk zapal
     */
    public void sendControlRequest(View v)
    {
        if (!ipAddressEditText.getText().toString().equals(""))
        {
            ipAddress = ipAddressEditText.getText().toString();
        }
        else
        {
            ipAddress = CommonData.DEFAULT_IP_ADDRESS;
            Toast.makeText(this, "Nie podałeś IP, wybrano domyślne!", Toast.LENGTH_LONG).show();
        }
        ipAddressEditText.setText(ipAddress);

        //Utworzenie zapytania typu POST, zdefiniowanie działania przy odpowiedzi oraz jej braku
        StringRequest postRequest = new StringRequest(Request.Method.POST, getURL(ipAddress),
            new Response.Listener<String>()
            {
                @Override
                public void onResponse(String response) {
                    Log.d("Response", response);
                }
            },
            new Response.ErrorListener()
            {
                @Override
                public void onErrorResponse(VolleyError error) {
                    String msg = error.getMessage();
                    if (msg != null)
                        Log.d("Error.Response", msg);
                }
            }
        ) {
            //Definicja czynności do wykonania przy uzyskaniu odpowiedzi z serwera
           @Override
           protected Map<String, String> getParams() {
               return getLedDisplayParams();
           }
        };
        postRequest.setRetryPolicy(new DefaultRetryPolicy(2500, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        //Dodanie zapytania do kolejki
        queue.add(postRequest);
    }

}
