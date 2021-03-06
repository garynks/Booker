package com.example.booker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * Handles viewing a photo by owner or borrower
 */
public class ViewPhotoActivity extends AppCompatActivity {
    private Book book;
    private String type;
    private ImageView imageView;
    private Button deleteBtn;
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference storageRef;
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_photo);

        // get book and user's access type
        book = (Book) getIntent().getSerializableExtra("Book");
        type = getIntent().getStringExtra("Type");

        // set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar6);
        setSupportActionBar(toolbar);
        ActionBar myToolbar = getSupportActionBar();
        myToolbar.setDisplayHomeAsUpEnabled(true);
        // toolbar back button
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        myToolbar.setTitle(book.getTitle());

        deleteBtn = findViewById(R.id.deletePhotoBtn);
        final CollectionReference collection = db.collection("Books");
        final HashMap<String, Object> map = new HashMap<>();

        // borrower can only view
        map.put("imageURI", "");
        if (type.compareTo("borrower") == 0) {
            storageRef = storage.getReference(book.getOwnerUsername() + "/" + book.getUID());
            deleteBtn.setClickable(false);
            deleteBtn.setVisibility(View.GONE);
        } // owner can delete picture
        else if (type.compareTo("owner") == 0) {
            storageRef = storage.getReference(user.getDisplayName() + "/" + book.getUID());
            deleteBtn.setClickable(true);
            deleteBtn.setVisibility(View.VISIBLE);
            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    book.setImageURI("");
                    collection.document(book.getUID()).update(map);
                    storageRef.delete();
                    imageView.setImageResource(R.drawable.defaultphoto);
                    deleteBtn.setAlpha(0.9f);
                    deleteBtn.setEnabled(false);
                }
            });
        }

        // Code for loading photo into the imageView
        imageView = findViewById(R.id.fullImage);
        try {
            final File file = File.createTempFile(book.getUID(), "jpg");
            storageRef.getFile(file).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                    imageView.setImageBitmap(bitmap);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
