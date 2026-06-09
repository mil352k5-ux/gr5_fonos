package com.example.myapplication.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.controller.FonosApiManager;
import com.example.myapplication.service.AudioPlayerService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ProfileActivity extends AppCompatActivity {

    private TextView btnBack, tvNameHeader, tvJoinedDate, tvAvatarPlaceholder;
    private ImageView imgAvatar;
    private EditText etFullName, etEmail;
    private View btnSave, btnLogout;

    private FonosApiManager apiManager;
    private String token;
    private String userId;
    private SharedPreferences prefs;
    private String currentAvatarUrl = "";
    private boolean profileExists = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        apiManager = new FonosApiManager(this);

        prefs = getSharedPreferences("FonosSession", MODE_PRIVATE);
        token = prefs.getString("access_token", "");
        userId = prefs.getString("user_id", "");

        if (token.isEmpty() || userId.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập để tiếp tục", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }

        btnBack = findViewById(R.id.btnBack);
        tvNameHeader = findViewById(R.id.tvNameHeader);
        tvJoinedDate = findViewById(R.id.tvJoinedDate);
        tvAvatarPlaceholder = findViewById(R.id.tvAvatarPlaceholder);
        imgAvatar = findViewById(R.id.imgAvatar);

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);

        String storedEmail = prefs.getString("email", "");
        if (!storedEmail.isEmpty()) {
            etEmail.setText(storedEmail);
            tvNameHeader.setText(storedEmail);
            setupAvatar("", storedEmail, "");
        }

        btnSave = findViewById(R.id.btnSave);
        btnLogout = findViewById(R.id.btnLogout);

        btnBack.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> logOut());

        loadProfileData();
    }

    private void loadProfileData() {
        apiManager.getProfile(token, userId, new FonosApiManager.ApiCallback() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    JSONArray array = new JSONArray(responseBody);
                    String storedEmailVal = prefs.getString("email", "");
                    String fullName = "";
                    String email = storedEmailVal;
                    String avatarUrl = "";
                    String createdAt = "";

                    if (array.length() > 0) {
                        JSONObject profile = array.getJSONObject(0);
                        fullName = profile.optString("full_name", "");
                        email = profile.optString("email", storedEmailVal);
                        avatarUrl = profile.optString("avatar_url", "");
                        createdAt = profile.optString("created_at", "");
                        currentAvatarUrl = avatarUrl;
                        profileExists = true;
                    } else {
                        profileExists = false;
                    }

                    etFullName.setText(fullName);
                    etEmail.setText(email);

                    if (!fullName.isEmpty()) {
                        tvNameHeader.setText(fullName);
                    } else {
                        tvNameHeader.setText(email);
                    }

                    setupAvatar(fullName, email, avatarUrl);
                    setupJoinedDate(createdAt);

                } catch (Exception e) {
                    Toast.makeText(ProfileActivity.this, "Lỗi giải mã thông tin hồ sơ", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (isJwtExpired(errorMessage)) {
                    handleJwtExpired(new Runnable() {
                        @Override
                        public void run() {
                            loadProfileData();
                        }
                    });
                } else {
                    Toast.makeText(ProfileActivity.this, "Không lấy được thông tin: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private boolean isJwtExpired(String errorMessage) {
        return errorMessage != null && (errorMessage.contains("JWT expired") || errorMessage.contains("PGRST303"));
    }

    private void handleJwtExpired(Runnable retryOperation) {
        String refreshToken = prefs.getString("refresh_token", "");

        if (refreshToken.isEmpty()) {
            Toast.makeText(this, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            logOut();
            return;
        }

        apiManager.refreshSession(refreshToken, new FonosApiManager.ApiCallback() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    JSONObject json = new JSONObject(responseBody);
                    String newAccessToken = json.getString("access_token");
                    String newRefreshToken = json.optString("refresh_token", "");

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("access_token", newAccessToken);
                    if (!newRefreshToken.isEmpty()) {
                        editor.putString("refresh_token", newRefreshToken);
                    }
                    editor.apply();

                    token = newAccessToken;
                    retryOperation.run();

                } catch (Exception e) {
                    Toast.makeText(ProfileActivity.this, "Lỗi gia hạn phiên đăng nhập.", Toast.LENGTH_LONG).show();
                    logOut();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ProfileActivity.this, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
                logOut();
            }
        });
    }

    private void setupAvatar(String fullName, String email, String avatarUrl) {
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            imgAvatar.setVisibility(View.VISIBLE);
            tvAvatarPlaceholder.setVisibility(View.GONE);
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .into(imgAvatar);
        } else {
            imgAvatar.setVisibility(View.GONE);
            tvAvatarPlaceholder.setVisibility(View.VISIBLE);
            String initial = "U";
            if (!fullName.isEmpty()) {
                initial = fullName.substring(0, 1).toUpperCase(Locale.ROOT);
            } else if (!email.isEmpty()) {
                initial = email.substring(0, 1).toUpperCase(Locale.ROOT);
            }
            tvAvatarPlaceholder.setText(initial);
        }
    }

    private void setupJoinedDate(String isoDateStr) {
        if (isoDateStr == null || isoDateStr.isEmpty()) {
            tvJoinedDate.setText("Tham gia: Không rõ");
            return;
        }
        try {
            // "2026-06-08T07:54:12.123+00:00"
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            parser.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = parser.parse(isoDateStr);
            if (date != null) {
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                tvJoinedDate.setText("Tham gia: " + formatter.format(date));
            }
        } catch (Exception e) {
            // fallback if format differs
            tvJoinedDate.setText("Tham gia: " + isoDateStr.split("T")[0]);
        }
    }

    private void saveProfile() {
        String newFullName = etFullName.getText().toString().trim();

        if (newFullName.isEmpty()) {
            etFullName.setError("Họ tên không được để trống");
            return;
        }

        btnSave.setEnabled(false);
        saveProfileInternal(newFullName, currentAvatarUrl);
    }

    private void saveProfileInternal(String newFullName, String newAvatarUrl) {
        String email = etEmail.getText().toString().trim();
        if (profileExists) {
            apiManager.updateProfile(token, userId, newFullName, newAvatarUrl, new FonosApiManager.ApiCallback() {
                @Override
                public void onSuccess(String responseBody) {
                    btnSave.setEnabled(true);
                    Toast.makeText(ProfileActivity.this, "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show();
                    tvNameHeader.setText(newFullName);
                    setupAvatar(newFullName, email, newAvatarUrl);
                }

                @Override
                public void onError(String errorMessage) {
                    if (isJwtExpired(errorMessage)) {
                        handleJwtExpired(new Runnable() {
                            @Override
                            public void run() {
                                saveProfileInternal(newFullName, newAvatarUrl);
                            }
                        });
                    } else {
                        btnSave.setEnabled(true);
                        Toast.makeText(ProfileActivity.this, "Lưu thất bại: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
            });
        } else {
            apiManager.insertProfile(token, userId, email, newFullName, newAvatarUrl, "email", new FonosApiManager.ApiCallback() {
                @Override
                public void onSuccess(String responseBody) {
                    btnSave.setEnabled(true);
                    profileExists = true;
                    Toast.makeText(ProfileActivity.this, "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show();
                    tvNameHeader.setText(newFullName);
                    setupAvatar(newFullName, email, newAvatarUrl);
                }

                @Override
                public void onError(String errorMessage) {
                    if (isJwtExpired(errorMessage)) {
                        handleJwtExpired(new Runnable() {
                            @Override
                            public void run() {
                                saveProfileInternal(newFullName, newAvatarUrl);
                            }
                        });
                    } else {
                        btnSave.setEnabled(true);
                        Toast.makeText(ProfileActivity.this, "Lưu thất bại: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    private void logOut() {
        // Clear shared preferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        // Stop AudioPlayerService if running
        Intent stopIntent = new Intent(this, AudioPlayerService.class);
        stopIntent.setAction(AudioPlayerService.ACTION_STOP);
        startService(stopIntent);

        Toast.makeText(this, "Đã đăng xuất tài khoản", Toast.LENGTH_SHORT).show();

        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
