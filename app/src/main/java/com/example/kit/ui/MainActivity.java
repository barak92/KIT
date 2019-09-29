package com.example.kit.ui;

import android.app.ActivityManager;
import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import androidx.annotation.NonNull;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.kit.Constants;
import com.example.kit.R;
import com.example.kit.models.Contact;
import com.example.kit.models.UChatroom;
import com.example.kit.models.User;
import com.example.kit.models.UserLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.example.kit.services.LocationService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import static com.example.kit.Constants.CONTACT;
import static com.example.kit.Constants.CONTACT_STATE;
import static com.example.kit.Constants.ERROR_DIALOG_REQUEST;
import static com.example.kit.Constants.FRIENDS;
import static com.example.kit.Constants.MY_REQUEST_PENDING;
import static com.example.kit.Constants.NOT_FRIENDS;
import static com.example.kit.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static com.example.kit.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;
import static com.example.kit.Constants.THEIR_REQUEST_PENDING;

public class MainActivity extends AppCompatActivity implements
        ContactsFragment.ContactsCallback,
        ChatsFragment.ChatroomsCallback,
        RequestsFragment.RequestsCallback,
        MapFragment.MapCallBack,
        RequestsDialogFragment.OnInputSelected,
        PendingFragment.PendingCallback
{
    //TODO
    // chat crashes on orientation change
    //TODO
    // actionbar functionality - additional menu options if necessary
    //TODO
    // should we make this the launcher activity that redirects to login if necessary?
    //TODO
    // add functionality to invite message type
    //TODO
    // Login activity isn't perfect yet
    //TODO
    // fix ImageListFragment (as a part of ProfileFragment)
    //TODO
    // does the AddContactsActivity only contain an AlertDialog?
    // if so, why is it an Activity? should be more than a dialog, fragment probably
    // TODO
    //  merge activity_login.xml & activity_username.xml
    //TODO
    // transitions between activities
    //TODO
    // what is the convention for sharing UserLocation between activities and fragments?
    // should we store all major data at application level? shared preferences?
    //TODO
    // make a nicer toolbar
    // TODO
    //  make Contacts recycler adapter compatible with request and pending views
    //TODO
    // empty display names are accepted

    //Tag
    private static final String TAG = "MainActivity";

    //Fragments
    private static final String LOADING_FRAG = "LOADING_FRAG";
    private static final String CHATS_FRAG = "CHATS_FRAG";
    private static final String MAP_FRAG = "MAP_FRAG";
    private static final String CONATCTS_FRAG = "CONTACTS_FRAG";
    private static final String PROFLE_FRAG = "PROFILE_FRAG";
    private static final String CONTACT_FRAG = "CONTACT_FRAG";
    private FragmentTransaction ft;

    //Firebase
    protected FirebaseFirestore mDb;

    //Location
    private static boolean mLocationPermissionGranted = false;
    private FusedLocationProviderClient mFusedLocationClient;
    protected UserLocation mUserLocation;
    private boolean mLocationFetched;

    //Contacts
    private ArrayList<Contact> mContacts = new ArrayList<>();
    private ArrayList<UserLocation> mContactLocations = new ArrayList<>();
    private HashMap<String, Contact> mId2Contact = new HashMap<>();
    private Set<String> mContactIds = new HashSet<>();
    private ListenerRegistration mContactEventListener;
    private boolean mContactsFetched;

    //Requests
    private ArrayList<Contact> mRequests = new ArrayList<>();
    private ListenerRegistration mRequestEventListener;
    private boolean mRequestsFetched;

    //Pending
    private ArrayList<Contact> mPending = new ArrayList<>();
    private ListenerRegistration mPendingEventListener;
    private boolean mPendingFetched;

    //Chatrooms
    private ArrayList<UChatroom> mChatrooms = new ArrayList<>();
    private Set<String> mChatroomIds = new HashSet<>();
    private ListenerRegistration mChatroomEventListener;
    private boolean mChatroomsFetched;

    /*
    ----------------------------- Lifecycle ---------------------------------
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDb = FirebaseFirestore.getInstance();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getLocationPermission();
        initLoadingView();
        initMessageService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.action_bar, menu);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkMapServices()) {
            if (isLocationPermissionGranted()) {
                getUserDetails();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mContactEventListener != null) {
            mContactEventListener.remove();
        }
        if (mChatroomEventListener != null) {
            mChatroomEventListener.remove();
        }
        if (mRequestEventListener != null) {
            mRequestEventListener.remove();
        }
        if (mPendingEventListener != null) {
            mPendingEventListener.remove();
        }
    }

    /*
    ----------------------------- init ---------------------------------
    */

    private void initLoadingView () {
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.upper_toolbar));
        setTitle(R.string.fragment_chats);
        if(!isDestroyed() && !isFinishing()) {
            ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.fragment_container, LoadingFragment.newInstance(), LOADING_FRAG)
                    .commit();
        }
    }

    private void initView() {
        initNavigationBar();
    }

    private void initNavigationBar () {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navi_bar);
        bottomNav.setVisibility(View.VISIBLE);
        replaceFragment(ChatsFragment.newInstance(), CHATS_FRAG);
        bottomNav.setOnNavigationItemSelectedListener
                (new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_chats: {
                                replaceFragment(ChatsFragment.newInstance(), CHATS_FRAG);
                                setTitle(R.string.fragment_chats);
                                return true;
                            }
                            case R.id.action_map: {
                                replaceFragment(MapFragment.newInstance(), MAP_FRAG);
                                setTitle(R.string.fragment_map);
                                return true;
                            }
                            case R.id.action_contacts: {
                                replaceFragment(ContactsRequestsPendingFragment.newInstance(), CONATCTS_FRAG);
                                setTitle(R.string.fragment_contacts);
                                return true;
                            }
                            case R.id.action_profile: {
                                replaceFragment(ProfileFragment.newInstance(), PROFLE_FRAG);
                                setTitle(R.string.fragment_profile);
                                return true;
                            }
                        }
                        return false;
                    }
                });
    }

    private void replaceFragment (Fragment newFragment, String tag){
        if(!isDestroyed() && !isFinishing()) {
            mChatroomEventListener.remove();
            mContactEventListener.remove();
            mRequestEventListener.remove();
            mPendingEventListener.remove();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_container, newFragment, tag).commit();
        }
    }

    private void initMessageService() {
        Intent intent = new Intent("com.example.kit.services.MyFirebaseMessagingService");
        intent.setPackage("com.example.kit");
        this.startService(intent);
    }

    private void startLocationService() {
        if (!isLocationServiceRunning()) {
            Intent serviceIntent = new Intent(this, LocationService.class);
            Log.d(TAG, "startLocationService: ASASAS");

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                Log.d(TAG, "startLocationService: sasa26+");

                MainActivity.this.startForegroundService(serviceIntent);
            } else {
                Log.d(TAG, "startLocationService: :'(");
                startService(serviceIntent);
            }
        }
    }

    private void checkReady () {
        if (mContactsFetched &&
                mLocationFetched &&
                mChatroomsFetched &&
                mRequestsFetched &&
                mPendingFetched) {
            mContactLocations.add(mUserLocation);
            initView();
    }
}

    /*
    ----------------------------- onClick ---------------------------------
    */

    @Override
    public boolean onOptionsItemSelected (MenuItem item){
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            navSettingsActivity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
    ----------------------------- ContactsCallback ---------------------------------
    */

    @Override
    public ArrayList<Contact> getContacts() {
        return mContacts;
    }

    @Override
    public Set<String> getContactIds() {
        return mContactIds;
    }

    @Override
    public HashMap<String, Contact> getId2Contact() {
        return mId2Contact;
    }

    @Override
    public void initContactFragment(String contactID) {
        if(!isDestroyed() && !isFinishing()) {
            ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.fragment_container, LoadingFragment.newInstance(), LOADING_FRAG)
                    .commit();
        }
        fetchUser(contactID);
    }

    /*
    ----------------------------- Chatrooms Callback ---------------------------------
    */

    @Override
    public Set<String> getChatroomIds() {
        return mChatroomIds;
    }

    @Override
    public ArrayList<UChatroom> getChatrooms() {
        return mChatrooms;
    }

    @Override
    public UserLocation getUserLocation() {
        return mUserLocation;
    }

    /*
    ----------------------------- Maps Callback ---------------------------------
    */

    @Override
    public ArrayList<UserLocation> getUserLocations () {
        return mContactLocations;
    }

    /*
    ----------------------------- Requests Callback ---------------------------------
    */

    @Override
    public ArrayList<Contact> getRequests() {
        return mRequests;
    }


    /*
    ----------------------------- Pending Callback ---------------------------------
    */

    @Override
    public ArrayList<Contact> getPending() {
        return mPending;
    }

    /*
    ----------------------------- Requests Dialog Fragment Callback ---------------------------------
    */

    @Override
    public void requestAccepted(String display_name, Contact contact) {
        handleRequest(contact, display_name, true);
    }

    @Override
    public void requestRemoved(Contact contact) {
        handleRequest(contact, "", false);
    }

    /*
    ----------------------------- nav ---------------------------------
    */

    private void navSettingsActivity () {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void navContactFragment(Contact contact, String state){
        ContactFragment contactFrag = ContactFragment.newInstance();
        Bundle args = new Bundle();
        args.putParcelable(CONTACT, contact);
        args.putString(CONTACT_STATE, state);
        contactFrag.setArguments(args);
        replaceFragment(contactFrag, CONTACT_FRAG);
    }

    /*
    ----------------------------- DB ---------------------------------
    */

    protected void getUserDetails () {
        if (mUserLocation == null) {
            mUserLocation = new UserLocation();
            DocumentReference userRef = mDb.collection(getString(R.string.collection_users))
                    .document(FirebaseAuth.getInstance().getUid());

            userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "onComplete: successfully set the user client.");
                        User user = task.getResult().toObject(User.class);
                        mUserLocation.setUser(user);
                        getLastKnownLocation();
                    }
                }
            });
        } else {
            getLastKnownLocation();
        }
    }

    private void saveUserLocation () {
        if (mUserLocation != null) {
            DocumentReference locationRef = mDb
                    .collection(getString(R.string.collection_user_locations))
                    .document(FirebaseAuth.getInstance().getUid());

            locationRef.set(mUserLocation).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "saveUserLocation: \ninserted user location into database." +
                                "\n latitude: " + mUserLocation.getGeo_point().getLatitude() +
                                "\n longitude: " + mUserLocation.getGeo_point().getLongitude());
                    }
                    mLocationFetched = true;
                    checkReady();
                }
            });
        }
