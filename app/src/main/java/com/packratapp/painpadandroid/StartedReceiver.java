package com.packratapp.painpadandroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartedReceiver extends BroadcastReceiver {

    // BroadcastReceiver for launching app when system boots.
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, MainActivity.class);  //MyActivity can be anything which you want to start on bootup...
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }
}
