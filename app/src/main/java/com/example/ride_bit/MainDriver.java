package com.example.ride_bit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.example.ride_bit.Common.Common;
import com.example.ride_bit.Model.Driver;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import dmax.dialog.SpotsDialog;

public class MainDriver extends AppCompatActivity {
    Button btnSingIn, btnRegister;
    FirebaseAuth auth;
    FirebaseDatabase db;
    DatabaseReference users;
    RelativeLayout rootLayout;
    ImageView back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_driver);
        //Init firebase
        auth = FirebaseAuth.getInstance();
        db= FirebaseDatabase.getInstance();
        users=db.getReference(Common.user_driver_tb1);

        rootLayout= findViewById(R.id.rootLayout);

        btnSingIn = findViewById(R.id.btn_singinDriver);
        btnRegister = findViewById(R.id.btn_registerDriver);

        btnSingIn .setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoginDialog();
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRegisterDialog();
            }
        });

        back = findViewById(R.id.backDriver);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainDriver.this, Home.class);
                startActivity(intent);
            }
        });
    }

    private void showLoginDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);

        LayoutInflater inflater = LayoutInflater.from(this);
        View logindriver = inflater.inflate(R.layout.logindriver, null);

        final EditText emaildriver= logindriver.findViewById(R.id.emaildriver);
        final EditText passdriver= logindriver.findViewById(R.id.passdriver);

        dialog.setView(logindriver);

        dialog.setPositiveButton("Log in", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                dialog.dismiss();
                //Check validation
                if (TextUtils.isEmpty(emaildriver.getText().toString())) {
                    Snackbar.make(rootLayout, "Please enter your email address", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(passdriver.getText().toString())) {
                    Snackbar.make(rootLayout, "Please enter your password", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                final android.app.AlertDialog waitingDialog = new SpotsDialog(MainDriver.this);
                waitingDialog.show();
                //Login
                auth.signInWithEmailAndPassword(emaildriver.getText().toString(), passdriver.getText().toString())
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                waitingDialog.dismiss();
                                startActivity(new Intent(MainDriver.this, DriverMap.class));
                                finish();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        waitingDialog.dismiss();
                        Snackbar.make(rootLayout, "Failed"+e.getMessage(), Snackbar.LENGTH_SHORT).show();
                    }
                });
            }
        });
        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void showRegisterDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);

        LayoutInflater inflater = LayoutInflater.from(this);
        View registerlayout = inflater.inflate(R.layout.register_driver, null);

        final EditText namedriver = registerlayout.findViewById(R.id.namedriver);
        final EditText emaildriver= registerlayout.findViewById(R.id.emaildriver);
        final EditText passdriver= registerlayout.findViewById(R.id.passdriver);
        final EditText phonedriver= registerlayout.findViewById(R.id.phonedriver);

        dialog.setView(registerlayout);

        dialog.setPositiveButton("Register", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                dialog.dismiss();

                //Check validation
                if(TextUtils.isEmpty(emaildriver.getText().toString())){
                    Snackbar.make(rootLayout, "Please enter your email address", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if(TextUtils.isEmpty(namedriver.getText().toString())){
                    Snackbar.make(rootLayout, "Please enter your name", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if(TextUtils.isEmpty(passdriver.getText().toString())){
                    Snackbar.make(rootLayout, "Please enter your password", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if(TextUtils.isEmpty(phonedriver.getText().toString())){
                    Snackbar.make(rootLayout, "Please enter your phone", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                //Register new user
                auth.createUserWithEmailAndPassword(emaildriver.getText().toString(), passdriver.getText().toString())
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                //Save user to db
                                Driver driver = new Driver();
                                driver.setEmail(emaildriver.getText().toString());
                                driver.setName(namedriver.getText().toString());
                                driver.setPassword(passdriver.getText().toString());
                                driver.setPhone(phonedriver.getText().toString());

                                users.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                        .setValue(driver)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Snackbar.make(rootLayout, "Register successful!!", Snackbar.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Snackbar.make(rootLayout, "Register failed!!"+e.getMessage(), Snackbar.LENGTH_SHORT).show();

                                            }
                                        });
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Snackbar.make(rootLayout, "Register failed!!"+e.getMessage(), Snackbar.LENGTH_SHORT).show();
                            }
                        });

            }
        });

        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }
}
