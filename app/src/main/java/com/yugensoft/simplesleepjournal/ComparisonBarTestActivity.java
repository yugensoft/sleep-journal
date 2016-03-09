package com.yugensoft.simplesleepjournal;

import android.os.Bundle;
import android.app.Activity;

import com.yugensoft.simplesleepjournal.customviews.SleepComparisonBar;

public class ComparisonBarTestActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comparison_bar_test);

        SleepComparisonBar bar = (SleepComparisonBar)findViewById(R.id.comparison_bar);

        bar.setTimeDifference(-10*25*60*1000);
    }

}
