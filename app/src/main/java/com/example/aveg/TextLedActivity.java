package com.example.aveg;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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


public class TextLedActivity extends AppCompatActivity {

    //Deklaracja elementów interfejsu użytkownika
    EditText ledMsg, ledColor;
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
        setContentView(R.layout.activity_text_led);

        //Inicjalizacja elementów interfejsu użytkownika
        ledMsg = findViewById(R.id.userInputTextLed);
        ledColor = findViewById(R.id.userInputTextLedColor);
        ipAddressEditText = findViewById(R.id.ipTextled);

        //Inicjalizacja preferencji, ustawienie aktualnych preferencji IP
        userSettings = getSharedPreferences("userPref", Activity.MODE_PRIVATE);
        String ipAddressPref = userSettings.getString(CommonData.CONFIG_IP_ADDRESS, CommonData.DEFAULT_IP_ADDRESS);
        ipAddress = ipAddressPref;
        ipAddressEditText.setText(ipAddress);

        //Inicjalizacja kolejki
        queue = Volley.newRequestQueue(TextLedActivity.this);
    }

    /**
     * @brief Funkcja startująca widok zapalania pojedynczej diody
     * @param v Kliknięty element wiodku(np. button, textView)
     */
    public void changeToSingleLedActivity(View v) {
        if (v.getId() == R.id.goToSingleLedBtn)
        {
            startActivity(new Intent(TextLedActivity.this, SingleLedActivity.class));
        }
    }

    /**
     * @param ip Adres IP serwera na którym znajduje się plik PHP
     * @brief Zwraca adres URL do pliku PHP z obsługą wyświetlania tekstu
     * @retval Pełen adres URL do pliku PHP
     */
    private String getURL(String ip) {
        return ("http://" + ip + "/" + CommonData.TEXT_LED_FILE_NAME);
    }

    /**
     * @brief Zapisuje tekst do wyświetlenia i jego kolor
     * @retval Parametry w postaci HashMapy
     */
    public Map<String, String> getLedDisplayParams() {
        Map<String, String> params = new HashMap<>();

        String ledMsgText = ledMsg.getText().toString();
        params.put("text", ledMsgText);

        String ledColorText = ledColor.getText().toString();
        params.put("color", ledColorText);

        return params;
    }

    /**
     * @brief Wysłanie zapytania POST, aby wyświetlić tekst
     * @note Jeśli pole IP jest puste, to wybrany zostaje domyślny adres IP oraz wyświetlany
     * jest odpowiedni komunikat jak Toast
     * @param v widok - klknięty przycisk zapal
     */
    public void sendControlRequestTxt(View v)
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
