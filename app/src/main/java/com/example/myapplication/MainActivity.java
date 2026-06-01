package com.example.myapplication;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.utils.FonosApiManager;

import org.json.JSONObject;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etEmail, etDob, etPassword, etConfirmPassword;
    private RadioGroup rgGender;
    private CheckBox cbAgree;
    private Button btnSelectDate, btnSignUp;
    private TextView tvLogin;
    private FonosApiManager apiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        apiManager = new FonosApiManager(this);

        // Ánh xạ View
        initViews();

        // Xử lý chọn ngày sinh
        btnSelectDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        // Xử lý nút Đăng ký
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleRegistration();
            }
        });

        // Chuyển sang màn hình Đăng nhập
        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }

    private void initViews() {
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etDob = findViewById(R.id.etDob);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        rgGender = findViewById(R.id.rgGender);
        cbAgree = findViewById(R.id.cbAgree);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnSignUp = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvGoToLogin);
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        etDob.setText(dayOfMonth + "/" + (monthOfYear + 1) + "/" + year);
                    }
                }, year, month, day);
        datePickerDialog.show();
    }

    private void handleRegistration() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String dob = etDob.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ họ tên", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email không được để trống");
            return;
        }

        if (TextUtils.isEmpty(dob)) {
            Toast.makeText(this, "Vui lòng chọn ngày sinh", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Mật khẩu phải ít nhất 6 ký tự");
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Mật khẩu nhập lại không khớp");
            return;
        }

        if (!cbAgree.isChecked()) {
            Toast.makeText(this, "Bạn phải đồng ý với điều khoản sử dụng", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullName = firstName + " " + lastName;

        // Gọi API Đăng ký thật qua Supabase
        apiManager.signUpWithEmail(email, password, new FonosApiManager.ApiCallback() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    // Parse response để lấy UID
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    String userId = jsonResponse.getJSONObject("user").getString("id");

                    // Sau khi đăng ký thành công, tạo Profile trong database
                    apiManager.createProfile(userId, email, fullName, "email", new FonosApiManager.ApiCallback() {
                        @Override
                        public void onSuccess(String profileResponse) {
                            Toast.makeText(MainActivity.this, "Đăng ký và tạo hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                            navigateToLogin();
                        }

                        @Override
                        public void onError(String profileError) {
                            // Vẫn cho qua Login dù lỗi profile (có thể do policy)
                            Log.e("MainActivity", "Lỗi tạo profile: " + profileError);
                            Toast.makeText(MainActivity.this, "Đăng ký thành công (Lỗi hồ sơ)", Toast.LENGTH_SHORT).show();
                            navigateToLogin();
                        }
                    });

                } catch (Exception e) {
                    Log.e("MainActivity", "Lỗi parse JSON: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    navigateToLogin();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MainActivity.this, "Đăng ký thất bại: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
