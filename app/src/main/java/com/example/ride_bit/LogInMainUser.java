package com.example.ride_bit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LogInMainUser extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {
    Button btn_log_in, google_log_in;
    TextView singIn;

    FirebaseAuth auth;
    GoogleSignInClient mGoogleSingInClient;


    public static  final int SING_IN_GOOGLE = 777;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in_main);

        btn_log_in = findViewById(R.id.btn_log_in);
        google_log_in = findViewById(R.id.google_log_in);
        singIn = findViewById(R.id.singIn);

        btn_log_in.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LogInUser.class);
                startActivityForResult(intent, 0);
            }
        });

        singIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SingInMainUser.class);
                startActivityForResult(intent, 0);
            }
        });

        auth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSingInClient = GoogleSignIn.getClient(this, gso);


        google_log_in.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = mGoogleSingInClient.getSignInIntent();
                startActivityForResult(intent, SING_IN_GOOGLE);
            }
        });

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==SING_IN_GOOGLE){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            //GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            //handleSingIn(result);
            try{
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            }catch (ApiException e){
                //Toast
            }
        }
    }

    private void firebaseAuthWithGoogle(final GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            FirebaseUser user = auth.getCurrentUser();
                            Intent intent = new Intent(getApplicationContext(), Home.class);
                            startActivityForResult(intent, 0);
                            finish();
                            Toast.makeText(getApplicationContext(),
                                    "Successfully sing in with Google account", Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(getApplicationContext(),
                                    "Failed sing in with Google account", Toast.LENGTH_SHORT).show();

                        }
                    }
                });
    }

}
