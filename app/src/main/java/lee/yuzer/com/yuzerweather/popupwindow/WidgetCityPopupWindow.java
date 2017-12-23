package lee.yuzer.com.yuzerweather.popupwindow;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;

import lee.yuzer.com.weatherdemo.R;
import lee.yuzer.com.yuzerweather.OptionActivity;
import lee.yuzer.com.yuzerweather.db.StoredCity;

/**
 * Created by Yuzer on 2017/12/22.
 */

public class WidgetCityPopupWindow extends PopupWindow {
    private Context mContext;
    private float mShowAlpha = 0.88f;
    private Drawable mBackgroundDrawable;
    private View contentView;
    private RadioGroup mRadioGroup2;
    private Button cancelButton;
    private String SelectedItemText;
    private Handler mHandler;
    private LinearLayout layout;

    public WidgetCityPopupWindow(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
        initBasePopupWindow();
    }

    @Override
    public void setOutsideTouchable(boolean touchable) {
        super.setOutsideTouchable(touchable);
        if (touchable) {
            if (mBackgroundDrawable == null) {
                mBackgroundDrawable = new ColorDrawable(0x00000000);
            }
            super.setBackgroundDrawable(mBackgroundDrawable);
        } else {
            super.setBackgroundDrawable(null);
        }
    }

    @Override
    public void setBackgroundDrawable(Drawable background) {
        mBackgroundDrawable = background;
        setOutsideTouchable(isOutsideTouchable());
    }

    /**
     * 初始化BasePopupWindow的一些信息
     */
    private void initBasePopupWindow() {
        setAnimationStyle(android.R.style.Animation_Dialog);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        setOutsideTouchable(true);  //默认设置outside点击无响应
        setFocusable(true);
    }

    @Override
    public void setContentView(View contentView) {
        if (contentView != null) {
            contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            super.setContentView(contentView);
            addKeyListener(contentView);
            mRadioGroup2 = (RadioGroup) contentView.findViewById(R.id.widgetcity_radiogroup);
            cancelButton = (Button) contentView.findViewById(R.id.cancel_button);

            List<StoredCity> tmpCity = new ArrayList<>();
            tmpCity = DataSupport.findAll(StoredCity.class);
            int count = 0;
            for(StoredCity city : tmpCity){
                RadioButton radioButton = (RadioButton)LayoutInflater.from(mContext).inflate(R.layout.widgetcity_radiobutton, null);
                radioButton.setText(city.getName());
                radioButton.setId(count++);
                if(OptionActivity.WidgetSendInfo.equals(city.getName())){
                    radioButton.setChecked(true);
                }else{
                    radioButton.setChecked(false);
                }
                radioButton.setWidth(900);

                mRadioGroup2.addView(radioButton);

                //添加横线
                View view = new View(mContext);
                int height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getContext().getResources().getDisplayMetrics());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
                view.setLayoutParams(params);
                view.setBackgroundColor(Color.parseColor("#f0f0f0"));
                mRadioGroup2.addView(view);
            }

            SelectedItemText = OptionActivity.WidgetSendInfo;//如果没有点击RadioButton，要先赋值，防止空值出现

            mRadioGroup2.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    int count = 0;
                    List<StoredCity> tmpCity = new ArrayList<>();
                    tmpCity = DataSupport.findAll(StoredCity.class);
                    for(StoredCity city : tmpCity){
                        if(count == checkedId){
                            SelectedItemText = city.getName().toString();
                            break;
                        }
                        count++;
                    }
                    dismiss();
                }
            });

            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }
    }

    public Context getContext() {
        return mContext;
    }

    @Override
    public void showAtLocation(View parent, int gravity, int x, int y) {
        super.showAtLocation(parent, gravity, x, y);
        //showAnimator().start();
    }

    @Override
    public void showAsDropDown(View anchor) {
        super.showAsDropDown(anchor);
        //showAnimator().start();
    }

    @Override
    public void showAsDropDown(View anchor, int xoff, int yoff) {
        super.showAsDropDown(anchor, xoff, yoff);
        //showAnimator().start();
    }

    @Override
    public void showAsDropDown(View anchor, int xoff, int yoff, int gravity) {
        super.showAsDropDown(anchor, xoff, yoff, gravity);
        //showAnimator().start();
    }

    @Override
    public void dismiss() {
        super.dismiss();
        Message msg = new Message();
        msg.what = 0;
        msg.obj = SelectedItemText;
        mHandler.sendMessage(msg);
        //dismissAnimator().start();
    }

    /**
     * 为窗体添加outside点击事件
     */
    private void addKeyListener(View contentView) {
        if (contentView != null) {
            contentView.setFocusable(true);
            contentView.setFocusableInTouchMode(true);
            contentView.setOnKeyListener(new View.OnKeyListener() {

                @Override
                public boolean onKey(View view, int keyCode, KeyEvent event) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_BACK:
                            dismiss();
                            return true;
                        default:
                            break;
                    }
                    return false;
                }
            });
        }
    }
}
