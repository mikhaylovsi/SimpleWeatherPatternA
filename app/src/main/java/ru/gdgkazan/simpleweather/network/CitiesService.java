package ru.gdgkazan.simpleweather.network;

import android.support.annotation.NonNull;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import ru.gdgkazan.simpleweather.data.model.City;
import ru.gdgkazan.simpleweather.data.model.WeatherCity;

/**
 * @author Artur Vasilov
 */
public interface CitiesService {

    @GET("help/city_list.txt")
    Call<WeatherCity> getCities();

}
