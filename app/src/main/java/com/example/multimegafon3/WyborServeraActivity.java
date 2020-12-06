package com.example.multimegafon3;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class WyborServeraActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wybor_servera);
    }

    public void moveToOdbieranie(View view) {
        Intent intent = new Intent(this, OdbieranieActivity.class);
        startActivity(intent);
    }
}