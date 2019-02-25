package study.hank.com.activityhookdemo.methodA;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import study.hank.com.activityhookdemo.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityHookHelper.hook(this);
        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityByActivity();
            }
        });

    }

    private void startActivityByActivity() {
        Intent i = new Intent(MainActivity.this, Main2Activity.class);
        startActivity(i);
    }


}
