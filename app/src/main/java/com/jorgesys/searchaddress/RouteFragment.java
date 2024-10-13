package com.jorgesys.searchaddress;

import android.app.ProgressDialog;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import java.util.Arrays;
import java.util.List;

public class RouteFragment extends Fragment implements RoutingListener {

    private static final String TAG = "RouteFragment";
    private static final LatLngBounds BOUNDS_CDMX = new LatLngBounds(
            new LatLng(19.20493613389559, -99.37448143959045),
            new LatLng(19.626470044363725, -98.78848314285278)
    );
    private static final LatLng CDMX_CENTER = new LatLng(19.4324512, -99.1329994);

    protected GoogleMap map;
    protected LatLng start;
    protected LatLng end;
    AutoCompleteTextView starting;
    AutoCompleteTextView destination;
    ImageView send;
    private PlacesAutoCompleteAdapter mAdapter;
    private ProgressDialog progressDialog;
    private Polyline polyline;
    private Button estimate;
    private CardView cardView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_directions, container, false);
        starting = rootView.findViewById(R.id.start);
        destination = rootView.findViewById(R.id.destination);
        send = rootView.findViewById(R.id.send);
        cardView = rootView.findViewById(R.id.cardview);

        // Initialize Places
        if (!Places.isInitialized()) {
            Places.initialize(getContext(), "YOUR_API_KEY");
        }
        PlacesClient placesClient = Places.createClient(getContext());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);

        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction().replace(R.id.map, mapFragment).commit();
        }

        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                map = googleMap;
                CameraUpdate center = CameraUpdateFactory.newLatLng(CDMX_CENTER);
                CameraUpdate zoom = CameraUpdateFactory.zoomTo(16);
                map.moveCamera(center);
                map.animateCamera(zoom);
            }
        });

        mAdapter = new PlacesAutoCompleteAdapter(getActivity(), android.R.layout.simple_list_item_1, placesClient, BOUNDS_CDMX, null);
        starting.setAdapter(mAdapter);
        destination.setAdapter(mAdapter);

        starting.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final PlacesAutoCompleteAdapter.PlaceAutocomplete item = mAdapter.getItem(position);
                final String placeId = String.valueOf(item.placeId);

                List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);
                FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();

                placesClient.fetchPlace(request).addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>() {
                    @Override
                    public void onSuccess(FetchPlaceResponse response) {
                        Place place = response.getPlace();
                        start = place.getLatLng();
                        Log.i(TAG, "*Place (Pick-up) latitude: " + place.getLatLng().latitude + " longitude: " + place.getLatLng().longitude);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception exception) {
                        Log.e(TAG, "Place not found: " + exception.getMessage());
                    }
                });
            }
        });

        destination.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final PlacesAutoCompleteAdapter.PlaceAutocomplete item = mAdapter.getItem(position);
                final String placeId = String.valueOf(item.placeId);

                List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);
                FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();

                placesClient.fetchPlace(request).addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>() {
                    @Override
                    public void onSuccess(FetchPlaceResponse response) {
                        Place place = response.getPlace();
                        end = place.getLatLng();
                        Log.i(TAG, "*Place (Drop-off) latitude: " + place.getLatLng().latitude + " longitude: " + place.getLatLng().longitude);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception exception) {
                        Log.e(TAG, "Place not found: " + exception.getMessage());
                    }
                });
            }
        });

        starting.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int startNum, int before, int count) {
                if (start != null) {
                    start = null;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        destination.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (end != null) {
                    end = null;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                route();
            }
        });

        return rootView;
    }

    public void route() {
        if (start == null || end == null) {
            if (start == null) {
                if (starting.getText().length() > 0) {
                    starting.setError("Choose location!.");
                } else {
                    Toast.makeText(getActivity(), "Please choose a pick up point.", Toast.LENGTH_SHORT).show();
                }
            }
            if (end == null) {
                if (destination.getText().length() > 0) {
                    destination.setError("Choose location!.");
                } else {
                    Toast.makeText(getActivity(), "Please choose a destination.", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            progressDialog = ProgressDialog.show(getActivity(), "Please wait...", "Fetching route information...", true);
            Routing routing = new Routing.Builder()
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .waypoints(start, end)
                    .build();
            routing.execute();
        }
    }

    @Override
    public void onRoutingFailure() {
        progressDialog.dismiss();
        Toast.makeText(getActivity(), "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRoutingStart() {}

    @Override
    public void onRoutingSuccess(PolylineOptions mPolyOptions, final Route route) {
        progressDialog.dismiss();
        CameraUpdate center = CameraUpdateFactory.newLatLng(start);
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(16);
        map.moveCamera(center);

        if (polyline != null)
            polyline.remove();

        polyline = null;
        PolylineOptions polyOptions = new PolylineOptions();
        polyOptions.color(getResources().getColor(R.color.colorPrimaryDark));
        polyOptions.width(12);
        polyOptions.addAll(mPolyOptions.getPoints());
        polyline = map.addPolyline(polyOptions);

        MarkerOptions options = new MarkerOptions()
                .position(start)
                .title("Pick up: " + route.getName())
                .snippet("Distance : " + route.getDistanceText());
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.start));
        map.addMarker(options);

        options = new MarkerOptions()
                .position(end)
                .title("Drop off: " + route.getEndAddressText())
                .snippet("Distance : " + route.getDistanceText() + ", Duration : " + route.getDurationText());
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.end));
        map.addMarker(options);
        cardView.setVisibility(View.GONE);
    }

    @Override
    public void onRoutingCancelled() {
        Log.i(TAG, "Routing was cancelled.");
    }
}
