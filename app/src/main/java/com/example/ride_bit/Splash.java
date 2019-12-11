package com.example.ride_bit;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

public class Splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        Thread myTread = new Thread(){
            @Override
            public void run() {
                try {
                    sleep(1000);
                    Intent intent = new Intent(getApplicationContext(), SingInMainUser.class);
                    startActivityForResult(intent, 0);
                    finish();
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        };
        myTread.start();
    }
}
