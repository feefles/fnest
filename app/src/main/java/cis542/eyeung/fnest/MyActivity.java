package cis542.eyeung.fnest;

import android.app.Activity;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.devadvance.circularseekbar.CircularSeekBar;

public class MyActivity extends Activity {

    TextView currentSetpoint;
    TextView currentTemp;
    CircularSeekBar tempBar;
    TextView tvIsConnected;
    Uri.Builder urlBase;

    String ip = "10.0.0.10";
    String port = "3933";

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return GET(urls[0]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            //Toast.makeText(getBaseContext(), "Request sent!!", Toast.LENGTH_LONG).show();
            JSONObject response;
            try {
                response = new JSONObject(result);
                String temp = response.getString("cur_temp");
                String setpoint = response.getString("set_temp");
                currentTemp.setText(temp + " F");
                currentSetpoint.setText(setpoint + " F");
                tempBar.setProgress(Integer.parseInt(setpoint) - 50);
                tvIsConnected.setText("Response received");
            } catch (JSONException e) {
                tvIsConnected.setText("Error getting response!");
            }
        }
    }

    private void getTemp() {
        Uri.Builder u = Uri.parse("http://" + ip + ":" + port).buildUpon();
        u.path("getTemp")
                .clearQuery()
                .build();
        Log.w("fnest", "u.toString() getTemp = " + u.toString());
        new HttpAsyncTask().execute(u.toString());
    }

    private void setTemp(int setpoint) {
        Uri.Builder u = Uri.parse("http://" + ip + ":" + port).buildUpon();
        u.path("setTemp")
                .clearQuery()
                .appendQueryParameter("setpoint", Integer.toString(setpoint))
                .build();
        Log.w("fnest", "u.toString() setTemp = " + u.toString());
        new HttpAsyncTask().execute(u.toString());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        // get reference to the views
        currentSetpoint = (TextView) findViewById(R.id.currentSetpoint);
        currentTemp = (TextView) findViewById(R.id.currentTemp);
        tvIsConnected = (TextView) findViewById(R.id.tvIsConnected);
        tempBar = (CircularSeekBar) findViewById(R.id.tempBar);

        setupSeekBar();
        setupSubmitButton();
        setupRefreshButton();
        setupSetupButton();
        // check if you are connected or not
        if(isConnected()){
            tvIsConnected.setBackgroundColor(0xFF00CC00);
            tvIsConnected.setText("");
        }
        else{
            tvIsConnected.setText("No network connection");
        }
        urlBase = new Uri.Builder();
        urlBase.scheme("http");
    }

    @Override
    protected void onResume() {
        super.onResume();
        getTemp();
    }



    public void setupSeekBar() {
        tempBar.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar seekBar, int progress, boolean fromUser) {
                currentSetpoint.setText(Integer.toString(progress + 50) + " F");
            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(CircularSeekBar seekBar) {
            }
        });
        tempBar.setProgress(68-50);
        currentSetpoint.setText(Integer.toString(tempBar.getProgress() + 50) + " F");
    }

    public void setupSubmitButton() {
        final Button submit = (Button) findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Integer setpoint = tempBar.getProgress() + 50;
                setTemp(setpoint);
            }
        });
    }

    public void setupRefreshButton() {
        final Button refresh = (Button) findViewById(R.id.refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
           public void onClick(View v) {
               getTemp();
           }
        });
    }

    public void setupSetupButton() {
        final android.content.Context c = this;

        final Button setup = (Button) findViewById(R.id.setup);
        setup.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AlertDialog.Builder alert = new AlertDialog.Builder(c);
                alert.setTitle("Setup Server");
                alert.setMessage("Input server IP");
                final EditText inputIP = new IPAddressText(c);
                alert.setView(inputIP);
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        final String tempip = inputIP.getText().toString();
                        if (tempip.isEmpty()) {
                            return;
                        }

                        AlertDialog.Builder alert = new AlertDialog.Builder(c);
                        alert.setTitle("Setup Server");
                        alert.setMessage("Input server Port");
                        final EditText inputPort = new EditText(c);
                        inputPort.setInputType(InputType.TYPE_CLASS_NUMBER);
                        inputPort.setKeyListener(DigitsKeyListener.getInstance());
                        alert.setView(inputPort);
                        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String tempport = inputPort.getText().toString();
                                if (tempport.isEmpty()) {
                                    return;
                                }

                                ip = tempip;
                                port = tempport;
                                tvIsConnected.setText(ip + ":" + port);
                            }
                        });
                        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        });
                        alert.show();
                    }
                });
                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                alert.show();


            }
        });
    }

    public static String GET(String url){
        InputStream inputStream;
        String result = "";
        try {

            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse httpResponse = httpclient.execute(new HttpGet(url));
            inputStream = httpResponse.getEntity().getContent();

            if(inputStream != null)
                result = convertInputStreamToString(inputStream);
            else
                result = "Did not work!";

        } catch (Exception e) {
            Log.d("fnest", e.getLocalizedMessage());
        }

        return result;
    }

    // convert inputstream to String
    private static String convertInputStreamToString(InputStream inputStream) throws IOException{
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line;
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;
        inputStream.close();
        return result;
    }

    // check network connection
    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

}