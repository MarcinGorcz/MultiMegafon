package com.example.multimegafon3;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void moveToNadawanieActivity(View view) {
        Intent intent = new Intent(this, NadawanieActivity.class);
        startActivity(intent);
    }

    public void moveToWyborServeraActivity(View view) {
        Intent intent = new Intent(this, WyborServeraActivity.class);
        startActivity(intent);
    }

    public void moveToOdbieranieActivity(View view) {
        Intent intent = new Intent(this, OdbieranieActivity.class);
        startActivity(intent);
    }
}