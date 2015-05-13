
package com.tg.patternlock.sample;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.tg.androidpatternlock.LockView;
import com.tg.androidpatternlock.LockView.PatternPasswordStorageFetcher;

public class PatternLockActivity extends Activity implements
        LockView.PatternListener {

    public static final String ext_key_launched_for_session_expired = "ext_key_need_renew_token";
    public static final String ext_key_is_altering = "ext_key_is_altering";

    private static final String LOG_TAG = "PatternLockActivity";
    private static final boolean DEBUG = true;
    protected static final int PATTERN_PW_MIN_LENGTH = 4;
    private static final String prefs_key_pattern_pw = "prefs_key_pattern_pw";

    private TextView mNameView;
    private LockView mLockView;
    private TextView mLableView;
    private String mEncryptedPatternStr;

    private boolean mIsAltering = false;
    private boolean mIsAlteringFromThisActivity = false;
    private boolean mIsInCreatingMode = false;

    private View mBottomView;
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pattern_lock);

        Intent intent = getIntent();
        mIsAltering = intent.getBooleanExtra(ext_key_is_altering, false);

        mNameView = (TextView) findViewById(R.id.name);
        mNameView.setText("John Doe");

        Button btnReset = (Button) findViewById(R.id.reset_pattern_pw);
        btnReset.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                reset();
            }

        });

        Button btnClear = (Button) findViewById(R.id.clear_pattern_pw);
        btnClear.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                clear();
            }
            
        });

        mLableView = (TextView) findViewById(R.id.label);

        mLockView = (LockView) findViewById(R.id.lockview);
        mLockView.setSkipPolicy(LockView.SkipPolicy_AutoConnect);
        mLockView.setPathColorCorrect(getResources().getColor(R.color.text_light_white));
        mLockView.setCircleColorNormal(getResources().getColor(R.color.text_light_white));
        mLockView.setCircleColorCorrect(getResources().getColor(R.color.text_light_white));
        mLockView.setUiStyle(LockView.UiStyle_Circle);
        mLockView.setPatternListener(this);
        mLockView
                .setPatternPasswordStorageFetcher(new PatternPasswordStorageFetcher() {

                    @Override
                    public ArrayList<Integer> fetch() {
                        return null;
                    }

                    @Override
                    public String fetchEncrypted() {
                        return mEncryptedPatternStr;
                    }

                });

        mLockView.setComplexityChecker(new LockView.ComplexityChecker() {

            @Override
            public boolean check(ArrayList<Integer> pattern) {
                return pattern != null && pattern.size() >= PATTERN_PW_MIN_LENGTH;
            }

        });

        mPrefs = getSharedPreferences("pattern_lock_sample",
                Context.MODE_PRIVATE);

        mEncryptedPatternStr = getPatternPasswordFromSharedPreference();

        setup();
    }

    protected void clear() {
        saveEncryptedPatternStr("");
        Toast.makeText(getApplicationContext(), "pattern pw cleared!", Toast.LENGTH_LONG).show();
    }

    private void setup() {
        mIsInCreatingMode = mIsAltering
                || TextUtils.isEmpty(mEncryptedPatternStr);
        setPatternLockWorkMode(mIsInCreatingMode);

        mBottomView = findViewById(R.id.bottom_view);
        setupViews();
    }

    private void setupViews() {
        if (mIsInCreatingMode) {
            mNameView.setVisibility(View.GONE);
            mBottomView.setVisibility(View.GONE);
        } else {
            mNameView.setVisibility(View.VISIBLE);
            mBottomView.setVisibility(View.VISIBLE);
        }
    }

    private void reset() {
        mIsAltering = true;
        mIsAlteringFromThisActivity = true;
        setup();
    }

    private void setPatternLockWorkMode(boolean create) {
        mLockView.setWorkMode(create ? LockView.WorkMode_Creating
                : LockView.WorkMode_Inputing);
        mLableView.setText(create ? R.string.draw_pattern
                : R.string.input_pattern_pw);
        mLableView.setTextColor(getResources().getColor(
                R.color.text_light_white));
    }

    @Override
    public void onCreatingInputOnce(ArrayList<Integer> pattern, boolean complexityCheckPass) {
        if (complexityCheckPass) {
            mLableView.setText(R.string.draw_pattern_again);
            mLableView.setTextColor(getResources().getColor(
                    R.color.text_light_white));
        } else {
            mLableView.setTextColor(Color.RED);
            mLableView.setText(R.string.pattern_complexity_tips);
            shakeTheView(mLableView);
        }
    }

    @Override
    public void onCreatingInputComplete(boolean match,
            String encryptedPatternStr) {
        if (match) {
            mLableView.setText(R.string.successfully_set);
            mLableView.setTextColor(Color.GREEN);

            saveEncryptedPatternStr(encryptedPatternStr);

            if (mIsAltering && !mIsAlteringFromThisActivity) {
                finish();
                overridePendingTransition(0, android.R.anim.fade_out);
            } else {
                gotoMainActivity();
            }
        } else {
            mLableView.setTextColor(Color.RED);
            mLableView.setText(R.string.pattern_not_match);
            shakeTheView(mLableView);
        }

    }

    private void saveEncryptedPatternStr(String encryptedPatternStr) {
        mEncryptedPatternStr = encryptedPatternStr;
        savePatternPasswordToSharedPreference(encryptedPatternStr);
    }

    @Override
    public void onInputCheckResult(boolean correct) {
        if (correct) {
            mLableView.setText(R.string.successfully_unlocked);
            mLableView.setTextColor(Color.GREEN);
            gotoMainActivity();
        } else {
            mLableView.setTextColor(Color.RED);
            mLableView.setText(R.string.pattern_pw_wrong);
            shakeTheView(mLableView);
        }
    }

    private void shakeTheView(View v) {
        if (v != null) {
            Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
            v.startAnimation(shake);
        }
    }

    // you can go to your main activity or elsewhere you need to
    private void gotoMainActivity() {
        // Intent i = new Intent(this, MainActivity.class);
        // startActivity(i);
        // finish();
        Toast.makeText(getApplicationContext(), "you can goto your main acitivy now",
                Toast.LENGTH_LONG).show();
    }

    private void savePatternPasswordToSharedPreference(String patternPw) {
        mPrefs.edit().putString(prefs_key_pattern_pw, patternPw).commit();
    }

    private String getPatternPasswordFromSharedPreference() {
        String pw = null;
        pw = mPrefs.getString(prefs_key_pattern_pw, null);
        if (DEBUG) {
            Log.d(LOG_TAG, "pattern str got from prefs: "
                    + pw);
        }
        return pw;
    }
}
