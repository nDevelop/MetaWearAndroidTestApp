package com.example.mbient.freefalldetector;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.AccelerometerBmi160;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.GyroBmi160.*;

import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import bolts.TaskCompletionSource;


public class MainActivity extends AppCompatActivity implements ServiceConnection {

    //MetaWear Objects
    public static final String EXTRA_BT_DEVICE = "com.example.mbient.freefalldetector.EXTRA_BT_DEVICE";
    private BluetoothDevice btDevice;
    public static final int REQUEST_START_APP= 1;
    private BtleService.LocalBinder serviceBinder;
    private MetaWearBoard board;
    private AccelerometerBmi160 accelModule;
    private GyroBmi160 gyroModule;

    private Led ledModule;

    //Constants
    private Button start;
    private Button stop;
    private Button disconnect;
    private TextView accelText;
    private TextView gyroText;

    private MadgwickAHRS AHRS;

    private static final String TAG = "MetaWear";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind the service when the activity is created
        getApplicationContext().bindService(new Intent(this, BtleService.class),
                this, Context.BIND_AUTO_CREATE);

        AHRS = new MadgwickAHRS(1f / 50f, 0.5f);

        btDevice= getIntent().getParcelableExtra(EXTRA_BT_DEVICE); getApplicationContext().bindService(new Intent(this, BtleService.class), this, BIND_AUTO_CREATE);

        accelText = (TextView) findViewById(R.id.textAccel);
        gyroText  = (TextView) findViewById(R.id.textGyro);

        start = (Button) findViewById(R.id.start);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                accelModule.acceleration().start();
                accelModule.start();

                gyroModule.angularVelocity().start();
                gyroModule.start();
            }
        });

        stop = (Button) findViewById(R.id.stop);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                accelModule.stop();
                accelModule.acceleration().stop();

                gyroModule.stop();
                gyroModule.angularVelocity().stop();
            }
        });

        disconnect = (Button) findViewById(R.id.disconnect);
        disconnect.setOnClickListener(v -> {
            Task.callInBackground(new Callable<Void>() {
                public Void call() {
                    ledModule.stop(true);

                    gyroModule.stop();
                    gyroModule.angularVelocity().stop();

                    accelModule.stop();
                    accelModule.acceleration().stop();

                    board.tearDown();
                    return null;
                }
            }).continueWith(task -> {
                board.disconnectAsync();
                Log.i(TAG, "Board Disconnected");
                return null;
            });

            finish();
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        // Typecast the binder to the service's LocalBinder class
        serviceBinder = (BtleService.LocalBinder) service;

        Log.i(TAG, "Service Connected");
        board = ((BtleService.LocalBinder) service).getMetaWearBoard(btDevice);

        //Connects necessary modules for the app functioning
        try {
            ledModule = board.getModuleOrThrow(Led.class);
            accelModule = board.getModule(AccelerometerBmi160.class);
            gyroModule = board.getModule(GyroBmi160.class);
        } catch (UnsupportedModuleException e) {
            e.printStackTrace();
        }
        ledModule.editPattern(Led.Color.GREEN, Led.PatternPreset.BLINK).commit();
        ledModule.play();

        //Configure sensors
        accelModule.configure()
                .odr(25f)       // Set sampling frequency to 25Hz, or closest valid ODR
                .commit();

        gyroModule.configure()
                .odr(GyroBmi160.OutputDataRate.ODR_25_HZ)
                .range(Range.FSR_2000)
                .commit();

        accelModule.acceleration().addRouteAsync(source -> source.stream((data, env) -> {
            Acceleration value = data.value(Acceleration.class);
            sensorMsg(value.toString(), "accel");
        }));

        gyroModule.angularVelocity().addRouteAsync(source -> source.stream((data, env) -> {
            AngularVelocity value = data.value(AngularVelocity.class);
            sensorMsg(value.toString(), "gyro");
        }));

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {    }

    public void sensorMsg(String output, final String sensor) {
        final String reading = output;
        runOnUiThread( () -> {
            if (sensor == "accel") {
                accelText.setText("Accel: " + reading);
            } else {
                gyroText.setText("Gyro: " + reading);
            }
        });
    }
}
