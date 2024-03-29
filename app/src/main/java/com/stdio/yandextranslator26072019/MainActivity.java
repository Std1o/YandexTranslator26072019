package com.stdio.yandextranslator26072019;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.pedromassango.doubleclick.DoubleClick;
import com.pedromassango.doubleclick.DoubleClickListener;
import com.yandex.metrica.YandexMetrica;
import com.yandex.metrica.YandexMetricaConfig;
import com.yandex.mobile.ads.AdEventListener;
import com.yandex.mobile.ads.AdRequest;
import com.yandex.mobile.ads.AdRequestError;
import com.yandex.mobile.ads.AdSize;
import com.yandex.mobile.ads.AdView;
import com.yandex.mobile.ads.InterstitialAd;
import com.yandex.mobile.ads.InterstitialEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements OnKeyboardVisibilityListener {

    //API запросов написан тут https://tech.yandex.ru/translate/doc/dg/reference/translate-docpage/
    private final String KEY = "trnsl.1.1.20180923T185306Z.c0c12e833188400b.99507f30e616465853f697c9b2961fac934fd5aa";// API ключ получить можно здесь https://translate.yandex.ru/developers/keys
    private final String URL = "https://translate.yandex.net/api/v1.5/tr.json/translate";

    EditText et;
    TextView tvTranslatedText, tvLanguage;
    FloatingActionButton FABClear;
    String result;
    String currentLang = "uz-ru";
    private AdView mAdView;
    String AD_API_KEY = "bcba2c29-77f1-4a77-a449-841e98c724ab";
    String BLOCK_ID = "R-M-472333-1";
    String BLOCK_ID_2 = "R-M-472333-2";
    InterstitialAd mInterstitialAd;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Creating an extended library configuration.
        YandexMetricaConfig config = YandexMetricaConfig.newConfigBuilder(AD_API_KEY).build();
        // Initializing the AppMetrica SDK.
        YandexMetrica.activate(getApplicationContext(), config);
        // Automatic tracking of user activity.
        YandexMetrica.enableActivityAutoTracking(getApplication());
        mInterstitialAd = new InterstitialAd(this);

        initViews();
        setKeyboardVisibilityListener(this);
    }

    private void initTimer() {
        disposables.add(getObservable()
                // Run on a background thread
                .subscribeOn(Schedulers.io())
                // Be notified on the main thread
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(getObserver()));
    }

    private Observable<? extends Long> getObservable() {
        return Observable.interval(300, 300, TimeUnit.SECONDS);
    }

    private DisposableObserver<Long> getObserver() {
        return new DisposableObserver<Long>() {

            @Override
            public void onNext(Long value) {
                mInterstitialAd.show();
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        };
    }

    private void setKeyboardVisibilityListener(final OnKeyboardVisibilityListener onKeyboardVisibilityListener) {
        final View parentView = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        parentView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            private boolean alreadyOpen;
            private final int defaultKeyboardHeightDP = 100;
            private final int EstimatedKeyboardDP = defaultKeyboardHeightDP + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? 48 : 0);
            private final Rect rect = new Rect();

            @Override
            public void onGlobalLayout() {
                int estimatedKeyboardHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, EstimatedKeyboardDP, parentView.getResources().getDisplayMetrics());
                parentView.getWindowVisibleDisplayFrame(rect);
                int heightDiff = parentView.getRootView().getHeight() - (rect.bottom - rect.top);
                boolean isShown = heightDiff >= estimatedKeyboardHeight;

                if (isShown == alreadyOpen) {
                    Log.i("Keyboard state", "Ignoring global layout change...");
                    return;
                }
                alreadyOpen = isShown;
                onKeyboardVisibilityListener.onVisibilityChanged(isShown);
            }
        });
    }


    @Override
    public void onVisibilityChanged(boolean visible) {
        if (visible) {
            FABClear.hide();
            mAdView.setVisibility(View.GONE);
        } else {
            FABClear.show();
            mAdView.setVisibility(View.VISIBLE);
        }
    }

    private void initViews() {
        et = findViewById(R.id.et);
        tvTranslatedText = findViewById(R.id.tvTranslatedText);
        tvLanguage = findViewById(R.id.tvLang);
        FABClear = findViewById(R.id.FABClear);
        setEditTextOnChangeListener();
        setDoubleTapListener();

        mInterstitialAd.setBlockId(BLOCK_ID);

        mAdView = (AdView) findViewById(R.id.banner_view);
        mAdView.setBlockId(BLOCK_ID);
        mAdView.setAdSize(AdSize.BANNER_320x50);
        final AdRequest adRequest = new AdRequest.Builder().build();

        // Регистрация слушателя для отслеживания событий, происходящих в баннерной рекламе.
        mAdView.setAdEventListener(new AdEventListener.SimpleAdEventListener() {
            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError error) {
                super.onAdFailedToLoad(error);
                System.out.println(error.getDescription());
            }

            @Override
            public void onAdLoaded() {
               System.out.println(adRequest.getParameters());
            }
        });
        mInterstitialAd.setInterstitialEventListener(new InterstitialEventListener.SimpleInterstitialEventListener() {
            @Override
            public void onInterstitialLoaded() {
                initTimer();
            }
        });

        // Загрузка объявления.
        mAdView.loadAd(adRequest);
        mInterstitialAd.loadAd(adRequest);
    }

    private void setDoubleTapListener() {
        tvTranslatedText.setOnClickListener( new DoubleClick(new DoubleClickListener() {
            @Override
            public void onSingleClick(View view) {

                // Single tap here.
            }

            @Override
            public void onDoubleClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) MainActivity.this.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("", tvTranslatedText.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, "Перевод скопирован", Toast.LENGTH_SHORT).show();
            }
        }));
    }



    public void swap(View view) {
        if (currentLang.equals("uz-ru")) {
            currentLang = "ru-uz";
            tvLanguage.setText("Русский - Узбекский");
        }
        else if (currentLang.equals("tg-ru")) {
            currentLang = "ru-tg";
            tvLanguage.setText("Русский - Таджикский");
        }
        else if (currentLang.equals("ru-uz")) {
            currentLang = "uz-ru";
            tvLanguage.setText("Узбекский - Русский");
        }
        else if (currentLang.equals("ru-tg")) {
            currentLang = "tg-ru";
            tvLanguage.setText("Таджикский - Русский");
        }
        et.setText(tvTranslatedText.getText().toString());
        et.setSelection(et.getText().length());
        translate();
    }

    public void clear(View view) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);

        alertDialogBuilder
                .setMessage("Очистить?")
                .setCancelable(false)
                .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        et.setText("");
                    }
                })
                .setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        alertDialogBuilder.setCancelable(true);

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    public void showSelectModeDialog(View view) {
        showDialog(0);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        final String[] mCatsName ={"Узбекский - Русский", "Таджикский - Русский"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите режим"); // заголовок для диалога

        builder.setItems(mCatsName, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (mCatsName[item].equals("Узбекский - Русский")) {
                    currentLang = "uz-ru";
                    tvLanguage.setText("Узбекский - Русский");
                    translate();
                }
                else if (mCatsName[item].equals("Таджикский - Русский")) {
                    currentLang = "tg-ru";
                    tvLanguage.setText("Таджикский - Русский");
                    translate();
                }
            }
        });
        builder.setCancelable(false);
        return builder.create();
    }

    private void setEditTextOnChangeListener() {
        et.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                if (et.getText().toString().isEmpty()) {
                    tvTranslatedText.setText("");
                }
            }

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                if (et.getText().toString().isEmpty()) {
                    tvTranslatedText.setText("");
                }
                else {
                    translate();
                }
            }
        });
    }

    private void translate() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            result = jsonObject.getString("text")
                                    .replace("[", "").replace("]", "");
                            result = result.replaceAll("\"", "");
                            tvTranslatedText.setText(result);
                        } catch (JSONException e) {
                            if (e.getMessage() != null) {
                                Log.e("JSONException аы", e.getMessage());
                            }
                        }
                    }

                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error.getMessage() != null) {
                            Log.e("VolleyError аы", error.getMessage());
                        }
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("key", KEY);
                params.put("text", et.getText().toString());
                params.put("lang", currentLang);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }
}
