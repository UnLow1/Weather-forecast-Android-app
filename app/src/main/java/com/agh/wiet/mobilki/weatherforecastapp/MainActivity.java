package com.agh.wiet.mobilki.weatherforecastapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    LocationManager locationManager;
    LocationProvider locationProvider;
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 313;
    Handler myHandler;
    public static String OPEN_WEATHER_WEATHER_QUERY = "https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&mode=html&appid=4526d487f12ef78b82b7a7d113faea64";

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            final double lat = (location.getLatitude());
            final double lon = location.getLongitude();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    updateWeather(lat, lon);
                }
            }).start();
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
        }

        @Override

        public void onProviderEnabled(String s) {
        }

        @Override
        public void onProviderDisabled(String s) {
        }
    };

    private void updateWeather(double lat, double lon) {
        String url = String.format(OPEN_WEATHER_WEATHER_QUERY, lat, lon);
        String weather = getContentFromUrl(url);
        String city = getCityFromHtml(weather);
        Message m = myHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putString("lat", String.valueOf(lat));
        b.putString("lon", String.valueOf(lon));
        b.putString("city", city);
        b.putString("web", weather);
        m.setData(b);
        myHandler.sendMessage(m);
    }

    private String getContentFromUrl(String addr) {
        String content = null;

        Log.v("[GEO WEATHER ACTIVITY]", addr);
        HttpURLConnection urlConnection = null;
        URL url;
        try {
            url = new URL(addr);
            urlConnection = (HttpURLConnection) url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            content = stringBuilder.toString();

            content = content.replaceAll("http://", "https://");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) urlConnection.disconnect();
        }
        return content;
    }

    private String getCityFromHtml(String html) {
        String textInBodySegment = Html.fromHtml(html).toString();
        return textInBodySegment.split("\n")[0];
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (locationProvider != null) {
            Toast.makeText(this, "Location listener unregistered!", Toast.LENGTH_SHORT).show();
            try {
                this.locationManager.removeUpdates(this.locationListener);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Location Provider is not avilable at the moment!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final WebView weatherWebView = findViewById(R.id.weatherWebView);
        weatherWebView.getSettings().setDomStorageEnabled(true);
        weatherWebView.getSettings().setAppCacheEnabled(true);
        weatherWebView.getSettings().setLoadsImagesAutomatically(true);
        weatherWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        weatherWebView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
//                Log.i(TAG, "Processing webview url click...");
                view.loadUrl(url);
                return true;
            }

//            public void onPageFinished(WebView view, String url) {
//                Log.i(TAG, "Finished loading URL: " +url);
//                if (pDialog.isShowing()) {
//                    pDialog.dismiss();
//                }
//            }

//            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
//                Log.e(TAG, "Error: " + description);
//                Toast.makeText(FundCardWeb.this, "Page Load Error" + description, Toast.LENGTH_SHORT).show();
//
//            }
        });
        final TextView latTextView = findViewById(R.id.latTextView);
        final TextView lonTextView = findViewById(R.id.lonTextView);
        final TextView cityTextView = findViewById(R.id.cityTextView);

        myHandler = new Handler() {
            public void handleMessage(Message msg) {
                String lat = msg.getData().getString("lat");
                String lon = msg.getData().getString("lon");
                String web = msg.getData().getString("web");
                String city = msg.getData().getString("city");

                latTextView.setText(lat);
                lonTextView.setText(lon);
                cityTextView.setText(city);

                weatherWebView.loadDataWithBaseURL(null, web, "text/html", "utf-8", null);
            }
        };
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    accessLocation();
                }
            }
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
            // MY_PERMISSIONS_REQUEST_LOCATION is an app-defined int constant. The callback method gets the result of the request.
        } else {
            accessLocation();
        }
    }

    private void accessLocation() {
        this.locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        this.locationProvider = this.locationManager.getProvider(LocationManager.GPS_PROVIDER);
        if (locationProvider != null) {
            Toast.makeText(this, "Location listener registered!", Toast.LENGTH_SHORT).show();
            try {
                this.locationManager.requestLocationUpdates(locationProvider.getName(), 0, 0, this.locationListener);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this,
                    "Location Provider is not available at the moment!", Toast.LENGTH_SHORT).show();
        }
    }
}
