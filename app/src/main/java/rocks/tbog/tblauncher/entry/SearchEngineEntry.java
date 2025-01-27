package rocks.tbog.tblauncher.entry;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import rocks.tbog.tblauncher.R;

public final class SearchEngineEntry extends UrlEntry {
    public static final String SCHEME = "search-engine://";

    public SearchEngineEntry(String engineName, String engineUrl) {
        super(SCHEME + engineName, engineUrl);
        setName(engineName, false);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Result methods
    ///////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    protected String getResultText(Context context) {
        return String.format(context.getString(R.string.ui_item_search), getName(), query);
    }

    @Override
    public void doLaunch(@NonNull View v, int flags) {
        Context context = v.getContext();
        if (isGoogleSearch(url)) {
            try {
                Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(SearchManager.QUERY, query); // query contains search string
                context.startActivity(intent);
                return;
            } catch (ActivityNotFoundException e) {
                // Google app not found, fall back to default method
            }
        }
        String encodedQuery;
        try {
            encodedQuery = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            encodedQuery = URLEncoder.encode(query);
        }
        String urlWithQuery = url.replaceAll("%s|\\{q\\}", encodedQuery);
        Uri uri = Uri.parse(urlWithQuery);
        Intent search = new Intent(Intent.ACTION_VIEW, uri);
        search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(search);
        } catch (ActivityNotFoundException e) {
            Log.w("SearchResult", "Unable to run search for url: " + url);
        }
    }
}
