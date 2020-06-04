package com.example.aveg;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Double.isNaN;

public class WeatherActivity extends AppCompatActivity {

    /* BEGIN config data */
    private String ipAddress = CommonData.DEFAULT_IP_ADDRESS;
    private int sampleTime = CommonData.DEFAULT_SAMPLE_TIME;
    /* END config data */

    /* BEGIN widgets */
    private GraphView temperatureDataGraph;
    private GraphView pressureDataGraph;
    private GraphView humidityDataGraph;

    private LineGraphSeries<DataPoint> temperatureDataSeries;
    private LineGraphSeries<DataPoint> pressureDataSeries;
    private LineGraphSeries<DataPoint> humidityDataSeries;

    //max and min ranges for x and y axes
    private final int dataGraphMaxDataPointsNumber = 1000;

    private final double dataGraphMaxX = 35.0d;
    private final double dataGraphMinX = 0.0d;

    private final double temperatureDataGraphMaxY = 110.0d;
    private final double temperatureDataGraphMinY = -40.0d;

    private final double pressureDataGraphMaxY = 1300.0d;
    private final double pressureDataGraphMinY = 200.0d;

    private final double humidityDataGraphMaxY = 100.0d;
    private final double humidityDataGraphMinY = 0.0d;

    private AlertDialog.Builder configAlterDialog;

    /* BEGIN request timer */
    private RequestQueue queue;
    private Timer requestTimer;
    private long requestTimerTimeStamp = 0;
    private long requestTimerPreviousTime = -1;
    private boolean requestTimerFirstRequest = true;
    private boolean requestTimerFirstRequestAfterStop;
    private TimerTask requestTimerTask;
    private final Handler handler = new Handler();
    /* END request timer */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        Intent intent = getIntent();
        Bundle configBundle = intent.getExtras();

        /* BEGIN initialize GraphView */
        // https://github.com/jjoe64/GraphView/wiki

        //Initializing temperature graph and setting ranges
        temperatureDataGraph = findViewById(R.id.temperatureDataGraph);
        temperatureDataSeries = new LineGraphSeries<>(new DataPoint[]{});
        temperatureDataGraph.addSeries(temperatureDataSeries);
        temperatureDataGraph.getViewport().setXAxisBoundsManual(true);
        temperatureDataGraph.getViewport().setMinX(dataGraphMinX);
        temperatureDataGraph.getViewport().setMaxX(dataGraphMaxX);
        temperatureDataGraph.getViewport().setYAxisBoundsManual(true);
        temperatureDataGraph.getViewport().setMinY(temperatureDataGraphMinY);
        temperatureDataGraph.getViewport().setMaxY(temperatureDataGraphMaxY);

        //Initializing pressure graph and setting ranges
        pressureDataGraph = findViewById(R.id.pressureDataGraph);
        pressureDataSeries = new LineGraphSeries<>(new DataPoint[]{});
        pressureDataGraph.addSeries(pressureDataSeries);
        pressureDataGraph.getViewport().setXAxisBoundsManual(true);
        pressureDataGraph.getViewport().setMinX(dataGraphMinX);
        pressureDataGraph.getViewport().setMaxX(dataGraphMaxX);
        pressureDataGraph.getViewport().setYAxisBoundsManual(true);
        pressureDataGraph.getViewport().setMinY(pressureDataGraphMinY);
        pressureDataGraph.getViewport().setMaxY(pressureDataGraphMaxY);

        //Initializing humidity graph and setting ranges
        humidityDataGraph = findViewById(R.id.humidityDataGraph);
        humidityDataSeries = new LineGraphSeries<>(new DataPoint[]{});
        humidityDataGraph.addSeries(humidityDataSeries);
        humidityDataGraph.getViewport().setXAxisBoundsManual(true);
        humidityDataGraph.getViewport().setMinX(dataGraphMinX);
        humidityDataGraph.getViewport().setMaxX(dataGraphMaxX);
        humidityDataGraph.getViewport().setYAxisBoundsManual(true);
        humidityDataGraph.getViewport().setMinY(humidityDataGraphMinY);
        humidityDataGraph.getViewport().setMaxY(humidityDataGraphMaxY);

