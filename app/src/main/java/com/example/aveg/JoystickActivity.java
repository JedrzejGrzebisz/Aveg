package com.example.aveg;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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

    //Ustawienie domyślnej wartości IP
    private String ipAddress = CommonData.DEFAULT_IP_ADDRESS;

    //Deklaracja elementów interfejsu użytkownika
    private GraphView joystickDataGraph;
    private TextView centerClickNb;

    //Deklaracja liste z odczytami joysticka
    private List<Integer> joystickValuesList;

    //Deklaracja punktu na wykresie, oraz stałych
    private PointsGraphSeries<DataPoint> joystickDataSeries;
    private final int dataGraphMaxDataPointsNumber = 10000;
    private final double dataGraphMaxX = 5;
    private final double dataGraphMinX = -5;
    private final double rpyDataGraphMaxY = 5;
    private final double rpyDataGraphMinY = -5;

    //Utworzenie handlara dla cyklicznego odczytu danych
    Handler handler = new Handler();
    Runnable runnable;
    int delay = 100; //odświeżenie odczytu stanu joysticka

    //Deklaracja interfejsu z preferencjami użytkownika
    SharedPreferences userSettings;

    //Deklarcja kolejki zapytań
    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_joystick);

        Intent intent = getIntent();
        Bundle configBundle = intent.getExtras();

        //Inicjalizacja wykresu oraz licznika kliknięć środkowego przycisku
        centerClickNb = findViewById(R.id.centerClickNb);
        joystickDataGraph = findViewById(R.id.joystickDataGraph);

        //Utworzenie wykresu punktowego, dodanie serii oraz parametryzacja wykresu
        joystickDataSeries = new PointsGraphSeries<>(new DataPoint[]{});
        joystickDataGraph.addSeries(joystickDataSeries);
        joystickDataGraph.getViewport().setXAxisBoundsManual(true);
        joystickDataGraph.getViewport().setMinX(dataGraphMinX);
        joystickDataGraph.getViewport().setMaxX(dataGraphMaxX);
        joystickDataGraph.getViewport().setYAxisBoundsManual(true);
        joystickDataGraph.getViewport().setMinY(rpyDataGraphMinY);
        joystickDataGraph.getViewport().setMaxY(rpyDataGraphMaxY);
        joystickDataGraph.setTitle("Joystick");
        joystickDataGraph.getGridLabelRenderer().setVerticalAxisTitle("y");
        joystickDataGraph.getGridLabelRenderer().setHorizontalAxisTitle("x");
        joystickDataSeries.setColor(Color.GREEN);
        joystickDataSeries.setShape(PointsGraphSeries.Shape.POINT);

        //Inicjalizacja preferencji, ustawienie aktualnych preferencji IP oraz TP
        userSettings = getSharedPreferences("userPref", Activity.MODE_PRIVATE);
        String ipAddressPref = userSettings.getString(CommonData.CONFIG_IP_ADDRESS, CommonData.DEFAULT_IP_ADDRESS);
        ipAddress = ipAddressPref;

        //Inicjalizacja kolejki
        queue = Volley.newRequestQueue(JoystickActivity.this);
    }

    /**
     * @brief Uruchomienie widoku powoduje wysyłanie zapytania, co określony czas
     * @note By cyklicznie wysyłać zapytania wykorzystany jest sposób z handlerem oraz metodą
     * postDelayed, czas próbkowania jest zdefiniowany stały, w procesie runnable wysyłamy zapytanie
     * GET na serwer
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
     * @brief Zamknięcie widoku przerywa działanie procesu runnable z handlera
     */
    @Override
    protected void onStop() {
        handler.removeCallbacks(runnable); //stop handler when activity not visible
        super.onStop();
    }

    /**
     * @brief Naciśnięcie powrotu przerywa działanie procesu runnable z handlera
     */
    @Override
    public void onBackPressed() {
        handler.removeCallbacks(runnable); //stop handler when back pressed
        super.onBackPressed();
    }

    /**
     * @param ip Adres IP serwera na którym znajduje się plik
     * @brief Zwraca adres URL do pliku z danymi o joysticku
     * @retval Pełen adres URL do pliku z danymi o joysticku
     */
    private String getURL(String ip) {
        return ("http://" + ip + "/" + CommonData.JOYSTICK_FILE_NAME);
    }

    /**
     * @brief Odczytuje dane z pliku JSON o joysticku
     * @param response Odpowiedź serwera jako JSON string
     * @retval Dane o joysticku w postaci listy
     */
    private List<Integer> getRawDataFromResponse(String response) {
        JSONObject jObject;
        joystickValuesList = new ArrayList<>();

        try {
            jObject = new JSONObject(response);
        } catch (JSONException e) {
            e.printStackTrace();
            return joystickValuesList;
        }

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
     * @brief Wysłanie zapytania GET na serwer z wykorzystaniem Volley,
     * w celu pobrania danych o położeniu
     */
    private void sendGetRequest()
    {
        String url = getURL(ipAddress);
        //Utworzenie nowego zapytania tpyu String, zdefiniowanie co zrobić przy odpowiedzi oraz braku
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
     * następnie aktualizowane jest pole textview, q przypadku wykresu usuwany jest poprzedni
     * punkt oraz ustawiany nowy
     * @param response Odpowiedź serwera jako JSON string
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
            joystickDataGraph.onDataChanged(true, true);

            final String centerRawDataString = Integer.toString(centerRawData);
            centerClickNb.setText(centerRawDataString);
        }
    }
}