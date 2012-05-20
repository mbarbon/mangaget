/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget;

import android.app.Activity;
import android.app.SearchManager;

import android.content.Intent;

import android.os.Bundle;

import android.util.SparseBooleanArray;

import android.view.View;

import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import org.barbon.mangaget.scrape.Scraper;

public class MangaAdvancedSearch extends Activity {
    private ListView tags;
    private TextView title;
    private ArrayAdapter<String> tagList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.manga_advanced_search);

        tagList = new ArrayAdapter<String>(
            this, android.R.layout.simple_list_item_multiple_choice,
            Scraper.getTagList());

        title = (TextView) findViewById(R.id.title);
        tags = (ListView) findViewById(R.id.tag_list);
        tags.setAdapter(tagList);
    }

    // implementation

    private ArrayList<String> getIncludeTags() {
        ArrayList<String> result = new ArrayList<String>();
        SparseBooleanArray selected = tags.getCheckedItemPositions();
        System.out.println("count " + selected.size());
        for (int i = 0; i < selected.size(); ++i)
            if (selected.valueAt(i)) {
                int index = selected.keyAt(i);
                System.out.println(i);
                System.out.println((String) tagList.getItem(index));
                result.add((String) tagList.getItem(index));
            }

        return result;
    }

    // event handlers

    public void performSearch(View view) {
        Intent search = new Intent(Intent.ACTION_SEARCH);
        Bundle extra = new Bundle();

        extra.putStringArrayList("include_tags", getIncludeTags());

        search.setClass(this, MangaSearch.class);
        search.putExtra(SearchManager.QUERY, title.getText().toString().trim());
        search.putExtra(SearchManager.APP_DATA, extra);

        startActivity(search);
    }
}
