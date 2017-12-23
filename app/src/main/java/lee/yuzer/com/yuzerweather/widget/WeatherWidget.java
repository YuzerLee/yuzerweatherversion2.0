package lee.yuzer.com.yuzerweather.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;

import lee.yuzer.com.weatherdemo.R;
import lee.yuzer.com.yuzerweather.db.StoredCity;
import lee.yuzer.com.yuzerweather.gson.Weather;
import lee.yuzer.com.yuzerweather.util.Utility;

/**
 * Implementation of App Widget functionality.
 */
public class WeatherWidget extends AppWidgetProvider {
    public static String CLICK_ACTION = "lee.yuzer.com.CLICK_ATION";
    public static String UPDATE_ACTION = "lee.yuzer.com.UPDATE_ACTION";
    private List<StoredCity>  mStoredCities = new ArrayList<>();;

    public void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.weather_widget);

        //读取数据库
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String cityname = prefs.getString("widgetcity_selected", "未选择");
        mStoredCities = DataSupport.where("name = ?", cityname).find(StoredCity.class);;
        Weather weather = Utility.handleWeatherResponse(mStoredCities.get(0).getContent());

        //设置RemoteViews的点击事件
        Intent clickintent = new Intent();
        clickintent.setAction(CLICK_ACTION);
        PendingIntent clickpendingintent = PendingIntent.getBroadcast(context, 0, clickintent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.container_layout, clickpendingintent);
        Log.d("RVservice", "clickintent");

        //根据数据库更新widget数据
        remoteViews.setTextViewText(R.id.wighet_city_textview, mStoredCities.get(0).getName());
        remoteViews.setTextViewText(R.id.widget_tmp_textview, weather.now.temperature + "°C");
        remoteViews.setTextViewText(R.id.widget_maxtmp_textview, "/ " + weather.forecastList.get(0).tmp_max + "°C");
        remoteViews.setTextViewText(R.id.widget_mintmp_textview, weather.forecastList.get(0).tmp_min + "°C");
        remoteViews.setTextViewText(R.id.widget_updatetime_textview, "已更新:" + weather.update.loc.split(" ")[1]);

        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        Log.d("RVservice", "update");
    }


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        String action = intent.getAction();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        //收到相应的广播，执行相应的逻辑
        if(action.equals(CLICK_ACTION)){
            //跳转到activity页面
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String cityname = prefs.getString("widgetcity_selected", "未选择");
            Intent startActivityIntent = new Intent();
            startActivityIntent.putExtra("fromwidget",cityname);
            startActivityIntent.setComponent(new ComponentName("lee.yuzer.com.weatherdemo", "lee.yuzer.com.yuzerweather.WeatherViewPagerActivity"));
            context.startActivity(startActivityIntent);
        }else if(action.equals(UPDATE_ACTION)){
            //更新城市
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.weather_widget);

            //读取数据库
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String cityname = prefs.getString("widgetcity_selected", "未选择");
            mStoredCities = DataSupport.where("name = ?", cityname).find(StoredCity.class);;
            Weather weather = Utility.handleWeatherResponse(mStoredCities.get(0).getContent());

            //设置RemoteViews的点击事件
            Intent clickintent = new Intent();
            clickintent.setAction(CLICK_ACTION);
            PendingIntent clickpendingintent = PendingIntent.getBroadcast(context, 0, clickintent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.container_layout, clickpendingintent);
            Log.d("RVservice", "clickintent");

            //根据数据库更新widget数据
            remoteViews.setTextViewText(R.id.wighet_city_textview, mStoredCities.get(0).getName());
            remoteViews.setTextViewText(R.id.widget_tmp_textview, weather.now.temperature + "°C");
            remoteViews.setTextViewText(R.id.widget_maxtmp_textview, "/ " + weather.forecastList.get(0).tmp_max + "°C");
            remoteViews.setTextViewText(R.id.widget_mintmp_textview, weather.forecastList.get(0).tmp_min + "°C");
            remoteViews.setTextViewText(R.id.widget_updatetime_textview, "已更新:" + weather.update.loc.split(" ")[1]);

            appWidgetManager.updateAppWidget(new ComponentName(context, WeatherWidget.class), remoteViews);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }
}

