package com.example.aveg;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

public class SingleLedActivity extends AppCompatActivity {

    //Pola tekstowe do wprowadzenia informacji
    EditText rowNb, colNb, ledColor;

    //Adres URL do pliku PHP na serwerze
    String url = "http://192.168.56.22/AndroidTasks/singleLedColor.php";

    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_led);

        rowNb = findViewById(R.id.userInputRowNb);
        colNb = findViewById(R.id.userInputColNb);
        ledColor = findViewById(R.id.userInputSingleLedColor);

        queue = Volley.newRequestQueue(SingleLedActivity.this);
    }

    /**
     * @param v view
     * @brief Funkcja startująca widok wyświetlania tekstu
     */
    public void changeToTextLedActivity(View v) {
        if (v.getId() == R.id.goToTextLedBtn)
        {
            startActivity(new Intent(SingleLedActivity.this, TextLedActivity.class));
        }
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
     */
    public void sendControlRequest(View v)
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
