package com.polysfactory.gdksandbox;

import android.app.Activity;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;

import com.polysfactory.gdksandbox.headgesture.HeadGestureDetector;

public class MainActivity extends Activity {

    private SensorManager mSensorManager;
    private HeadGestureDetector mHeadGestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHeadGestureDetector = new HeadGestureDetector(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHeadGestureDetector.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHeadGestureDetector.stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

}
