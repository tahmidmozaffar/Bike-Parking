package com.sunrider.bikeparking.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.sunrider.bikeparking.R;
import com.sunrider.bikeparking.activities.helper.AppFragmentManager;
import com.sunrider.bikeparking.db.DBManager;
import com.sunrider.bikeparking.db.entities.ParkingLocationEntity;
import com.sunrider.bikeparking.fragments.HomeFragment;
import com.sunrider.bikeparking.interfaces.MainView;
import com.sunrider.bikeparking.presenters.MainPresenterImpl;
import com.sunrider.bikeparking.services.dexter.DexterPermissionChecker;
import com.sunrider.bikeparking.services.firebase.FirebaseManager;
import com.sunrider.bikeparking.services.locationservice.LocationServiceImpl;
import com.sunrider.bikeparking.utils.AppUtilMethods;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends BaseActivity implements MainView, HomeFragment.OnFragmentInteractionListener, FirebaseManager.FirebaseServiceListener {

    @BindView(R.id.nav_view)
    NavigationView navigationView;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;
    @BindView(R.id.fab)
    FloatingActionButton fab;

    private View navHeader;
    private ImageView imgNavHeaderBg;

    private Handler mHandler;

    private NavigationDrawerManager navigationDrawerManager;
    private MainPresenterImpl presenter;

    //state variables
    private boolean shouldLoadHomeFragOnBackPress = true;
    private boolean isMapLoaded = false;
    private boolean addingNewLocationEntry = false;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ButterKnife.bind(this);
        presenter = new MainPresenterImpl(
                this,
                new FirebaseManager(this),
                DBManager.getInstance(this),
                LocationServiceImpl.getInstance(this),
                new DexterPermissionChecker(this));
        presenter.init();
        presenter.checkLocationPermission();

        if (savedInstanceState == null) {
            navigationDrawerManager.setNavItemIndex(0);
            navigationDrawerManager.setCurrentTag(NavigationDrawerManager.TAG_HOME);
            loadFragment();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(presenter.getLocation() == null){
            presenter.checkLocationPermission();
        }
        else{
            presenter.onLocationFound(presenter.getLocation());
        }

    }

    @Override
    protected void onStop() {
        super.onStop();

//        if (locationServiceImpl != null) {
//            locationServiceImpl.stopLocationUpdates();
//        }
    }

    public boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            AppUtilMethods.showSnackbar(this, R.string.permission_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });

        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }


    @Override
    public int getLayoutResourceId() {
        return R.layout.activity_main;
    }

    @Override
    public String getActivityTitle() {
        return getString(R.string.app_name);
    }

    @Override
    public void init() {

        mHandler = new Handler();
        navHeader = navigationView.getHeaderView(0);
        imgNavHeaderBg = (ImageView) navHeader.findViewById(R.id.img_header_bg);
        navigationDrawerManager = new NavigationDrawerManager(this, navigationView, drawer, navHeader, imgNavHeaderBg);


    }

    @Override
    public void setupNavigationDrawer() {

        loadNavHeader();
        setUpNavigationView();
    }

    private void loadNavHeader() {

        Glide.with(this).asDrawable()
                .load(R.mipmap.nav_menu_header_bg)
                .into(imgNavHeaderBg);

        navigationDrawerManager.setActionView(3, R.layout.menu_dot);
    }

    private void setUpNavigationView() {

        navigationDrawerManager.setUpNavigationView();
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.openDrawer, R.string.closeDrawer) {

            @Override
            public void onDrawerClosed(View drawerView) {
                // Code here will be triggered once the drawer closes as we dont want anything to happen so we leave this blank
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // Code here will be triggered once the drawer open as we dont want anything to happen so we leave this blank
                super.onDrawerOpened(drawerView);
            }
        };

        drawer.setDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }

    @Override
    public void defineClickListener() {

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final HomeFragment homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag(HomeFragment.class.getSimpleName());

                if (!addingNewLocationEntry) {
                    fab.setImageResource(R.mipmap.ic_action_check);
                    addingNewLocationEntry = true;

                    homeFragment.enableLocationPicker();
                } else {
                    fab.setImageResource(R.mipmap.ic_action_add);
                    addingNewLocationEntry = false;

                    final ParkingLocationEntity location = homeFragment.getParkingLocation();
                    AppUtilMethods.showToast(MainActivity.this, location.getLat() + " - " + location.getLng());

                    //progressBar.setVisibility(View.VISIBLE);

                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {

                            final String address = LocationServiceImpl.getInstance(MainActivity.this).getAddress(location.getLat(), location.getLng());
                            location.setAddress(address);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    homeFragment.disableLocationPicker();
                                    //progressBar.setVisibility(View.GONE);

                                    Intent intent = new Intent(MainActivity.this, LocationEntryActivity.class);
                                    startActivity(intent);
                                }
                            });

                        }
                    };

                    new Thread(runnable).start();

                }
            }
        });
    }

    public void loadFragment() {

        navigationDrawerManager.selectNavMenu(NavigationDrawerManager.navItemIndex);
        navigationDrawerManager.setToolbarTitle(getSupportActionBar());

        Runnable mPendingRunnable = new Runnable() {
            @Override
            public void run() {
                Fragment fragment = AppFragmentManager.getFragment(MainActivity.this);
                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.setCustomAnimations(android.R.anim.fade_in,
                        android.R.anim.fade_out);
                fragmentTransaction.replace(R.id.frame, fragment, NavigationDrawerManager.CURRENT_TAG);
                fragmentTransaction.commit();
            }
        };

        if (mPendingRunnable != null) {
            mHandler.post(mPendingRunnable);
        }

        toggleFab();
        navigationDrawerManager.closeDrawer();

    }

    @Override
    public void showLocationOnMap(Location location) {

        HomeFragment homeFragment = (HomeFragment) AppFragmentManager.getFragment(this, NavigationDrawerManager.TAG_HOME);
        if(homeFragment != null){
            homeFragment.showLocation(location);
        }
    }

    @Override
    public void saveAsLastKnownLocation(Location location) {

    }

    @Override
    public void openDeviceSettingsPage() {

        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawers();
            return;
        }

        if (shouldLoadHomeFragOnBackPress) {

            if (NavigationDrawerManager.navItemIndex != 0) {
                navigationDrawerManager.setNavItemIndex(0);
                navigationDrawerManager.setCurrentTag(NavigationDrawerManager.TAG_HOME);
                loadFragment();
                return;
            }
        }

        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_logout) {
            Toast.makeText(getApplicationContext(), "Logout user!", Toast.LENGTH_LONG).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void toggleFab() {
        if (NavigationDrawerManager.navItemIndex == 0)
            fab.show();
        else
            fab.hide();
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        Log.i(TAG, "onRequestPermissionResult");
//        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
//            if (grantResults.length <= 0) {
//                Log.i(TAG, "User interaction was cancelled.");
//            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Log.i(TAG, "Permission granted, updates requested, starting location updates");
//                //locationServiceImpl.startLocationUpdates();
//            } else {
//
//                AppUtilMethods.showSnackbar(this, R.string.permission_denied_explanation,
//                        R.string.nav_settings, new View.OnClickListener() {
//                            @Override
//                            public void onClick(View view) {
//                                // Build intent that displays the App settings screen.
//                                Intent intent = new Intent();
//                                intent.setAction(
//                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                                Uri uri = Uri.fromParts("package",
//                                        BuildConfig.APPLICATION_ID, null);
//                                intent.setData(uri);
//                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                startActivity(intent);
//                            }
//                        });
//            }
//        }
//    }

//    @Override
//    public void onLocationFound(Location location) {
//        if (location != null) {
//            HomeFragment homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag(HomeFragment.class.getSimpleName());
//            if (homeFragment != null) {
//
//                homeFragment.showLocation(location);
//
//                if (locationServiceImpl != null) {
//                    locationServiceImpl.stopLocationUpdates();
//                }
//            }
//
//        }
//    }
//
//    @Override
//    public void onLocationResolutionSuccess() {
//
//    }
//
//    @Override
//    public void onLocationResolutionFailed() {
//
//    }

    @Override
    public void onMapLoadingComplete() {
        isMapLoaded = true;
        //initializeLocationUpdates();
    }

    @Override
    public void onDatabaseError(String message) {
        AppUtilMethods.showToast(this, message);
    }
}
