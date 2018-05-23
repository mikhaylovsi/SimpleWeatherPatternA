package ru.gdgkazan.simpleweather.data.model;

import java.util.List;

public class CityWeatherList {

    private List<City> list;

    public CityWeatherList(){}

    public List<City> getList() {
        return list;
    }

    public void setList(List<City> list) {
        this.list = list;
    }
}
