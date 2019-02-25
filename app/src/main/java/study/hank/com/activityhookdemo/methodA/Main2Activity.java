package study.hank.com.activityhookdemo.methodA;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import study.hank.com.activityhookdemo.R;

public class Main2Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityByActivity();
            }
        });
    }

    private void startActivityByActivity() {
        Intent i = new Intent(Main2Activity.this, Main3Activity.class);
        startActivity(i);
    }


}
