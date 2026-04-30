package com.faris.strideai.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.faris.strideai.models.ActivitySession;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SessionManager {
    private static final String PREF_NAME = "StrideAI_Sessions";
    private static final String KEY_SESSIONS = "saved_sessions";

    public static void saveSession(Context context, ActivitySession session) {
        List<ActivitySession> sessions = getSessions(context);
        sessions.add(0, session); // Add to beginning (most recent first)
        
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(sessions);
        editor.putString(KEY_SESSIONS, json);
        editor.apply();
    }

    public static List<ActivitySession> getSessions(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString(KEY_SESSIONS, null);
        Type type = new TypeToken<ArrayList<ActivitySession>>() {}.getType();
        List<ActivitySession> sessions = gson.fromJson(json, type);
        
        if (sessions == null) {
            sessions = new ArrayList<>();
        }
        return sessions;
    }
}
