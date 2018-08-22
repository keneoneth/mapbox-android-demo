package com.mapbox.mapboxandroiddemo.examples.javaservices;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxandroiddemo.R;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.utils.BitmapUtils;

import java.net.URL;
import java.util.List;

import static com.mapbox.mapboxsdk.style.expressions.Expression.eq;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconTranslate;

/**
 * Use the Mapbox Tilequery API to retrieve information about Features on a Vector Tileset. More info about
 * the Tilequery API can be found at https://www.mapbox.com/api-documentation/#tilequery
 */
public class TilequeryActivity extends AppCompatActivity implements
  OnMapReadyCallback, PermissionsListener, MapboxMap.OnMapClickListener {

  private static final String TAG = "TilequeryActivity";
  private static final String GEOJSON_SOURCE_ID = "GEOJSON_SOURCE_ID";
  private static final String TILESET_ID = "mapbox.mapbox-streets-v7";
  private static final int TILEQUERY_RADIUS = 100;
  private static final int TILEQUERY_LIMIT = 10;
  private static int index;

  private PermissionsManager permissionsManager;
  private MapboxMap mapboxMap;
  private MapView mapView;
  private TextView tilequeryResponseTextView;
  private GeoJsonSource resultBlueMarkerGeoJsonSource;
  private String requestUrl;

  @Override

  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    index = 0;


    // Mapbox access token is configured here. This needs to be called either in your application
    // object or in the same activity which contains the mapview.
    Mapbox.getInstance(this, getString(R.string.access_token));

    // This contains the MapView in XML and needs to be called after the access token is configured.
    setContentView(R.layout.activity_javaservices_tilequery);

    tilequeryResponseTextView = findViewById(R.id.tilequery_response_info_textview);

    mapView = findViewById(R.id.mapView);
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);
  }

  @SuppressWarnings( {"MissingPermission"})
  @Override
  public void onMapReady(MapboxMap mapboxMap) {
    TilequeryActivity.this.mapboxMap = mapboxMap;
    displayDeviceLocation();
    initResultSource();
    initResultSymbolLayer();
    makeTilequeryApiCall(new LatLng(mapboxMap.getLocationComponent().getLastKnownLocation().getLatitude(),
      mapboxMap.getLocationComponent().getLastKnownLocation().getLongitude()));
    mapboxMap.addOnMapClickListener(this);
  }

  /**
   * Add a GeoJson source to the map for the SymbolLayer icons to represent the Tilequery response locations
   */
  private void initResultSource() {
    GeoJsonSource resultGeoJsonSource = new GeoJsonSource(GEOJSON_SOURCE_ID,
      FeatureCollection.fromFeatures(new Feature[] {}));
    mapboxMap.addSource(resultGeoJsonSource);
  }

  /**
   * Add a SymbolLayer for icons that will represent the Tilequery API response locations
   */
  private void initResultSymbolLayer() {
    mapboxMap.addImage("RESULT-ICON-ID", BitmapUtils.getBitmapFromDrawable(
      getResources().getDrawable(R.drawable.blue_marker)));
    SymbolLayer resultSymbolLayer = new SymbolLayer("LAYER_ID", GEOJSON_SOURCE_ID);
    resultSymbolLayer.withProperties(
      iconImage("RESULT-ICON-ID"),
      iconSize(8f),
      iconIgnorePlacement(true),
      iconAllowOverlap(true),
      iconTranslate(new Float[] {0f, 8f})
    );
    resultSymbolLayer.setFilter(eq(literal("$type"), literal("Point")));
    mapboxMap.addLayer(resultSymbolLayer);
  }

  @Override
  public void onMapClick(@NonNull LatLng point) {
    makeTilequeryApiCall(point);
  }

  /**
   * Build and use the Tilequery API request URL
   *
   * @param point
   */
  private void makeTilequeryApiCall(@NonNull LatLng point) {
    /*MapboxTilequery tilequery = MapboxTilequery.builder()
      .accessToken(getString(R.string.access_token))
      .mapIds("mapbox.mapbox-streets-v7")
      .query(point)
      .radius(1000)
      .limit(30)
      .geometry("point")
      .dedupe(true)
      .layers("poi-label,admin-state-province,building,poi-label,country-label")
      .build();

    tilequery.enqueueCall(new Callback<FeatureCollection>() {
      @Override
      public void onResponse(Call<FeatureCollection> call, Response<FeatureCollection> response) {
        tilequeryResponseTextView.setText(response.body().toJson());
      }

      @Override
      public void onFailure(Call<FeatureCollection> call, Throwable t) {
        Timber.d("Request failed: " + t.getMessage());
      }
    });*/

    // Build a request URL to get information from the Mapbox Tilequery API. Notice that the geometry parameter
    // is set to return Point geometries.
    requestUrl = "https://api.mapbox.com/v4/"
      + TILESET_ID
      + "/tilequery/"
      + point.getLongitude() + ","
      + point.getLatitude()
      + ".json?radius=" + String.valueOf(TILEQUERY_RADIUS)
      + "&limit=" + String.valueOf(TILEQUERY_LIMIT)
      + "&geometry=point&access_token=" + getString(R.string.access_token);

    Log.d(TAG, "makeTilequeryApiCall: requestUrl = " + requestUrl);

    try {

      // Retrieve GeoJSON information from the Mapbox Tilequery API
      resultBlueMarkerGeoJsonSource = new GeoJsonSource(
        GEOJSON_SOURCE_ID, new URL(requestUrl));
      // Add the GeoJsonSource to map
      mapboxMap.addSource(resultBlueMarkerGeoJsonSource);

    } catch (Throwable throwable) {
      Log.e(TAG, "Couldn't add GeoJsonSource to map", throwable);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  @Override
  public void onExplanationNeeded(List<String> permissionsToExplain) {
    Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
  }

  @Override
  public void onPermissionResult(boolean granted) {
    if (granted) {
      displayDeviceLocation();
    } else {
      Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
      finish();
    }
  }

  // Add the mapView lifecycle to the activity's lifecycle methods
  @Override
  public void onResume() {
    super.onResume();
    mapView.onResume();
  }

  @Override
  protected void onStart() {
    super.onStart();
    mapView.onStart();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mapView.onStop();
  }

  @Override
  public void onPause() {
    super.onPause();
    mapView.onPause();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mapView.onDestroy();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }

  /**
   * Use the Maps SDK's LocationComponent to display the device location on the map
   */
  @SuppressWarnings( {"MissingPermission"})
  private void displayDeviceLocation() {
    // Check if permissions are enabled and if not request
    if (PermissionsManager.areLocationPermissionsGranted(this)) {

      LocationComponentOptions options = LocationComponentOptions.builder(this)
        .trackingGesturesManagement(true)
        .accuracyColor(ContextCompat.getColor(this, R.color.mapboxGreen))
        .build();

      // Get an instance of the component
      LocationComponent locationComponent = mapboxMap.getLocationComponent();

      // Activate with options
      locationComponent.activateLocationComponent(this, options);

      // Enable to make component visible
      locationComponent.setLocationComponentEnabled(true);

      // Set the component's camera mode
      locationComponent.setCameraMode(CameraMode.TRACKING);
      locationComponent.setRenderMode(RenderMode.COMPASS);
    } else {
      permissionsManager = new PermissionsManager(this);
      permissionsManager.requestLocationPermissions(this);
    }
  }
}