package com.jeremykenedy.skycastweather;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public final class SettingsProvider extends ContentProvider {
  private WeatherStore store;

  @Override
  public boolean onCreate() {
    store = new WeatherStore(getContext());
    return true;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
    MatrixCursor cursor = new MatrixCursor(new String[] {"key", "value"});
    if ("/schema".equals(uri.getPath())) {
      cursor.addRow(new Object[] {"units", "metric,imperial"});
      cursor.addRow(new Object[] {"days", "1,3,5,7"});
      cursor.addRow(new Object[] {"motion", "gentle,flowing"});
      cursor.addRow(new Object[] {"show_weather", "true,false"});
      cursor.addRow(
          new Object[] {
            "location",
            "Update location_name,latitude,longitude together; choose explicit coordinates"
          });
    } else if ("/settings".equals(uri.getPath())) {
      for (String key : new String[] {"units", "days", "motion", "show_weather"})
        cursor.addRow(new Object[] {key, store.choice(key)});
      WeatherLocation location = store.location();
      cursor.addRow(new Object[] {"location", location == null ? "" : location.name});
    } else throw new IllegalArgumentException("Unknown settings path");
    return cursor;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] args) {
    if (!"/settings".equals(uri.getPath()) || values == null || values.size() == 0)
      throw new IllegalArgumentException("Use /settings with supported values");
    if (values.containsKey("clear_location")) {
      if (values.size() != 1 || !Boolean.TRUE.equals(values.getAsBoolean("clear_location")))
        throw new IllegalArgumentException("Use clear_location=true by itself");
      store.clearLocation();
      return 1;
    }
    boolean hasLocation =
        values.containsKey("location_name")
            || values.containsKey("latitude")
            || values.containsKey("longitude");
    WeatherLocation location = null;
    if (hasLocation) {
      if (!values.containsKey("location_name")
          || !values.containsKey("latitude")
          || !values.containsKey("longitude"))
        throw new IllegalArgumentException("Location requires name and both coordinates");
      location =
          new WeatherLocation(
              values.getAsString("location_name"),
              Double.parseDouble(values.getAsString("latitude")),
              Double.parseDouble(values.getAsString("longitude")));
    }
    for (String key : values.keySet()) {
      if ("location_name".equals(key) || "latitude".equals(key) || "longitude".equals(key))
        continue;
      if (!WeatherOptions.valid(key, values.getAsString(key)))
        throw new IllegalArgumentException("Unsupported weather setting");
    }
    if (location != null) store.location(location);
    for (String key : new String[] {"units", "days", "motion", "show_weather"})
      if (values.containsKey(key)) store.choice(key, values.getAsString(key));
    return values.size();
  }

  @Override
  public String getType(Uri uri) {
    return "vnd.android.cursor.dir/vnd.skycastweather.settings";
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    throw new UnsupportedOperationException("Use update");
  }

  @Override
  public int delete(Uri uri, String selection, String[] args) {
    throw new UnsupportedOperationException("Settings cannot be deleted");
  }
}
