package lee.yuzer.com.yuzerweather;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lee.yuzer.com.weatherdemo.R;
import lee.yuzer.com.yuzerweather.db.City;
import lee.yuzer.com.yuzerweather.db.County;
import lee.yuzer.com.yuzerweather.db.Province;
import lee.yuzer.com.yuzerweather.db.StoredCity;
import lee.yuzer.com.yuzerweather.gson.AQI;
import lee.yuzer.com.yuzerweather.gson.Weather;
import lee.yuzer.com.yuzerweather.util.HttpUtil;
import lee.yuzer.com.yuzerweather.util.RequestHelper;
import lee.yuzer.com.yuzerweather.util.Utility;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Yuzer on 2017/12/6.
 */

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    private ProgressDialog mProgressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView mListView;
    private ArrayAdapter<String> mAdapter;
    private List<String> dataList = new ArrayList<>();
    private List<Province> mProvinceList;
    private List<City> mCityList;
    private List<County> mCountyList;
    private List<StoredCity> mStoredCities;
    private Province selectedProvince;
    private City selectedCity;
    private int currentLevel;
    private Button optionButton;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText = (TextView) view.findViewById(R.id.title_textview);//中央标题
        backButton = (Button) view.findViewById(R.id.back_button);//返回按钮
        optionButton = (Button) view.findViewById(R.id.option_button);//设置按钮
        mListView = (ListView) view.findViewById(R.id.area_listview);
        mAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        mListView.setAdapter(mAdapter);
        mStoredCities = new ArrayList<>();//初始化数据库列表
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //注册Item监听事件
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = mProvinceList.get(position);
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = mCityList.get(position);
                    queryCounties();
                } else if (currentLevel == LEVEL_COUNTY) {
                    //ViewPager版本
                    String selectedCityName = mCountyList.get(position).getCountyName();//所点击的城市名称
                    mStoredCities = DataSupport.where("name = ?", selectedCityName).find(StoredCity.class);


                    if (getActivity() instanceof MainActivity) {
                        //如果是从MainActivity来的，则说明是第一次进入，数据库中一定没有数据，
                        //进行网络申请在跳转
                        RequestHelper.requestAllWeatherInfoAndStored(selectedCityName);
                        while (true) {
                            mStoredCities = DataSupport.where("name = ?", selectedCityName).find(StoredCity.class);
                            if (mStoredCities.size() > 0) {
                                if (mStoredCities.get(0).getAqicontent() != null && mStoredCities.get(0).getContent() != null) {
                                    //网络申请成功并且成功存入数据库，则直接进入WeatherActivity中并跳转到该页
                                    Intent intent = new Intent(getActivity(), WeatherViewPagerActivity.class);
                                    intent.putExtra("selectedcityname", selectedCityName);
                                    startActivity(intent);
                                    getActivity().finish();
                                    break;
                                }
                            }
                        }
                    } else if (getActivity() instanceof WeatherViewPagerActivity) {
                        //如果是从WeatherViewPagerActivity中来的，则通过mStoredCities的大小来判断点击的城市
                        //是否已经在ViewPager中显示了
                        queryProvinces();//让选择城市的页面恢复到省级别
                        if (mStoredCities.size() > 0) {
                            //在数据库查询到所点击的城市，说明该城市已经在ViewPager中，直接进入WeatherActivity中并跳转到该页
                            WeatherViewPagerActivity activity = (WeatherViewPagerActivity) getActivity();
                            activity.mDrawerLayout.closeDrawers();
                            int index = 0;
                            List<StoredCity> ccity = DataSupport.findAll(StoredCity.class);
                            for (StoredCity city : ccity) {
                                if (city.getName().equals(selectedCityName))
                                    break;
                                index++;
                            }
                            activity.mViewPager.setCurrentItem(index);
                        } else {
                            //在数据库中没有查询到该城市的从信息，说明该城市并没有在ViewPager中，则先进行网络申请数据并存入数据库中
                            // ，然后跳转到WeatherActivity读取数据库，进行显示
                            RequestHelper.requestAllWeatherInfoAndStored(selectedCityName);
                            WeatherViewPagerActivity activity = (WeatherViewPagerActivity) getActivity();
                            while (true) {
                                mStoredCities = DataSupport.where("name = ?", selectedCityName).find(StoredCity.class);
                                if (mStoredCities.size() > 0) {
                                    if (mStoredCities.get(0).getAqicontent() != null && mStoredCities.get(0).getContent() != null) {
                                        //网络申请成功并且成功存入数据库，则直接进入WeatherActivity中并跳转到该页
                                        activity.mDrawerLayout.closeDrawers();
                                        Weather weather = Utility.handleWeatherResponse(mStoredCities.get(0).getContent());
                                        if (mStoredCities.get(0).getAqicontent().equals("没有权限")) {
                                            activity.addViewPagerItemUI(weather, null);
                                        } else {
                                            AQI aqi = Utility.handleWeatherAQIResponse(mStoredCities.get(0).getAqicontent());
                                            activity.addViewPagerItemUI(weather, aqi);
                                        }
                                        activity.mViewPager.setCurrentItem(DataSupport.findAll(StoredCity.class).size());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });

        //注册返回按钮事件
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_COUNTY) {
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    queryProvinces();
                    ;
                }
            }
        });

        //注册设置按钮事件
        optionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WeatherViewPagerActivity activity = (WeatherViewPagerActivity) getActivity();
                activity.mDrawerLayout.closeDrawers();
                Intent intent = new Intent(activity, OptionActivity.class);
                startActivity(intent);
            }
        });

        //首先刷新省级列表
        queryProvinces();
    }

    /**
     * 查询全国的省份信息，首先从数据库中查询，如果数据库中没有则从服务器上取数据
     */
    private void queryProvinces() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        mProvinceList = DataSupport.findAll(Province.class);
        if (mProvinceList.size() > 0) {
            dataList.clear();
            for (Province province : mProvinceList) {
                dataList.add(province.getProvinceName());
            }
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        } else {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");
        }
    }

    /**
     * 查询选中的省份所包含的城市信息，首先从数据库中查找，如果数据库中没有则从网上获取
     */
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        mCityList = DataSupport.where("provinceid = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (mCityList.size() > 0) {
            dataList.clear();
            for (City city : mCityList) {
                dataList.add(city.getCityName());
            }
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
        }
    }

    /**
     * 查询所选中城市的所有县信息，先从数据库中查询，如果数据库中没有则从网上获取信息
     */
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        mCountyList = DataSupport.where("cityid = ?", String.valueOf(selectedCity.getId())).find(County.class);
        if (mCountyList.size() > 0) {
            dataList.clear();
            for (County county : mCountyList) {
                dataList.add(county.getCountyName());
            }
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromServer(address, "county");
        }
    }

    /**
     * 根据传入的地址和类型从服务器上查询省市县的信息
     *
     * @param address
     * @param type
     */
    private void queryFromServer(String address, final String type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if ("province".equals(type)) {
                    result = Utility.handleProvinceResponse(responseText);
                } else if ("city".equals(type)) {
                    result = Utility.handleCityResponse(responseText, selectedProvince.getId());
                } else if ("county".equals(type)) {
                    result = Utility.handleCountyResponse(responseText, selectedCity.getId());
                }

                if (result) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)) {
                                queryProvinces();
                                ;
                            } else if ("city".equals(type)) {
                                queryCities();
                            } else if ("county".equals(type)) {
                                queryCounties();
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * 显示进度对话框
     */
    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage("正在加载。。。");
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        mProgressDialog.show();
    }

    /**
     * 关闭对话框
     */
    private void closeProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }
}
