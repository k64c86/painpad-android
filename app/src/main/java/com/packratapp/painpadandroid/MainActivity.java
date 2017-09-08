package com.packratapp.painpadandroid;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements
        ButtonFragment.OnFragmentInteractionListener, SliderFragment.OnFragmentInteractionListener {

    final String TAG = "MainActivity";

    final static String interfaceIDKey = "interface_list";
    final static String patientIDKey = "patient_id";
    final static String screenOnForKey = "screen_on_time";
    final static String updateIntervalKey = "update_interval";

    final public String painScoreFile = "pain_scores.txt";
    private static AlarmManager alarmManager;

    // Preferences
    public String interfaceID = "";
    public String patientID = "";
    public long screenOnFor = 0;
    public long alarmInterval = 0; // in milliseconds

    public long playSoundFor = 0; // milliseconds
    public long playSoundInterval = 1000; // milliseconds

    public int endTimeHour = 22;
    public int endTimeMinute = 0;

    public int startTimeHour = 8;
    public int startTimeMinute = 0;

    public PendingIntent pendingIntent;

    public int hashPressCount;

    public int currentBackground = Color.WHITE;
    public int firstBackgroundColor = Color.RED;
    public int secondBackgroundColor = Color.GREEN;
    public long backgroundAlternateInterval = 750;

    public CountDownTimer mSoundTimer;
    public CountDownTimer mScreenOnTimer;

    /**
     * A BroadcastReceiver that onReceive sets a new alarm, and during the correct time plays the
     * prompt sound, switches the screen on and flashes it.
     */
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            setAlarm();

            if (allowAlarm()) {
                playPromptSound(context);
                screenOn();
//                flashBackground = true;
            }

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: loading...");

        // Make activity full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        alarmManager = (AlarmManager) this.getSystemService(ALARM_SERVICE);


        // Load preferences
        setPreferences();

        // Hide the action bar (the bar at the top)
        try {
            getSupportActionBar().hide();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Make settings button transparent (to hide it)
        Button settingsButton = (Button) findViewById(R.id.settingsButton);
        settingsButton.setBackgroundColor(Color.TRANSPARENT);

        // Setup pain score input interface by injecting the correct fragment
        injectInterfaceFragment(interfaceID);

        // Register a Broadcast receiver, calls onReceive if Intent for SOUND_ALARM
        // is broadcast.
        registerReceiver(broadcastReceiver, new IntentFilter("SOUND_ALARM"));

        // Initialize Alarm Manager and setup alarm
        setAlarm();
    }


    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume: loading...");
        // On resume load the preferences and inject the correct interface.
        setPreferences();
        injectInterfaceFragment(interfaceID);
    }


    @Override
    public void onFragmentInteraction(Uri uri) {
    }


    /**
     * Get experiment preferences from the SharedPreferences and set corresponding variables.
     * Finally set the patient Id on the Text View.
     */
    public void setPreferences() {
        long multiplier =  60 * 1000;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());

        // If the setting preferences are loaded and are different from those in memory, then reset
        // the alarm

        // Get interfaceID
        String newInterfaceID = preferences.getString(interfaceIDKey, "button");
        if(!newInterfaceID.equals(interfaceID)) {
            interfaceID = newInterfaceID;
            setAlarm();
        }
        interfaceID = newInterfaceID;

        // Get the patient id
        String newPatientID = preferences.getString(patientIDKey, "000000");
        if(!patientID.equals(newPatientID)) {
            patientID = newPatientID;
            setAlarm();
        }
        patientID = newPatientID;

        // Get the alarm interval.
        long settingsAlarmInterval = Long.parseLong(preferences.getString(updateIntervalKey,
                "60")) * multiplier;
        if(settingsAlarmInterval != alarmInterval) {
            alarmInterval = settingsAlarmInterval;
            setAlarm();
        }
        alarmInterval = settingsAlarmInterval;

        screenOnFor = Long.parseLong(preferences.getString(screenOnForKey, "2")) * multiplier;
        hashPressCount = 0;

        String patientIDText = "Patient ID: " + patientID;
        ((TextView) findViewById(R.id.tv_patient_id)).setText(patientIDText);
    }

    /**
     * Creates an alarm for currentTime + alarmInterval
     */
    public void setAlarm() {
        // Next alarm should be currentTime + alarmInterval
        long nextAlarm = System.currentTimeMillis() + alarmInterval;

        // Set Calendar for currentTime + alarmInterval
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(nextAlarm);
        Log.d(TAG, "setAlarm: next alarm at: " + calendar.getTime().toString());

        // Create intent for PromptReceiver
        Intent intent = new Intent("SOUND_ALARM");
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        // Set alarm. This broadcasts the SOUND_ALARM Intent when executed.
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    public void submitPainScore(View view) {

        // If mSoundTimer is not null then cancel timer
        // execute onFinish to clean up wake lock and then make
        // timer null
        if (mSoundTimer != null) {
            mSoundTimer.cancel();
            mSoundTimer.onFinish();
            mSoundTimer = null;
        }

        // If mScreenOnTimer is not null then cancel timer
        // execute onFinish to clean up wake lock and then make
        // timer null
        if (mScreenOnTimer != null) {
            mScreenOnTimer.cancel();
            mScreenOnTimer.onFinish();
            mScreenOnTimer = null;
        }

        // User has entered value, stop flashing the background and reset to white
        resetBackground();

        // If # increment hash count otherwise save pain score and set new alarm
        if (((TextView) view).getText().equals("#")) {

            openSettings(view);

        } else {
            // Save pain score and set new alarm for currentTime + alarmInterval
            savePainScore(view);
            setAlarm();
        }
    }

    public void savePainScore(View view) {
        // Play button tone
        playButtonTone();

        // If button interface grab text from Button view, else need to find SeekBar by ID and
        // get value from it.
        String painScore;
        if (interfaceID.equals("button")) {
            Log.d(TAG, "savePainScore: button interface.");
            painScore = ((Button) (view)).getText().toString();

        } else {
            Log.d(TAG, "savePainScore: slider interface.");
            SeekBar seekBar = (SeekBar) findViewById(R.id.sb_pain);
            painScore = Integer.toString(seekBar.getProgress());

            // Reset SeekBar value to 0
            seekBar.setProgress(0);
        }

        Log.d(TAG, "savePainScore: pain score of " + painScore + " entered.");
        Toast.makeText(this, "Pain Score of " + painScore + " entered.",
                Toast.LENGTH_LONG).show();

        // Write pain score to file
        writePainScoreToFile(patientID, interfaceID, painScore);
    }

    /**
     * Get the layout view and alternate the background colour between firstBackgroundColor and
     * secondBackgroundColor.
     */
    public void flipBackgroundColor() {
        View mainView = findViewById(R.id.layout_view);

        if (currentBackground == firstBackgroundColor) {
            mainView.setBackgroundColor(secondBackgroundColor);
            currentBackground = secondBackgroundColor;
        } else {
            mainView.setBackgroundColor(firstBackgroundColor);
            currentBackground = firstBackgroundColor;
        }
    }

    /**
     * Get the layout view and reset the background to Color.WHITE
     */
    public void resetBackground() {
        View mainView = findViewById(R.id.layout_view);
        mainView.setBackgroundColor(Color.WHITE);
        currentBackground = Color.WHITE;
    }

    /**
     * Play an mp3 on a Train Bell
     *
     * @param context the context of the calling broadcast receiver
     */
    public void playPromptSound(Context context) {
        final MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.trainalarm);
        mediaPlayer.start();

        mSoundTimer = new CountDownTimer(playSoundFor, playSoundInterval) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                Log.d(TAG, "onFinish: stopping playback.");
                mediaPlayer.release();
                mSoundTimer = null;
            }
        };
    }

    /**
     * Play a tone
     */
    public void playButtonTone() {
        ToneGenerator buttonPressTone = new ToneGenerator(AudioManager.STREAM_MUSIC,
                ToneGenerator.MAX_VOLUME);
        buttonPressTone.startTone(ToneGenerator.TONE_CDMA_PIP, ToneGenerator.MAX_VOLUME);
    }


    /**
     * Use the PowerManager to acquire a wakelock and switch the screen on.
     * Keep the wakelock for screenOnFor milliseconds, in realty the screen will stay on for
     * the global display on time setting (in the Display Settings) + screenOnFor
     * This also alternates the color of the layout views background.
     */
    public void screenOn() {
        Log.d(TAG, "screenOn: Turning screen on.");

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE, TAG);
        wakeLock.acquire();

        // Set CountDownTime to release wake lock in screeOnFor milliseconds and alternate the
        // background color every backgroundAlternateInterval milliseconds
        mScreenOnTimer = new CountDownTimer(screenOnFor,
                backgroundAlternateInterval) {

            @Override
            public void onTick(long millisUntilFinished) {
                Log.d(TAG, "onTick: alternate background.");
                flipBackgroundColor();
            }

            @Override
            public void onFinish() {
                Log.d(TAG, "onFinish: Wake lock released.");
                // Release wakelock, stop flashing and reset background color.
                wakeLock.release();
                resetBackground();
                mScreenOnTimer = null;
            }
        };

        Log.d(TAG, "screenOn: starting screenOn count down timer.");
        mScreenOnTimer.start();
    }

    /**
     * @param patientID   the id of the patient
     * @param interfaceID the interface currently being used
     * @param painScore   the score that the patient entered
     */
    public void writePainScoreToFile(String patientID, String interfaceID, String painScore) {

        String filePath = "";
        try {
            filePath = this.getExternalFilesDir(null).getAbsolutePath();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "writePainScoreToFile: " + filePath);
        PainScoreFile file = new PainScoreFile(filePath, painScoreFile);
        file.writeScore(patientID, interfaceID, painScore, getCurrentDatetime());
    }


    /**
     * Start the SettingsActivity
     * @param view the view that was clicked.
     */
    public void openSettings(View view) {
        // Play button tone
        playButtonTone();

        Log.d(TAG, "openSettings: button pressed hash count: " + hashPressCount);

        if (hashPressCount == 2) {
            hashPressCount = 0;
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else {
            hashPressCount += 1;
        }
    }

    /**
     * Method that creates an instance of a fragment based on its id.
     * @param fragmentID the fragments id (e.g. button or slider)
     */
    public void injectInterfaceFragment(String fragmentID) {

        if (fragmentID.equals("button")) {
            ButtonFragment fragment = ButtonFragment.newInstance();
            updateContent(fragment);

        } else {
            SliderFragment fragment = SliderFragment.newInstance();
            updateContent(fragment);

        }
    }

    /**
     * This method replaces the fragment in R.id.content with fragment
     * @param fragment the fragment that is being added.
     */
    public void updateContent(Fragment fragment) {
        if (getSupportFragmentManager().findFragmentById(R.id.content) != null) {
            getSupportFragmentManager()
                    .beginTransaction().
                    remove(getSupportFragmentManager().findFragmentById(R.id.content)).commit();
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.content, fragment).commit();
    }

    /**
     * Gets the current datetime in the format yyyy-MM-ddTHH:mm:ss
     * @return a String of the datetime
     */
    public String getCurrentDatetime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.UK);
        return df.format(new Date());
    }

    /**
     * Checks if the current time is between startDatetime and endDatetime
     * @return boolean to decide if alarm is allowed
     */
    public boolean allowAlarm() {

        // Get the current time
        Calendar currentDatetime = Calendar.getInstance();

        // Get the current day and set time to the hour and minute
        // when prompts may start.
        Calendar startDatetime = Calendar.getInstance();
        startDatetime.set(Calendar.HOUR_OF_DAY, startTimeHour);
        startDatetime.set(Calendar.MINUTE, startTimeMinute);


        // Get the current day and set time to the hour and minute
        // when the prompts must end.
        Calendar endDatetime = Calendar.getInstance();
        endDatetime.set(Calendar.HOUR_OF_DAY, endTimeHour);
        endDatetime.set(Calendar.MINUTE, endTimeMinute);

        // Check if current time is after end or before start, if so allow then don't allow alarm
        return !(currentDatetime.after(endDatetime) || currentDatetime.before(startDatetime));
    }

}
