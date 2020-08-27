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

import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Double.isNaN;

public class JoystickActivity extends AppCompatActivity {

    /* BEGIN config data */
    private String ipAddress = CommonData.DEFAULT_IP_ADDRESS;
    private int sampleTime = CommonData.DEFAULT_SAMPLE_TIME;
    /* END config data */

    /* BEGIN widgets */
    private GraphView joystickDataGraph;
    private TextView centerClickNb;

    private PointsGraphSeries<DataPoint> joystickDataSeries;

    //max and min ranges for x and y axes
    private final int dataGraphMaxDataPointsNumber = 100;

    private final double dataGraphMaxX = 50;
    private final double dataGraphMinX = -50;

    private final double rpyDataGraphMaxY = 50;
    private final double rpyDataGraphMinY = -50;

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

        //Start timer
        //startRequestTimer();
    }

    @Override
    public void onBackPressed() {
        stopRequestTimerTask();
        finish();
    }

    /* BEGIN config alert dialog */
    /*
    public void dialogAlertShow() {
        configAlertDialog = new AlertDialog.Builder(JoystickActivity.this);
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
    }*/
    /* END config alter dialog */

    /*
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
    }*/

    /*
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
    }*/

    private String getURL(String ip) {
        return ("http://" + ip + "/" + CommonData.FILE_NAME3);
    }

    /*
    private void openWeatherOptions() {
        Intent openConfigIntent = new Intent(RpyActivity.this, WeatherOptionsActivity.class);
        Bundle configBundle = new Bundle();
        configBundle.putString(CommonData.CONFIG_IP_ADDRESS, ipAddress);
        configBundle.putInt(CommonData.CONFIG_SAMPLE_TIME, sampleTime);
        openConfigIntent.putExtras(configBundle);
        startActivityForResult(openConfigIntent, CommonData.REQUEST_CODE_CONFIG);
    }*/

    /**
     * @param response IoT server JSON response as string
     * @brief Reading raw chart data from JSON response.
     * @retval new chart data
     */

    private double getRawDataFromResponse_xAxis(String response) {
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
            x = (double)jObject.get("xAxis");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return x;
    }

    private double getRawDataFromResponse_yAxis(String response) {
        JSONObject jObject;
        double y = Double.NaN;

        // Create generic JSON object form string
        try {
            jObject = new JSONObject(response);
        } catch (JSONException e) {
            e.printStackTrace();
            return y;
        }

        // Read chart data form JSON object
        try {
            y = (double) jObject.get("yAxis");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return y;
    }

    private double getRawDataFromResponse_center(String response) {
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
            x = (double) jObject.get("center");
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
        stopRequestTimerTask();
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
            double xAxisRawData = getRawDataFromResponse_xAxis(response);
            double yAxisRawData = getRawDataFromResponse_yAxis(response);
            double centerRawData = getRawDataFromResponse_center(response);

            // update chart
            if (isNaN(xAxisRawData) || isNaN(yAxisRawData) || isNaN(centerRawData)) {
                errorHandling(CommonData.ERROR_NAN_DATA);
            }
            else {

                // update plot series
                double timeStamp = requestTimerTimeStamp / 1000.0; // [sec]
                boolean scrollGraph = (timeStamp > dataGraphMaxX);
                joystickDataSeries.appendData(new DataPoint(xAxisRawData, yAxisRawData), true, dataGraphMaxDataPointsNumber);

                // refresh chart
                joystickDataGraph.onDataChanged(true, true);

                //refresh number of center button clicks
                String centerRawDataString = Double.toString(getRawDataFromResponse_center(response));
                centerClickNb.setText(centerRawDataString);
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