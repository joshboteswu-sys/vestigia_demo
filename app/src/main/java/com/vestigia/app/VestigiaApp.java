package com.vestigia.app;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;

public class VestigiaApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Every screen gets the same system-bar and keyboard handling automatically
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityStarted(@NonNull Activity activity) {
                SystemBarInsets.apply(activity);
            }
            @Override public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) { }
            @Override public void onActivityResumed(@NonNull Activity activity) { }
            @Override public void onActivityPaused(@NonNull Activity activity) { }
            @Override public void onActivityStopped(@NonNull Activity activity) { }
            @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) { }
            @Override public void onActivityDestroyed(@NonNull Activity activity) { }
        });
    }
}