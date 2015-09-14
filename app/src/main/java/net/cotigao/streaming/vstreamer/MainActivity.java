package net.cotigao.streaming.vstreamer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.util.Log;
import android.content.Context;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View.OnTouchListener;
import android.view.MotionEvent;
import android.graphics.PorterDuff;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.ArrayList;
import java.net.UnknownHostException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;



/**
 * Created by vikram on 27/8/15.
 */
public class MainActivity extends Activity {
    private final static String TAG = "MainActivity";
    private static final String USER_AGENT = "redskull";

    private Button mButton;
    private ListView mListView;
    private ArrayAdapter<String> adapter;
    private EditText mEditText;
    private ArrayList<String> strArr;
    private ArrayList<String> udnArr;
    String  ip = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListView = (ListView) findViewById(R.id.listView1);
        mEditText = (EditText) findViewById(R.id.editText1);
        mButton = (Button) findViewById(R.id.button1);

        strArr = new ArrayList<String>();
        udnArr = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(getApplicationContext(),
                android.R.layout.simple_list_item_1, strArr);
        mListView.setAdapter(adapter);

        /*ArrayAdapter<String> adapter=new ArrayAdapter<String>(
                this,android.R.layout.simple_list_item_1, strArr){

            @Override
            public View getView(int position, View convertView,
                                ViewGroup parent) {
                View view =super.getView(position, convertView, parent);

                TextView textView=(TextView) view.findViewById(android.R.id.text1);

                textView.setTextColor(Color.BLUE);

                return view;
            }
        };
        mListView.setAdapter(adapter);*/

        ip = getIntent().getStringExtra("IP");
        if (ip != null)
            mEditText.setText(ip);

        mButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Scan button clicked");
                mButton.setText("Scanning..");
                mEditText.setEnabled(false);
                mButton.setEnabled(false);
                new GetDMR().execute();
            }
        });

        //strArr.add("Bbabush"); udnArr.add("Samuel");
        /*mButton.setOnTouchListener(new OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN: {
                            v.getBackground().setColorFilter(0xe0f47521,PorterDuff.Mode.SRC_ATOP);
                            v.invalidate();
                            break;
                        }
                        case MotionEvent.ACTION_UP: {
                            v.getBackground().clearColorFilter();
                            v.invalidate();
                            break;
                        }
                    }
                    return false;
                }
        });*/

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String entry = (String) parent.getAdapter().getItem(position);
                Log.i(TAG, "This is my position buddy: " + position);
                String udn = udnArr.get(position);
                Intent intent = new Intent(MainActivity.this, MainActivity2.class);
                intent.putExtra("IP", ip);
                intent.putExtra("DMR", udn);
                intent.putExtra("NICK", entry);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private class GetDMR extends AsyncTask<String,String,String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ip = mEditText.getText().toString();
            strArr.clear();
            udnArr.clear();
        }

        private String getDMRList() {
            try {
                String url = "http://" + ip + ":7070/cotigao";
                HttpClient client = new DefaultHttpClient();
                HttpGet get = new HttpGet(url);
                get.addHeader("User-Agent", USER_AGENT);
                Log.i(TAG, "I am connecting to get dmrlist");
                HttpResponse httpResponse = client.execute(get);
                Log.i(TAG, "I am connecting done to get dmrlist");
                if (httpResponse.getEntity().getContentLength() > 0) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                            httpResponse.getEntity().getContent()));
                    String inputLine;
                    int i = 0;

                    Log.i(TAG, "getting the dmrs...");
                    while ((inputLine = reader.readLine()) != null) {
                        if (i % 2 == 0) {
                            udnArr.add(inputLine);
                        } else {
                            strArr.add(inputLine);
                        }
                        i++;
                    }
                    reader.close();
                    Log.i(TAG, "got the dmrs");
                }

            } catch (Exception e) {
                Log.i(TAG, "I am connecting failed to get dmrlist");
                return "fail";
            }

            return "done";
        }

        @Override
        protected String doInBackground(String... v) {
            return getDMRList();
        }

        @Override
        protected void onProgressUpdate(String...v) {

        }

        @Override
        protected void onPostExecute(String result) {
            if (result != "done") {
                Toast.makeText(getApplicationContext(), "Failed to connect to " + ip, Toast.LENGTH_SHORT).show();
            }
            mEditText.setEnabled(true);
            mButton.setEnabled(true);
            mButton.setText("Scan");
            adapter.notifyDataSetChanged();
        }
    }
}

