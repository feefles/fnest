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

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.app.Activity;
import android.widget.Toast;

public class MyActivity extends Activity {

    TextView etResponse;
    TextView tvIsConnected;
    SeekBar tempBar;
    TextView tempPreview;
    Uri.Builder urlBase;

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            return GET(urls[0]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getBaseContext(), "Success!", Toast.LENGTH_LONG).show();
            JSONObject response;
            try {
                response = new JSONObject(result);
                String temp = response.getString("temp");
                etResponse.setText("Current Temperature: " + temp);
                tempBar.setProgress(Integer.parseInt(temp) - 50);
                tempPreview.setText("Set Temperature to: " + temp);

            } catch (JSONException e) {
                etResponse.setText("Error getting response!");

            }
        }
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        // get reference to the views
        etResponse = (TextView) findViewById(R.id.etResponse);
        tvIsConnected = (TextView) findViewById(R.id.tvIsConnected);
        tempBar = (SeekBar) findViewById(R.id.tempBar);
        tempPreview = (TextView) findViewById((R.id.tempPreview));

        setupSeekBar();
        setupSubmitButton();
        // check if you are connected or not
        if(isConnected()){
            tvIsConnected.setBackgroundColor(0xFF00CC00);
            tvIsConnected.setText("You are connected");
        }
        else{
            tvIsConnected.setText("You are NOT conncted");
        }

        urlBase = Uri.parse("http://10.0.3.2:5000").buildUpon();

        Uri u = urlBase.path("getTemp")
//                .appendQueryParameter("temp", Integer.toString(50))
                .build();

        System.out.println("u.toString() = " + u.toString());
        //call AsyncTask to perform network operation
        new HttpAsyncTask().execute(u.toString());

    }

    public void setupSeekBar() {
        tempBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tempPreview.setText("Set Temperature to: " + Integer.toString(progress + 50));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public void setupSubmitButton() {
        final Button submit = (Button) findViewById(R.id.setTemp);
        submit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Integer temp = tempBar.getProgress() + 50;
                Uri u = urlBase.path("setTemp")
                        .clearQuery()
                        .appendQueryParameter("temp", Integer.toString(temp))
                        .build();
                System.out.println("u.toString() submit = " + u.toString());
                new HttpAsyncTask().execute(u.toString());
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