        //Initializing chart titles
        temperatureDataGraph.setTitle("Temperatura");
        pressureDataGraph.setTitle("Ciśnienie");
        humidityDataGraph.setTitle("Wilgotność");

        //Initializing axes titles
        temperatureDataGraph.getGridLabelRenderer().setVerticalAxisTitle("T[°C]");
        pressureDataGraph.getGridLabelRenderer().setVerticalAxisTitle("p[hPa]");
        humidityDataGraph.getGridLabelRenderer().setVerticalAxisTitle("H[%]");

        temperatureDataGraph.getGridLabelRenderer().setHorizontalAxisTitle("t[s]");
        pressureDataGraph.getGridLabelRenderer().setHorizontalAxisTitle("t[s]");
        humidityDataGraph.getGridLabelRenderer().setHorizontalAxisTitle("t[s]");


        /* BEGIN config alter dialog */
        configAlterDialog = new AlertDialog.Builder(WeatherActivity.this);
        configAlterDialog.setTitle("Pobieranie danych zostanie zatrzymane");
        configAlterDialog.setIcon(android.R.drawable.ic_dialog_alert);
        configAlterDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                stopRequestTimerTask();
                openWeatherOptions();
            }
        });
        configAlterDialog.setNegativeButton("Anuluj", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        /* END initialize GraphView */
        queue = Volley.newRequestQueue(WeatherActivity.this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent dataIntent) {
        super.onActivityResult(requestCode, resultCode, dataIntent);
        if ((requestCode == CommonData.REQUEST_CODE_CONFIG) && (resultCode == RESULT_OK)) {

            // IoT server IP address
            ipAddress = dataIntent.getStringExtra(CommonData.CONFIG_IP_ADDRESS);

            // Sample time (ms)
            String sampleTimeText = dataIntent.getStringExtra(CommonData.CONFIG_SAMPLE_TIME);
            sampleTime = Integer.parseInt(sampleTimeText);
        }
    }

    public void btns_onClick(View v) {
        switch (v.getId()) {
            case R.id.goToWOptionsBtn: {
                if (requestTimer != null)
                   configAlterDialog.show();
                else
                    openWeatherOptions();
                break;
            }
            case R.id.startWChartsBtn: {
                startRequestTimer();
                break;
            }
            case R.id.stopWChartsBtn: {
                stopRequestTimerTask();
                break;
            }
            default: {
                // do nothing
            }
        }
    }

    private String getURL(String ip) {
        return ("http://" + ip + "/" + CommonData.FILE_NAME);
    }

    private void openWeatherOptions() {
        Intent openConfigIntent = new Intent(WeatherActivity.this, WeatherOptionsActivity.class);
        Bundle configBundle = new Bundle();
        configBundle.putString(CommonData.CONFIG_IP_ADDRESS, ipAddress);
        configBundle.putInt(CommonData.CONFIG_SAMPLE_TIME, sampleTime);
        openConfigIntent.putExtras(configBundle);
        startActivityForResult(openConfigIntent, CommonData.REQUEST_CODE_CONFIG);
    }

    /**
     * @param response IoT server JSON response as string
     * @brief Reading raw chart data from JSON response.
     * @retval new chart data
     */

    private double getRawDataFromResponse_temperature(String response) {
        JSONObject jObject;
        double x = Double.NaN;

        // Create generic JSON object form string
        try {
            jObject = new JSONObject(response);
        } catch (JSONException e) {
            e.printStackTrace();
            return x;
        }
        // Read chart data form JSON object
        try {
            x = (double)jObject.get("Temperature");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return x;
    }

    private double getRawDataFromResponse_pressure(String response) {
        JSONObject jObject;
        double x = Double.NaN;

        // Create generic JSON object form string
        try {
            jObject = new JSONObject(response);
        } catch (JSONException e) {
            e.printStackTrace();
            return x;
        }

        // Read chart data form JSON object
        try {
            x = (double) jObject.get("Pressure");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return x;
    }

    private double getRawDataFromResponse_humidity(String response) {
        JSONObject jObject;
        double x = Double.NaN;

        // Create generic JSON object form string
        try {
            jObject = new JSONObject(response);
        } catch (JSONException e) {
            e.printStackTrace();
            return x;
        }

        // Read chart data form JSON object
        try {
            x = (double) jObject.get("Humidity");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return x;
    }

    /**
     * @brief Starts new 'Timer' (if currently not exist) and schedules periodic task.
     */
    private void startRequestTimer() {
        if (requestTimer == null) {
            // set a new Timer
            requestTimer = new Timer();
            // initialize the TimerTask's job
            initializeRequestTimerTask();
            requestTimer.schedule(requestTimerTask, 0, sampleTime);
        }
    }

    /**
     * @brief Stops request timer (if currently exist)
     * and sets 'requestTimerFirstRequestAfterStop' flag.
     */
    private void stopRequestTimerTask() {
        // stop the timer, if it's not already null
        if (requestTimer != null) {
            requestTimer.cancel();
            requestTimer = null;
            requestTimerFirstRequestAfterStop = true;
        }
    }

    /**
     * @brief Initialize request timer period task with 'Handler' post method as 'sendGetRequest'.
     */
    private void initializeRequestTimerTask() {
        requestTimerTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        sendGetRequest();
                    }
                });
            }
        };
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

    private void errorHandling(int errorCode) {

    }

    /**
     * @brief GET response handling - chart data series updated with IoT server data.
     */
    private void responseHandling(String response) {
        if (requestTimer != null) {
            // get time stamp with SystemClock
            long requestTimerCurrentTime = SystemClock.uptimeMillis(); // current time
            requestTimerTimeStamp += getValidTimeStampIncrease(requestTimerCurrentTime);

            // get raw data from JSON response
            double temperatureRawData = getRawDataFromResponse_temperature(response);
            double pressureRawData = getRawDataFromResponse_pressure(response);
            double humidityRawData = getRawDataFromResponse_humidity(response);

            // update chart
            if (isNaN(temperatureRawData)) {
                errorHandling(CommonData.ERROR_NAN_DATA);

            } else {

                // update plot series
                double timeStamp = requestTimerTimeStamp / 1000.0; // [sec]
                boolean scrollGraph = (timeStamp > dataGraphMaxX);
                temperatureDataSeries.appendData(new DataPoint(timeStamp, temperatureRawData), scrollGraph, dataGraphMaxDataPointsNumber);
                pressureDataSeries.appendData(new DataPoint(timeStamp, pressureRawData), scrollGraph, dataGraphMaxDataPointsNumber);
                humidityDataSeries.appendData(new DataPoint(timeStamp, humidityRawData), scrollGraph, dataGraphMaxDataPointsNumber);

                // refresh chart
                temperatureDataGraph.onDataChanged(true, true);
                pressureDataGraph.onDataChanged(true, true);
                humidityDataGraph.onDataChanged(true, true);
            }

            // remember previous time stamp
            requestTimerPreviousTime = requestTimerCurrentTime;
        }
    }


    /**
     * @brief Validation of client-side time stamp based on 'SystemClock'.
     */
    private long getValidTimeStampIncrease(long currentTime) {
        // Right after start remember current time and return 0
        if (requestTimerFirstRequest) {
            requestTimerPreviousTime = currentTime;
            requestTimerFirstRequest = false;
            return 0;
        }

        // After each stop return value not greater than sample time
        // to avoid "holes" in the plot
        if (requestTimerFirstRequestAfterStop) {
            if ((currentTime - requestTimerPreviousTime) > sampleTime)
                requestTimerPreviousTime = currentTime - sampleTime;

            requestTimerFirstRequestAfterStop = false;
        }

        // If time difference is equal zero after start
        // return sample time
        if ((currentTime - requestTimerPreviousTime) == 0)
            return sampleTime;

        // Return time difference between current and previous request
        return (currentTime - requestTimerPreviousTime);
    }
}
