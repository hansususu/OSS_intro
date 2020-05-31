// IFetchWeatherService.aidl
package com.example.weatherforecastapplication;

// Declare any non-default types here with import statements
import com.example.weatherforecastapplication.IFetchDataListener;

interface IFetchWeatherService {
    void retrieveWeatherData();
    void registerFetchDataListener(IFetchDataListener listener);
    void unregisterFetchDataListener(IFetchDataListener listener);

}
