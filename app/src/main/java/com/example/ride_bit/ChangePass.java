package com.example.ride_bit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.ride_bit.Common.Common;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class ChangePass extends AppCompatActivity {

    ImageView back;
    EditText pass, passConfirm, old_pass;
    Button change;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_pass);

        back = findViewById(R.id.backChangePass);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChangePass.this, Home.class);
                startActivity(intent);
            }
        });

        old_pass = findViewById(R.id.old_pass);
        pass = findViewById(R.id.changePass);
        passConfirm = findViewById(R.id.confirmChangePass);

        change = findViewById(R.id.btn_change_pass);
        change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                changePassword();
    }

    private void changePassword() {

        if(pass.getText().toString().length()>6){
            if (pass.getText().toString().equals(passConfirm.getText().toString()) ){
                String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                //Get aut credentials from the user for re-authentication.
                AuthCredential credential = EmailAuthProvider.getCredential(email, old_pass.getText().toString());
                FirebaseAuth.getInstance().getCurrentUser()
                        .reauthenticate(credential)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()){
                                    FirebaseAuth.getInstance().getCurrentUser()
                                            .updatePassword(passConfirm.getText().toString())
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if(task.isSuccessful()){
                                                        Map<String, Object> password = new HashMap<>();
                                                        password.put("password", passConfirm.getText().toString());

                                                        DatabaseReference riderInfo = FirebaseDatabase.getInstance().getReference(Common.user_rider_tb1);

                                                        riderInfo.child((FirebaseAuth.getInstance().getCurrentUser().getUid()))
                                                                .updateChildren(password)
                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                        if(task.isSuccessful())
                                                                            Toast.makeText(ChangePass.this, "Password was changed", Toast.LENGTH_SHORT).show();
                                                                        else
                                                                            Toast.makeText(ChangePass.this, "Password was changed, but not update to Database", Toast.LENGTH_SHORT).show();

                                                                    }
                                                                });
                                                    }else{
                                                        Toast.makeText(ChangePass.this, "Password doesn't change", Toast.LENGTH_LONG).show();
                                                    }
                                                }
                                            });
                                }else{
                                    Toast.makeText(ChangePass.this, "Wrong old password.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }else{
                Toast.makeText(ChangePass.this, "Password doesn't match.", Toast.LENGTH_SHORT).show();
            }
        }else {
            pass.setError("Password too short");
        }

    }
        });
    }
}
