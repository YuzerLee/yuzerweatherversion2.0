package lee.yuzer.com.yuzerweather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Yuzer on 2017/12/7.
 */

public class Now {
    @SerializedName("tmp")
    public String temperature;

    public String cond_txt;

    public String fl;

    public String hum;

    public String pcpn;
}
