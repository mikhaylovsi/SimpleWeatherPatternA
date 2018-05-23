package ru.gdgkazan.simpleweather.network;

import android.support.annotation.NonNull;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import ru.gdgkazan.simpleweather.data.model.City;
import ru.gdgkazan.simpleweather.data.model.CityWeatherList;
import ru.gdgkazan.simpleweather.data.model.WeatherCity;

/**
 * @author Artur Vasilov
 */
public interface WeatherService {

    @GET("data/2.5/weather?units=metric")
    Call<City> getWeather(@NonNull @Query("q") String query);

    @GET("data/2.5/group?units=metric")
    Call<CityWeatherList> getAllWeather(@NonNull @Query("id") String query);

}
