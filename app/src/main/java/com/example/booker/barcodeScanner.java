package com.example.booker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;

public class barcodeScanner extends AppCompatActivity {

    private SurfaceView surfaceView;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private static final int REQUEST_CAMERA_PERMISSION = 201;
    //This class provides methods to play DTMF tones
    private ToneGenerator toneGen1;
    private TextView barcodeText;
    private String barcodeData;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private String userEmail = user.getEmail();
    private CollectionReference bookCollection = db.collection("Books");
    private String scanType;
    private TextView name;
    private TextView author;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC,     100);
        surfaceView = findViewById(R.id.surface_view);
        barcodeText = findViewById(R.id.barcode_text);
        name = findViewById(R.id.name);
        author = findViewById(R.id.author);
        
        // Grabs the type of scan this activity is doing
        scanType = getIntent().getExtras().getString("ScanType");
        
        initialiseDetectorsAndSources();
    }

    /** Barcode Scanner from Hari Lee on Medium
     * https://medium.com/analytics-vidhya/creating-a-barcode-scanner-using-android-studio-71cff11800a2#id_token=eyJhbGciOiJSUzI1NiIsImtpZCI6ImRlZGMwMTJkMDdmNTJhZWRmZDVmOTc3ODRlMWJjYmUyM2MxOTcyNGQiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJuYmYiOjE2MDU4NDMxODAsImF1ZCI6IjIxNjI5NjAzNTgzNC1rMWs2cWUwNjBzMnRwMmEyamFtNGxqZGNtczAwc3R0Zy5hcHBzLmdvb2dsZXVzZXJjb250ZW50LmNvbSIsInN1YiI6IjExNzMzMDY2MzM1OTE1ODEzNjcxMSIsImVtYWlsIjoiYXJiZWxhZXpjaEBnbWFpbC5jb20iLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiYXpwIjoiMjE2Mjk2MDM1ODM0LWsxazZxZTA2MHMydHAyYTJqYW00bGpkY21zMDBzdHRnLmFwcHMuZ29vZ2xldXNlcmNvbnRlbnQuY29tIiwibmFtZSI6IkFyYmVsYWV6Y2giLCJwaWN0dXJlIjoiaHR0cHM6Ly9saDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL2EtL0FPaDE0R2haZUxfUzg5VzY4NHgzMVhsU19JYWdJbmJfQ1pmakFHT18wR1JTPXM5Ni1jIiwiZ2l2ZW5fbmFtZSI6IkFyYmVsYWV6Y2giLCJpYXQiOjE2MDU4NDM0ODAsImV4cCI6MTYwNTg0NzA4MCwianRpIjoiZTZkZGRmZGFhMDJiOGE2NzdhYzczNjJkMTNhNzAwMzk1NGMxNTg0MSJ9.QU_019yMX8sGRhQJIEQ0xnqykda_IkR241pU6AcXGFHMmRVPHoCVRxeIa77jqVNU1BRTGymuEXezj4jgu-tg6y17KVQP0Anb5aaPN3hRf8elJXfz3ynHOIuuemww6_u6dIU05e3dkVcSwtwzXUm7yE-d8UnzdpetxqjZL3MXGQ5GTWY_b4u4oe0CtgRFrOaUi2smGcthlulQ7nMTrBC6QhO7UMAYiLuSWVyNHQNLcdujBLCd1nboxasxi2aTkUfWBjqUl9CUtiJ3tZ7kcMPM5sQzrSmgIarPJrsRcgaq6ZP8z4yVo4SJFnmjJ6eRjxLCQPi2ne79miZQsifNUNHt6Q
     */
    private void initialiseDetectorsAndSources() {

        //Toast.makeText(getApplicationContext(), "Barcode scanner started", Toast.LENGTH_SHORT).show();

        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build();

        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(1920, 1080)
                .setAutoFocusEnabled(true) //you should add this feature
                .build();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraSource.start(surfaceView.getHolder());
                    } else {
                        ActivityCompat.requestPermissions(barcodeScanner.this, new
                                String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }


            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });




        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
                // Toast.makeText(getApplicationContext(), "To prevent memory leaks barcode scanner has been stopped", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() != 0) {


                    barcodeText.post(new Runnable() {

                        @Override
                        public void run() {

                            if (barcodes.valueAt(0).email != null) {
                                barcodeText.removeCallbacks(null);
                                barcodeData = barcodes.valueAt(0).email.address;
                                barcodeText.setText(barcodeData);
                                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
                            } else {

                                barcodeData = barcodes.valueAt(0).displayValue;
                                barcodeText.setText(barcodeData);
                                checkBookBorrowed(barcodeData);
                                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);

                            }
                        }
                    });

                }
            }
        });
    }
    
    private void checkBookBorrowed(String ISBN) {
        Query query = bookCollection.whereEqualTo("ownerEmail", userEmail).whereEqualTo("ISBN", ISBN);
        
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        document.getData();
                        name.setText(document.getString("title"));
                        author.setText(document.getString("author"));
                    }
                }
            }
        });
        
    }


    
}
