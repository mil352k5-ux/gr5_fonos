package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.myapplication.utils.SupabaseConfig;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.controller.FonosApiManager;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmailUsername, etPassword;
    private Button btnLogin;
    private TextView tvSignUp;

    private Button btnGoogleLogin;
    private GoogleSignInClient googleDoorClient;
    private ActivityResultLauncher<Intent> googleDoorLauncher;

    private FonosApiManager apiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmailUsername = findViewById(R.id.etEmailUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUp = findViewById(R.id.tvSignUp);

        apiManager = new FonosApiManager(this);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogin();
            }
        });

        tvSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);

        prepareGoogleLogin();

        btnGoogleLogin.setOnClickListener(v -> {
            googleDoorClient.signOut().addOnCompleteListener(task -> {
                Intent intent = googleDoorClient.getSignInIntent();
                googleDoorLauncher.launch(intent);
            });
        });
    }

    private void handleLogin() {
        String email = etEmailUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmailUsername.setError("Email không được để trống");
            etEmailUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Mật khẩu không được để trống");
            etPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Đang đăng nhập...");

        apiManager.loginWithEmail(email, password, new FonosApiManager.ApiCallback() {
            @Override
            public void onSuccess(String responseBody) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Đăng nhập");

                try {
                    JSONObject json = new JSONObject(responseBody);

                    String accessToken = json.getString("access_token");
                    String refreshToken = json.optString("refresh_token", "");

                    JSONObject user = json.getJSONObject("user");
                    String userId = user.getString("id");
                    String userEmail = user.getString("email");

                    saveLoginSession(accessToken, refreshToken, userId, userEmail);

                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish();

                } catch (Exception e) {
                    Toast.makeText(LoginActivity.this, "Lỗi xử lý dữ liệu đăng nhập", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Đăng nhập");

                Toast.makeText(LoginActivity.this, "Sai email hoặc mật khẩu!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveLoginSession(String accessToken, String refreshToken, String userId, String email) {
        SharedPreferences sharedPreferences = getSharedPreferences("FonosSession", MODE_PRIVATE);

        sharedPreferences.edit()
                .putString("access_token", accessToken)
                .putString("refresh_token", refreshToken)
                .putString("user_id", userId)
                .putString("email", email)
                .putBoolean("is_logged_in", true)
                .apply();
    }

    private void prepareGoogleLogin() {
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .requestIdToken(SupabaseConfig.GOOGLE_WEB_CLIENT_ID)
                .build();

        googleDoorClient = GoogleSignIn.getClient(this, options);

        googleDoorLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Intent data = result.getData();
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        String googleIdToken = account.getIdToken();

                        if (googleIdToken == null) {
                            Toast.makeText(this, "Không lấy được Google ID Token", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        loginSupabaseByGoogle(googleIdToken);

                    } catch (ApiException e) {
                        Toast.makeText(this, "Google Login lỗi: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void loginSupabaseByGoogle(String googleIdToken) {
        btnGoogleLogin.setEnabled(false);
        btnGoogleLogin.setText("Đang xác thực Google...");

        apiManager.loginWithGoogleToken(googleIdToken, new FonosApiManager.ApiCallback() {
            @Override
            public void onSuccess(String responseBody) {
                btnGoogleLogin.setEnabled(true);
                btnGoogleLogin.setText("Google");

                try {
                    JSONObject json = new JSONObject(responseBody);

                    String accessToken = json.getString("access_token");
                    String refreshToken = json.optString("refresh_token", "");

                    JSONObject user = json.getJSONObject("user");
                    String userId = user.getString("id");
                    String userEmail = user.optString("email", "");

                    saveLoginSession(accessToken, refreshToken, userId, userEmail);

                    Toast.makeText(LoginActivity.this, "Đăng nhập Google thành công!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish();

                } catch (Exception e) {
                    Toast.makeText(LoginActivity.this, "Lỗi đọc dữ liệu Google Login", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                btnGoogleLogin.setEnabled(true);
                btnGoogleLogin.setText("Google");

                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}