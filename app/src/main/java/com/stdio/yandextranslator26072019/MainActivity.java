package com.stdio.yandextranslator26072019;

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
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.pedromassango.doubleclick.DoubleClick;
import com.pedromassango.doubleclick.DoubleClickListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MobileAds.initialize(this, "ca-app-pub-3024705759390244~3123263346");
        initViews();
        setKeyboardVisibilityListener(this);
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
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
               // .addTestDevice("74F085F66C1A111A09027CC90265B556")
                .build();
        System.out.println("AAAAAAAAAAAA" + adRequest.isTestDevice(this));
        mAdView.loadAd(adRequest);
        setEditTextOnChangeListener();
        setDoubleTapListener();
        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                // Code to be executed when an ad request fails.
                Log.wtf("helppp", String.valueOf(errorCode));
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
            }

            @Override
            public void onAdClicked() {
                // Code to be executed when the user clicks on an ad.
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
            }
        });
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
