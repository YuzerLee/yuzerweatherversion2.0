package lee.yuzer.com.yuzerweather;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lee.yuzer.com.weatherdemo.R;
import lee.yuzer.com.yuzerweather.customizeview.MyProgressBar;
import lee.yuzer.com.yuzerweather.customizeview.MyScrollView;
import lee.yuzer.com.yuzerweather.db.StoredCity;
import lee.yuzer.com.yuzerweather.gson.AQI;
import lee.yuzer.com.yuzerweather.gson.Forecast;
import lee.yuzer.com.yuzerweather.gson.Lifestyle;
import lee.yuzer.com.yuzerweather.gson.Weather;
import lee.yuzer.com.yuzerweather.scrollviewinterface.MyOnScrollChangedListener;
import lee.yuzer.com.yuzerweather.service.AutoUpdateService;
import lee.yuzer.com.yuzerweather.util.HttpUtil;
import lee.yuzer.com.yuzerweather.util.RequestHelper;
import lee.yuzer.com.yuzerweather.util.Utility;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherViewPagerActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener {
    private MyScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    private ImageView bingPicImg;
    public SwipeRefreshLayout mSwipeRefreshLayout;
    private String countyName;
    public DrawerLayout mDrawerLayout;
    private Button chooseCityButton;
    public ViewPager mViewPager;
    private PagerAdapter mPagerAdapter;
    private List<View> mViewPagerList;
    private List<StoredCity> mStoredCities;
    private List<ImageView> dotView;
    private Button removeCityButton;
    private static String SERVICE_START = "lee.yuzer.com.houtai";
    private TextView pm10_text;
    private TextView pm25_text;
    private TextView no2_text;
    private TextView so2_text;
    private TextView o3_text;
    private TextView co_text;
    private TextView body_text;
    private TextView pcpn_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);

        initView();

        //ViewPager版本
        initViewPager();

        //判断是不是有widget发起
        String name = getIntent().getStringExtra("fromwidget");
        if(name != null){
            Log.d("name", name);
            int index = 0;
            List<StoredCity> ccity = DataSupport.findAll(StoredCity.class);
            for (StoredCity city : ccity) {
                if (city.getName().equals(name))
                    break;
                index++;
            }
            mViewPager.setCurrentItem(index);
        }
    }

    private void initView() {
        mStoredCities = new ArrayList<>();

        titleCity = (TextView) findViewById(R.id.title_city);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        bingPicImg = (ImageView) findViewById(R.id.bing_pic_img);
        dotView = new ArrayList<>();
        removeCityButton = (Button) findViewById(R.id.deletecity_button);
        mStoredCities = DataSupport.findAll(StoredCity.class);
        if (mStoredCities.size() <= 1) {
            removeCityButton.setVisibility(View.GONE);
        }
        removeCityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(WeatherViewPagerActivity.this);
                String cityname = prefs.getString("widgetcity_selected", "未选择");
                if(titleCity.getText().equals(cityname)){
                    Toast.makeText(WeatherViewPagerActivity.this, "你删除的城市是widget的显示城市，删除无效", Toast.LENGTH_SHORT).show();
                }else{
                    mStoredCities = DataSupport.findAll(StoredCity.class);
                    if (mStoredCities.size() > 1) {
                        View ViewPagerLayout = MyViewPagerAdatper.mCurrentView;
                        mStoredCities = DataSupport.where("name = ?", titleCity.getText().toString()).find(StoredCity.class);
                        DataSupport.deleteAll(StoredCity.class, "name = ?", mStoredCities.get(0).getName());
                        mViewPagerList.remove(ViewPagerLayout);
                        mViewPagerList.clear();
                        LinearLayout containerlayout = (LinearLayout) findViewById(R.id.container_layout);
                        containerlayout.removeView(mViewPager);
                        mPagerAdapter = new MyViewPagerAdatper(mViewPagerList);
                        mViewPager = new ViewPager(WeatherViewPagerActivity.this);
                        mViewPager.setAdapter(mPagerAdapter);
                        mViewPager.setOnPageChangeListener(WeatherViewPagerActivity.this);
                        containerlayout.addView(mViewPager);
                        mStoredCities = DataSupport.findAll(StoredCity.class);
                        showWeatherInfoFromDB(mStoredCities);
                    }
                }
            }
        });
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        chooseCityButton = (Button) findViewById(R.id.nav_button);
        chooseCityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawerLayout.openDrawer(GravityCompat.START);
            }
        });
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String bingPic = prefs.getString("bing_pic", null);
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(bingPicImg);
        } else {
            loadBingPic();
        }
    }



    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String name = intent.getStringExtra("fromwidget");
        if(name != null){
            Log.d("name", name);
            int index = 0;
            List<StoredCity> ccity = DataSupport.findAll(StoredCity.class);
            for (StoredCity city : ccity) {
                if (city.getName().equals(name))
                    break;
                index++;
            }
            mViewPager.setCurrentItem(index);
        }
    }

    private void initViewPager() {
        mStoredCities = DataSupport.findAll(StoredCity.class);
        mViewPager = (ViewPager) findViewById(R.id.myViewPager);
        mViewPagerList = new ArrayList<>();
        mPagerAdapter = new MyViewPagerAdatper(mViewPagerList);
        mViewPager.setAdapter(mPagerAdapter);

        showWeatherInfoFromDB(mStoredCities);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean state = prefs.getBoolean("switch_state", false);
        if (state == true) {
            TriggerService();
        }

        mViewPager.setOnPageChangeListener(this);
    }

    //从数据库中加载数据并显示
    public void showWeatherInfoFromDB(List<StoredCity> storedCities) {
        //加载viewpager信息
        for (int i = 0; i < storedCities.size(); i++) {
            Weather weather = Utility.handleWeatherResponse(mStoredCities.get(i).getContent());
            if (mStoredCities.get(i).getAqicontent().equals("没有权限")) {
                addViewPagerItemUI(weather, null);
            } else {
                AQI aqi = Utility.handleWeatherAQIResponse(mStoredCities.get(i).getAqicontent());
                addViewPagerItemUI(weather, aqi);
            }
        }

        //加载该页面上的其他信息
        mViewPager.setCurrentItem(0);

        titleCity.setText(storedCities.get(0).getName());
        titleUpdateTime.setText(storedCities.get(0).getTime().split(" ")[1]);

        if (storedCities.size() == 1) {
            removeCityButton.setVisibility(View.GONE);
        } else {
            removeCityButton.setVisibility(View.VISIBLE);
        }

        if (storedCities.size() > 1) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(15, 15);
            params.setMargins(10, 0, 10, 0);
            LinearLayout dotlayout = (LinearLayout) findViewById(R.id.dot_layout);
            dotlayout.removeAllViews();
            dotView.clear();
            for (int i = 0; i < storedCities.size(); i++) {
                ImageView iv = new ImageView(this);
                iv.setLayoutParams(params);
                iv.setImageResource(R.drawable.dot_selector);
                if (i == 0) {
                    iv.setSelected(true);

                    //对被选中dot进行放大处理
                    LinearLayout.LayoutParams paramsBig = new LinearLayout.LayoutParams(30, 30);
                    params.setMargins(10, 0, 10, 0);
                    iv.setLayoutParams(paramsBig);
                } else {
                    iv.setSelected(false);
                }
                dotView.add(iv);
                dotlayout.addView(iv);
            }
        } else {
            LinearLayout dotlayout = (LinearLayout) findViewById(R.id.dot_layout);
            dotlayout.removeAllViews();
        }
    }

    //形成ViewPager一个Item的所有信息
    public void addViewPagerItemUI(Weather weather, AQI aqi) {
        View ViewPagerLayout = getLayoutInflater().from(this).inflate(R.layout.viewpager_item, null);
        mSwipeRefreshLayout = (SwipeRefreshLayout) ViewPagerLayout.findViewById(R.id.swipe_refresh);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                String RefreshCityName = titleCity.getText().toString();
                RequestHelper.requestAllWeatherInfoAndStored(RefreshCityName);
                refreshViewPagerItemUi(RefreshCityName);
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });

        String degree = weather.now.temperature + "°C";
        String weatherInfo = weather.now.cond_txt;
        degreeText = (TextView) ViewPagerLayout.findViewById(R.id.degree_text);
        weatherInfoText = (TextView) ViewPagerLayout.findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout) ViewPagerLayout.findViewById(R.id.forecast_layout);
        comfortText = (TextView) ViewPagerLayout.findViewById(R.id.comfort_text);
        carWashText = (TextView) ViewPagerLayout.findViewById(R.id.car_wash_text);
        sportText = (TextView) ViewPagerLayout.findViewById(R.id.sport_text);
        weatherLayout = (MyScrollView) ViewPagerLayout.findViewById(R.id.weather_layout);
        final LinearLayout aqilayout = (LinearLayout) ViewPagerLayout.findViewById(R.id.aqi_layout);
        final LinearLayout comfortlayout = (LinearLayout) ViewPagerLayout.findViewById(R.id.comfort_layout);
        final MyProgressBar comfort_myProgressBar = (MyProgressBar) ViewPagerLayout.findViewById(R.id.comfort_myProgressBar);
        body_text = (TextView) ViewPagerLayout.findViewById(R.id.body_text);
        pcpn_text = (TextView) ViewPagerLayout.findViewById(R.id.pcpn_text);
        body_text.setText(weather.now.fl);
        pcpn_text.setText(weather.now.pcpn);
        comfort_myProgressBar.setCurrentProgress(Integer.valueOf(weather.now.hum));
        final MyProgressBar myProgressBar = (MyProgressBar) ViewPagerLayout.findViewById(R.id.myProgressBar);
        pm10_text = (TextView) ViewPagerLayout.findViewById(R.id.pm10_text);
        pm25_text = (TextView) ViewPagerLayout.findViewById(R.id.pm25_text);
        so2_text = (TextView) ViewPagerLayout.findViewById(R.id.so2_text);
        co_text = (TextView) ViewPagerLayout.findViewById(R.id.co_text);
        no2_text = (TextView) ViewPagerLayout.findViewById(R.id.no2_text);
        o3_text = (TextView) ViewPagerLayout.findViewById(R.id.o3_text);
        if (aqi != null) {
            pm10_text.setText(aqi.air_now_city.getPm10());
            pm25_text.setText(aqi.air_now_city.getPm25());
            so2_text.setText(aqi.air_now_city.getSo2());
            co_text.setText(aqi.air_now_city.getCo());
            no2_text.setText(aqi.air_now_city.getNo2());
            o3_text.setText(aqi.air_now_city.getO3());
            myProgressBar.setCurrentProgress(Integer.valueOf(aqi.air_now_city.getAqi()));
        } else {
            pm10_text.setText("没有权限");
            pm25_text.setText("没有权限");
            so2_text.setText("没有权限");
            co_text.setText("没有权限");
            no2_text.setText("没有权限");
            o3_text.setText("没有权限");
            myProgressBar.setCurrentProgress(100);
        }
        weatherLayout.setMyOnScrollChangedListener(new MyOnScrollChangedListener() {
            boolean isShow = false;
            boolean isShow_comfort = false;

            @Override
            public void onScrollChanged(int top, int oldTop) {
                if (top > oldTop) {
                    //上滑操作
                    Rect scrollBounds = new Rect();
                    weatherLayout.getHitRect(scrollBounds);
                    if (aqilayout.getLocalVisibleRect(scrollBounds) && !isShow) {
                        ObjectAnimator.ofInt(myProgressBar, "currentProgress", 0, myProgressBar.getCurrentProgress()).setDuration(1500).start();
                        isShow = true;
                    } else if (!aqilayout.getLocalVisibleRect(scrollBounds)) {
                        isShow = false;
                    }

                    if (comfortlayout.getLocalVisibleRect(scrollBounds) && !isShow_comfort) {
                        ObjectAnimator.ofInt(comfort_myProgressBar, "currentProgress", 0, comfort_myProgressBar.getCurrentProgress()).setDuration(1500).start();
                        isShow_comfort = true;
                    } else if (!comfortlayout.getLocalVisibleRect(scrollBounds)) {
                        isShow_comfort = false;
                    }
                } else {
                    //下滑操作
                    Rect scrollBounds = new Rect();
                    weatherLayout.getHitRect(scrollBounds);
                    if (aqilayout.getLocalVisibleRect(scrollBounds) && !isShow) {
                        ObjectAnimator.ofInt(myProgressBar, "currentProgress", 0, myProgressBar.getCurrentProgress()).setDuration(1500).start();
                        isShow = true;
                    } else if (!aqilayout.getLocalVisibleRect(scrollBounds)) {
                        isShow = false;
                    }

                    if (comfortlayout.getLocalVisibleRect(scrollBounds) && !isShow_comfort) {
                        ObjectAnimator.ofInt(comfort_myProgressBar, "currentProgress", 0, comfort_myProgressBar.getCurrentProgress()).setDuration(1500).start();
                        isShow_comfort = true;
                    } else if (!comfortlayout.getLocalVisibleRect(scrollBounds)) {
                        isShow_comfort = false;
                    }

                }
            }
        });

        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
            TextView dateText = (TextView) view.findViewById(R.id.date_text);
            TextView infoText = (TextView) view.findViewById(R.id.info_text);
            TextView maxText = (TextView) view.findViewById(R.id.max_text);
            TextView minText = (TextView) view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.cond_txt_d);
            maxText.setText(forecast.tmp_max);
            minText.setText(forecast.tmp_min);
            forecastLayout.addView(view);
        }
        String comfort;
        String carWash;
        String sport;
        for (Lifestyle lifestyle : weather.lifestyleList) {
            if (lifestyle.type.equals("comf")) {
                comfort = "舒适度：" + lifestyle.txt;
                comfortText.setText(comfort);
            } else if (lifestyle.type.equals("cw")) {
                carWash = "洗车指数：" + lifestyle.txt;
                carWashText.setText(carWash);
            } else if (lifestyle.type.equals("sport")) {
                sport = "运动建议：" + lifestyle.txt;
                sportText.setText(sport);
            }
        }
        weatherLayout.setVisibility(View.VISIBLE);

        mViewPagerList.add(ViewPagerLayout);
        mPagerAdapter.notifyDataSetChanged();
    }

    private void TriggerService() {
        Intent intent = new Intent(this, AutoUpdateService.class);
        intent.setAction(SERVICE_START);
        //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startService(intent);
        Log.d("service", "start");
    }

    private void refreshViewPagerItemUi(String name) {
        List<StoredCity> mTempCities = new ArrayList<>();
        mTempCities = DataSupport.where("name = ?", name).find(StoredCity.class);
        Weather weather = Utility.handleWeatherResponse(mTempCities.get(0).getContent());
        AQI aqi;
        if (mTempCities.get(0).getAqicontent().equals("没有权限")) {
            aqi = null;
        } else {
            aqi = Utility.handleWeatherAQIResponse(mTempCities.get(0).getAqicontent());
        }

        View ViewPagerLayout = MyViewPagerAdatper.mCurrentView;
        mSwipeRefreshLayout = (SwipeRefreshLayout) ViewPagerLayout.findViewById(R.id.swipe_refresh);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                String RefreshCityName = titleCity.getText().toString();
                RequestHelper.requestAllWeatherInfoAndStored(RefreshCityName);
                refreshViewPagerItemUi(RefreshCityName);
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
        String cityName = weather.basic.cityName;
        String updateTime = weather.update.loc.split(" ")[1];
        String degree = weather.now.temperature + "°C";
        String weatherInfo = weather.now.cond_txt;
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText = (TextView) ViewPagerLayout.findViewById(R.id.degree_text);
        weatherInfoText = (TextView) ViewPagerLayout.findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout) ViewPagerLayout.findViewById(R.id.forecast_layout);
        comfortText = (TextView) ViewPagerLayout.findViewById(R.id.comfort_text);
        carWashText = (TextView) ViewPagerLayout.findViewById(R.id.car_wash_text);
        sportText = (TextView) ViewPagerLayout.findViewById(R.id.sport_text);
        weatherLayout = (MyScrollView) ViewPagerLayout.findViewById(R.id.weather_layout);
        final LinearLayout aqilayout = (LinearLayout) ViewPagerLayout.findViewById(R.id.aqi_layout);
        final LinearLayout comfortlayout = (LinearLayout) ViewPagerLayout.findViewById(R.id.comfort_layout);
        final MyProgressBar comfort_myProgressBar = (MyProgressBar) ViewPagerLayout.findViewById(R.id.comfort_myProgressBar);
        body_text = (TextView) ViewPagerLayout.findViewById(R.id.body_text);
        pcpn_text = (TextView) ViewPagerLayout.findViewById(R.id.pcpn_text);
        body_text.setText(weather.now.fl);
        pcpn_text.setText(weather.now.pcpn);
        comfort_myProgressBar.setCurrentProgress(Integer.valueOf(weather.now.hum));
        final MyProgressBar myProgressBar = (MyProgressBar) ViewPagerLayout.findViewById(R.id.myProgressBar);
        pm10_text = (TextView) ViewPagerLayout.findViewById(R.id.pm10_text);
        pm25_text = (TextView) ViewPagerLayout.findViewById(R.id.pm25_text);
        so2_text = (TextView) ViewPagerLayout.findViewById(R.id.so2_text);
        co_text = (TextView) ViewPagerLayout.findViewById(R.id.co_text);
        no2_text = (TextView) ViewPagerLayout.findViewById(R.id.no2_text);
        o3_text = (TextView) ViewPagerLayout.findViewById(R.id.o3_text);
        if (aqi != null) {
            pm10_text.setText(aqi.air_now_city.getPm10());
            pm25_text.setText(aqi.air_now_city.getPm25());
            so2_text.setText(aqi.air_now_city.getSo2());
            co_text.setText(aqi.air_now_city.getCo());
            no2_text.setText(aqi.air_now_city.getNo2());
            o3_text.setText(aqi.air_now_city.getO3());
            myProgressBar.setCurrentProgress(Integer.valueOf(aqi.air_now_city.getAqi()));
        } else {
            pm10_text.setText("没有权限");
            pm25_text.setText("没有权限");
            so2_text.setText("没有权限");
            co_text.setText("没有权限");
            no2_text.setText("没有权限");
            o3_text.setText("没有权限");
            myProgressBar.setCurrentProgress(100);
        }
        weatherLayout.setMyOnScrollChangedListener(new MyOnScrollChangedListener() {
            boolean isShow = false;
            boolean isShow_comfort = false;

            @Override
            public void onScrollChanged(int top, int oldTop) {
                if (top > oldTop) {
                    //上滑操作
                    Rect scrollBounds = new Rect();
                    weatherLayout.getHitRect(scrollBounds);
                    if (aqilayout.getLocalVisibleRect(scrollBounds) && !isShow) {
                        ObjectAnimator.ofInt(myProgressBar, "currentProgress", 0, myProgressBar.getCurrentProgress()).setDuration(1500).start();
                        isShow = true;
                    } else if (!aqilayout.getLocalVisibleRect(scrollBounds)) {
                        isShow = false;
                    }

                    if (comfortlayout.getLocalVisibleRect(scrollBounds) && !isShow_comfort) {
                        ObjectAnimator.ofInt(comfort_myProgressBar, "currentProgress", 0, comfort_myProgressBar.getCurrentProgress()).setDuration(1500).start();
                        isShow_comfort = true;
                    } else if (!comfortlayout.getLocalVisibleRect(scrollBounds)) {
                        isShow_comfort = false;
                    }
                } else {
                    //下滑操作
                    Rect scrollBounds = new Rect();
                    weatherLayout.getHitRect(scrollBounds);
                    if (aqilayout.getLocalVisibleRect(scrollBounds) && !isShow) {
                        ObjectAnimator.ofInt(myProgressBar, "currentProgress", 0, myProgressBar.getCurrentProgress()).setDuration(1500).start();
                        isShow = true;
                    } else if (!aqilayout.getLocalVisibleRect(scrollBounds)) {
                        isShow = false;
                    }

                    if (comfortlayout.getLocalVisibleRect(scrollBounds) && !isShow_comfort) {
                        ObjectAnimator.ofInt(comfort_myProgressBar, "currentProgress", 0, comfort_myProgressBar.getCurrentProgress()).setDuration(1500).start();
                        isShow_comfort = true;
                    } else if (!comfortlayout.getLocalVisibleRect(scrollBounds)) {
                        isShow_comfort = false;
                    }

                }
            }
        });

        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
            TextView dateText = (TextView) view.findViewById(R.id.date_text);
            TextView infoText = (TextView) view.findViewById(R.id.info_text);
            TextView maxText = (TextView) view.findViewById(R.id.max_text);
            TextView minText = (TextView) view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.cond_txt_d);
            maxText.setText(forecast.tmp_max);
            minText.setText(forecast.tmp_min);
            forecastLayout.addView(view);
        }
        String comfort;
        String carWash;
        String sport;
        for (Lifestyle lifestyle : weather.lifestyleList) {
            if (lifestyle.type.equals("comf")) {
                comfort = "舒适度：" + lifestyle.txt;
                comfortText.setText(comfort);
            } else if (lifestyle.type.equals("cw")) {
                carWash = "洗车指数：" + lifestyle.txt;
                carWashText.setText(carWash);
            } else if (lifestyle.type.equals("sport")) {
                sport = "运动建议：" + lifestyle.txt;
                sportText.setText(sport);
            }
        }
        weatherLayout.setVisibility(View.VISIBLE);
        mPagerAdapter.notifyDataSetChanged();

        loadBingPic();
    }

    private void loadBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherViewPagerActivity.this).edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherViewPagerActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        mStoredCities = DataSupport.findAll(StoredCity.class);
        if (mStoredCities.size() == 1) {
            removeCityButton.setVisibility(View.GONE);
        } else {
            removeCityButton.setVisibility(View.VISIBLE);
        }
        for (int i = 0; i < mStoredCities.size(); i++) {
            if (i == position) {
                titleCity.setText(mStoredCities.get(i).getName());
                titleUpdateTime.setText(mStoredCities.get(i).getTime().split(" ")[1]);
            }
        }
        if (mStoredCities.size() > 1) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(15, 15);
            params.setMargins(10, 0, 10, 0);
            LinearLayout dotlayout = (LinearLayout) findViewById(R.id.dot_layout);
            dotlayout.removeAllViews();
            dotView.clear();
            for (int i = 0; i < mStoredCities.size(); i++) {
                ImageView iv = new ImageView(WeatherViewPagerActivity.this);
                iv.setLayoutParams(params);
                iv.setImageResource(R.drawable.dot_selector);
                if (i == position) {
                    iv.setSelected(true);

                    //对被选中dot进行放大处理
                    LinearLayout.LayoutParams paramsBig = new LinearLayout.LayoutParams(30, 30);
                    params.setMargins(10, 0, 10, 0);
                    iv.setLayoutParams(paramsBig);
                } else {
                    iv.setSelected(false);
                }
                dotView.add(iv);
                dotlayout.addView(iv);
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
