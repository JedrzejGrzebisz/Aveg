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

    EditText rowNb, colNb, ledColor;
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

    public void changeToTextLedActivity(View v) {
        if (v.getId() == R.id.goToTextLedBtn)
        {
            startActivity(new Intent(SingleLedActivity.this, TextLedActivity.class));
        }
    }

    private String changeColorToNb(String colorString) {
        String colorNb;
        switch (colorString) {
            case "red":
                colorNb = "10";
                break;
            case "green":
                colorNb = "20";
                break;
            case "blue":
                colorNb = "30";
                break;
            case "orange":
                colorNb = "40";
                break;
            case "white":
                colorNb = "50";
                break;
            default:
                colorNb = "0";
        }
        return colorNb;
    }

    public Map<String, String> getLedDisplayParams() {
        Map<String, String> params = new HashMap<String, String>();

        String rowNbText = rowNb.getText().toString();
        params.put("row", rowNbText);

        String colNbText = colNb.getText().toString();
        params.put("column", colNbText);

        String ledColorText = ledColor.getText().toString();
        //ledColorText = changeColorToNb(ledColorText);
        params.put("color", ledColorText);

        //TextView someText = findViewById(R.id.testText);
        //someText.setText(String.format("%s%s%s", params.get("row"), params.get("column"), params.get("color")));
        //someText.setText(params.get("column"));
        return params;
    }

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
