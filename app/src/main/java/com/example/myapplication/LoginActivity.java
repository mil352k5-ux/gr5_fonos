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

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.utils.FonosApiManager;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmailUsername, etPassword;
    private Button btnLogin;
    private TextView tvSignUp;

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
}