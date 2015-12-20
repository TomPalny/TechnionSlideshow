package com.example.tpalny.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Created by Tom Palny on 16/12/2015.
 */
public class StartMyAppAtBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences settings = context.getSharedPreferences("com.example.tpalny.myapplication_preferences", Context.MODE_PRIVATE);
        String boot = settings.getString("start_on_boot", "false");
        Boolean start_on_boot = boot.equals("true");
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) && start_on_boot) {
            Intent i = new Intent(context, Select_Folders.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }
}
