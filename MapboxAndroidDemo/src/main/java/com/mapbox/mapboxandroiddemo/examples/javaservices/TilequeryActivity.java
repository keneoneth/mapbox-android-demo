package com.mapbox.mapboxandroiddemo.examples.javaservices;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.net.URL;
import java.util.List;

import okhttp3.HttpUrl;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;

/**
 * Use the Mapbox Tilequery API to retrieve information about Features on a Vector Tileset. More info about
 * the Tilequery API can be found at https://www.mapbox.com/api-documentation/#tilequery
 */
public class TilequeryActivity extends AppCompatActivity implements
  OnMapReadyCallback, PermissionsListener, MapboxMap.OnMapClickListener {

  private static final String TAG = "TilequeryActivity";
  private static final String RESULT_GEOJSON_SOURCE_ID = "RESULT_GEOJSON_SOURCE_ID";
  private static final String TILESET_ID = "mapbox.mapbox-streets-v7";
  private static final String LAYER_ID = "LAYER_ID";
  private static final int TILEQUERY_RADIUS = 100;
  private static final int TILEQUERY_LIMIT = 10;
  private static int index;

  private PermissionsManager permissionsManager;
  private MapboxMap mapboxMap;
  private MapView mapView;
  private TextView tilequeryResponseTextView;
  private GeoJsonSource resultBlueMarkerGeoJsonSource;
  private HttpUrl requestUrl;
  private SymbolLayer clickSymbolLayer;
  private SymbolLayer resultSymbolLayer;
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


    addClickLayer();
    addResultLayer();

    displayDeviceLocation();

    mapboxMap.addOnMapClickListener(this);
  }

  private void addClickLayer() {
    Bitmap clickSymbolIcon = BitmapFactory.decodeResource(
      TilequeryActivity.this.getResources(), R.drawable.red_marker);
    mapboxMap.addImage("CLICK-ICON-ID", clickSymbolIcon);

    GeoJsonSource clickGeoJsonSource = new GeoJsonSource("click-source", FeatureCollection.fromFeatures(new Feature[]{}));
    mapboxMap.addSource(clickGeoJsonSource);

    clickSymbolLayer = new SymbolLayer("click-layer", "click-source");
    clickSymbolLayer.setProperties(
      iconImage("CLICK-ICON-ID"),
      iconOffset(new Float[]{0f,-12f}),
      iconIgnorePlacement(true),
      iconAllowOverlap(true)
    );
    mapboxMap.addLayer(clickSymbolLayer);
  }

  private void addResultLayer() {

    // Add the marker image to map
    Bitmap resultSymbolIcon = BitmapFactory.decodeResource(
      TilequeryActivity.this.getResources(), R.drawable.blue_marker);
    mapboxMap.addImage("RESULT-ICON-ID", resultSymbolIcon);

    resultSymbolLayer = new SymbolLayer(LAYER_ID, RESULT_GEOJSON_SOURCE_ID);
    resultSymbolLayer.setProperties(
      iconImage("RESULT-ICON-ID"),
      iconOffset(new Float[]{0f,-12f}),
      iconIgnorePlacement(true),
      iconAllowOverlap(true)
    );
    mapboxMap.addLayer(resultSymbolLayer);
  }

  @Override
  public void onMapClick(@NonNull LatLng point) {
    /*GeoJsonSource source = mapboxMap.getSourceAs("click-source");
    if (source != null) {
      source.setGeoJson(Feature.fromGeometry(Point.fromLngLat(point.getLongitude(), point.getLatitude())));
    }*/
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
     requestUrl = new HttpUrl.Builder()
      .scheme("https")
      .host("api.mapbox.com")
      .addPathSegment("v4")
      .addPathSegment(TILESET_ID)
      .addPathSegment("tilequery")
      .addPathSegment(String.valueOf(point.getLongitude())+
        ","+String.valueOf(point.getLatitude())+".json")
      .addQueryParameter("radius", String.valueOf(TILEQUERY_RADIUS))
      .addQueryParameter("limit", String.valueOf(TILEQUERY_LIMIT))
      .addQueryParameter("geometry", String.valueOf("point"))
      .addQueryParameter("access_token", getString(R.string.access_token))
      .build();

    Log.d(TAG, "makeTilequeryApiCall: requestUrl = " + requestUrl);

    try {

      String randomNum = String.valueOf(Math.random());

      // Retrieve GeoJSON information from the Mapbox Tilequery API
      resultBlueMarkerGeoJsonSource = new GeoJsonSource(RESULT_GEOJSON_SOURCE_ID, new URL(requestUrl.toString()));
      mapboxMap.addSource(resultBlueMarkerGeoJsonSource);
      Log.d(TAG, "makeTilequeryApiCall: result geojsonsource added");



    } catch (Throwable throwable) {
      Log.e(TAG, "Couldn't add GeoJsonSource to map", throwable);
    }
  }

  /**
   * Use the Maps SDK's LocationComponent to display the device location on the map
   */
  @SuppressWarnings( {"MissingPermission"})
  private void displayDeviceLocation() {
    // Check if permissions are enabled and if not request
    if (PermissionsManager.areLocationPermissionsGranted(this)) {

      // Get an instance of the component
      LocationComponent locationComponent = mapboxMap.getLocationComponent();

      // Activate with options
      locationComponent.activateLocationComponent(this);

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
}