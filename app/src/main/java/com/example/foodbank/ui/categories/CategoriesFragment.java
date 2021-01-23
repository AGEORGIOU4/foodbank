package com.example.foodbank.ui.categories;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.foodbank.R;
import com.example.foodbank.db.CategoriesRoomDatabase;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class CategoriesFragment extends Fragment implements SearchView.OnQueryTextListener {
    private static final String SERVICE_URL = "https://world.openfoodfacts.org/categories.json";

    // Activity states for switching layouts
    private static final int INITIAL_STATE = 2001;
    private static final int ERROR_STATE = 2002;

    // Simple list view
    private final List<Category> categoriesList = new Vector<>();
    String tagsResponse = "";
    private ProgressDialog progressDialog;
    private ListView listView_Categories;
    private ArrayAdapter<Category> arrayAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.c1_fragment_categories, container, false);


        // Simple list view implementation
        this.listView_Categories = root.findViewById(R.id.listView_Categories);
        this.arrayAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, categoriesList);
        listView_Categories.setAdapter(arrayAdapter);
        listView_Categories.setOnItemClickListener((parent, view, position, id) -> {
            Category selected = arrayAdapter.getItem(position);
            String categoryId = selected.getId();
            String categoryName = selected.getName();

            Intent intent = new Intent(getActivity(), ProductsInCategoryActivity.class);
            intent.putExtra("selected_item_id", categoryId);
            intent.putExtra("selected_item_name", categoryName);
            startActivity(intent);
        });

        getResponse();


        // Search
        SearchView searchView_categories = root.findViewById(R.id.searchView_categories);
        searchView_categories.setImeOptions(EditorInfo.IME_ACTION_DONE);
        searchView_categories.setOnQueryTextListener(this);

        // Try to get response again
        Button button_categories_tryAgain = root.findViewById(R.id.button_categories_tryAgain);
        button_categories_tryAgain.setOnClickListener(v -> getResponse());

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /*-------------------------------RESPONSE-----------------------------------*/
    private void handleResponse(final String response) {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(response);
            tagsResponse = jsonObject.getString("tags");
            Category[] categoryArray = new Gson().fromJson(tagsResponse, Category[].class);
            this.categoriesList.clear();
            this.categoriesList.addAll(Arrays.asList(categoryArray));

            arrayAdapter.notifyDataSetChanged();

            progressDialog.dismiss();
            switchLayout(INITIAL_STATE);
        } catch (JSONException e) {
            e.printStackTrace();
            progressDialog.dismiss();
            switchLayout(INITIAL_STATE);
        }
    }

    private void handleError(VolleyError volleyError) {
        Snackbar.make(listView_Categories, "Something went wrong. Please check your connection.", BaseTransientBottomBar.LENGTH_LONG).show();
        progressDialog.dismiss();
        switchLayout(ERROR_STATE);
    }

    public void getResponse() {
        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());

        // Set up progress bar before call
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMax(100);
        progressDialog.setMessage("Loading....");
        progressDialog.setTitle("Fetching data from world.openfoodfacts.org");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        // Show it
        progressDialog.show();

        final StringRequest request = new StringRequest(
                Request.Method.GET,
                SERVICE_URL,
                this::handleResponse, // method reference, equivalent to: response -> handleResponse(response)
                this::handleError); // method reference, equivalent to: error -> handleError(error)

        requestQueue.add(request);
    }

    /*-------------------------------DATABASE-----------------------------------*/
    List<Category> getAllCategoriesSortedByProducts() {
        return CategoriesRoomDatabase.getDatabase(getContext()).categoriesDao().getCategoriesSortedByProducts();
    }

    /*--------------------------------SEARCH------------------------------------*/
    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        String text = newText;
        arrayAdapter.getFilter().filter(newText);
        return false;
    }

    /*--------------------------------------------------------------------------*/
    private void switchLayout(int state) {
        // Layout elements
        FrameLayout frameLayout_categories = requireView().findViewById(R.id.frameLayout_categories);
        listView_Categories = requireView().findViewById(R.id.listView_Categories);

        switch (state) {
            case INITIAL_STATE:
                frameLayout_categories.setVisibility(View.INVISIBLE);
                listView_Categories.setVisibility(View.VISIBLE);
                break;
            case ERROR_STATE:
                frameLayout_categories.setVisibility(View.VISIBLE);
                listView_Categories.setVisibility(View.INVISIBLE);
                break;
        }
    }
}