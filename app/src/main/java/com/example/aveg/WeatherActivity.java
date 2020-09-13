package com.example.aveg;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
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
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import javax.sql.CommonDataSource;

import static java.lang.Double.isNaN;

public class WeatherActivity extends AppCompatActivity {

    //Ustawienie domyślnych wartości
    private String ipAddress = CommonData.DEFAULT_IP_ADDRESS;
    private int sampleTime = CommonData.DEFAULT_SAMPLE_TIME;
    private String temperatureUnit = CommonData.DEFAULT_TEMPERATURE_UNIT;
    private String pressureUnit = CommonData.DEFAULT_PRESSURE_UNIT;
    private String humidityUnit = CommonData.DEFAULT_HUMIDITY_UNIT;

    //Deklaracja wykresu, oraz serii danych
    private GraphView temperatureDataGraph;
    private GraphView pressureDataGraph;
    private GraphView humidityDataGraph;
    private LineGraphSeries<DataPoint> temperatureDataSeries;
    private LineGraphSeries<DataPoint> pressureDataSeries;
    private LineGraphSeries<DataPoint> humidityDataSeries;

    //Deklaracja listy z odczytami czujników
    private List<Double> weatherValuesList;

    //Deklaracja odczytów z konkretnych czujników
    double temperatureRawData;
    double pressureRawData;
    double humidityRawData;

    //Deklaracja stałych parametrów wykresu
    private final int dataGraphMaxDataPointsNumber = 1000;
    private final double dataGraphMaxX = 25.0d;
    private final double dataGraphMinX = 0.0d;
    //dla temperatury
    private final double temperatureDataGraphMaxYCelsius = 120.0d;
    private final double temperatureDataGraphMinYCelsius = -40.0d;
    private final double temperatureDataGraphMaxYFahrenheit = 250.0d;
    private final double temperatureDataGraphMinYFahrenheit = -50.0d;
    //dla ciśnienia
    private final double pressureDataGraphMaxYhPa = 1400.0d;
    private final double pressureDataGraphMinYhPa = 200.0d;
    private final double pressureDataGraphMaxYmmHg = 1000.0d;
    private final double pressureDataGraphMinYmmHg = 0.0d;
    //dla wilgotności
    private final double humidityDataGraphMaxYPercentage = 100.0d;
    private final double humidityDataGraphMinYPercentage = 0.0d;
    private final double humidityDataGraphMaxY_01 = 1.0d;
    private final double humidityDataGraphMinY_01 = 0.0d;

    //Deklaracja alertu przy włączeniu opcji
    private AlertDialog.Builder configAlertDialog;

    //Deklaracja zmiennych potrzebnych dla działania wykresu
    private Timer requestTimer;
    private long requestTimerTimeStamp = 0;
    private long requestTimerPreviousTime = -1;
    private boolean requestTimerFirstRequest = true;
    private boolean requestTimerFirstRequestAfterStop;
    private TimerTask requestTimerTask;
    private final Handler handler = new Handler();
    private RequestQueue queue;

    //Deklaracja interfejsu z preferencjami użytkownika
    SharedPreferences userSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        Intent intent = getIntent();
        Bundle configBundle = intent.getExtras();

        //Inicjalizacja wykresów, dodanie serii, parametryzacja częsci wspólnej(niezależnej od jednostki)
        //dla temperatury
        temperatureDataGraph = findViewById(R.id.temperatureDataGraph);
        temperatureDataSeries = new LineGraphSeries<>(new DataPoint[]{});
        temperatureDataGraph.addSeries(temperatureDataSeries);
        temperatureDataGraph.getViewport().setXAxisBoundsManual(true);
        temperatureDataGraph.getViewport().setMinX(dataGraphMinX);
        temperatureDataGraph.getViewport().setMaxX(dataGraphMaxX);
        temperatureDataGraph.setTitle("Temperatura");
        //dla ciśnienia
        pressureDataGraph = findViewById(R.id.pressureDataGraph);
        pressureDataSeries = new LineGraphSeries<>(new DataPoint[]{});
        pressureDataGraph.addSeries(pressureDataSeries);
        pressureDataGraph.getViewport().setXAxisBoundsManual(true);
        pressureDataGraph.getViewport().setMinX(dataGraphMinX);
        pressureDataGraph.getViewport().setMaxX(dataGraphMaxX);
        pressureDataGraph.setTitle("Ciśnienie");
        //dla wilgotności
        humidityDataGraph = findViewById(R.id.humidityDataGraph);
        humidityDataSeries = new LineGraphSeries<>(new DataPoint[]{});
        humidityDataGraph.addSeries(humidityDataSeries);
        humidityDataGraph.getViewport().setXAxisBoundsManual(true);
        humidityDataGraph.getViewport().setMinX(dataGraphMinX);
        humidityDataGraph.getViewport().setMaxX(dataGraphMaxX);
        humidityDataGraph.setTitle("Wilgotność");

