package com.example.aveg;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.jjoe64.graphview.series.DataPoint;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.Double.isNaN;

public class AllDataActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    //Initialize default units
    private String temperatureUnit = CommonData.DEFAULT_TEMPERATURE_UNIT;
    private String pressureUnit = CommonData.DEFAULT_PRESSURE_UNIT;
    private String humidityUnit = CommonData.DEFAULT_HUMIDITY_UNIT;
    private String rpyUnit = CommonData.DEFAULT_RPY_UNIT;
    private String ipAddress = CommonData.DEFAULT_IP_ADDRESS;
    private int sampleTime = CommonData.DEFAULT_SAMPLE_TIME;

    private TextView temperatureValue;
    private TextView pressureValue;
    private TextView humidityValue;
    private TextView rollValue;
    private TextView pitchValue;
    private TextView yawValue;

    private EditText ipAdressEditText;
    private EditText sampleTimeEditText;

    private Button setIpAndTpAllData;

    Spinner temperatureSpinner;
    Spinner pressureSpinner;
    Spinner humiditySpinner;
    Spinner rpySpinner;

    private List<Double> rpyValuesList;
    private List<Double> weatherValuesList;

    double rollRawData;
    double pitchRawData;
    double yawRawData;

    double temperatureRawData;
    double pressureRawData;
    double humidityRawData;

    Handler handler = new Handler();
    Runnable runnable;

    SharedPreferences userSettings;

    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_data);

        //Initialize spinners
        temperatureSpinner = findViewById(R.id.temperatureSpinner);
        pressureSpinner = findViewById(R.id.pressureSpinner);
        humiditySpinner = findViewById(R.id.humiditySpinner);
        rpySpinner = findViewById(R.id.rpySpinner);

        temperatureValue = findViewById(R.id.temperatureValue);
        pressureValue = findViewById(R.id.pressureValue);
        humidityValue = findViewById(R.id.humidityValue);
        rollValue = findViewById(R.id.rollValue);
        pitchValue = findViewById(R.id.pitchValue);
        yawValue = findViewById(R.id.yawValue);

        ArrayAdapter<CharSequence> temperatureAdapter = ArrayAdapter.createFromResource(this, R.array.temperatureUnit, android.R.layout.simple_spinner_item);
        ArrayAdapter<CharSequence> pressureAdapter = ArrayAdapter.createFromResource(this, R.array.pressureUnit, android.R.layout.simple_spinner_item);
        ArrayAdapter<CharSequence> humidityAdapter = ArrayAdapter.createFromResource(this, R.array.humidityUnit, android.R.layout.simple_spinner_item);
        ArrayAdapter<CharSequence> rpyAdapter = ArrayAdapter.createFromResource(this, R.array.rpyUnit, android.R.layout.simple_spinner_item);

        temperatureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pressureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        humidityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rpyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        temperatureSpinner.setAdapter(temperatureAdapter);
        temperatureSpinner.setOnItemSelectedListener(this);
        pressureSpinner.setAdapter(pressureAdapter);
        pressureSpinner.setOnItemSelectedListener(this);
        humiditySpinner.setAdapter(humidityAdapter);
        humiditySpinner.setOnItemSelectedListener(this);
        rpySpinner.setAdapter(rpyAdapter);
        rpySpinner.setOnItemSelectedListener(this);

        userSettings = getSharedPreferences("userPref", Activity.MODE_PRIVATE);
        String ipAddressPref = userSettings.getString(CommonData.CONFIG_IP_ADDRESS, CommonData.DEFAULT_IP_ADDRESS);
        int sampleTimePref = userSettings.getInt(CommonData.CONFIG_SAMPLE_TIME, CommonData.DEFAULT_SAMPLE_TIME);
        ipAddress = ipAddressPref;
        sampleTime = sampleTimePref;

        //Initialize EditText
        ipAdressEditText = findViewById(R.id.ipAllDataEditText);
        sampleTimeEditText = findViewById(R.id.tpAllDataEditText);
        ipAdressEditText.setText(ipAddress);
        sampleTimeEditText.setText(Integer.toString(sampleTime));

        //Initialize Button
        setIpAndTpAllData = findViewById(R.id.setIpAndTpAllData);

        queue = Volley.newRequestQueue(AllDataActivity.this);
    }

    public void setTpAndIp(View v)
    {
        if (v.getId() == setIpAndTpAllData.getId())
        {
            if (!ipAdressEditText.getText().toString().equals("") && !sampleTimeEditText.getText().toString().equals(""))
            {
                ipAddress = ipAdressEditText.getText().toString();
                sampleTime = Integer.parseInt(sampleTimeEditText.getText().toString());
            }
            else
            {
                ipAddress = CommonData.DEFAULT_IP_ADDRESS;
                sampleTime = CommonData.DEFAULT_SAMPLE_TIME;
                Toast.makeText(this, "Nie podałeś IP lub TP, ustawiono wartości domyślne", Toast.LENGTH_LONG).show();
            }
            sampleTimeEditText.setText(Integer.toString(sampleTime));
            ipAdressEditText.setText(ipAddress);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == temperatureSpinner.getId())
        {
            temperatureUnit = parent.getItemAtPosition(position).toString();
        }
        if (parent.getId() == pressureSpinner.getId())
        {
            pressureUnit = parent.getItemAtPosition(position).toString();
        }
        if (parent.getId() == humiditySpinner.getId())
        {
            humidityUnit = parent.getItemAtPosition(position).toString();
        }
        if (parent.getId() == rpySpinner.getId())
        {
            rpyUnit = parent.getItemAtPosition(position).toString();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        //nothing
    }

    /**
     * @note Uruchomienie widoku powoduje wysyłanie zapytania, co określony czas
     */
    @Override
    protected void onResume() {
        handler.postDelayed(runnable = new Runnable() {
            public void run() {
                handler.postDelayed(runnable, sampleTime);
                sendGetRequestWeather();
                sendGetRequestRpy();
            }
        }, sampleTime);
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
    private String getURLWeather(String ip) {
        return ("http://" + ip + "/" + CommonData.WEATHER_FILE_NAME);
    }

    private String getURLRpy(String ip) {
        if (rpyUnit.equals("rad"))
        {
            return ("http://" + ip + "/" + CommonData.RPY_RAD_FILE_NAME);
        }
        else
        {
            return ("http://" + ip + "/" + CommonData.RPY_DEG_FILE_NAME);
        }
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * @param response odpowiedź serwera jako JSON string
     * @brief Odczytuje dane z pliku JSON
     * @retval dane o joysticku w postaci listy
     */
    private List<Double> getRawDataFromResponseWeather(String response) {
        JSONObject jObject;
        weatherValuesList = new ArrayList<>();
        double temperature;
        double pressure;
        double humidity;

        // Create generic JSON object form string
        try {
            jObject = new JSONObject(response);
        } catch (JSONException e) {
            e.printStackTrace();
            return weatherValuesList;
        }
        // Read chart data form JSON object
        try {
            if (temperatureUnit.equals("C"))
                temperature = (double) jObject.get("TemperatureC");
            else
                temperature = (double) jObject.get("TemperatureF");

            if (pressureUnit.equals("hPa"))
                pressure = (double) jObject.get("PressureHPa");
            else
                pressure = (double) jObject.get("PressureMmHg");

            if (humidityUnit.equals("%"))
                humidity = (double) jObject.get("HumidityPercentage");
            else
                humidity = (double) jObject.get("Humidity01");

            weatherValuesList.add(temperature);
            weatherValuesList.add(pressure);
            weatherValuesList.add(humidity);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return weatherValuesList;
    }

    private List<Double> getRawDataFromResponseRpy(String response) {
        JSONObject jObject;
        rpyValuesList = new ArrayList<>();

        // Create generic JSON object form string
        try {
            jObject = new JSONObject(response);
        } catch (JSONException e) {
            e.printStackTrace();
            return rpyValuesList;
        }
        // Read chart data form JSON object
        try {
            double roll = (double)jObject.get("Roll");
            double pitch = (double)jObject.get("Pitch");
            double yaw = (double)jObject.get("Yaw");
            rpyValuesList.add(roll);
            rpyValuesList.add(pitch);
            rpyValuesList.add(yaw);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return rpyValuesList;
    }

    /**
     * @brief Sending GET request to IoT server using 'Volley'.
     */
    private void sendGetRequestWeather()
    {
        // Instantiate the RequestQueue with Volley
        // https://javadoc.io/doc/com.android.volley/volley/1.1.0-rc2/index.html
        String url = getURLWeather(ipAddress);

        // Request a string response from the provided URL
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) { responseHandlingWeather(response); }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) { errorHandling(CommonData.ERROR_RESPONSE); }
                });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    /**
     * @brief Sending GET request to IoT server using 'Volley'.
     */
    private void sendGetRequestRpy()
    {
        // Instantiate the RequestQueue with Volley
        // https://javadoc.io/doc/com.android.volley/volley/1.1.0-rc2/index.html
        String url = getURLRpy(ipAddress);

        // Request a string response from the provided URL
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) { responseHandlingRpy(response); }
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
        //Toast errorToast = Toast.makeText(this, "ERROR: "+errorCode, Toast.LENGTH_SHORT);
        //errorToast.show();
    }

    /**
     * @param response odpowiedź serwera jako JSON string
     * @brief GET response handling - chart data series updated with IoT server data.
     */
    private void responseHandlingRpy(String response) {

        // get raw data from JSON response
        if (getRawDataFromResponseRpy(response).size() != 0) {
            rollRawData = round(getRawDataFromResponseRpy(response).get(0), 2);
            pitchRawData = round(getRawDataFromResponseRpy(response).get(1), 2);
            yawRawData = round(getRawDataFromResponseRpy(response).get(2), 2);
        }
        // update chart
        if (isNaN(rollRawData) || isNaN(pitchRawData) || isNaN(yawRawData)) {
            errorHandling(CommonData.ERROR_NAN_DATA);
        }
        else {
            // update rpy values
            final String rollRawDataString = Double.toString(rollRawData);
            final String pitchRawDataString = Double.toString(pitchRawData);
            final String yawRawDataString = Double.toString(yawRawData);
            rollValue.setText(rollRawDataString);
            pitchValue.setText(pitchRawDataString);
            yawValue.setText(yawRawDataString);
        }
    }

    private void responseHandlingWeather(String response) {

        // get raw data from JSON response
        if (getRawDataFromResponseWeather(response).size() != 0) {
            temperatureRawData = round(getRawDataFromResponseWeather(response).get(0), 2);
            pressureRawData = round(getRawDataFromResponseWeather(response).get(1), 2);
            humidityRawData = round(getRawDataFromResponseWeather(response).get(2), 2);
        }
        // update chart
        if (isNaN(temperatureRawData) || isNaN(pressureRawData) || isNaN(humidityRawData)) {
            errorHandling(CommonData.ERROR_NAN_DATA);
        }
        else {
            // update rpy values
            final String temperatureRawDataString = Double.toString(temperatureRawData);
            final String pressureRawDataString = Double.toString(pressureRawData);
            final String humidityRawDataString = Double.toString(humidityRawData);
            temperatureValue.setText(temperatureRawDataString);
            pressureValue.setText(pressureRawDataString);
            humidityValue.setText(humidityRawDataString);
        }
    }
}