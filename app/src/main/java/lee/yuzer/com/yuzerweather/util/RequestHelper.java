package lee.yuzer.com.yuzerweather.util;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lee.yuzer.com.yuzerweather.db.StoredCity;
import lee.yuzer.com.yuzerweather.gson.AQI;
import lee.yuzer.com.yuzerweather.gson.Weather;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Yuzer on 2017/12/17.
 */

public class RequestHelper {
    //从网络获取全部天气信息
    public static void requestAllWeatherInfoAndStored(String cityName) {
        requestWeatherStandardAndStored(cityName);
        requestWeatherAqiAndStored(cityName);
    }

    //网络获取常规天气数据
    public static void requestWeatherStandardAndStored(String countyName) {
        String weatherStandardUrl = "https://free-api.heweather.com/s6/weather?location=" + countyName + "&key=4a379f0c0bde4c53b70c47628a72a8c3";

        HttpUtil.sendOkHttpRequest(weatherStandardUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                Weather weather = Utility.handleWeatherResponse(responseText);
                List<StoredCity> mTempCities = new ArrayList<>();
                mTempCities = DataSupport.where("name = ?", weather.basic.cityName).find(StoredCity.class);
                if (mTempCities.size() == 0) {
                    // 新城市，进行添加操作
                    StoredCity city = new StoredCity();
                    city.setName(weather.basic.cityName);
                    city.setContent(responseText);
                    city.setTime(weather.update.loc);
                    city.save();
                } else {
                    //进行该城市的信息刷新
                    StoredCity city = new StoredCity();
                    city.setContent(responseText);
                    city.setTime(weather.update.loc);
                    city.updateAll("name = ?", weather.basic.cityName);
                }
            }
        });

    }

    //网络获取AQI天气数据
    public static void requestWeatherAqiAndStored(String countyName) {
        final String name = countyName;
        String weatherAqiUrl = "https://free-api.heweather.com/s6/air/now?location=" + countyName + "&key=4a379f0c0bde4c53b70c47628a72a8c3";

        //网络获取常规天气数据
        HttpUtil.sendOkHttpRequest(weatherAqiUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                AQI aqi = Utility.handleWeatherAQIResponse(responseText);
                if (aqi.status.equals("permission denied")) {
                    List<StoredCity> mTempCities = new ArrayList<>();
                    mTempCities = DataSupport.where("name = ?", name).find(StoredCity.class);
                    if (mTempCities.size() == 0) {
                        // 新城市，进行添加操作
                        StoredCity city = new StoredCity();
                        city.setName(name);
                        city.setAqicontent("没有权限");
                        city.save();
                    } else {
                        //进行该城市的信息刷新
                        StoredCity city = new StoredCity();
                        city.setAqicontent("没有权限");
                        city.updateAll("name = ?", name);
                    }
                } else {
                    List<StoredCity> mTempCities = new ArrayList<>();
                    mTempCities = DataSupport.where("name = ?", aqi.basic.cityName).find(StoredCity.class);
                    if (mTempCities.size() == 0) {
                        // 新城市，进行添加操作
                        StoredCity city = new StoredCity();
                        city.setName(aqi.basic.cityName);
                        city.setAqicontent(responseText);
                        city.save();
                    } else {
                        //进行该城市的信息刷新
                        StoredCity city = new StoredCity();
                        city.setAqicontent(responseText);
                        city.updateAll("name = ?", aqi.basic.cityName);
                    }
                }
            }
        });
    }
}