//        mLocationFetched = true;
//        checkReady();
    }

    private void fetchContacts () {
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder().build();
        mDb.setFirestoreSettings(settings);
        CollectionReference contactsCollection = mDb
                .collection(getString(R.string.collection_users))
                .document(FirebaseAuth.getInstance().getUid())
                .collection(getString(R.string.collection_contacts));
        mContactEventListener = contactsCollection.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                Log.d(TAG, "onEvent: called.");
                if (e != null) {
                    Log.e(TAG, "onEvent: Listen failed.", e);
                    return;
                }
                if (queryDocumentSnapshots != null) {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                        Contact contact = doc.toObject(Contact.class);
                        if (!mContactIds.contains(contact.getCid())) {
                            mContactIds.add(contact.getCid());
                            mContacts.add(contact);
                            mId2Contact.put(contact.getCid(), contact);
                            getContactLocation(contact, queryDocumentSnapshots.size());
                        }
                    }
                    if(queryDocumentSnapshots.size() == 0){
                        mContactsFetched = true;
                        checkReady();
                    }
                    Log.d(TAG, "onEvent: number of contacts: " + mContacts.size());
                }
            }
        });
    }

    private void getContactLocation(final Contact user, final int numTotal){
        DocumentReference locRef = mDb.collection(getString(R.string.collection_user_locations)).document(user.getCid());
        locRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    UserLocation userLocation = task.getResult().toObject(UserLocation.class);
                    if (userLocation != null){
                        userLocation.getUser().setUsername(user.getUsername());
                        mContactLocations.add(userLocation);
                        if (mContactLocations.size() == numTotal){
                            mContactsFetched = true;
                            checkReady();
                        }
                    }
                }
            }
        });
    }

    private void fetchRequests () {
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder().build();
        mDb.setFirestoreSettings(settings);
        CollectionReference requestsCollection = mDb
                .collection(getString(R.string.collection_users))
                .document(FirebaseAuth.getInstance().getUid())
                .collection(getString(R.string.collection_requests));
        mRequestEventListener = requestsCollection.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                Log.d(TAG, "onEvent: called.");
                if (e != null) {
                    Log.e(TAG, "onEvent: Listen failed.", e);
                    return;
                }
                if (queryDocumentSnapshots != null) {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Contact contact = doc.toObject(Contact.class);
                        mRequests.add(contact);
                    }
                    Log.d(TAG, "onEvent: number of contacts: " + mContacts.size());
                }
                mRequestsFetched = true;
                checkReady();
            }
        });
    }

    private void fetchChatrooms() {
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder().build();
        mDb.setFirestoreSettings(settings);
        CollectionReference chatroomsCollection = mDb
                .collection(getString(R.string.collection_users))
                .document(FirebaseAuth.getInstance().getUid())
                .collection(getString(R.string.collection_user_chatrooms));
        mChatroomEventListener = chatroomsCollection.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                Log.d(TAG, "onEvent: called.");
                if (e != null) {
                    Log.e(TAG, "onEvent: Listen failed.", e);
                    return;
                }
                if (queryDocumentSnapshots != null) {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                        UChatroom chatroom = doc.toObject(UChatroom.class);
                        if (!mChatroomIds.contains(chatroom.getChatroom_id())) {
                            mChatroomIds.add(chatroom.getChatroom_id());
                            mChatrooms.add(chatroom);
                        }
                    }
                    Log.d(TAG, "onEvent: number of chatrooms: " + mChatrooms.size());
                }
                mChatroomsFetched = true;
                checkReady();
            }
        });
    }

    private void fetchPending(){
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder().build();
        mDb.setFirestoreSettings(settings);
        CollectionReference pendingCollection = mDb
                .collection(getString(R.string.collection_users))
                .document(FirebaseAuth.getInstance().getUid())
                .collection(getString(R.string.collection_pending));
        mPendingEventListener = pendingCollection.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                Log.d(TAG, "onEvent: called.");
                if (e != null) {
                    Log.e(TAG, "onEvent: Listen failed.", e);
                    return;
                }
                if (queryDocumentSnapshots != null) {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Contact contact = doc.toObject(Contact.class);
                        mPending.add(contact);
                    }
                    Log.d(TAG, "onEvent: number of contacts: " + mContacts.size());
                }
                mPendingFetched = true;
                checkReady();
            }
        });
    }

    private void fetchUser(final String userID){
        if(mContactIds.contains(userID)){
            Contact contact = mId2Contact.get(userID);
            navContactFragment(contact, FRIENDS);
            return;
        }

        for(Contact request : mRequests){
            if(request.getCid().equals(userID)){
                navContactFragment(request, THEIR_REQUEST_PENDING);
                return;
            }
        }

        for(Contact pending : mPending){
            if(pending.getCid().equals(userID)){
                navContactFragment(pending, MY_REQUEST_PENDING);
                return;
            }
        }

        final Contact contact = new Contact();
        DocumentReference userRef = mDb.collection(getString(R.string.collection_users))
                .document(userID);
        userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "onComplete: successfully found contact.");
                    User user = task.getResult().toObject(User.class);
                    contact.setCid(userID);
                    contact.setName(user.getUsername());
                    contact.setAvatar(user.getAvatar());
                    navContactFragment(contact, NOT_FRIENDS);
                }
            }
        });

    }


    /*
    ----------------------------- Location ---------------------------------
    */

    protected boolean isLocationPermissionGranted() {
        return mLocationPermissionGranted;
    }

    protected void setLocationPermissionGranted(boolean permissionGranted) {
        mLocationPermissionGranted = permissionGranted;
    }

    private boolean isLocationServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.example.kit.services.LocationService".equals(service.service.getClassName())) {
                Log.d(TAG, "isLocationServiceRunning: location service is already running.");
                return true;
            }
        }
        Log.d(TAG, "isLocationServiceRunning: location service is not running.");
        return false;
    }

    protected void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            setLocationPermissionGranted(true);
            startLocationService();
            getUserDetails();
            fetchContacts();
            fetchRequests();
            fetchPending();
            fetchChatrooms();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    private void getLastKnownLocation() {
        Log.d(TAG, "getLastKnownLocation: called.");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful()) {
                    Location location = task.getResult();
                    if (location != null) {
                        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        mUserLocation.setGeo_point(geoPoint);
                        mUserLocation.setTimestamp(null);
                        saveUserLocation();
                    }
                }
            }
        });

    }

    protected boolean checkMapServices() {
        return (isServicesOK() && isMapsEnabled());
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("This application requires GPS to work properly, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent enableGpsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private boolean isMapsEnabled() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
            return false;
        }
        return true;
    }

    private boolean isServicesOK() {
        Log.d(TAG, "isServicesOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);

        if (available == ConnectionResult.SUCCESS) {
            //everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            //an error occured but we can resolve it
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: called.");
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ENABLE_GPS: {
                getLocationPermission();
                break;
            }
        }
    }

    private void handleRequest ( final Contact contact, final String display_name,
        final boolean accepted){
        final FirebaseFirestore fs = FirebaseFirestore.getInstance();
        final String uid = FirebaseAuth.getInstance().getUid();
        final DocumentReference userRef = fs.collection(Constants.COLLECTION_USERS).document(uid);
        if (accepted) {
            userRef.collection(Constants.COLLECTION_CONTACTS).document(contact.getCid()).set(new Contact(display_name,
                    contact.getUsername(), contact.getAvatar(), contact.getCid())).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    userRef.collection(Constants.COLLECTION_REQUESTS).document(contact.getCid()).delete();
                    final DocumentReference contactRef =
                            fs.collection(Constants.COLLECTION_USERS).document(contact.getCid());
                    contactRef.collection(Constants.COLLECTION_PENDING).document(FirebaseAuth.getInstance().getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (!task.isSuccessful()) {
                                return;
                            }
                            final Contact ucontact = task.getResult().toObject(Contact.class);
                            contactRef.collection(Constants.COLLECTION_CONTACTS).document(ucontact.getCid()).set(ucontact).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (!task.isSuccessful()) {
                                        return;
                                    }
                                    contactRef.collection(Constants.COLLECTION_PENDING).document(ucontact.getCid()).delete();
                                    mRequests.remove(contact);
//                                        Contact nContact = new Contact(display_name, contact.getUsername(), contact.getAvatar(),
//                                            contact.getCid());
//                                    mContacts.add(nContact);
//                                    mContactIds.add(contact.getCid());
//                                    mId2Contact.put(contact.getCid(), nContact);
//                                    updateAdapters();
                                    replaceFragment(ContactsRequestsPendingFragment.newInstance(), CONATCTS_FRAG);
                                    setTitle(R.string.fragment_contacts);
                                }
                            });
                        }
                    });
                }

            });
        } else {
            userRef.collection(Constants.COLLECTION_REQUESTS).document(contact.getCid()).delete();
            fs.collection(Constants.COLLECTION_USERS).document(contact.getCid()).collection(Constants.COLLECTION_PENDING).document(uid).delete();
            mRequests.remove(contact);
            replaceFragment(ContactsRequestsPendingFragment.newInstance(), CONATCTS_FRAG);
            setTitle(R.string.fragment_contacts);

        }
    }
}