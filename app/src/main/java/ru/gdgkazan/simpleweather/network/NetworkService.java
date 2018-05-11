package ru.gdgkazan.simpleweather.network;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import ru.arturvasilov.sqlite.core.SQLite;
import ru.arturvasilov.sqlite.core.Where;
import ru.gdgkazan.simpleweather.data.GsonHolder;
import ru.gdgkazan.simpleweather.data.model.City;
import ru.gdgkazan.simpleweather.data.model.WeatherCity;
import ru.gdgkazan.simpleweather.data.tables.CityTable;
import ru.gdgkazan.simpleweather.data.tables.RequestTable;
import ru.gdgkazan.simpleweather.data.tables.WeatherCityTable;
import ru.gdgkazan.simpleweather.network.model.NetworkRequest;
import ru.gdgkazan.simpleweather.network.model.Request;
import ru.gdgkazan.simpleweather.network.model.RequestStatus;

/**
 * @author Artur Vasilov
 */
public class NetworkService extends IntentService {

    private static final String REQUEST_KEY = "request";
    private static final String CITY_NAME_KEY = "city_name";
    private static final String CITY_NAMES_LIST = "city_names_list";

    public static void start(@NonNull Context context, @NonNull Request request, @NonNull String cityName) {
        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra(REQUEST_KEY, GsonHolder.getGson().toJson(request));
        intent.putExtra(CITY_NAME_KEY, cityName);
        context.startService(intent);
    }


    public static void startInititalLoading(@NonNull Context context, @NonNull Request request, ArrayList<String> cityNames) {
        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra(REQUEST_KEY, GsonHolder.getGson().toJson(request));
        intent.putStringArrayListExtra(CITY_NAMES_LIST, cityNames);
        context.startService(intent);
    }

    @SuppressWarnings("unused")
    public NetworkService() {
        super(NetworkService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Request request = GsonHolder.getGson().fromJson(intent.getStringExtra(REQUEST_KEY), Request.class);
        Request savedRequest = SQLite.get().querySingle(RequestTable.TABLE,
                Where.create().equalTo(RequestTable.REQUEST, request.getRequest()));

        if (savedRequest != null && request.getStatus() == RequestStatus.IN_PROGRESS) {
            return;
        }
        request.setStatus(RequestStatus.IN_PROGRESS);
        SQLite.get().insert(RequestTable.TABLE, request);
        SQLite.get().notifyTableChanged(RequestTable.TABLE);


        switch (request.getRequest()){
            case NetworkRequest.CITY_WEATHER:
                String cityName = intent.getStringExtra(CITY_NAME_KEY);
                executeCityRequest(request, cityName);
                break;
            case NetworkRequest.CITY_LIST:
                if(executeLoadCitiesRequest(request)){
                    ArrayList<String> cityNames = intent.getStringArrayListExtra(CITY_NAMES_LIST);
                    List<WeatherCity> weatherCities = findWeatherCitiesByNames(cityNames);
                }
                break;
        }


    }


    private List<WeatherCity> findWeatherCitiesByNames(ArrayList<String> cityNames) {

        Where where = Where.create();
        for(String cityName : cityNames) {
            where.like(WeatherCityTable.CITY_NAME, cityName);
        }

       return  SQLite.get().query(WeatherCityTable.TABLE, where);
    }

    private boolean executeLoadCitiesRequest(Request request) {

        boolean loadCitiesSuccess = false;

        try {

            InputStream is = ApiFactory.getCitiesCall().execute().body().byteStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            ArrayList<WeatherCity> weatherCities = new ArrayList<>();
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] lineArray = line.split("\t");
                int cityId = Integer.parseInt(lineArray[0]);
                String cityName = lineArray[1];
                WeatherCity weatherCity = new WeatherCity(cityId, cityName);
                weatherCities.add(weatherCity);
            }
            is.close();
            SQLite.get().insert(WeatherCityTable.TABLE, weatherCities);
            loadCitiesSuccess = true;

        } catch (IOException e) {
            request.setStatus(RequestStatus.ERROR);
            request.setError(e.getMessage());
        } finally {
            SQLite.get().insert(RequestTable.TABLE, request);
            SQLite.get().notifyTableChanged(RequestTable.TABLE);
        }

        return loadCitiesSuccess;

    }

    private void executeCityRequest(@NonNull Request request, @NonNull String cityName) {
        try {
            City city = ApiFactory.getWeatherService()
                    .getWeather(cityName)
                    .execute()
                    .body();
            SQLite.get().delete(CityTable.TABLE);
            SQLite.get().insert(CityTable.TABLE, city);
            request.setStatus(RequestStatus.SUCCESS);
        } catch (IOException e) {
            request.setStatus(RequestStatus.ERROR);
            request.setError(e.getMessage());
        } finally {
            SQLite.get().insert(RequestTable.TABLE, request);
            SQLite.get().notifyTableChanged(RequestTable.TABLE);
        }
    }
}

