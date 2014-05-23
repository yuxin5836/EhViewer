/*
 * Copyright (C) 2014 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.activity;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Toast;

import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.ImageGeterManager;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.Data;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.service.DownloadService;
import com.hippo.ehviewer.service.DownloadServiceConnection;
import com.hippo.ehviewer.util.Cache;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.widget.AlertButton;
import com.hippo.ehviewer.widget.DialogBuilder;
import com.hippo.ehviewer.widget.LoadImageView;

public class FavouriteActivity extends Activity{
    @SuppressWarnings("unused")
    private static final String TAG = "FavouriteActivity";
    
    private AppContext mAppContext;
    private Data mData;
    
    private FlAdapter flAdapter;
    private List<GalleryInfo> mFavouriteLmd;
    private int longClickItemIndex;
    
    private ImageGeterManager mImageGeterManager;
    
    private DownloadServiceConnection mServiceConn = new DownloadServiceConnection();
    
    // List item long click dialog
    private AlertDialog longClickDialog;

    private AlertDialog setLongClickDialog() {
        return new DialogBuilder(this).setTitle(R.string.what_to_do)
                .setItems(R.array.favourite_item_long_click, new OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1,
                            int position, long arg3) {
                        switch (position) {
                        case 0: // Remove favourite item
                            mData.deleteLocalFavourite(mFavouriteLmd.get(longClickItemIndex).gid);
                            flAdapter.notifyDataSetChanged();
                            break;
                        case 1:
                            GalleryInfo lmd = mFavouriteLmd.get(longClickItemIndex);
                            Intent it = new Intent(FavouriteActivity.this, DownloadService.class);
                            startService(it);
                            mServiceConn.getService().add(String.valueOf(lmd.gid), lmd.thumb, 
                                    EhClient.getDetailUrl(lmd.gid, lmd.token), lmd.title);
                            Toast.makeText(FavouriteActivity.this,
                                    getString(R.string.toast_add_download),
                                    Toast.LENGTH_SHORT).show();
                            break;
                        default:
                            break;
                        }
                        longClickDialog.cancel();
                    }
                }).setNegativeButton(android.R.string.cancel, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                    }
                }).create();
    }
    
    private class FlAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public FlAdapter() {
            mInflater = LayoutInflater.from(FavouriteActivity.this);
        }

        @Override
        public int getCount() {
            return mFavouriteLmd.size();
        }

        @Override
        public Object getItem(int arg0) {
            return mFavouriteLmd.get(arg0);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            GalleryInfo lmd= mFavouriteLmd.get(position);
            if (convertView == null)
                convertView = mInflater.inflate(R.layout.list_item, null);
            
            LoadImageView thumb = (LoadImageView)convertView.findViewById(R.id.cover);
            if (!String.valueOf(lmd.gid).equals(thumb.getKey())) {
                
                Bitmap bmp = null;
                if (Cache.memoryCache != null &&
                        (bmp = Cache.memoryCache.get(String.valueOf(lmd.gid))) != null) {
                    thumb.setLoadInfo(lmd.thumb, String.valueOf(lmd.gid));
                    thumb.setImageBitmap(bmp);
                    thumb.setState(LoadImageView.LOADED);
                } else {
                    thumb.setImageDrawable(null);
                    thumb.setLoadInfo(lmd.thumb, String.valueOf(lmd.gid));
                    thumb.setState(LoadImageView.NONE);
                    mImageGeterManager.add(lmd.thumb, String.valueOf(lmd.gid),
                            ImageGeterManager.DISK_CACHE | ImageGeterManager.DOWNLOAD,
                            new LoadImageView.SimpleImageGetListener(thumb));
                }

                // Set manga name
                TextView name = (TextView) convertView.findViewById(R.id.name);
                name.setText(lmd.title);
                
                // Set uploder
                TextView uploader = (TextView) convertView.findViewById(R.id.uploader);
                uploader.setText(lmd.uploader);
                
                // Set category
                TextView category = (TextView) convertView.findViewById(R.id.category);
                String newText = Ui.getCategoryText(lmd.category);
                if (!newText.equals(category.getText())) {
                    category.setText(newText);
                    category.setBackgroundColor(Ui.getCategoryColor(lmd.category));
                }
                
                // Add star
                RatingBar rate = (RatingBar) convertView
                        .findViewById(R.id.rate);
                rate.setRating(lmd.rating);
                
                // set posted
                TextView posted = (TextView) convertView.findViewById(R.id.posted);
                posted.setText(lmd.posted);
            }
            return convertView;
        }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        int screenOri = Config.getScreenOriMode();
        if (screenOri != getRequestedOrientation())
            setRequestedOrientation(screenOri);
        
        if (flAdapter != null)
            flAdapter.notifyDataSetChanged();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.favourite);
        
        mAppContext = (AppContext)getApplication();
        mData = mAppContext.getData();
        mImageGeterManager = mAppContext.getImageGeterManager();
        
        int screenOri = Config.getScreenOriMode();
        if (screenOri != getRequestedOrientation())
            setRequestedOrientation(screenOri);
        
        Ui.translucent(this);
        
        // Download service
        Intent it = new Intent(FavouriteActivity.this, DownloadService.class);
        bindService(it, mServiceConn, BIND_AUTO_CREATE);
        
        longClickDialog = setLongClickDialog();
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        mFavouriteLmd = mData.getAllLocalFavourites();
        
        ListView listView = (ListView)findViewById(R.id.favourite);
        flAdapter = new FlAdapter();
        listView.setAdapter(flAdapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                    int position, long arg3) {
                Intent intent = new Intent(FavouriteActivity.this,
                        MangaDetailActivity.class);
                GalleryInfo gi = mFavouriteLmd.get(position);
                intent.putExtra("url", EhClient.getDetailUrl(gi.gid, gi.token));
                intent.putExtra(MangaDetailActivity.KEY_G_INFO, gi);
                startActivity(intent);
            }
        });
        listView.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                    int position, long arg3) {
                longClickItemIndex = position;
                longClickDialog.show();
                return true;
            }
        });
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConn);
    }
    
    public void buttonListItemCancel(View v) {
        longClickDialog.cancel();
    }
}
