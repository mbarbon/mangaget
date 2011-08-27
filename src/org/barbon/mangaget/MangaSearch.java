package org.barbon.mangaget;

import android.app.ListActivity;
import android.app.SearchManager;

import android.content.Context;
import android.content.Intent;

import android.os.Bundle;

import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.barbon.mangaget.data.DB;

import org.barbon.mangaget.scrape.animea.Scraper;

import org.barbon.mangaget.scrape.Downloader;

public class MangaSearch extends ListActivity {
    private class SearchAdapter extends BaseAdapter
            implements Scraper.OnSearchResults{
        private Scraper.ResultPager pager;

        public SearchAdapter(Context context, Scraper scraper, String title) {
            pager = scraper.searchManga(title, this);
        }

        @Override
        public int getCount() {
            return pager.getCount();
        }

        @Override
        public long getItemId(int position) {
            return pager.getItem(position).hashCode();
        }

        @Override
        public Object getItem(int position) {
            return pager.getItem(position);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();

            if (view == null)
                view = inflater.inflate(android.R.layout.simple_list_item_1,
                                        parent, false);

            TextView text1 = (TextView) view.findViewById(android.R.id.text1);

            text1.setText(pager.getItem(position).title);

            return view;
        }

        @Override
        public void resultsUpdated() {
            notifyDataSetChanged();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());

        registerForContextMenu(getListView());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    // implementation

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            DB db = DB.getInstance(this);
            Downloader downloader = new Downloader();
            Scraper scraper = new Scraper(db, downloader);
            String query = intent.getStringExtra(SearchManager.QUERY);

            setListAdapter(new SearchAdapter(this, scraper, query));
        }
    }

    private void addManga(int index) {
        Scraper.MangaInfo info =
            (Scraper.MangaInfo) getListAdapter().getItem(index);
        DB db = DB.getInstance(this);
        // TODO make configurable
        String path = "Pictures/Comics/" + info.pattern + "-%02d.cbz";

        db.insertManga(info.title, path, info.url);
    }

    // event handlers

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.search_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
            (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
        case R.id.add_manga:
            addManga(info.position);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }
}