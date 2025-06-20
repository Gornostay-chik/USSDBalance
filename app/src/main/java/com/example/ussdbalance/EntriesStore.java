package com.example.ussdbalance;

import android.content.Context;
import android.content.SharedPreferences;

public class EntriesStore {
    private static final String PREFS_NAME = "USSDStorePrefs";
    private static final String KEY_LAST_ANSWER = "lastAnswer";
    private SharedPreferences sharedPreferences;

    public EntriesStore(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Сохраняем ответ USSD
    public void add(String answer) {
        sharedPreferences.edit().putString(KEY_LAST_ANSWER, answer).apply();
    }

    // Получаем последний сохранённый USSD-ответ
    public String getLast() {
        return sharedPreferences.getString(KEY_LAST_ANSWER, "");
    }
}
