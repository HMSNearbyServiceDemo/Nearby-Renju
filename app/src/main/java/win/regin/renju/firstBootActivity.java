package win.regin.renju;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class firstBootActivity extends Activity {
    private EditText editText;
    private Button button;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.first_boot);


        editText = (EditText) findViewById(R.id.nickname1);
        button = (Button) findViewById(R.id.button);

        button.setEnabled(false);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.d("wmq", "beforeTextChanged: " + s);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d("wmq", "onTextChanged: " + s);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                Log.d("wmq", "afterTextChanged: " + editable.toString());
                SharedPreferences preferences = getSharedPreferences("usrInfo",MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();

                editor.putString("Nickname", editable.toString());
                editor.putInt("count", 1);
                editor.apply();
                button.setEnabled(true);
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), gameHallActivity.class);
                startActivity(intent);
            }
        });
    }
}
