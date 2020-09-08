package win.regin.renju;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.hms.nearby.Nearby;
import com.huawei.hms.nearby.message.Policy;
import com.huawei.hms.nearby.message.GetOption;
import com.huawei.hms.nearby.message.Message;
import com.huawei.hms.nearby.message.MessageEngine;
import com.huawei.hms.nearby.message.MessageHandler;
import com.huawei.hms.nearby.message.MessagePicker;
import com.huawei.hms.nearby.message.PutOption;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import static android.content.ContentValues.TAG;


public class gameHallActivity extends Activity implements OnClickListener{
    private Button button;
    private TextView textView;
    private TextView nickName;
    private MessageEngine engine;
    private int rank;
    private String name;
    private Message nameMessage;
    private PutOption putOption;
    private ListView lv;
    private ArrayList<String> list = new ArrayList<String>();
    private ArrayAdapter<String> adapter;
    private static final String[] REQUIRED_PERMISSIONS =
            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,};
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("renju", "gamehall onCreate");
        setContentView(R.layout.game_hall);
        requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);

        button = (Button) findViewById(R.id.button1);
        textView = (TextView) findViewById(R.id.textView1);
        nickName = (TextView) findViewById(R.id.nickname);
//        TextView textView = (TextView)findViewById(R.id.textView2);
//        textView .setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        if (isNetWorkAvailable(this)) {
            list.clear();
            adapter.notifyDataSetChanged();
            Intent intent = new Intent(this, MainActivity.class);
            /**
             * 第一个参数是Intent对象
             * 第二个参数是请求的一个标识
             */
            startActivityForResult(intent, 1);
        }
        else
        {
            Toast.makeText(this,"Not connected to Internet. Please check.", Toast.LENGTH_LONG).show();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        if(requestCode == 1 && resultCode == 2){
            String content = data.getStringExtra("data");
            textView.setText(content);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("renju", "gamehall onResume");
    }



    @Override
    protected void onStart() {
        super.onStart();
        initMessage();

        Policy policy = new Policy.Builder().setTtlSeconds(3600).build();
        putOption = new PutOption.Builder().setPolicy(policy).build();

        if (isNetWorkAvailable(this))
        {
            engine = Nearby.getMessageEngine(this);
            engine.put(nameMessage, putOption);
            startScan();
        }

        initPlayerList();

    }

    private void startScan() {
        MessagePicker picker = new MessagePicker.Builder().includeNamespaceType("renju", "chart").build();
        Policy policy = new Policy.Builder().setTtlSeconds(3600).build();
        GetOption getOption = new GetOption.Builder().setPicker(picker).setPolicy(policy).build();
        MessageHandler messageHandler = new MessageHandler() {
            @Override
            public void onFound(Message message) {
                super.onFound(message);
                doOnFound(message);
            }

            @Override
            public void onLost(Message message) {
                super.onLost(message);
                doOnLost(message);
            }
        };
        engine.get(messageHandler, getOption);
    }

    private void doOnFound(Message message) {
        String content = new String(message.getContent());
        Log.d("renju", "Message onFound: " + content);

        boolean isFound = false;

        for (String data : list)
        {
            if (content.equals(data))
            {
                isFound = true;
            }
        }

        if (!isFound)
        {
            list.add(content);
            adapter.notifyDataSetChanged();
        }
    }

    private void doOnLost(Message message) {
        String content = new String(message.getContent());
        Log.d("renju", "Message onLost: " +  content);
        for (String data : list)
        {
            if (content.equals(data))
            {
                list.remove(data);
                adapter.notifyDataSetChanged();
                break;
            }
        }
    }

    public static boolean isNetWorkAvailable(Context context){
        boolean isAvailable = false ;
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if(networkInfo!=null && networkInfo.isAvailable()){
            isAvailable = true;
        }
        return isAvailable;
    }

    private void initMessage() {
        Log.d("renju", "gamehall onStart");
        button.setOnClickListener(this);

        SharedPreferences preferences = getSharedPreferences("usrInfo",MODE_PRIVATE);
        name = preferences.getString("Nickname", "unknownPlayer");
        nickName.setText(name);

        rank = preferences.getInt("rank",0);
        {
            if (rank == 0)
            {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt("rank", 1);
                editor.apply();
                rank = 1;
            }
        }
        TextView rankText = (TextView) findViewById(R.id.rankText);
        rankText.setText("rank"+rank);

        String msgContent = name + " " + "rank" + rank;
        try {
            nameMessage = new Message(msgContent.getBytes("UTF-8"), "chart", "renju");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "nickNameStr getBytes error", e);
        }
    }

    private void initPlayerList() {
        lv = (ListView)findViewById(R.id.listView1);
        adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_expandable_list_item_1,
                list);
        lv.setAdapter(adapter);
    }
}

