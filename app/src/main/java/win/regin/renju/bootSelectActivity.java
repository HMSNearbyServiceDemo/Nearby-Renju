package win.regin.renju;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

public class bootSelectActivity extends Activity {


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.boot_select);



        SharedPreferences preferences = getSharedPreferences("usrInfo", MODE_PRIVATE);
        int count = preferences.getInt("count", 0);

        //判断程序与第几次运行，如果是第一次运行则跳转到引导页面
        if (count == 0) {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), firstBootActivity.class);
            startActivity(intent);
            this.finish();
        }

        else
        {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), gameHallActivity.class);
            startActivity(intent);
            this.finish();
        }
    }
}
