package org.phpnet.openDrivinCloudAndroid.Activities;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import org.phpnet.openDrivinCloudAndroid.Adapter.ImageViewerAdapter;
import org.phpnet.openDrivinCloudAndroid.Adapter.MyFile;
import org.phpnet.openDrivinCloudAndroid.Common.CurrentUser;
import org.phpnet.openDrivinCloudAndroid.Listener.ClickDownload;
import org.phpnet.openDrivinCloudAndroid.R;

public class ImageActivity extends AppCompatActivity {

    private static ImageActivity activity;

    public static ImageActivity getActivity() {
        return activity;
    }

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeButtonEnabled(true);
        final CurrentUser currentUser = CurrentUser.getInstance();
        ab.setTitle(currentUser.listImage.get(currentUser.indexCurrentImage).name);

        //Utilisation d'un ViewPager pour naviguer entre les images
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewPagerId);
        ImageViewerAdapter viewerAdapter = new ImageViewerAdapter(getSupportFragmentManager(), currentUser.getPreviewableImages(getApplicationContext()));
        viewPager.setAdapter(viewerAdapter);
        viewPager.setCurrentItem(currentUser.indexCurrentImage);
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentUser.indexCurrentImage = position;
                getSupportActionBar().setTitle(currentUser.listImage.get(position).name);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_image, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()){
            case android.R.id.home:
                finish();
                break;
            case R.id.action_download:
                CurrentUser currentUser = CurrentUser.getInstance();
                MyFile img = currentUser.listImage.get(currentUser.indexCurrentImage);
                ClickDownload.getInstance().onClickDownload(img);
                break;
        }
        return super.onOptionsItemSelected(menuItem);
    }

}
