package study.hank.com.activityhookdemo.methodA;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import study.hank.com.activityhookdemo.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityByActivity();
            }
        });
        Log.d("LifeCircle", "onCreate()");
    }

    private void startActivityByActivity() {
        Intent i = new Intent(MainActivity.this, Main2Activity.class);
        startActivity(i);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d("LifeCircle", "onRestart()");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("LifeCircle", "onStart()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("LifeCircle", "onResume()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("LifeCircle", "onStop()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("LifeCircle", "onPause()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("LifeCircle", "onDestroy()");
    }
}
