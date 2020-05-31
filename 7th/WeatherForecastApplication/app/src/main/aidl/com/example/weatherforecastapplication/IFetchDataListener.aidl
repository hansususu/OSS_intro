// IFetchDataListener.aidl
package com.example.weatherforecastapplication;

// Declare any non-default types here with import statements

interface IFetchDataListener {
    void onWeatherDataRetrieved(out String[] data);
}
