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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MyActivity extends Activity {

    TextView currentSetpoint;
    TextView currentTemp;
    SeekBar tempBar;
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
            Toast.makeText(getBaseContext(), "Request sent!!", Toast.LENGTH_LONG).show();
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




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        // get reference to the views
        currentSetpoint = (TextView) findViewById(R.id.currentSetpoint);
        currentTemp = (TextView) findViewById(R.id.currentTemp);
        tvIsConnected = (TextView) findViewById(R.id.tvIsConnected);
        tempBar = (SeekBar) findViewById(R.id.tempBar);

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

//        urlBase = Uri.parse("http://10.0.3.2:5000").buildUpon();
        urlBase = Uri.parse("http://" + ip + ":" + port).buildUpon();

        Uri u = urlBase.path("getTemp")
                .build();

        Log.w("fnest", "u.toString() = " + u.toString());
        //call AsyncTask to perform network operation
        new HttpAsyncTask().execute(u.toString());

    }

    public void setupSeekBar() {
        tempBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentSetpoint.setText(Integer.toString(progress + 50) + " F");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

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
                Uri u = urlBase.path("setTemp")
                        .clearQuery()
                        .appendQueryParameter("setpoint", Integer.toString(setpoint))
                        .build();
                System.out.println("u.toString() submit = " + u.toString());
                new HttpAsyncTask().execute(u.toString());
            }
        });
    }

    public void setupRefreshButton() {
        final Button refresh = (Button) findViewById(R.id.refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
           public void onClick(View v) {
               Uri u = urlBase.path("getTemp")
                       .clearQuery()
                       .build();
               Log.w("fnest", "u.toString() refresh = " + u.toString());
               new HttpAsyncTask().execute(u.toString());
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
        InputStream inputStream = null;
        String result = "";
        try {

            // create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // make GET request to the given URL
            HttpResponse httpResponse = httpclient.execute(new HttpGet(url));

            // receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // convert inputstream to string
            if(inputStream != null)
                result = convertInputStreamToString(inputStream);
            else
                result = "Did not work!";

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        return result;
    }

    // convert inputstream to String
    private static String convertInputStreamToString(InputStream inputStream) throws IOException{
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;
        inputStream.close();
        return result;
    }

    // check network connection
    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(this.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }

}