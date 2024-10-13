package com.jorgesys.searchaddress;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.gms.common.api.ApiException;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.List;

public class PlacesAutoCompleteAdapter
        extends ArrayAdapter<PlacesAutoCompleteAdapter.PlaceAutocomplete> implements Filterable {

    private static final String TAG = "PlacesAutoCompleteAdapter";
    private List<PlaceAutocomplete> mResultList = new ArrayList<>();
    private PlacesClient placesClient;
    private LatLngBounds mBounds;
    private TypeFilter mPlaceFilter;


    public PlacesAutoCompleteAdapter(Context context, int resource, PlacesClient placesClient,
                                     LatLngBounds bounds, TypeFilter filter) {
        super(context, resource);
        this.placesClient = placesClient;
        this.mBounds = bounds;
        this.mPlaceFilter = filter;
    }

    public void setBounds(LatLngBounds bounds) {
        mBounds = bounds;
    }

    @Override
    public int getCount() {
        return mResultList.size();
    }

    @Override
    public PlaceAutocomplete getItem(int position) {
        return mResultList.get(position);
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                if (constraint != null) {
                    mResultList = getAutocomplete(constraint);
                    if (mResultList != null) {
                        results.values = mResultList;
                        results.count = mResultList.size();
                    }
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
    }

    private List<PlaceAutocomplete> getAutocomplete(CharSequence constraint) {
        Log.d(TAG, "Query: " + constraint);

        RectangularBounds bounds = RectangularBounds.newInstance(mBounds.southwest, mBounds.northeast);
        AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setTypeFilter(mPlaceFilter)
                .setLocationBias(bounds)
                .setSessionToken(token)
                .setQuery(constraint.toString())
                .build();

        final List<PlaceAutocomplete> resultList = new ArrayList<>();

        placesClient.findAutocompletePredictions(request).addOnSuccessListener(response -> {
            for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                resultList.add(new PlaceAutocomplete(prediction.getPlaceId(), prediction.getFullText(null).toString()));
            }
        }).addOnFailureListener(exception -> {
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                Log.e(TAG, "Error getting autocomplete prediction API call: " + apiException.getStatusCode());
                Toast.makeText(getContext(), "Error contacting API: " + apiException.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        return resultList;
    }

    class PlaceAutocomplete {
        public CharSequence placeId;
        public CharSequence description;

        PlaceAutocomplete(CharSequence placeId, CharSequence description) {
            this.placeId = placeId;
            this.description = description;
        }

        @Override
        public String toString() {
            return description.toString();
        }
    }
}
