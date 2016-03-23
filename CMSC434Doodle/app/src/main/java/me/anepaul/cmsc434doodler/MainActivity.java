package me.anepaul.cmsc434doodler;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;

public class MainActivity extends RoboActivity {
    private final String TAG = getClass().getSimpleName();

    private static final int REQUEST_WRITE_STORAGE = 112;

    private boolean isBarOpen = false, isFabOpen = false;

    private Animation fabOpenPort, fabClosePort, fabOpenLand, fabCloseLand;
//    rotateClockwise, rotateAnticlockwise;

    @InjectView(R.id.main_dv_doodle) DoodleView mDoodleView;

    @InjectView(R.id.main_sb_seek) SeekBar mSeekBar;

    @InjectView(R.id.main_fab_color) FloatingActionButton mFabColo;
    @InjectView(R.id.main_fab_size) FloatingActionButton mFabSize;
    @InjectView(R.id.main_fab_opacity) FloatingActionButton mFabOpac;
    @InjectView(R.id.main_fab_save) FloatingActionButton mFabSave;

    List<FloatingActionButton> mHiddenFABs;
    List<Integer> mColorList;
    ListIterator<Integer> mColorIterator;

    private float mBrushSize = 5;
    private int mBrushOpacity = 100;

    private int mDeviceOrientation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton[] hiddenFAB = new FloatingActionButton[]{mFabSize, mFabOpac, mFabSave};
        mHiddenFABs = Arrays.asList(hiddenFAB);

        fabOpenPort = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open_port);
        fabClosePort = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close_port);
        fabOpenLand = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open_land);
        fabCloseLand = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close_land);

        Integer[] colorList = new Integer[]{Color.RED, Color.BLACK, Color.BLUE,
                Color.GREEN, Color.YELLOW};
        mColorList = Arrays.asList(colorList);
        mColorIterator = mColorList.listIterator();

        mSeekBar.setMax(100);
        mSeekBar.setOnSeekBarChangeListener(new SizeSeekBarListener());
        mSeekBar.setProgress((int) mBrushSize);

        mFabColo.setBackgroundTintList(ColorStateList.valueOf(mColorIterator.next()));

        mFabColo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mColorIterator.hasNext()) {
                    mColorIterator = mColorList.listIterator();
                }
                int color = mColorIterator.next();
                mFabColo.setBackgroundTintList(ColorStateList.valueOf(color));
                mDoodleView.setDoodleColor(color);
            }
        });

        mFabColo.setOnLongClickListener(new View.OnLongClickListener(){
            @Override
            public boolean onLongClick(View v) {
                animateFAB();
                return true;
            }
        });

        mFabSize.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mSeekBar.setOnSeekBarChangeListener(new SizeSeekBarListener());
                mSeekBar.setProgress((int) mBrushSize);
            }
        });

        mFabOpac.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSeekBar.setOnSeekBarChangeListener(new AlphaSeekBarListener());
                mSeekBar.setProgress(mBrushOpacity);
            }
        });

        mFabSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new SaveImageTask().execute(mDoodleView.getBitmap());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDeviceOrientation = getResources().getConfiguration().orientation;
    }

    private class SizeSeekBarListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            mDoodleView.setDoodleWidth(progress);
            mBrushSize = progress;
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            animateFAB();
        }
    }

    private class AlphaSeekBarListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            mDoodleView.setDoodleOpacity((int) ((double) progress * 2.5));
            mBrushOpacity = progress;
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            animateFAB();
        }
    }

    public void animateFAB(){
        if(isFabOpen){
            if (mDeviceOrientation == Configuration.ORIENTATION_PORTRAIT) {
                mSeekBar.startAnimation(fabClosePort);
                for (FloatingActionButton fab : mHiddenFABs) {
                    fab.startAnimation(fabClosePort);
                    fab.setClickable(false);
                }
            } else {
                mSeekBar.startAnimation(fabCloseLand);
                for (FloatingActionButton fab : mHiddenFABs) {
                    fab.startAnimation(fabCloseLand);
                    fab.setClickable(false);
                }
            }
            isFabOpen = false;

        } else {
            if (mDeviceOrientation == Configuration.ORIENTATION_PORTRAIT) {
                mSeekBar.startAnimation(fabOpenPort);
                for (FloatingActionButton fab : mHiddenFABs) {
                    fab.startAnimation(fabOpenPort);
                    fab.setClickable(true);
                }
            } else {
                for (FloatingActionButton fab : mHiddenFABs) {
                    mSeekBar.startAnimation(fabOpenLand);
                    fab.startAnimation(fabOpenLand);

                }
            }
            isFabOpen = true;

        }
    }

    private class SaveImageTask extends AsyncTask<Bitmap, Void, Void> {

        @Override
        protected Void doInBackground(Bitmap... data) {
            FileOutputStream outStream = null;

            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                data[0].compress(Bitmap.CompressFormat.PNG, 95, stream);
                byte[] byteArray = stream.toByteArray();

                Log.i(TAG, "doInBackground: Saving image of size = " + byteArray.length);

                boolean hasPermission = (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                if (!hasPermission) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_WRITE_STORAGE);
                }

                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    File sdCard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

                    String fileName = String.format("%d.png", System.currentTimeMillis());
                    File outFile = new File(sdCard, fileName);
                    outStream = new FileOutputStream(outFile);
                    outStream.write(byteArray);
                    outStream.flush();
                    outStream.close();
//                    if (outFile.canWrite()) {
//
//                    } else {
//                        Log.w(TAG, "doInBackground: Cannot write to file");
//                    }

                    refreshGallery(outFile);
                } else {
                    Log.w(TAG, "doInBackground: SD card not writable");
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Toast.makeText(MainActivity.this, "Your masterpiece was saved", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }
}
