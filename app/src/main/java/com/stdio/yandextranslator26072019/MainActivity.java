package com.stdio.yandextranslator26072019;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnKeyboardVisibilityListener {

    //API запросов написан тут https://tech.yandex.ru/translate/doc/dg/reference/translate-docpage/
    private final String KEY = "trnsl.1.1.20180923T185306Z.c0c12e833188400b.99507f30e616465853f697c9b2961fac934fd5aa";// API ключ получить можно здесь https://translate.yandex.ru/developers/keys
    private final String URL = "https://translate.yandex.net/api/v1.5/tr.json/translate";

    EditText et;
    TextView tvTranslatedText, tvLanguage;
    FloatingActionButton FABSwap, FABClear;
    String result;
    String currentLang = "uz-ru";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
            FABSwap.hide();
            FABClear.hide();
        } else {
            FABSwap.show();
            FABClear.show();
        }
    }

    private void initViews() {
        et = findViewById(R.id.et);
        tvTranslatedText = findViewById(R.id.tvTranslatedText);
        tvLanguage = findViewById(R.id.tvLang);
        FABSwap = findViewById(R.id.FABSwap);
        FABClear = findViewById(R.id.FABClear);
        setEditTextOnChangeListener();
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
            }

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                translate();
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
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }

                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(MainActivity.this, error.toString(), Toast.LENGTH_SHORT).show();
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
