package com.doctors;


import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Objects;

public class register extends AppCompatActivity {

    EditText txt1, txt2, txt3, Email;
    ImageView ImgUserPhoto;
    Button rgbtn;
    static int PReqCode = 1;
    static int REQUESCODE = 1;

    Uri pickedImgUri;
    FirebaseAuth mAuth;

    private DatabaseReference mUser = FirebaseDatabase.getInstance().getReference();
    private ProgressDialog mRegProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        txt1 = findViewById(R.id.name);
        txt2 = findViewById(R.id.password);
        txt3 = findViewById(R.id.number);
        Email = findViewById(R.id.email);
        mRegProgress = new ProgressDialog(this);
        mAuth = FirebaseAuth.getInstance();
        rgbtn = findViewById(R.id.button_register);


        rgbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String Username = txt1.getText().toString();
                final String password = txt2.getText().toString();
                final String number = txt3.getText().toString();

                final String email = Email.getText().toString();
                if (email.isEmpty() || Username.isEmpty() || password.isEmpty()) {

                    showMessage("Please Verify all fields");

                    // something goes wrong : all fields must be filled
                    // we need to display an error message

                } else {
                    // everything is ok and all fields are filled now we can start creating user account
                    // CreateUserAccount method will try to create the user if the email is valid

                    CreateUserAccount(email, Username, password);
                }

            }
        });

        ImgUserPhoto = findViewById(R.id.doctors_image);
        ImgUserPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (Build.VERSION.SDK_INT >= 24) {

                    checkAndRequestForPermission();


                } else {
                    openGallery();
                }


            }
        });


    }


    private void openGallery() {
        //TODO: open gallery intent and wait for user to pick an image !

        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, REQUESCODE);
    }


    private void checkAndRequestForPermission() {

        if (ContextCompat.checkSelfPermission(register.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(register.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

                Toast.makeText(register.this, "Please accept for required permission", Toast.LENGTH_SHORT).show();

            } else {
                ActivityCompat.requestPermissions(register.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PReqCode);
            }

        } else
            openGallery();


    }

    private void CreateUserAccount(final String email, final String username, final String password) {

        mRegProgress.setTitle("Registering User");
        mRegProgress.setMessage("Please wait registration depends on image size!");
        mRegProgress.setCanceledOnTouchOutside(false);
        mRegProgress.show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                            assert currentUser != null;
                            String uid = currentUser.getUid();
                            HashMap<String, String> userDetails = new HashMap<>();
                            userDetails.put("Name", username);
                            userDetails.put("Email", email);
                            userDetails.put("id",uid);
                            mUser.child("test").child(uid).setValue(userDetails);


                            // user account created successfully

                            // after we created user account we need to update his profile picture and name
                            updateUserInfo(username, pickedImgUri, mAuth.getCurrentUser());


                        } else {
                            showMessage("account creation failed" + Objects.requireNonNull(task.getException()).getMessage());

                            // account creation failed


                        }
                    }
                });

    }

    private void updateUserInfo(final String username, Uri pickedImgUri, final FirebaseUser currentUser) {


        StorageReference mStorage = FirebaseStorage.getInstance().getReference().child("users_photos");
        final StorageReference imageFilePath = mStorage.child(Objects.requireNonNull(pickedImgUri.getLastPathSegment()));
        imageFilePath.putFile(pickedImgUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                // image uploaded succesfully
                // now we can get our image url

                imageFilePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {

                        // uri contain user image url


                        UserProfileChangeRequest profleUpdate = new UserProfileChangeRequest.Builder()
                                .setDisplayName(username)
                                .setPhotoUri(uri)
                                .build();


                        currentUser.updateProfile(profleUpdate)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {

                                        if (task.isSuccessful()) {
                                            mRegProgress.dismiss();
                                            // user info updated successfully
                                            showMessage("Register Complete");

                                            updateUI();
                                        }

                                    }
                                });

                    }
                });


            }
        });


    }

    private void updateUI() {

        Intent homeActivity = new Intent(getApplicationContext(), dash.class);
        startActivity(homeActivity);
        finish();


    }

    private void showMessage(String message) {

        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == REQUESCODE && data != null) {

            // the user has successfully picked an image
            // we need to save its reference to a Uri variable
            pickedImgUri = data.getData();
            ImgUserPhoto.setImageURI(pickedImgUri);


        }
    }
}