        //Parametryzacja wykresu - zależne od jednostki
        setRangesAndTitles();

        //Inicjalizacja preferencji, ustawienie aktualnych preferencji IP oraz TP
        userSettings = getSharedPreferences("userPref", Activity.MODE_PRIVATE);
        String ipAddressPref = userSettings.getString(CommonData.CONFIG_IP_ADDRESS, CommonData.DEFAULT_IP_ADDRESS);
        int sampleTimePref = userSettings.getInt(CommonData.CONFIG_SAMPLE_TIME, CommonData.DEFAULT_SAMPLE_TIME);
        ipAddress = ipAddressPref;
        sampleTime = sampleTimePref;

        //Inicjalizacja kolejki
        queue = Volley.newRequestQueue(WeatherActivity.this);
    }

    /**
     * @brief Parametryzacja wykresu przy włączeniu widoku
     */
    @Override
    protected void onResume() {
        setRangesAndTitles();
        super.onResume();
    }

    /**
     * @brief Ustawienie zakresów oraz tytułów w zależności od wybranej jednostki
     */
    private void setRangesAndTitles() {

        //Wspólne dla wszystkich jednostek
        temperatureDataGraph.getViewport().setYAxisBoundsManual(true);
        pressureDataGraph.getViewport().setYAxisBoundsManual(true);
        humidityDataGraph.getViewport().setYAxisBoundsManual(true);

        temperatureDataGraph.getGridLabelRenderer().setHorizontalAxisTitle("t[s]");
        pressureDataGraph.getGridLabelRenderer().setHorizontalAxisTitle("t[s]");
        humidityDataGraph.getGridLabelRenderer().setHorizontalAxisTitle("t[s]");

        if (temperatureUnit.equals("C"))
        {
            temperatureDataGraph.getGridLabelRenderer().setVerticalAxisTitle("T[°C]");
            temperatureDataGraph.getViewport().setMinY(temperatureDataGraphMinYCelsius);
            temperatureDataGraph.getViewport().setMaxY(temperatureDataGraphMaxYCelsius);
        }
        else
        {
            temperatureDataGraph.getGridLabelRenderer().setVerticalAxisTitle("T[°F]");
            temperatureDataGraph.getViewport().setMinY(temperatureDataGraphMinYFahrenheit);
            temperatureDataGraph.getViewport().setMaxY(temperatureDataGraphMaxYFahrenheit);
        }
        if (pressureUnit.equals("hPa"))
        {
            pressureDataGraph.getGridLabelRenderer().setVerticalAxisTitle("p[hPa]");
            pressureDataGraph.getViewport().setMinY(pressureDataGraphMinYhPa);
            pressureDataGraph.getViewport().setMaxY(pressureDataGraphMaxYhPa);
        }
        else
        {
            pressureDataGraph.getGridLabelRenderer().setVerticalAxisTitle("p[mmHg]");
            pressureDataGraph.getViewport().setMinY(pressureDataGraphMinYmmHg);
            pressureDataGraph.getViewport().setMaxY(pressureDataGraphMaxYmmHg);
        }
        if (humidityUnit.equals("%"))
        {
            humidityDataGraph.getGridLabelRenderer().setVerticalAxisTitle("H[%]");
            humidityDataGraph.getViewport().setMinY(humidityDataGraphMinYPercentage);
            humidityDataGraph.getViewport().setMaxY(humidityDataGraphMaxYPercentage);
        }
        else
        {
            humidityDataGraph.getGridLabelRenderer().setVerticalAxisTitle("H[0-1]");
            humidityDataGraph.getViewport().setMinY(humidityDataGraphMinY_01);
            humidityDataGraph.getViewport().setMaxY(humidityDataGraphMaxY_01);
        }
    }

    /**
     * @brief Wyświetlenie ostrzeżenia o zatrzymaniu pobierania danych przy przejściu do opcji
     * @note Kliknięcie przycisku opcje wyświetla komunikat, który informuje użytkownika
     * o wstrzymaniu pobierania danych, wybranie OK powoduje przejście do opcji, natomiast
     * Anuluj wyłącza okno bez przerywania pobierania danych
     */
    public void dialogAlertShow() {
        configAlertDialog = new AlertDialog.Builder(WeatherActivity.this);
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

    /**
     * @brief Wczytanie intencji, informacji o IP, TP oraz jednostkach
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent dataIntent) {
        super.onActivityResult(requestCode, resultCode, dataIntent);
        if ((requestCode == CommonData.REQUEST_CODE_CONFIG) && (resultCode == RESULT_OK)) {

            //Pobranie intencji o ustawionym IP, jednostkach oraz TP w opcjach
            ipAddress = dataIntent.getStringExtra(CommonData.CONFIG_IP_ADDRESS);
            temperatureUnit = dataIntent.getStringExtra(CommonData.CONFIG_TEMPERATURE_UNIT);
            pressureUnit = dataIntent.getStringExtra(CommonData.CONFIG_PRESSURE_UNIT);
            humidityUnit = dataIntent.getStringExtra(CommonData.CONFIG_HUMIDITY_UNIT);
            String sampleTimeText = dataIntent.getStringExtra(CommonData.CONFIG_SAMPLE_TIME);
            assert sampleTimeText != null;
            sampleTime = Integer.parseInt(sampleTimeText);
        }
    }

    /**
     * @brief Obsługa wciśnięcia przycisków w danym wiodku
     * @note Wciśnięcie start oraz stop odpowiednio uruchamia oraz zatrzymuje timer,
     * wciśnięcie opcji w przypadku działania timera wyświetla Alert, natomiast
     * gdy nie działa timer to od razu przechodzi do wiodku opcji
     * @param v Wciśnięty widok(np. button, textView)
     */
    public void btns_onClick(View v) {
        switch (v.getId()) {
            case R.id.goToWOptionsBtn: {
                if (requestTimer != null)
                    dialogAlertShow();
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

    /**
     * @param ip Adres IP serwera na którym znajduje się plik
     * @brief Zwraca adres URL do pliku z danymi o pogodzie
     * @retval Pełen adres URL do pliku z danymi o pogodzie
     */
    private String getURL(String ip) {
        return ("http://" + ip + "/" + CommonData.WEATHER_FILE_NAME);
    }

    /**
     * @brief Uruchamia widok opcji dla wykresów
     * @note W intencji przekazywana jest informacja o aktualnych jednostkach, IP oraz TP
     */
    private void openWeatherOptions() {
        //Utworzenie nowej intencji oraz paczki danych
        Intent openConfigIntent = new Intent(WeatherActivity.this, WeatherOptionsActivity.class);
        Bundle configBundle = new Bundle();

        //Umieszczenie w paczce informacji o IP, TP oraz jednostkach
        configBundle.putString(CommonData.CONFIG_IP_ADDRESS, ipAddress);
        configBundle.putInt(CommonData.CONFIG_SAMPLE_TIME, sampleTime);
        configBundle.putString(CommonData.CONFIG_TEMPERATURE_UNIT, temperatureUnit);
        configBundle.putString(CommonData.CONFIG_PRESSURE_UNIT, pressureUnit);
        configBundle.putString(CommonData.CONFIG_HUMIDITY_UNIT, humidityUnit);

        //Umieszczenie paczki w intencji oraz uruchomienie activity, jako ForResult
        openConfigIntent.putExtras(configBundle);
        startActivityForResult(openConfigIntent, CommonData.REQUEST_CODE_CONFIG);
    }

    /**
     * @brief Odczytuje dane z pliku JSON o pogodzie
     * @note W bloku "try" funkcji w zależności od wybranej jednostki odczytywana jest odpowiednia
     * wartość z pliku JSON
     * @param response Odpowiedź serwera jako JSON string
     * @retval Dane o pogodzie w postaci listy
     */
    private List<Double> getRawDataFromResponse(String response) {
        JSONObject jObject;
        weatherValuesList = new ArrayList<>();
        double temperature;
        double pressure;
        double humidity;

        try {
            jObject = new JSONObject(response);
        } catch (JSONException e) {
            e.printStackTrace();
            return weatherValuesList;
        }

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
     * @brief Uruchamia nowy timer, jeśli takowy nie istnieje oraz dodaje do niego zadanie
     */
    private void startRequestTimer() {
        if (requestTimer == null) {
            //Utworzenie nowego timera
            requestTimer = new Timer();
            //Inicjalizacja zadania TimerTask
            initializeRequestTimerTask();
            requestTimer.schedule(requestTimerTask, 0, sampleTime);
        }
    }

    /**
     * @brief Zatrzymuje timer, jeśli takowy istnieje
     */
    private void stopRequestTimerTask() {
        //Zatrzymanie timera, jeśli istnieje
        if (requestTimer != null) {
            requestTimer.cancel();
            requestTimer = null;
            requestTimerFirstRequestAfterStop = true;
        }
    }

    /**
     * @brief Inicjalizacja zadania TimerTask z wykorzystaniem metody post handlera
     * @note W procesie runnable wysyłamy cyklicznie zapytanie typu GET
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
     * @brief Wysłanie zapytania GET na serwer z wykorzystaniem Volley,
     * w celu pobrania danych o pogodzie
     */
    private void sendGetRequest()
    {
        String url = getURL(ipAddress);
        //Utworzenie nowego zapytania typu String, zdefiniowanie co zrobić przy odpowiedzi oraz braku
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) { responseHandling(response); }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) { errorHandling(CommonData.ERROR_RESPONSE); }
                });

        //Dodanie zapytania do kolejki
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
     * @brief Obsługa uzyskanej odpowiedzi na zapytanie GET z serwera
     * @note Dane odczytywane są z wykrozystaniem funckji getRawDataFromResponse,
     * następnie aktualizowany jest wykres
     * @param response Odpowiedź serwera jako JSON string
     */
    private void responseHandling(String response) {
        if (requestTimer != null) {
            //Pobranie informacji o aktualnym czasie
            long requestTimerCurrentTime = SystemClock.uptimeMillis(); // current time
            requestTimerTimeStamp += getValidTimeStampIncrease(requestTimerCurrentTime);

            //Pobranie danych z pliku JSON
            if (getRawDataFromResponse(response).size() != 0) {
                temperatureRawData = getRawDataFromResponse(response).get(0);
                pressureRawData = getRawDataFromResponse(response).get(1);
                humidityRawData = getRawDataFromResponse(response).get(2);
            }

            //Aktualizacja wykresu, gdy próbki są liczbami
            if (isNaN(temperatureRawData) || isNaN(pressureRawData) || isNaN(humidityRawData)) {
                errorHandling(CommonData.ERROR_NAN_DATA);

            } else {

                //Aktualizacja serii
                double timeStamp = requestTimerTimeStamp / 1000.0; // [sec]
                boolean scrollGraph = (timeStamp > dataGraphMaxX); //Skrollowanie gdy czas jest większy niż na osi x
                temperatureDataSeries.appendData(new DataPoint(timeStamp, temperatureRawData), scrollGraph, dataGraphMaxDataPointsNumber);
                pressureDataSeries.appendData(new DataPoint(timeStamp, pressureRawData), scrollGraph, dataGraphMaxDataPointsNumber);
                humidityDataSeries.appendData(new DataPoint(timeStamp, humidityRawData), scrollGraph, dataGraphMaxDataPointsNumber);

                //Odświeżanie widoku
                temperatureDataGraph.onDataChanged(true, true);
                pressureDataGraph.onDataChanged(true, true);
                humidityDataGraph.onDataChanged(true, true);
            }

            //Zapamiętanie ostatniej próbki
            requestTimerPreviousTime = requestTimerCurrentTime;
        }
    }


    /**
     * @brief Sprawdzenie aktualnej próbki czasu po stronie klienta
     * @param currentTime Aktualny czas
     */
    private long getValidTimeStampIncrease(long currentTime) {
        //Zapamiętanie aktualnego czasu po starcie
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

        //Jeśli różnica czasu jest równa 0 zwracamy TP
        if ((currentTime - requestTimerPreviousTime) == 0)
            return sampleTime;

        //Zwraca różnicę czasu pomiędzy aktualnym i poprzednim zapytaniem
        return (currentTime - requestTimerPreviousTime);
    }
}
