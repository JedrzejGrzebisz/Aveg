package com.example.aveg;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

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

    //Pola tekstowe do wprowadzenia informacji
    EditText ledMsg, ledColor;

    //Adres URL do pliku PHP na serwerze
    String url = "http://192.168.56.22/AndroidTasks/textLedColor.php";

    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_led);

        ledMsg = findViewById(R.id.userInputTextLed);
        ledColor = findViewById(R.id.userInputTextLedColor);

        queue = Volley.newRequestQueue(TextLedActivity.this);
    }

    /**
     * @param v view
     * @brief Funkcja startująca widok zapalania pojedynczej diody
     */
    public void changeToSingleLedActivity(View v) {
        if (v.getId() == R.id.goToSingleLedBtn)
        {
            startActivity(new Intent(TextLedActivity.this, SingleLedActivity.class));
        }
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
     * @brief Wysłanie zapytania POST, aby zapalić diodę
     */
    public void sendControlRequestTxt(View v)
    {
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
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
            @Override
            protected Map<String, String> getParams() {
                return getLedDisplayParams();
            }
        };
        postRequest.setRetryPolicy(new DefaultRetryPolicy(2500, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(postRequest);
    }

}
