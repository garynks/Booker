package com.example.booker;

import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.booker.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Screen for viewing a user profile. Has edit or view only modes
 * Bugs: editing username and email can cause the database to lose references
 */
public class user_profile extends AppCompatActivity {

    private String userID, userEmail;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
    private FirebaseAuth firebaseAuth;
    private TextView name, email, phone;
    private Button saveBtn;
    private List<String> s;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar2);
        setSupportActionBar(toolbar);
        ActionBar myToolbar = getSupportActionBar();
        myToolbar.setDisplayHomeAsUpEnabled(true);
        myToolbar.setTitle("User Profile");
        saveBtn = findViewById(R.id.saveButton);

        // initialize edittexts
        final EditText nameEditText = findViewById(R.id.editTextName);
        final EditText emailEditText = findViewById(R.id.editTextEmail);
        final EditText phoneEditText = findViewById(R.id.editTextPhone);
        final TextView textViewUsername = findViewById(R.id.textViewUsername);

        // get passed access type and email
        String profileType = getIntent().getStringExtra("profileType");
        final String profileEmail = getIntent().getStringExtra("profileEmail");

        // disable editing if read only
        if (profileType.equals("READ_ONLY")) {
            saveBtn.setVisibility(View.GONE);
            nameEditText.setInputType(InputType.TYPE_NULL);
            emailEditText.setInputType(InputType.TYPE_NULL);
            phoneEditText.setInputType(InputType.TYPE_NULL);
        }

        // access user's document
        final Query query = db.collection("Users")
                .whereEqualTo("email", profileEmail);

        // set edit text to current values
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                for (QueryDocumentSnapshot documentSnapshot: task.getResult()) {
                    nameEditText.setText(documentSnapshot.getString("name"));
                    emailEditText.setText(documentSnapshot.getString("email"));
                    phoneEditText.setText(documentSnapshot.getString("phone"));
                    textViewUsername.setText(documentSnapshot.getString("username"));
                }
            }
        });

        // save edit text values to firestore and exit
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HashMap<String, String> data = new HashMap<>();
                final String newEmail = emailEditText.getText().toString();

                // search books for old email and change to new email
                Query queryBooks = db.collection("Books")
                        .whereEqualTo("ownerEmail", profileEmail);
                queryBooks.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        for (QueryDocumentSnapshot documentSnapshot: task.getResult()) {
                            db.collection("Books")
                                    .document(documentSnapshot.getId())
                                    .update("ownerEmail", newEmail);
                        }
                    }
                });

                // search requests for old email and change to new email
                Query queryRequests = db.collection("Requests")
                        .whereEqualTo("ownerEmail", profileEmail);
                queryRequests.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        for (QueryDocumentSnapshot documentSnapshot: task.getResult()) {
                            db.collection("Requests")
                                    .document(documentSnapshot.getId())
                                    .update("ownerEmail", newEmail);
                        }
                    }
                });

                data.put("name", nameEditText.getText().toString());
                data.put("email", newEmail);
                data.put("phone", phoneEditText.getText().toString());
                data.put("username", textViewUsername.getText().toString());

                // add new user with old information but new email
                db.collection("Users").document(newEmail).set(data);

                // delete old user with old email
                db.collection("Users").document(profileEmail).delete();

                // update email for FiresbaseAuth
                firebaseUser.updateEmail(emailEditText.getText().toString());

                finish();
            }
        });

        // toolbar back button
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

}
