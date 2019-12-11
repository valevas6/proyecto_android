package com.example.ride_bit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ride_bit.Common.Common;
import com.example.ride_bit.Model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LogInUser extends AppCompatActivity {
    Button btnregistro;
    TextView email;
    TextView pass;
    TextView name;
    TextView phone;
    FirebaseAuth auth;
    FirebaseDatabase db;
    DatabaseReference users;
    ImageView back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);
        auth= FirebaseAuth.getInstance();
        db= FirebaseDatabase.getInstance();
        users= db.getReference(Common.user_rider_tb1);
        email=findViewById(R.id.email);
        pass=findViewById(R.id.pass);
        name=findViewById(R.id.name);
        phone=findViewById(R.id.phone);
        btnregistro=findViewById(R.id.register);

        back = findViewById(R.id.backLogIn);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LogInUser.this, LogInMainUser.class);
                startActivity(intent);
            }
        });


        btnregistro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }
        });

    }

    private void register() {
        if(name.getText().toString().isEmpty()){
            name.setError("This field can not be blank");
            return;
        }
        if(email.getText().toString().isEmpty()){
            email.setError("This field can not be blank");
            return;
        }
        if(pass.getText().toString().isEmpty()){
            pass.setError("This field can not be blank");
            return;
        }
        if(pass.getText().toString().length()<6){
            pass.setError("Password too short");
            return;
        }
        if(phone.getText().toString().isEmpty()){
            phone.setError("This field can not be blank");
            return;
        }
        if(phone.getText().toString().length()<8){
            phone.setError("Incorrect phone number");
            return;
        }

        //Add new user
        auth.createUserWithEmailAndPassword(email.getText().toString(), pass.getText().toString())
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        User user = new User();
                        user.setEmail(email.getText().toString());
                        user.setName(name.getText().toString());
                        user.setPassword(pass.getText().toString());
                        user.setPhone(phone.getText().toString());

                        users.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                .setValue(user)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        startActivity(new Intent(LogInUser.this, SingInUser.class));
                                        Toast toast =
                                                Toast.makeText(getApplicationContext(),
                                                        "Sing in successfully", Toast.LENGTH_SHORT);

                                        toast.show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast toast =
                                                Toast.makeText(getApplicationContext(),
                                                        "Sing in failure"+e.getMessage(), Toast.LENGTH_SHORT);

                                        toast.show();

                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast toast =
                                Toast.makeText(getApplicationContext(),
                                        "Sing in failure"+e.getMessage(), Toast.LENGTH_SHORT);

                        toast.show();
                        return;
                    }
                });
    }
}
