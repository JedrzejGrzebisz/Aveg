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

    //Ustawienie domyślnych wartości
    private String temperatureUnit = CommonData.DEFAULT_TEMPERATURE_UNIT;
    private String pressureUnit = CommonData.DEFAULT_PRESSURE_UNIT;
    private String humidityUnit = CommonData.DEFAULT_HUMIDITY_UNIT;
    private String rpyUnit = CommonData.DEFAULT_RPY_UNIT;
    private String ipAddress = CommonData.DEFAULT_IP_ADDRESS;
    private int sampleTime = CommonData.DEFAULT_SAMPLE_TIME;

    //Deklaracja elementów interfejsu użytkownika
    private TextView temperatureValue;
    private TextView pressureValue;
    private TextView humidityValue;
    private TextView rollValue;
    private TextView pitchValue;
    private TextView yawValue;
    private EditText ipAdressEditText;
    private EditText sampleTimeEditText;
    private Button setIpAndTpAllData;
    private Spinner temperatureSpinner;
    private Spinner pressureSpinner;
    private Spinner humiditySpinner;
    private Spinner rpySpinner;

    //Deklaracja list z odczytami czujników
    private List<Double> rpyValuesList;
    private List<Double> weatherValuesList;

    //Deklaracja odczytów z konkretnych czujników
    double rollRawData;
    double pitchRawData;
    double yawRawData;
    double temperatureRawData;
    double pressureRawData;
    double humidityRawData;

    //Utworzenie handlara dla cyklicznego odczytu danych
    Handler handler = new Handler();
    Runnable runnable;

    //Deklaracja interfejsu z preferencjami użytkownika
    SharedPreferences userSettings;

    //Deklarcja kolejki zapytań
    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_data);

        //Inicjalizacja spinnerów
        temperatureSpinner = findViewById(R.id.temperatureSpinner);
        pressureSpinner = findViewById(R.id.pressureSpinner);
        humiditySpinner = findViewById(R.id.humiditySpinner);
        rpySpinner = findViewById(R.id.rpySpinner);

        //Inicjalizacja pól textView z wartościami
        temperatureValue = findViewById(R.id.temperatureValue);
        pressureValue = findViewById(R.id.pressureValue);
        humidityValue = findViewById(R.id.humidityValue);
        rollValue = findViewById(R.id.rollValue);
        pitchValue = findViewById(R.id.pitchValue);
        yawValue = findViewById(R.id.yawValue);

        //cd. inicjalizacji spinnerów
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

        //Inicjalizacja preferencji, ustawienie aktualnych preferencji IP oraz TP
        userSettings = getSharedPreferences("userPref", Activity.MODE_PRIVATE);
        String ipAddressPref = userSettings.getString(CommonData.CONFIG_IP_ADDRESS, CommonData.DEFAULT_IP_ADDRESS);
        int sampleTimePref = userSettings.getInt(CommonData.CONFIG_SAMPLE_TIME, CommonData.DEFAULT_SAMPLE_TIME);
        ipAddress = ipAddressPref;
        sampleTime = sampleTimePref;

        //Inicjalizacja EditText, ustawienie w nich aktualnych wartości IP i TP
        ipAdressEditText = findViewById(R.id.ipAllDataEditText);
        sampleTimeEditText = findViewById(R.id.tpAllDataEditText);
        ipAdressEditText.setText(ipAddress);
        sampleTimeEditText.setText(Integer.toString(sampleTime));

        //Inicjalizacja przycisku do ustawienia IP oraz TP
        setIpAndTpAllData = findViewById(R.id.setIpAndTpAllData);

        //Inicjalizacja kolejki
        queue = Volley.newRequestQueue(AllDataActivity.this);
    }

    /**
     * @brief Odczyt wartości z pól, ustawienie nowego IP oraz TP
     * @note Jeśli w polach edit text nic nie zostanie wpisane, to ustawiona zostaje domyślna
     * wartość oraz pokazany zostaje komunikat w postaci Toasta
     * @param v Kliknięty widok(np. button, textview)
     */
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

    //Funkcja ustawia jednostkę w zależnośi od aktualnie wybranej wartości ze spinnera
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

    //Pusta funckja, która musi być zdefiniowana, ze względu na implementację interfejsu OnItemSelectedListener
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        //nothing
    }

    /**
     * @brief Uruchomienie widoku powoduje wysyłanie zapytania, co określony czas
     * @note By cyklicznie wysyłać zapytania wykorzystany jest sposób z handlerem oraz metodą
     * postDelayed, czas próbkowania definiowany jest przez użytkownika. Wysyłane są dwa zapytania
     * jedno odpowiedzialne za odczyt danych o pogodzie, drugie o położeniu kątowym
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
     * @brief Zamknięcie widoku przerywa działanie procesu runnable z handlera
     */
    @Override
    protected void onStop() {
        handler.removeCallbacks(runnable);
        super.onStop();
    }

    /**
     * @brief Naciśnięcie powrotu przerywa działanie procesu runnable z handlera
     */
    @Override
    public void onBackPressed() {
        handler.removeCallbacks(runnable);
        super.onBackPressed();
    }

    /**
     * @param ip Adres IP serwera na którym znajduje się plik
     * @brief Zwraca adres URL do pliku z danymi o pogodzie
     * @retval Pełen adres URL do pliku z danymi o pogodzie
     */
    private String getURLWeather(String ip) {
        return ("http://" + ip + "/" + CommonData.WEATHER_FILE_NAME);
    }

    /**
     * @brief Zwraca adres URL do pliku z danymi o kątach
     * @note W zależności od wybranej jednostki rad/deg zwracany jest adres konkretnego pliku
     * na serwerze
     * @param ip Adres IP serwera na którym znajduje się plik
     * @retval Pełen adres URL do pliku z danymi o kątach
     */
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

    /**
     * @brief Zaokrągla wartość typu double do wybranej liczby miejsc po przecinku
     * @param value Wartości zmiennoprzecinkowa do zaokrąglenia
     * @param places Liczba miejsc po przecinku
     * @retval Zaokrąglona wartość
     */
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * @brief Odczytuje dane z pliku JSON o pogodzie
     * @note W bloku "try" funkcji w zależności od wybranej jednostki odczytywana jest odpowiednia
     * wartość z pliku JSON
     * @param response Odpowiedź serwera jako JSON string
     * @retval Dane o pogodzie w postaci listy
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

    /**
     * @brief Odczytuje dane z pliku JSON o położeniu
     * @param response Odpowiedź serwera jako JSON string
     * @retval Dane o położeniu w postaci listy
     */
    private List<Double> getRawDataFromResponseRpy(String response) {
        JSONObject jObject;
        rpyValuesList = new ArrayList<>();

        try {
            jObject = new JSONObject(response);
        } catch (JSONException e) {
            e.printStackTrace();
            return rpyValuesList;
        }

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
     * @brief Wysłanie zapytania GET na serwer z wykorzystaniem Volley,
     * w celu pobrania danych o pogodzie
     */
    private void sendGetRequestWeather()
    {
        String url = getURLWeather(ipAddress);
        //Utworzenie nowego zapytania tpyu String, zdefiniowanie co zrobić przy odpowiedzi oraz braku
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) { responseHandlingWeather(response); }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) { errorHandling(CommonData.ERROR_RESPONSE); }
                });

        //Dodanie zapytania do kolejki
        queue.add(stringRequest);
    }

    /**
     * @brief Wysłanie zapytania GET na serwer z wykorzystaniem Volley,
     * w celu pobrania danych o położeniu
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
     * @brief Obsługa błędu zapytania w przypadku jego wystąpienia
     * @param errorCode Kod błędu
     */
    private void errorHandling(int errorCode) {
        //Toast errorToast = Toast.makeText(this, "ERROR: "+errorCode, Toast.LENGTH_SHORT);
        //errorToast.show();
    }

    /**
     * @brief Obsługa uzyskanej odpowiedzi na zapytanie GET(położenie) z serwera
     * @note Dane odczytywane są z wykrozystaniem funckji getRawDataFromResponseRpy oraz zaokrąglane
     * do dwóch miejsc po przecinku, następnie aktualizowane są pola textView
     * @param response Odpowiedź serwera jako JSON string
     */
    private void responseHandlingRpy(String response) {

        if (getRawDataFromResponseRpy(response).size() != 0) {
            rollRawData = round(getRawDataFromResponseRpy(response).get(0), 2);
            pitchRawData = round(getRawDataFromResponseRpy(response).get(1), 2);
            yawRawData = round(getRawDataFromResponseRpy(response).get(2), 2);
        }

        if (isNaN(rollRawData) || isNaN(pitchRawData) || isNaN(yawRawData)) {
            errorHandling(CommonData.ERROR_NAN_DATA);
        }
        else {
            final String rollRawDataString = Double.toString(rollRawData);
            final String pitchRawDataString = Double.toString(pitchRawData);
            final String yawRawDataString = Double.toString(yawRawData);
            rollValue.setText(rollRawDataString);
            pitchValue.setText(pitchRawDataString);
            yawValue.setText(yawRawDataString);
        }
    }

    /**
     * @brief Obsługa uzyskanej odpowiedzi na zapytanie GET(pogoda) z serwera
     * @note Dane odczytywane są z wykrozystaniem funckji getRawDataFromResponseWeather oraz zaokrąglane
     * do dwóch miejsc po przecinku, następnie aktualizowane są pola textView
     * @param response Odpowiedź serwera jako JSON string
     */
    private void responseHandlingWeather(String response) {

        if (getRawDataFromResponseWeather(response).size() != 0) {
            temperatureRawData = round(getRawDataFromResponseWeather(response).get(0), 2);
            pressureRawData = round(getRawDataFromResponseWeather(response).get(1), 2);
            humidityRawData = round(getRawDataFromResponseWeather(response).get(2), 2);
        }

        if (isNaN(temperatureRawData) || isNaN(pressureRawData) || isNaN(humidityRawData)) {
            errorHandling(CommonData.ERROR_NAN_DATA);
        }
        else {
            final String temperatureRawDataString = Double.toString(temperatureRawData);
            final String pressureRawDataString = Double.toString(pressureRawData);
            final String humidityRawDataString = Double.toString(humidityRawData);
            temperatureValue.setText(temperatureRawDataString);
            pressureValue.setText(pressureRawDataString);
            humidityValue.setText(humidityRawDataString);
        }
    }
}