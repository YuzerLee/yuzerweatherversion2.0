package lee.yuzer.com.yuzerweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.List;

import lee.yuzer.com.yuzerweather.db.StoredCity;
import lee.yuzer.com.yuzerweather.gson.Weather;
import lee.yuzer.com.yuzerweather.util.HttpUtil;
import lee.yuzer.com.yuzerweather.util.RequestHelper;
import lee.yuzer.com.yuzerweather.util.Utility;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {
    private List<StoredCity> mStoredCities;
    private static String SERVICE_START = "lee.yuzer.com.houtai";

    public AutoUpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences prefs1 = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean state = prefs1.getBoolean("switch_state", true);
        if (state == true) {
            //如果状态为开启，则继续定时操作
            updateWeather();
            updateBingPic();
            AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
            //从SP中读取用户的选择，进行相应时间间隔的刷新
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String info = prefs.getString("selected_text", "未知");
            int RefreshHour = 0;
            if (info.equals("半小时")) {
                RefreshHour = 30 * 60 * 1000;
            } else if (info.equals("1小时")) {
                RefreshHour = 60 * 60 * 1000;
            } else if (info.equals("6小时")) {
                RefreshHour = 6 * 60 * 60 * 1000;
            } else if (info.equals("12小时")) {
                RefreshHour = 12 * 60 * 60 * 1000;
            } else if (info.equals("24小时")) {
                RefreshHour = 24 * 60 * 60 * 1000;
            } else {
                RefreshHour = 30 * 60 * 1000;
            }
            long triggerAtTime = SystemClock.elapsedRealtime() + RefreshHour;
            Intent i = new Intent(this, AutoUpdateService.class);
            intent.setAction(SERVICE_START);
            //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
            manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
        } else {
            //如果状态为取消，则将闹钟取消
            AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
            Intent i = new Intent(this, AutoUpdateService.class);
            intent.setAction(SERVICE_START);
            //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
            manager.cancel(pi);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    //更新所有城市天气
    private void updateWeather() {
        mStoredCities = DataSupport.findAll(StoredCity.class);
        for (int i = 0; i < mStoredCities.size(); i++) {
            String refreshCityName = mStoredCities.get(i).getName();
            RequestHelper.requestAllWeatherInfoAndStored(refreshCityName);
        }
        Log.d("service", "start");
    }

    //更新必应每日一图
    private void updateBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
            }
        });
    }
}
