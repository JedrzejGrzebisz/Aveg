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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Double.isNaN;

public class RpyActivity extends AppCompatActivity {

    //Ustawienie domyślnych wartości
    private String ipAddress = CommonData.DEFAULT_IP_ADDRESS;
    private int sampleTime = CommonData.DEFAULT_SAMPLE_TIME;
    private String rpyUnit = CommonData.DEFAULT_RPY_UNIT;

    //Deklaracja wykresu, oraz serii danych
    private GraphView rpyDataGraph;
    private LineGraphSeries<DataPoint> rollDataSeries;
    private LineGraphSeries<DataPoint> pitchDataSeries;
    private LineGraphSeries<DataPoint> yawDataSeries;

    //Deklaracja listy z odczytami czujników
    private List<Double> rpyValuesList;

    //Deklaracja odczytów z konkretnych czujników
    double rollRawData;
    double pitchRawData;
    double yawRawData;

    //Deklaracja stałych parametrów wykresu
    private final int dataGraphMaxDataPointsNumber = 1000;
    private final double dataGraphMaxX = 25.0d;
    private final double dataGraphMinX = 0.0d;
    private final double rpyDataGraphMaxYRad = 3.2d;
    private final double rpyDataGraphMinYRad = -3.2d;
    private final double rpyDataGraphMaxYDeg = 360.0d;
    private final double rpyDataGraphMinYDeg = 0.0d;

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
        setContentView(R.layout.activity_rpy);

        Intent intent = getIntent();
        Bundle configBundle = intent.getExtras();

        //Inicjalizacja wykresu
        rpyDataGraph = (GraphView) findViewById(R.id.rpyDataGraph);

        //Utworzenie wykresów liniowych, dodanie serii na graphView
        rollDataSeries = new LineGraphSeries<>(new DataPoint[]{});
        pitchDataSeries = new LineGraphSeries<>(new DataPoint[]{});
        yawDataSeries = new LineGraphSeries<>(new DataPoint[]{});
        rpyDataGraph.addSeries(rollDataSeries);
        rpyDataGraph.addSeries(pitchDataSeries);
        rpyDataGraph.addSeries(yawDataSeries);

        //Parametryzacja części wspólnej wykresu(niezależne od jednostki)
        rpyDataGraph.getViewport().setXAxisBoundsManual(true);
        rpyDataGraph.getViewport().setMinX(dataGraphMinX);
        rpyDataGraph.getViewport().setMaxX(dataGraphMaxX);
        rpyDataGraph.setTitle("Położenie kątowe (RPY)");
        rollDataSeries.setTitle("Roll");
        pitchDataSeries.setTitle("Pitch");
        yawDataSeries.setTitle("Yaw");
        rpyDataGraph.getLegendRenderer().setVisible(true);
        rpyDataGraph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        rollDataSeries.setColor(Color.RED);
        pitchDataSeries.setColor(Color.BLUE);
        yawDataSeries.setColor(Color.GREEN);

        //Parametryzacja wykresu - zależne od jednostki
        setRangesAndTitles();

        //Inicjalizacja preferencji, ustawienie aktualnych preferencji IP oraz TP
        userSettings = getSharedPreferences("userPref", Activity.MODE_PRIVATE);
        String ipAddressPref = userSettings.getString(CommonData.CONFIG_IP_ADDRESS, CommonData.DEFAULT_IP_ADDRESS);
        int sampleTimePref = userSettings.getInt(CommonData.CONFIG_SAMPLE_TIME, CommonData.DEFAULT_SAMPLE_TIME);
        ipAddress = ipAddressPref;
        sampleTime = sampleTimePref;

        //Inicjalizacja kolejki
        queue = Volley.newRequestQueue(RpyActivity.this);
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
    private void setRangesAndTitles()
    {
        if (rpyUnit.equals("rad"))
        {
            rpyDataGraph.getViewport().setYAxisBoundsManual(true);
            rpyDataGraph.getViewport().setMinY(rpyDataGraphMinYRad);
            rpyDataGraph.getViewport().setMaxY(rpyDataGraphMaxYRad);
            rpyDataGraph.getGridLabelRenderer().setVerticalAxisTitle("RPY[rad]");
            rpyDataGraph.getGridLabelRenderer().setHorizontalAxisTitle("t[s]");
            rpyDataGraph.getGridLabelRenderer().setNumVerticalLabels(16);
        }
        else if (rpyUnit.equals("deg"))
        {
            rpyDataGraph.getViewport().setYAxisBoundsManual(true);
            rpyDataGraph.getViewport().setMinY(rpyDataGraphMinYDeg);
            rpyDataGraph.getViewport().setMaxY(rpyDataGraphMaxYDeg);
            rpyDataGraph.getGridLabelRenderer().setVerticalAxisTitle("RPY[deg]");
            rpyDataGraph.getGridLabelRenderer().setHorizontalAxisTitle("t[s]");
            rpyDataGraph.getGridLabelRenderer().setNumVerticalLabels(10);
        }
    }

