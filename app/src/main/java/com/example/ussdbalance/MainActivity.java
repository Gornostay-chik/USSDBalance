package com.example.ussdbalance;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {
    private static final int REQ = 123;
    private static final String TAG = "MainActivity";
    private TextView statusTextView;

    // Флаги, чтобы запрос разрешений выполнялся только один раз
    private boolean permissionsRequested = false;
    private boolean serviceLaunched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() called");
        setContentView(R.layout.activity_main);
        statusTextView = findViewById(R.id.textStatus);
        statusTextView.setText("Идет проверка разрешений...");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() called");

        if (!serviceLaunched) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {

                statusTextView.setText("Разрешения уже предоставлены. Запускаем сервис...");
                Log.d(TAG, "Разрешения уже есть, запускаем сервис.");
                launchUssdService();
            } else {
                if (!permissionsRequested) {
                    statusTextView.setText("Запрос разрешений...");
                    permissionsRequested = true;
                    Log.d(TAG, "Запрос разрешений еще не выполнялся. Отложенный запрос.");
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        ActivityCompat.requestPermissions(
                                MainActivity.this,
                                new String[]{
                                        Manifest.permission.CALL_PHONE,
                                        Manifest.permission.READ_PHONE_STATE
                                },
                                REQ
                        );
                    }, 200);
                } else {
                    statusTextView.setText("Ожидание предоставления разрешений...");
                    Log.d(TAG, "Разрешения ещё не предоставлены, ожидаем ответа.");
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult() called");
        if (requestCode == REQ && grantResults.length > 0) {
            boolean granted = true;
            for (int res : grantResults) {
                if (res != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (granted) {
                statusTextView.setText("Все разрешения получены. Запускаем сервис...");
                Log.d(TAG, "Все разрешения получены");
                launchUssdService();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Ошибка")
                        .setMessage("Не все разрешения предоставлены. Приложение не сможет работать корректно.")
                        .setPositiveButton("ОК", (dialog, which) -> {
                            statusTextView.setText("Ошибка: разрешения не предоставлены.");
                            Log.d(TAG, "Пользователь отказал в разрешениях");
                        })
                        .setCancelable(false)
                        .show();
            }
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Ошибка")
                    .setMessage("Ошибка при запросе разрешений.")
                    .setPositiveButton("ОК", (dialog, which) -> {
                        statusTextView.setText("Ошибка: запрос разрешений не выполнен.");
                        Log.d(TAG, "Ошибка: пустой grantResults");
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    private void launchUssdService() {
        serviceLaunched = true;
        Intent svc = new Intent(this, UssdService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }
        statusTextView.append("\nСервис запущен.");
        Log.d(TAG, "USSD-сервис запущен.");
    }
}
