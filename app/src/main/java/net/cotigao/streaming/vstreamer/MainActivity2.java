package net.cotigao.streaming.vstreamer;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.audio.AudioQuality;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.video.VideoQuality;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


/**
 * A straightforward example of how to stream AMR and H.263 to some public IP using libstreaming.
 * Note that this example may not be using the latest version of libstreaming !
 */
public class MainActivity2 extends Activity implements OnClickListener, Session.Callback, SurfaceHolder.Callback {

	private static final String TAG = "MainActivity2";
    private static final String USER_AGENT = "redskull";

	private Button mButton1, mButton2;
	private SurfaceView mSurfaceView;
	private TextView mEditText;
	private Session mSession;
	private String dest, dmr;
    private HttpURLConnection conn;
    private boolean fullstop = false, error = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main2);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		mButton1 = (Button) findViewById(R.id.button1);
		mButton2 = (Button) findViewById(R.id.button2);
		mSurfaceView = (SurfaceView) findViewById(R.id.surface);
		mEditText = (TextView) findViewById(R.id.editText1);

		mSession = SessionBuilder.getInstance()
		.setCallback(this)
		.setSurfaceView(mSurfaceView)
		.setPreviewOrientation(90)
		.setContext(getApplicationContext())
		.setAudioEncoder(SessionBuilder.AUDIO_NONE)
		.setAudioQuality(new AudioQuality(16000, 32000))
		.setVideoEncoder(SessionBuilder.VIDEO_H264)
		.setVideoQuality(new VideoQuality(320,240,20,500000))
		.build();

		mButton1.setOnClickListener(this);
		mButton2.setOnClickListener(this);

		mSurfaceView.getHolder().addCallback(this);

		dest = getIntent().getStringExtra("IP");
		dmr = getIntent().getStringExtra("DMR");
        mEditText.setText(dest);
        getActionBar().setTitle(dest+ " <" + getIntent().getStringExtra("NICK") +">");
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mSession.isStreaming()) {
			mButton1.setText(R.string.stop);
		} else {
			mButton1.setText(R.string.start);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mSession.release();
	}

	@Override
	public void onBackPressed ()  {
        if (mSession.isStreaming()) {
            fullstop = true;
            new Action().execute("stop");
        } else {
            Intent home = new Intent(MainActivity2.this, MainActivity.class);
            home.putExtra ("IP", dest);
            startActivity(home);
            finish();
        }
	}

	@Override
	public void onClick(View v)  {
		if (v.getId() == R.id.button1) {
            mButton1.setEnabled(false);
			// Starts/stops streaming
			if (!mSession.isStreaming()) {
                new Action().execute("play");
			} else {
                new Action().execute("stop");
			}
		} else {
			// Switch between the two cameras
			mSession.switchCamera();
		}
	}

	@Override
	public void onBitrateUpdate(long bitrate) {
		Log.d(TAG, "Bitrate: " + bitrate);
	}

	@Override
	public void onSessionError(int message, int streamType, Exception e) {
		if (e != null) {
			logError(e.getMessage());
		}
        error = true;
        new Action().execute("stop");
	}

	@Override
	public void onPreviewStarted() {
		Log.d(TAG, "Preview started.");
	}

	@Override
	public void onSessionConfigured() {
		Log.d(TAG,"Preview configured.");
		// Once the stream is configured, you can get a SDP formated session description
		// that you can send to the receiver of the stream.
		// For example, to receive the stream in VLC, store the session description in a .sdp file
		// and open it with VLC while streming.
		Log.d(TAG, mSession.getSessionDescription());
		mSession.start();
	}

	@Override
	public void onSessionStarted() {
		Log.d(TAG,"Session started.");
		mButton1.setEnabled(true);
		mButton1.setText(R.string.stop);
	}

	@Override
	public void onSessionStopped() {
		Log.d(TAG, "Session stopped.");
		mButton1.setEnabled(true);
		mButton1.setText(R.string.start);
        if (fullstop) {
            Intent home = new Intent(MainActivity2.this, MainActivity.class);
            home.putExtra ("IP", dest);
            startActivity(home);
            finish();
        }
	}	
	
	/** Displays a popup to report the eror to the user */
	private void logError(final String msg) {
		final String error = (msg == null) ? "Error unknown" : msg; 
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity2.this);
		builder.setMessage(error).setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {}
		});
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mSession.startPreview();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mSession.stop();
	}

    private class Action extends AsyncTask<String,String,String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        private synchronized String action (String act) {
            try {
                String url = "http://" + dest + ":7070/camera.mp4?dmr="+dmr+"&action="+act;

                HttpClient client = new DefaultHttpClient();
                HttpPost post = new HttpPost(url);
                client.execute(post);
            }  catch (Exception e) {
                //Toast.makeText(getApplicationContext(), "Failed to connect to " + dest, Toast.LENGTH_SHORT).show();
                return act+"fail";
            }

            return act+"done";
        }

        @Override
        protected String doInBackground(String... v) {
            return action(v[0]);
        }

        @Override
        protected void onProgressUpdate(String...v) {

        }

        @Override
        protected void onPostExecute(String result) {
            Log.i(TAG, "PostExecuteee: " + result);
            if (result.trim().equals("playfail")) {
                mButton1.setEnabled(true);
                mButton1.setText(R.string.start);
                Toast.makeText(getApplicationContext(), "Failed to connect to " + dest, Toast.LENGTH_SHORT).show();
            } else if (result.trim().equals("stopfail") || result.trim().equals("stopdone")) {
                if ((result.trim().equals("stopfail")) && !fullstop && !error )
                    Toast.makeText(getApplicationContext(), "Failed to connect to " + dest, Toast.LENGTH_SHORT).show();

                if (!error)
                    mSession.stop();
                else {
                    Toast.makeText(getApplicationContext(), "Error in Session", Toast.LENGTH_SHORT).show();
                    Intent home = new Intent(MainActivity2.this, MainActivity.class);
                    home.putExtra ("IP", dest);
                    startActivity(home);
                    finish();
                }
            } else if (result.trim().equals("playdone")) {
                mSession.setDestination(mEditText.getText().toString());
                mSession.configure();
            }
        }
    }

}
