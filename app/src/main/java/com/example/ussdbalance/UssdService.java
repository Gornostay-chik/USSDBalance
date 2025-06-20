package com.example.ussdbalance;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;

public class UssdService extends Service {
    private static final String CHANNEL_ID = "ussd_channel";
    private static final int NOTIF_ID = 42;
    private static final String TAG = "UssdService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate() called");

        // Создаем уведомительный канал (требуется для API >= 26)
        createNotificationChannel();

        // Формируем уведомление для Foreground-сервиса
        Notification.Builder notificationBuilder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("USSD запрос")
                    .setContentText("Отправка USSD запроса...")
                    .setSmallIcon(android.R.drawable.stat_notify_sync)
                    .setOngoing(true);
        } else {
            notificationBuilder = new Notification.Builder(this)
                    .setContentTitle("USSD запрос")
                    .setContentText("Отправка USSD запроса...")
                    .setSmallIcon(android.R.drawable.stat_notify_sync)
                    .setOngoing(true);
        }
        Notification notification = notificationBuilder.build();

        // Для API ≥ Q (29) можно явно передать тип NONE, чтобы не требовались дополнительные разрешения
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE);
        } else {
            startForeground(NOTIF_ID, notification);
        }
        Log.d(TAG, "Foreground-сервис запущен");

        // Отправляем USSD-запрос.
        doUssd("*131*2#");
    }

    /**
     * Создает канал уведомлений для устройств с API >= 26.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "USSD Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Канал для USSD запросов");
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created");
            } else {
                Log.e(TAG, "NotificationManager is null");
            }
        }
    }

    /**
     * Отправляет USSD- запрос с помощью TelephonyManager.sendUssdRequest().
     * Если вызов завершается ошибкой, выполняется fallbackDialUSSD().
     */
    private void doUssd(final String code) {
        Log.d(TAG, "doUssd() called with code: " + code);
        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (tm == null) {
            Log.e(TAG, "TelephonyManager is null");
            return;
        }
        tm.sendUssdRequest(code, new TelephonyManager.UssdResponseCallback() {
            @Override
            public void onReceiveUssdResponse(TelephonyManager t, String request, CharSequence response) {
                Log.d(TAG, "USSD response received: " + response);
                handleResult(response != null ? response.toString() : "Пустой ответ");
            }

            @Override
            public void onReceiveUssdResponseFailed(TelephonyManager t, String request, int error) {
                Log.e(TAG, "USSD request failed with error: " + error);
                // Если запрос не удался (например, код ошибки -1), выполняется fallback.
                fallbackCallUSSD(code);
            }
        }, new Handler(Looper.getMainLooper()));
        Log.d(TAG, "USSD request sent");
    }

    /**
     * Фолбэковый метод, который формирует Intent для набора USSD-кода через диалер.
     * Здесь ключевой момент — кодируются только символы "#" (звездочки остаются без изменений).
     */
    private void fallbackCallUSSD(String code) {
        Log.d(TAG, "Fallback: attempting ACTION_CALL with USSD code: " + code);
        // Кодируем только символ '#' — заменяем его на закодированное значение,
        // звездочки остаются без изменений.
        String encodedUssd = code.replace("#", Uri.encode("#"));
        String uriString = "*131*2#";//"tel:" + encodedUssd; // например, "tel:*131*2%23"
        Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse(uriString));
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(callIntent);
            Log.d(TAG, "ACTION_CALL intent started with USSD code: " + uriString);
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException in fallbackCallUSSD: " + e.getMessage(), e);
        }
    }


    /**
     * Обрабатывает полученный результат USSD-запроса.
     * Здесь можно выполнять сохранение ответа, отправку broadcast и т.д.
     */
    private void handleResult(String text) {
        Log.d(TAG, "handleResult() called with text: " + text);
        // Формируем новое уведомление с результатом.
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Результат USSD")
                    .setContentText(text)
                    .setSmallIcon(android.R.drawable.stat_notify_sync)
                    .setAutoCancel(true);
        } else {
            builder = new Notification.Builder(this)
                    .setContentTitle("Результат USSD")
                    .setContentText(text)
                    .setSmallIcon(android.R.drawable.stat_notify_sync)
                    .setAutoCancel(true);
        }
        Notification resultNotification = builder.build();
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIF_ID + 1, resultNotification);
            Log.d(TAG, "Result notification posted");
        }
        // Останавливаем сервис после обработки результата.
        stopSelf();
        Log.d(TAG, "Service stopped after handling result");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind() called");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() called");
        return START_NOT_STICKY;
    }
}
