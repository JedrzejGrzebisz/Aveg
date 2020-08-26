package com.example.aveg;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Double.isNaN;

public class RpyActivity extends AppCompatActivity {

    /* BEGIN config data */
    private String ipAddress = CommonData.DEFAULT_IP_ADDRESS;
    private int sampleTime = CommonData.DEFAULT_SAMPLE_TIME;
    /* END config data */

    /* BEGIN widgets */
    private GraphView rpyDataGraph;

    private LineGraphSeries<DataPoint> rollDataSeries;
    private LineGraphSeries<DataPoint> pitchDataSeries;
    private LineGraphSeries<DataPoint> yawDataSeries;

    //max and min ranges for x and y axes
    private final int dataGraphMaxDataPointsNumber = 1000;

    private final double dataGraphMaxX = 25.0d;
    private final double dataGraphMinX = 0.0d;

    private final double rpyDataGraphMaxY = 3.15d;
    private final double rpyDataGraphMinY = -3.15d;

    private AlertDialog.Builder configAlertDialog;

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
        setContentView(R.layout.activity_rpy);

        Intent intent = getIntent();
        Bundle configBundle = intent.getExtras();

        /* BEGIN initialize GraphView */
        // https://github.com/jjoe64/GraphView/wiki

        //Initializing roll, pitch, yaw graph and setting ranges

        rpyDataGraph = (GraphView) findViewById(R.id.rpyDataGraph);
        //Creating 3 GraphSeries
        rollDataSeries = new LineGraphSeries<>(new DataPoint[]{});
        pitchDataSeries = new LineGraphSeries<>(new DataPoint[]{});
        yawDataSeries = new LineGraphSeries<>(new DataPoint[]{});
        //Adding 3 GraphSeries to one GraphView
        rpyDataGraph.addSeries(rollDataSeries);
        rpyDataGraph.addSeries(pitchDataSeries);
        rpyDataGraph.addSeries(yawDataSeries);
        //Setting ranges for GraphView
        rpyDataGraph.getViewport().setXAxisBoundsManual(true);
        rpyDataGraph.getViewport().setMinX(dataGraphMinX);
        rpyDataGraph.getViewport().setMaxX(dataGraphMaxX);
        rpyDataGraph.getViewport().setYAxisBoundsManual(true);
        rpyDataGraph.getViewport().setMinY(rpyDataGraphMinY);
        rpyDataGraph.getViewport().setMaxY(rpyDataGraphMaxY);

        //Setting chart title
        rpyDataGraph.setTitle("Położenie kątowe (RPY)");

        //Setting axes titles
        rpyDataGraph.getGridLabelRenderer().setVerticalAxisTitle("RPY[rad]");
        rpyDataGraph.getGridLabelRenderer().setHorizontalAxisTitle("t[s]");

        //Setting legend
        rollDataSeries.setTitle("Roll");
        pitchDataSeries.setTitle("Pitch");
        yawDataSeries.setTitle("Yaw");
        rpyDataGraph.getLegendRenderer().setVisible(true);
        rpyDataGraph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);

        //Setting GraphSeries color
        rollDataSeries.setColor(Color.RED);
        pitchDataSeries.setColor(Color.BLUE);
        yawDataSeries.setColor(Color.GREEN);

        /* END initialize GraphView */
        queue = Volley.newRequestQueue(RpyActivity.this);
    }

    /* BEGIN config alert dialog */
    public void dialogAlertShow() {
        configAlertDialog = new AlertDialog.Builder(RpyActivity.this);
        configAlertDialog.setTitle("Pobieranie danych zostanie zatrzymane");
        configAlertDialog.setIcon(android.R.drawable.ic_dialog_alert);
        configAlertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                stopRequestTimerTask();
                openWeatherOptions();
            }
        });
        configAlertDialog.setNegativeButton("Anuluj", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        configAlertDialog.setCancelable(false);
        configAlertDialog.show();
    }
    /* END config alter dialog */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent dataIntent) {
        super.onActivityResult(requestCode, resultCode, dataIntent);
        if ((requestCode == CommonData.REQUEST_CODE_CONFIG) && (resultCode == RESULT_OK)) {

            // IoT server IP address
            ipAddress = dataIntent.getStringExtra(CommonData.CONFIG_IP_ADDRESS);

            // Sample time (ms)
            String sampleTimeText = dataIntent.getStringExtra(CommonData.CONFIG_SAMPLE_TIME);
            assert sampleTimeText != null;
            sampleTime = Integer.parseInt(sampleTimeText);
        }
    }

    public void btnsRpy_onClick(View v) {
        switch (v.getId()) {
            case R.id.goToRpyOptionsBtn: {
                if (requestTimer != null)
                    dialogAlertShow();
                else
                    openWeatherOptions();
                break;
            }
            case R.id.startRpyChartsBtn: {
                startRequestTimer();
                break;
            }
            case R.id.stopRpyChartsBtn: {
                stopRequestTimerTask();
                break;
            }
            default: {
                // do nothing
            }
        }
    }

    private String getURL(String ip) {
        return ("http://" + ip + "/" + CommonData.FILE_NAME2);
    }

    private void openWeatherOptions() {
        Intent openConfigIntent = new Intent(RpyActivity.this, WeatherOptionsActivity.class);
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

    private double getRawDataFromResponse_roll(String response) {
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
            x = (double)jObject.get("Roll");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return x;
    }

    private double getRawDataFromResponse_pitch(String response) {
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
            x = (double) jObject.get("Pitch");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return x;
    }

    private double getRawDataFromResponse_yaw(String response) {
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
            x = (double) jObject.get("Yaw");
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
        Toast errorToast = Toast.makeText(this, "ERROR", Toast.LENGTH_SHORT);
        errorToast.show();
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
            double rollRawData = getRawDataFromResponse_roll(response);
            double pitchRawData = getRawDataFromResponse_pitch(response);
            double yawRawData = getRawDataFromResponse_yaw(response);

            // update chart
            if (isNaN(rollRawData) || isNaN(pitchRawData) || isNaN(yawRawData)) {
                errorHandling(CommonData.ERROR_NAN_DATA);
            }
            else {

                // update plot series
                double timeStamp = requestTimerTimeStamp / 1000.0; // [sec]
                boolean scrollGraph = (timeStamp > dataGraphMaxX);
                rollDataSeries.appendData(new DataPoint(timeStamp, rollRawData), scrollGraph, dataGraphMaxDataPointsNumber);
                pitchDataSeries.appendData(new DataPoint(timeStamp, pitchRawData), scrollGraph, dataGraphMaxDataPointsNumber);
                yawDataSeries.appendData(new DataPoint(timeStamp, yawRawData), scrollGraph, dataGraphMaxDataPointsNumber);

                // refresh chart
                rpyDataGraph.onDataChanged(true, true);
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

