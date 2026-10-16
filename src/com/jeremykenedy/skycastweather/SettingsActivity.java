package com.jeremykenedy.skycastweather;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.List;

public final class SettingsActivity extends Activity {
  private final Handler handler = new Handler(Looper.getMainLooper());
  private WeatherStore store;
  private TextView location, status;
  private WeatherClient search;
  private boolean alive;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    alive = true;
    store = new WeatherStore(this);
    ScrollView scroll = new ScrollView(this);
    LinearLayout content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setPadding(64, 36, 64, 50);
    scroll.addView(content);
    TextView title = new TextView(this);
    title.setText("Skycast Weather");
    title.setTextSize(30);
    content.addView(title);
    TextView intro = new TextView(this);
    intro.setText(
        "Live model forecasts and a continuously moving sky. Choose a city; no device location"
            + " permission is used.");
    content.addView(intro);
    Button preview = new Button(this);
    preview.setText("Preview weather");
    preview.setOnClickListener(v -> startActivity(new Intent(this, PreviewActivity.class)));
    content.addView(preview);
    location = new TextView(this);
    updateLocation();
    content.addView(location);
    EditText city = new EditText(this);
    city.setSingleLine(true);
    city.setHint("Search city or town");
    content.addView(city);
    Button find = new Button(this);
    find.setText("Find location");
    find.setOnClickListener(v -> search(city.getText().toString()));
    content.addView(find);
    status = new TextView(this);
    content.addView(status);
    choice(
        content,
        "Temperature and wind units",
        "units",
        new String[] {"metric", "imperial"},
        new String[] {"Celsius / km/h", "Fahrenheit / mph"});
    choice(
        content,
        "Forecast days",
        "days",
        new String[] {"1", "3", "5", "7"},
        new String[] {"1 day", "3 days", "5 days", "7 days"});
    choice(
        content,
        "Cloud motion",
        "motion",
        new String[] {"gentle", "flowing"},
        new String[] {"Gentle", "Flowing"});
    TextView notice = new TextView(this);
    notice.setText(
        "Weather and city search use Open-Meteo. Requests send the selected coordinates or search"
            + " text and your network address. No app analytics, identifiers or diagnostic"
            + " reporting. Forecasts refresh while the scene is open; failed refreshes retain dated"
            + " cached content. Open-Meteo's free endpoint is for noncommercial use. Forecasts are"
            + " not emergency warnings.");
    notice.setPadding(0, 24, 0, 12);
    content.addView(notice);
    Button credits = new Button(this);
    credits.setText("Weather sources and license");
    credits.setOnClickListener(
        v ->
            new AlertDialog.Builder(this)
                .setTitle("Weather sources and license")
                .setMessage(
                    "Weather: Open-Meteo, https://open-meteo.com/\n"
                        + "Forecast data: CC BY 4.0. City search: GeoNames via Open-Meteo, CC BY"
                        + " 4.0.\n\n"
                        + "Data is model derived. Provider terms:"
                        + " https://open-meteo.com/en/terms\n\n"
                        + "Skycast Weather source code: Apache License 2.0. Copyright 2026 Jeremy"
                        + " Kenedy.\n"
                        + "https://github.com/jeremykenedy/skycast-weather")
                .setPositiveButton("Close", null)
                .show());
    content.addView(credits);
    setContentView(scroll);
    preview.setFocusableInTouchMode(true);
    preview.requestFocus();
  }

  private void updateLocation() {
    WeatherLocation selected = store.location();
    location.setText(selected == null ? "No location selected" : "Location: " + selected.name);
  }

  private void choice(
      LinearLayout content, String label, String key, String[] values, String[] titles) {
    TextView text = new TextView(this);
    text.setText(label);
    content.addView(text);
    Spinner spinner = new Spinner(this);
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, titles);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(adapter);
    for (int i = 0; i < values.length; i++)
      if (values[i].equals(store.choice(key))) spinner.setSelection(i);
    spinner.setOnItemSelectedListener(
        new android.widget.AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(
              android.widget.AdapterView<?> parent, View view, int position, long id) {
            store.choice(key, values[position]);
          }

          @Override
          public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    content.addView(spinner);
  }

  private void search(String query) {
    if (search != null) search.close();
    WeatherClient request = new WeatherClient();
    search = request;
    status.setText("Searching locations");
    new Thread(
            () -> {
              try {
                List<WeatherLocation> results =
                    WeatherLocation.searchResults(request.findLocations(query));
                handler.post(
                    () -> {
                      if (!alive || search != request) return;
                      if (results.isEmpty()) {
                        status.setText("No matching city found");
                        return;
                      }
                      status.setText("");
                      String[] names = new String[results.size()];
                      for (int i = 0; i < names.length; i++) names[i] = results.get(i).name;
                      new AlertDialog.Builder(this)
                          .setTitle("Choose location")
                          .setItems(
                              names,
                              (dialog, index) -> {
                                store.location(results.get(index));
                                updateLocation();
                              })
                          .setNegativeButton("Cancel", null)
                          .show();
                    });
              } catch (Exception unavailable) {
                handler.post(
                    () -> {
                      if (alive && search == request)
                        status.setText(
                            "Location search unavailable. Enter at least two characters and try"
                                + " again.");
                    });
              } finally {
                request.close();
              }
            },
            "weather-city-search")
        .start();
  }

  @Override
  protected void onDestroy() {
    alive = false;
    if (search != null) search.close();
    handler.removeCallbacksAndMessages(null);
    super.onDestroy();
  }

  @Override
  protected void onStop() {
    WeatherClient request = search;
    search = null;
    if (request != null) request.close();
    status.setText("");
    super.onStop();
  }
}
