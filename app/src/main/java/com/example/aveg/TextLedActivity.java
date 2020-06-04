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

    EditText ledMsg, ledColor;
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

    public void changeToSingleLedActivity(View v) {
        if (v.getId() == R.id.goToSingleLedBtn)
        {
            startActivity(new Intent(TextLedActivity.this, SingleLedActivity.class));
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


        String ledMsgText = ledMsg.getText().toString();
        params.put("text", ledMsgText);

        String ledColorText = ledColor.getText().toString();
        //ledColorText = changeColorToNb(ledColorText);
        params.put("color", ledColorText);

        //TextView someText1 = findViewById(R.id.testText1);
        //someText1.setText(String.format("%s%s", params.get("text"), params.get("color")));
        //someText1.setText(params.get("text"));
        return params;
    }

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
