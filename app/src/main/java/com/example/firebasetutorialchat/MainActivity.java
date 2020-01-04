package com.example.firebasetutorialchat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.Notification;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.example.firebasetutorialchat.App.CHANNEL_1_ID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    public static final int RC_SIGN_IN = 1;
    public static final int RC_PHOTO_PICKER = 2;
    public static final String MSG_LENGTH_LIMIT_KEY = "message_length_limit";


    private FirebaseDatabase mFirebaseDatabase;  //This is the access point to our database
    private DatabaseReference mMessagesDatabaseReference;  //This is the reference to our specific messages
    private ChildEventListener mChildEventListener;  //This gets active whenever there is any change in the child node
    private FirebaseAuth mFirebaseAuth;  //Access point to authentications
    private FirebaseAuth.AuthStateListener mAuthStateListener;  //Object of Auth state Listener
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mChatPhotosStorageReference;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;


    private ListView mMessageListView;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;

    private String mUsername;

    private MessageAdapter mMessageAdapter;

    private NotificationManagerCompat notificationManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsername = ANONYMOUS;


        mFirebaseDatabase = FirebaseDatabase.getInstance();  //This creates an instance for the FirebaseDatabase
        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child("messages");
        //This takes the instance and gets a reference of the database root and then creates a node named "messages"

        mFirebaseAuth = FirebaseAuth.getInstance();  //This creates an instance for the FirebaseAuth

        mFirebaseStorage = FirebaseStorage.getInstance();//This creates an instance for the FirebaseStorage
        mChatPhotosStorageReference = mFirebaseStorage.getReference().child("chat_photos");
        //This takes the instance and gets a reference of the storage root and then creates a folder named "chat_photos"

        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        notificationManager = NotificationManagerCompat.from(this);


        setViewByIds();  //Sets all the ids in the main activity


        //Initialize message ListView and its adapter
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);


        mProgressBar.setVisibility(ProgressBar.INVISIBLE); //Initialize progress bar


        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Pick an image via this button

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete Action Using"), RC_PHOTO_PICKER);
            }
        });


        //The job of text watcher is to see if the text is changed and perform certain actions if true
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {


                if(s.toString().trim().length() > 0){
                    mSendButton.setEnabled(true);
                }
                else{
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        //Sets length limit for the edit text which send messages
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});


        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Send the message

                if(mMessageEditText.getText().toString().equals("")){

                }
                else{


                    Calendar calendar = Calendar.getInstance();
                    final String currentdate = DateFormat.getDateTimeInstance().format(calendar.getTime());
                    SimpleDateFormat format = new SimpleDateFormat("MMM dd, yyyy- EEE h:mm a", Locale.ENGLISH);
                    String time = format.format(calendar.getTime());


                    FriendlyMessage friendlyMessage = new FriendlyMessage(mMessageEditText.getText().toString(), mUsername, null, time);
                    //This object has all the keys that we'll store as a message in the realtime database

                    mMessagesDatabaseReference.push().setValue(friendlyMessage); //This sends the message to the database

                    mMessageEditText.setText(""); //Clear input box after sending message
                }
            }
        });




        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            //This is triggered whenever there is a change in authentication state which includes sign in or sign out
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                //The parameter firebaseAuth, unlike mFirebaseAuth, is guaranteed to contain at that moment user is authenticated or not

                FirebaseUser user = firebaseAuth.getCurrentUser();

                if(user != null){
                    //User is signed in- DO NOTHING
                    onSignedInInitialize(user.getDisplayName());//This sends the username of the user
                }
                else{
                    //User is signed out- OPEN THE LOGIN PAGE
                    onSignedOutCleanup();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false) //Save user credentials and try to log them in- DISABLED
                                    .setTheme(R.style.LoginChatAppTheme)
                                    .setLogo(R.drawable.app_icon_logo_360dp)
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.GoogleBuilder().build(),
                                            new AuthUI.IdpConfig.EmailBuilder().build(),
                                            new AuthUI.IdpConfig.FacebookBuilder().build()
                                    ))
                                    .build(),
                            RC_SIGN_IN); //RC is request code which acts as a flag during sign in
                }
            }
        };


        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);

        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put(MSG_LENGTH_LIMIT_KEY, DEFAULT_MSG_LENGTH_LIMIT);
        mFirebaseRemoteConfig.setDefaults(defaultConfigMap);
        fetchConfig();

    }

    private void fetchConfig() {

        long cacheExpiration = 3600;

        if(mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()){
            cacheExpiration = 0;
        }

        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                mFirebaseRemoteConfig.activateFetched();
                applyRetrieveLengthLimit();
            }
        })
                .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                Log.w(TAG, "Error fetching config", e);
                applyRetrieveLengthLimit();

            }
        });
    }

    private void applyRetrieveLengthLimit() {

        long friendly_msg_length = mFirebaseRemoteConfig.getLong(MSG_LENGTH_LIMIT_KEY);
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter((int)friendly_msg_length)});
        Log.d(TAG, MSG_LENGTH_LIMIT_KEY + "=" + friendly_msg_length);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //Returns the results here when startActivityForResult is called

        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == RC_SIGN_IN){
            //This method is used to let the back button close the app rather than coming back to the same login page due to failed login

            if(resultCode == RESULT_OK){
                Toast.makeText(MainActivity.this, "Signed in Successfully", Toast.LENGTH_SHORT).show();
            }
            else if(resultCode == RESULT_CANCELED){
                Toast.makeText(MainActivity.this, "Sign in Failed", Toast.LENGTH_SHORT).show();
                finish(); //This closes the activity when back button is pressed at the login screen
            }
        }
        else if(requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK){

            Uri selectedImageUri = data.getData(); //Get the info of the photo in form of uri and store it in selectedImageUri
            final StorageReference photoRef = mChatPhotosStorageReference.child(selectedImageUri.getLastPathSegment());
            //Take the storage reference and create a child with data selectedImageUri and name through getLastPathSegment()


            photoRef.putFile(selectedImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    //Gets to this if push is successful


                    photoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        //The Storage Reference photoRef is used to get the download url and this code is executed if it's successful

                        @Override
                        public void onSuccess(Uri uri) {

                            Uri downloadUrl = uri;  //Gets the url of the photo which is stored in uri

                            Calendar calendar = Calendar.getInstance();
                            final String currentdate = DateFormat.getTimeInstance().format(calendar.getTime());
                            SimpleDateFormat format = new SimpleDateFormat("MMM dd, yyyy- EEE h:mm a", Locale.ENGLISH);
                            String time = format.format(calendar.getTime());

                            FriendlyMessage friendlyMessage = new FriendlyMessage(null, mUsername, downloadUrl.toString(), time);
                            //Make an object of the friendly message with username and photo url

                            mMessagesDatabaseReference.push().setValue(friendlyMessage);
                            //Store a message in the database with the values of friendlyMessage
                        }
                    });

                }
            }); //Upload photo to storage and get its url

        }


    }



    //Used to convert Menu xml files to Menu objects
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu); //It takes R.menu.main_menu xml file and convert it to the Menu object menu
        return true;
    }


    //Provides action to the menus
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.sign_out_menu:
                //Sign out

                AuthUI.getInstance().signOut(this);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }



    public void setViewByIds(){

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);
    }


    public void onSignedInInitialize(String username){
        mUsername = username;
        attachDatabaseReadListener();
    }


    public void onSignedOutCleanup(){

        mUsername = ANONYMOUS;
        mMessageAdapter.clear(); //As an anonymous person should not be able to see our messages and remove duplicate messages for multiple login
        detachDatabaseReadListener();
    }


    public void attachDatabaseReadListener() {

        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    //Trigger every time it is attached or app is opened and every time a new message is added

                    FriendlyMessage friendlyMessage = dataSnapshot.getValue(FriendlyMessage.class);
                    //dataSnapshot has the values of the current messages
                    // so we take the dataSnapshot and get the value and convert it to a FriendlyMessage object

                    mMessageAdapter.add(friendlyMessage);
                    //Adds the FriendlyMessage object to the adapter

                    getNotify(friendlyMessage.name, friendlyMessage.text);

                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    //Trigger every time the contents of a message are changes
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                    //Trigger every time a message is removed
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    //Trigger every time the position of amy message is shifted
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    //Trigger every time a message is failed to be added
                }
            };
            mMessagesDatabaseReference.addChildEventListener(mChildEventListener);
            //This attaches the child event listener to the given database reference
        }
    }


    public void detachDatabaseReadListener(){

        if(mChildEventListener != null){
            mMessagesDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener); //Adds authStateListener every time app is opened
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);//Removes authStateListener every time app is stopped
        }
        detachDatabaseReadListener();
        mMessageAdapter.clear();
    }


    public void getNotify(String title, String description){

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_1_ID)
                .setSmallIcon(R.drawable.ic_forum_black_24dp)
                .setContentTitle(title)
                .setContentText(description)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build();

        notificationManager.notify(1, notification);
    }

}