    /**
     * @brief Wyświetlenie ostrzeżenia o zatrzymaniu pobierania danych przy przejściu do opcji
     * @note Kliknięcie przycisku opcje wyświetla komunikat, który informuje użytkownika
     * o wstrzymaniu pobierania danych, wybranie OK powoduje przejście do opcji, natomiast
     * Anuluj wyłącza okno bez przerywania pobierania danych
     */
    public void dialogAlertShow() {
        configAlertDialog = new AlertDialog.Builder(RpyActivity.this);
        configAlertDialog.setTitle("Pobieranie danych zostanie zatrzymane");
        configAlertDialog.setIcon(android.R.drawable.ic_dialog_alert);
        configAlertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                stopRequestTimerTask();
                openRpyOptions();
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
     * @brief Wczytanie intencji, informacji o IP, TP oraz jednostce
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent dataIntent) {
        super.onActivityResult(requestCode, resultCode, dataIntent);
        if ((requestCode == CommonData.REQUEST_CODE_CONFIG) && (resultCode == RESULT_OK)) {

            //Pobranie intencji o ustawionym IP, jednostce oraz TP w opcjach
            ipAddress = dataIntent.getStringExtra(CommonData.CONFIG_IP_ADDRESS);
            rpyUnit = dataIntent.getStringExtra(CommonData.CONFIG_RPY_UNIT);
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
    public void btnsRpy_onClick(View v) {
        switch (v.getId()) {
            case R.id.goToRpyOptionsBtn: {
                if (requestTimer != null)
                    dialogAlertShow();
                else
                    openRpyOptions();
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

    /**
     * @brief Zwraca adres URL do pliku z danymi o kątach
     * @note W zależności od wybranej jednostki rad/deg zwracany jest adres konkretnego pliku
     * na serwerze
     * @param ip Adres IP serwera na którym znajduje się plik
     * @retval Pełen adres URL do pliku z danymi o kątach
     */
    private String getURL(String ip) {
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
     * @brief Uruchamia widok opcji dla wykresów
     * @note W intencji przekazywana jest informacja o aktualnej jednostce, IP oraz TP
     */
    private void openRpyOptions() {
        //Utworzenie nowej intencji oraz paczki danych
        Intent openConfigIntent = new Intent(RpyActivity.this, RpyOptionsActivity.class);
        Bundle configBundle = new Bundle();

        //Umieszczenie w paczce informacji o IP, TP oraz jednostce
        configBundle.putString(CommonData.CONFIG_IP_ADDRESS, ipAddress);
        configBundle.putInt(CommonData.CONFIG_SAMPLE_TIME, sampleTime);
        configBundle.putString(CommonData.CONFIG_RPY_UNIT, rpyUnit);

        //Umieszczenie paczki w intencji oraz uruchomienie activity, jako ForResult
        openConfigIntent.putExtras(configBundle);
        startActivityForResult(openConfigIntent, CommonData.REQUEST_CODE_CONFIG);
    }

    /**
     * @brief Odczytuje dane z pliku JSON o położeniu
     * @param response Odpowiedź serwera jako JSON string
     * @retval Dane o położeniu w postaci listy
     */
    private List<Double> getRawDataFromResponse(String response) {
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
     * w celu pobrania danych o położeniu
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
            long requestTimerCurrentTime = SystemClock.uptimeMillis();
            requestTimerTimeStamp += getValidTimeStampIncrease(requestTimerCurrentTime);

            //Pobranie danych z pliku JSON
            if (getRawDataFromResponse(response).size() != 0) {
                rollRawData = getRawDataFromResponse(response).get(0);
                pitchRawData = getRawDataFromResponse(response).get(1);
                yawRawData = getRawDataFromResponse(response).get(2);
            }
            //Aktualizacja wykresu, gdy próbki są liczbami
            if (isNaN(rollRawData) || isNaN(pitchRawData) || isNaN(yawRawData)) {
                errorHandling(CommonData.ERROR_NAN_DATA);
            }
            else {

                //Aktualizacja serii
                double timeStamp = requestTimerTimeStamp / 1000.0; // [sec]
                boolean scrollGraph = (timeStamp > dataGraphMaxX); //Skrollowanie gdy czas jest większy niż na osi x
                rollDataSeries.appendData(new DataPoint(timeStamp, rollRawData), scrollGraph, dataGraphMaxDataPointsNumber);
                pitchDataSeries.appendData(new DataPoint(timeStamp, pitchRawData), scrollGraph, dataGraphMaxDataPointsNumber);
                yawDataSeries.appendData(new DataPoint(timeStamp, yawRawData), scrollGraph, dataGraphMaxDataPointsNumber);
                //Odświeżanie widoku
                rpyDataGraph.onDataChanged(true, true);
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

