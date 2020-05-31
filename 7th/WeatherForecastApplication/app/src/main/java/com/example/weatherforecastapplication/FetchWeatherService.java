package com.example.weatherforecastapplication;


import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Vector;

import com.example.weatherforecastapplication.provider.WeatherContract;

public class FetchWeatherService extends Service {
    public static final String ACTION_RETRIEVE_WEATHER_DATA = "com.example.weatherforecastapplication.RETRIEVE_DATA";
    public static final String EXTRA_WEATHER_DATA = "weather-data";
    public FetchWeatherService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String action = intent.getAction();
        if (action.equals(ACTION_RETRIEVE_WEATHER_DATA)) {
            retrieveWeatherData(startId);
        }
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public IBinder onBind(Intent intent) {
        return new FetchWeatherServiceProxy(this);
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, Void> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
        private int mStartId = -1;

        public FetchWeatherTask(int startId) {
            mStartId = startId;
        }

        private void getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            Vector<ContentValues> cVVector = new Vector<ContentValues>(weatherArray.length());

            Time dayTime = new Time();
            dayTime.setToNow();

            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            dayTime = new Time();

            String[] resultStrs = new String[numDays];
            for (int i = 0; i < weatherArray.length(); i++) {
                String day;
                String description;
                String highAndLow;

                JSONObject dayForecast = weatherArray.getJSONObject(i);

                long dateTime;
                dateTime = dayTime.setJulianDay(julianStartDay + i);
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                ContentValues values = new ContentValues();
                values.put(WeatherContract.WeatherColumns.COLUMN_DATE, dateTime);
                values.put(WeatherContract.WeatherColumns.COLUMN_SHORT_DESC, description);
                values.put(WeatherContract.WeatherColumns.COLUMN_MIN_TEMP, low);
                values.put(WeatherContract.WeatherColumns.COLUMN_MAX_TEMP, high);

                cVVector.add(values);
            }
            if ( cVVector.size() > 0 ) {
                Log.d("AAAAAA", "OH YEA");
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                getContentResolver().bulkInsert(WeatherContract.WeatherColumns.CONTENT_URI, cvArray);

                getContentResolver().delete(WeatherContract.WeatherColumns.CONTENT_URI,
                        WeatherContract.WeatherColumns.COLUMN_DATE + " <= ?",
                        new String[] {Long.toString(dayTime.setJulianDay(julianStartDay-1))});
                Log.d("AAAAAA", "OH YEA");
            }
        }

        @Override
        protected Void doInBackground(String... params){
            if (params.length == 0) {
                return null;
            }
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String forecastJsonStr = null;

            String format = "json";
            String units = "metric";
            int numDays = 14;

            try {
                final String FORECAST_BASE_URL =
                        "http://api.openweathermap.org/data/2.5/forecast/daily?id=1835847&mode=json&units=metric&cnt=7&appid=45d4e8b802af2ccbf82468d3b7d339a4";
                final String QUERY_PARAM = "id";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";
                final String APPID_PARAM = "APPID";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                        .appendQueryParameter(APPID_PARAM, "5fd2f2cde90c1533efb95b19c048a528")
                        .build();

                URL url = new URL(builtUri.toString());

                Log.v(LOG_TAG, "Built URI " + builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }
                forecastJsonStr = buffer.toString();

                Log.v(LOG_TAG, "Forecast string: " + forecastJsonStr);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                getWeatherDataFromJson(forecastJsonStr, numDays);
            } catch (JSONException ex) {
                Log.e(LOG_TAG, ex.getMessage(),ex);
                ex.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void avoid) {
            if (mStartId < 0)
                return;

            stopSelf(mStartId);
        }
    }

    private void notifyWeatherDataRetrieved(String[] result) {
        synchronized (mListeners){
            for (IFetchDataListener listener : mListeners) {
                try {
                    listener.onWeatherDataRetrieved(result);
                } catch (RemoteException ex){
                    ex.printStackTrace();
                }
            }
        }
        Intent intent = new Intent(ACTION_RETRIEVE_WEATHER_DATA);
        intent.putExtra(EXTRA_WEATHER_DATA, result);
        sendBroadcast(intent);
    }

    private void retrieveWeatherData(int startId) {
        FetchWeatherTask weatherTask = new FetchWeatherTask(startId);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String cityId = prefs.getString("city", "1835847");
        weatherTask.execute(cityId);
    }

    private ArrayList<IFetchDataListener> mListeners = new ArrayList<IFetchDataListener>();
    private void registerFetchDataListener(IFetchDataListener listener) {
        synchronized (mListeners){
            if (!mListeners.contains(listener)){
                return;
            }
            mListeners.remove(listener);
        }
    }

    private void unregisterFetchDataListener(IFetchDataListener listener){
        synchronized (mListeners){
            if (mListeners.contains(listener)){
                return;
            }
            mListeners.remove(listener);
        }
    }

    private class FetchWeatherServiceProxy extends IFetchWeatherService.Stub {
        private WeakReference<FetchWeatherService> mService = null;

        public FetchWeatherServiceProxy(FetchWeatherService service) {
            mService = new WeakReference<FetchWeatherService>(service);
        }

        @Override
        public void retrieveWeatherData() throws RemoteException {
            mService.get().retrieveWeatherData(-1);
        }

        @Override
        public void registerFetchDataListener(IFetchDataListener listener) throws RemoteException {
            mService.get().registerFetchDataListener(listener);
        }

        @Override
        public void unregisterFetchDataListener(IFetchDataListener listener) throws RemoteException {
            mService.get().unregisterFetchDataListener(listener);
        }
    }

}
