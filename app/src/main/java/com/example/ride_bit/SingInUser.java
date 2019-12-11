package com.example.ride_bit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ride_bit.Common.Common;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import dmax.dialog.SpotsDialog;

public class SingInUser extends AppCompatActivity {
    TextView email;
    TextView pass;
    FirebaseAuth auth;
    FirebaseDatabase db;
    DatabaseReference users;
    Button btnSingIn;
    ImageView back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sing_in);
        auth= FirebaseAuth.getInstance();
        db= FirebaseDatabase.getInstance();
        users= db.getReference(Common.user_rider_tb1);
        email=findViewById(R.id.s_email);
        pass=findViewById(R.id.s_pass);
        btnSingIn=findViewById(R.id.btn_sing_in);

        btnSingIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                singIn();
            }
        });

        back = findViewById(R.id.backSingIn);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SingInUser.this, SingInMainUser.class);
                startActivity(intent);
            }
        });

        if(auth.getCurrentUser()!=null){
            FirebaseUser user= auth.getCurrentUser();
        }
    }

    private void singIn() {
        if(email.getText().toString().isEmpty()){
            email.setError("This field can not be blank");
            return;
        }
        if(pass.getText().toString().isEmpty()){
            pass.setError("This field can not be blank");
            return;
        }

        final AlertDialog waitingDialog = new SpotsDialog(SingInUser.this);
        waitingDialog.show();

        auth.signInWithEmailAndPassword(email.getText().toString(), pass.getText().toString())
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        waitingDialog.dismiss();
                        startActivity(new Intent(SingInUser.this, Home.class));
                        Toast toast =
                                Toast.makeText(getApplicationContext(),
                                        "Sing in successfully", Toast.LENGTH_SHORT);

                        toast.show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                waitingDialog.dismiss();
                Toast toast =
                        Toast.makeText(getApplicationContext(),
                                "Sing in failure"+e.getMessage(), Toast.LENGTH_SHORT);

                toast.show();
                return;
            }
        });
    }
}
