/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.fragments;

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
import android.widget.ListView;
import android.widget.TextView;

import android.support.v4.app.ListFragment;

import org.barbon.mangaget.Download;
import org.barbon.mangaget.Preferences;
import org.barbon.mangaget.R;

import org.barbon.mangaget.data.DB;

import org.barbon.mangaget.scrape.Scraper;

public class MangaSearchResults extends ListFragment {
    private OnMangaSelected selected = null;

    public interface OnMangaSelected {
        public void onMangaSelected(String title, String url);
    }

    private class SearchAdapter extends BaseAdapter
            implements Scraper.OnSearchResults{
        private Scraper.ResultPager pager;

        public SearchAdapter(Context context, Scraper scraper, String title) {
            pager = scraper.searchManga(title, this);
        }

        @Override
        public int getCount() {
            int count = pager.getCount();

            if (count == 0 || pager.isLast())
                return count;
            else
                return count + 1;
        }

        @Override
        public long getItemId(int position) {
            if (isRequestMoreItem(position))
                return -1;

            return pager.getItem(position).hashCode();
        }

        @Override
        public Object getItem(int position) {
            if (isRequestMoreItem(position))
                return null;

            return pager.getItem(position);
        }

        @Override
        public boolean isEmpty() {
            return pager.isEmpty();
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            LayoutInflater inflater = getActivity().getLayoutInflater();

            if (view == null)
                view = inflater.inflate(android.R.layout.simple_list_item_2,
                                        parent, false);

            TextView text1 = (TextView) view.findViewById(android.R.id.text1);
            TextView text2 = (TextView) view.findViewById(android.R.id.text2);

            if (position < pager.getCount()) {
                text1.setText(pager.getItem(position).title);
                text2.setText(pager.getItem(position).provider);
            } else {
                text1.setText(getResources().getString(R.string.more_results));
                text2.setText("");
            }

            return view;
        }

        @Override
        public void resultsUpdated() {
            notifyDataSetChanged();
        }

        public boolean isRequestMoreItem(int position) {
            return position >= pager.getCount();
        }

        public void requestMore() {
            pager.nextPage();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.manga_search_results, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        registerForContextMenu(getListView());
    }

    public void performSearch(Intent intent) {
        Scraper scraper = Scraper.getInstance(getActivity());
        String query = intent.getStringExtra(SearchManager.QUERY);

        setListAdapter(new SearchAdapter(getActivity(), scraper, query));
    }

    public void setOnMangaSelected(OnMangaSelected handler) {
        selected = handler;
    }

    // implementation

    private void addManga(int index) {
        Scraper.MangaInfo info =
            (Scraper.MangaInfo) getListAdapter().getItem(index);
        DB db = DB.getInstance(getActivity());
        // TODO make configurable
        String basePath = "Pictures/Comics/";

        if (Preferences.getUseMangaSubdir(getActivity()))
            basePath = basePath + info.title.replaceAll("/" ,"-") + "/";

        String path = basePath + info.pattern + "-%02d.cbz";
        long mangaId = db.insertOrUpdateManga(info.title, path, info.url,
                                              DB.SUBSCRIPTION_SAVED);

        Download.startMangaUpdate(getActivity(), mangaId);
    }

    // event handlers

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();

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

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        SearchAdapter adapter = (SearchAdapter) getListAdapter();

        if (adapter.isRequestMoreItem(position)) {
            // TODO feedback that more is loading
            adapter.requestMore();
        } else if (selected != null) {
            Scraper.MangaInfo item = (Scraper.MangaInfo) adapter.getItem(position);

            selected.onMangaSelected(item.title, item.url);
        }
    }
}
