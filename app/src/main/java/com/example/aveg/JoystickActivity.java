package com.example.aveg;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Double.isNaN;

public class JoystickActivity extends AppCompatActivity {

    /* BEGIN config data */
    private String ipAddress = CommonData.DEFAULT_IP_ADDRESS;
    /* END config data */

    /* BEGIN widgets */
    private GraphView joystickDataGraph;
    private TextView centerClickNb;

    private PointsGraphSeries<DataPoint> joystickDataSeries;
    private List<Integer> joystickValuesList;
    /* END widgets */

    //max and min ranges for x and y axes
    private final int dataGraphMaxDataPointsNumber = 10000;

    private final double dataGraphMaxX = 5;
    private final double dataGraphMinX = -5;

    private final double rpyDataGraphMaxY = 5;
    private final double rpyDataGraphMinY = -5;

    Handler handler = new Handler();
    Runnable runnable;
    int delay = 300; //refreshing chart and counter

    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_joystick);

        Intent intent = getIntent();
        Bundle configBundle = intent.getExtras();

        //Initializing textView of center click nb
        centerClickNb = (TextView) findViewById(R.id.centerClickNb);

        /* BEGIN initialize GraphView */
        // https://github.com/jjoe64/GraphView/wiki

        //Initializing joystick graph and setting ranges

        joystickDataGraph = (GraphView) findViewById(R.id.joystickDataGraph);
        //Creating 1 PointsGraphSeries
        joystickDataSeries = new PointsGraphSeries<>(new DataPoint[]{});
        //Adding 1 PointsGraphSeries to one GraphView
        joystickDataGraph.addSeries(joystickDataSeries);
        //Setting ranges for GraphView
        joystickDataGraph.getViewport().setXAxisBoundsManual(true);
        joystickDataGraph.getViewport().setMinX(dataGraphMinX);
        joystickDataGraph.getViewport().setMaxX(dataGraphMaxX);
        joystickDataGraph.getViewport().setYAxisBoundsManual(true);
        joystickDataGraph.getViewport().setMinY(rpyDataGraphMinY);
        joystickDataGraph.getViewport().setMaxY(rpyDataGraphMaxY);

        //Setting chart title
        joystickDataGraph.setTitle("Joystick");

        //Setting axes titles
        joystickDataGraph.getGridLabelRenderer().setVerticalAxisTitle("y");
        joystickDataGraph.getGridLabelRenderer().setHorizontalAxisTitle("x");

        //Setting GraphSeries color and shape
        joystickDataSeries.setColor(Color.GREEN);
        joystickDataSeries.setShape(PointsGraphSeries.Shape.POINT);

        /* END initialize GraphView */
        queue = Volley.newRequestQueue(JoystickActivity.this);

    }

    /**
     * @note Uruchomienie widoku powoduje wysyłanie zapytania, co określony czas
     */
    @Override
    protected void onResume() {
        handler.postDelayed(runnable = new Runnable() {
            public void run() {
                handler.postDelayed(runnable, delay);
                sendGetRequest();
            }
        }, delay);
        super.onResume();
    }

    /**
     * @note Zamknięcie widoku przerywa działanie handlera
     */
    @Override
    protected void onStop() {
        handler.removeCallbacks(runnable); //stop handler when activity not visible
        super.onStop();
    }

    /**
     * @note Naciśnięcie powrotu przerywa działanie handlera
     */
    @Override
    public void onBackPressed() {
        handler.removeCallbacks(runnable); //stop handler when back pressed
        super.onBackPressed();
    }

    /**
     * @param ip adres IP serwera na którym znajduje się plik
     * @brief Zwraca adres URL do pliku z danymi
     * @retval pełen adres URL do pliku z danymi
     */
    private String getURL(String ip) {
        return ("http://" + ip + "/" + CommonData.JOYSTICK_FILE_NAME);
    }

    /**
     * @param response odpowiedź serwera jako JSON string
     * @brief Odczytuje dane z pliku JSON
     * @retval dane o joysticku w postaci listy
     */
    private List<Integer> getRawDataFromResponse(String response) {
        JSONObject jObject;
        joystickValuesList = new ArrayList<>();

        // Create generic JSON object form string
        try {
            jObject = new JSONObject(response);
        } catch (JSONException e) {
            e.printStackTrace();
            return joystickValuesList;
        }
        // Read chart data form JSON object
        try {
            int x = (int) jObject.get("xAxis");
            int y = (int) jObject.get("yAxis");
            int middle = (int) jObject.get("center");
            joystickValuesList.add(x);
            joystickValuesList.add(y);
            joystickValuesList.add(middle);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return joystickValuesList;
    }

    /**
     * @brief Sending GET request to IoT server using 'Volley'.
     */
    private void sendGetRequest()
    {
        // Instantiate the RequestQueue with Volley
        // https://javadoc.io/doc/com.android.volley/volley/1.1.0-rc2/index.html
        String url = getURL(ipAddress);

        // Request a string response from the provided URL
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) { responseHandling(response); }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) { errorHandling(CommonData.ERROR_RESPONSE); }
                });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    /**
     * @param errorCode kod błędu
     * @brief obsługa błędu w przypadku jego wystąpienia
     */
    private void errorHandling(int errorCode) {
        Toast errorToast = Toast.makeText(this, "ERROR: "+errorCode, Toast.LENGTH_SHORT);
        errorToast.show();
        //stopRequestTimerTask();
    }

    /**
     * @param response odpowiedź serwera jako JSON string
     * @brief GET response handling - chart data series updated with IoT server data.
     */
    private void responseHandling(String response) {

        joystickDataSeries.resetData(new DataPoint[]{});

        int xAxisRawData = getRawDataFromResponse(response).get(0);
        int yAxisRawData = getRawDataFromResponse(response).get(1);
        int centerRawData = getRawDataFromResponse(response).get(2);

        if (isNaN(xAxisRawData) || isNaN(yAxisRawData) || isNaN(centerRawData)) {
            errorHandling(CommonData.ERROR_NAN_DATA);
        }

        else {

            joystickDataSeries.appendData(new DataPoint(xAxisRawData, yAxisRawData), false, dataGraphMaxDataPointsNumber);
            // refresh chart
            joystickDataGraph.onDataChanged(true, true);

            //refresh number of center button clicks
            final String centerRawDataString = Integer.toString(centerRawData);
            centerClickNb.setText(centerRawDataString);
        }
    }
}