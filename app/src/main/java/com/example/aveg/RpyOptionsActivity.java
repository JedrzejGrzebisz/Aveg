package com.example.aveg;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RpyOptionsActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    /* BEGIN config widgets */
    private EditText ipEditText;
    private EditText sampleTimeEditText;
    private String rpyUnit;
    private ArrayAdapter<CharSequence> adapter;
    /* END config widgets */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rpy_options);

        // get the Intent that started this Activity
        Intent intent = getIntent();

        // get the Bundle that stores the data of this Activity
        Bundle configBundle = intent.getExtras();

        ipEditText = findViewById(R.id.userInputRpyIP);
        String ip = configBundle.getString(CommonData.CONFIG_IP_ADDRESS, CommonData.DEFAULT_IP_ADDRESS);
        ipEditText.setText(ip);

        sampleTimeEditText = findViewById(R.id.userInputRpyTp);
        int tp = configBundle.getInt(CommonData.CONFIG_SAMPLE_TIME, CommonData.DEFAULT_SAMPLE_TIME);
        sampleTimeEditText.setText(Integer.toString(tp));

        //Initialize spinner
        Spinner rpyUnitPicked = findViewById(R.id.rpyUnitPicked);
        adapter = ArrayAdapter.createFromResource(this, R.array.rpyUnit, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rpyUnitPicked.setAdapter(adapter);
        rpyUnitPicked.setOnItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra(CommonData.CONFIG_IP_ADDRESS, ipEditText.getText().toString());
        intent.putExtra(CommonData.CONFIG_SAMPLE_TIME, sampleTimeEditText.getText().toString());
        intent.putExtra(CommonData.CONFIG_RPY_UNIT, rpyUnit);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        rpyUnit = parent.getItemAtPosition(position).toString();
        //Toast.makeText(parent.getContext(), rpyUnit, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
