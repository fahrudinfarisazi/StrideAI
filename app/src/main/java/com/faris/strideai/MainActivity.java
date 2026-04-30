package com.faris.strideai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.content.SharedPreferences;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Html;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.ai.client.generativeai.java.ChatFutures;
import java.util.concurrent.Executors;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Locale;
import android.location.Geocoder;
import android.location.Address;
import java.util.List;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.config.Configuration;
import org.osmdroid.views.MapView;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.overlay.TilesOverlay;
import android.graphics.ColorMatrixColorFilter;
import android.preference.PreferenceManager;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import android.os.Looper;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import android.graphics.Bitmap;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements android.hardware.SensorEventListener {

    private MapView mMapView;
    private List<GeoPoint> routePoints = new java.util.ArrayList<>();
    private LocationCallback locationCallback;

    private android.hardware.SensorManager sensorManager;
    private android.hardware.Sensor stepCounterSensor;
    private SharedPreferences prefs;
    private Runnable updateProfileUI;

    // Session State
    private boolean isSessionActive = false;
    private long sessionStartTimeMs = 0;
    private int sessionStartSteps = 0;
    private android.os.Handler timerHandler = new android.os.Handler();
    private Runnable timerRunnable;

    // Map Views
    private TextView tvMapPace;
    private TextView tvMapTime;
    private TextView tvMapCalories;
    private TextView tvLocation;
    private FusedLocationProviderClient fusedLocationClient;

    private ActivityResultLauncher<Intent> photoPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        prefs = getSharedPreferences("StrideAI_Prefs", MODE_PRIVATE);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        View dashboardLayout = findViewById(R.id.dashboardLayout);
        View mapLayout = findViewById(R.id.mapLayout);
        View fabAI = findViewById(R.id.fabAI);

        View profileLayout = findViewById(R.id.profileLayout);
        View editProfileLayout = findViewById(R.id.editProfileLayout);
        
        View btnEditProfile = findViewById(R.id.btnEditProfile);
        View btnBackEdit = findViewById(R.id.btnBackEdit);

        TextView tvName = findViewById(R.id.tvName);
        EditText etEditName = findViewById(R.id.etEditName);
        TextView tvPhysicalStats = findViewById(R.id.tvPhysicalStats);
        EditText etEditHeight = findViewById(R.id.etEditHeight);
        EditText etEditWeight = findViewById(R.id.etEditWeight);
        EditText etEditEmail = findViewById(R.id.etEditEmail);
        EditText etEditAge = findViewById(R.id.etEditAge);
        View btnSaveChanges = findViewById(R.id.btnSaveChanges);
        View btnSaveTop = findViewById(R.id.btnSaveTop);

        photoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedUri = result.getData().getData();
                    if (selectedUri != null) {
                        try {
                            getContentResolver().takePersistableUriPermission(selectedUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (SecurityException e) {
                            e.printStackTrace();
                        }
                        prefs.edit().putString("profile_photo_uri", selectedUri.toString()).apply();
                        if (updateProfileUI != null) updateProfileUI.run();
                    }
                }
            }
        );

        View btnChangePhoto = findViewById(R.id.btnChangePhoto);
        View cardAvatarEdit = findViewById(R.id.cardAvatarEdit);
        View.OnClickListener pickPhotoListener = v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            photoPickerLauncher.launch(intent);
        };
        if (btnChangePhoto != null) btnChangePhoto.setOnClickListener(pickPhotoListener);
        if (cardAvatarEdit != null) cardAvatarEdit.setOnClickListener(pickPhotoListener);

        // Dashboard stats views
        TextView tvDashboardDate = findViewById(R.id.tvDashboardDate);
        TextView tvDashboardGreeting = findViewById(R.id.tvDashboardGreeting);
        TextView tvStepCount = findViewById(R.id.tvStepCount);
        TextView tvStepGoal = findViewById(R.id.tvStepGoal);
        TextView tvCalories = findViewById(R.id.tvCalories);
        TextView tvDistance = findViewById(R.id.tvDistance);
        tvLocation = findViewById(R.id.tvLocation);
        android.widget.ProgressBar stepProgressBar = findViewById(R.id.stepProgressBar);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fetchLocation();

        updateProfileUI = () -> {
            String name = prefs.getString("name", "ELARA VANCE");
            String gender = prefs.getString("gender", "Male");
            String height = prefs.getString("height", "180");
            String weight = prefs.getString("weight", "75");
            String email = prefs.getString("email", "");
            String age = prefs.getString("age", "28");

            // Stats
            int stepCount = prefs.getInt("step_count", 0);
            int stepGoal = prefs.getInt("step_goal", 10000);
            int calories = prefs.getInt("calories", 0);
            float distance = prefs.getFloat("distance", 0f);

            if (tvName != null) tvName.setText(name);
            if (etEditName != null) etEditName.setText(name);
            if (tvPhysicalStats != null) tvPhysicalStats.setText(gender + " • " + height + " cm • " + weight + " kg");
            if (etEditHeight != null) etEditHeight.setText(height);
            if (etEditWeight != null) etEditWeight.setText(weight);
            if (etEditEmail != null) etEditEmail.setText(email);
            if (etEditAge != null) etEditAge.setText(age);

            // Update Dashboard
            if (tvDashboardDate != null) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEEE, MMM dd", java.util.Locale.US);
                tvDashboardDate.setText(sdf.format(new java.util.Date()).toUpperCase());
            }
            if (tvDashboardGreeting != null) tvDashboardGreeting.setText("Hello, " + name.split(" ")[0]);
            if (tvStepCount != null) tvStepCount.setText(String.format("%,d", stepCount));
            if (tvStepGoal != null) tvStepGoal.setText("🏁 GOAL: " + String.format("%,d", stepGoal));
            if (tvCalories != null) tvCalories.setText(String.valueOf(calories));
            if (tvDistance != null) tvDistance.setText(String.format("%.1f", distance));
            if (stepProgressBar != null) {
                stepProgressBar.setMax(stepGoal);
                stepProgressBar.setProgress(stepCount);
            }
            
            TextView tvMonthlyRunsCount = findViewById(R.id.tvMonthlyRunsCount);
            TextView tvStreakCount = findViewById(R.id.tvStreakCount);
            if (tvMonthlyRunsCount != null) {
                java.util.List<com.faris.strideai.models.ActivitySession> sessions = com.faris.strideai.utils.SessionManager.getSessions(MainActivity.this);
                java.util.Calendar currentCal = java.util.Calendar.getInstance();
                int currentMonth = currentCal.get(java.util.Calendar.MONTH);
                int currentYear = currentCal.get(java.util.Calendar.YEAR);
                
                int monthlyRuns = 0;
                boolean activeToday = false;
                for (com.faris.strideai.models.ActivitySession session : sessions) {
                    java.util.Calendar sessionCal = java.util.Calendar.getInstance();
                    sessionCal.setTimeInMillis(session.getTimestamp());
                    if (sessionCal.get(java.util.Calendar.MONTH) == currentMonth && 
                        sessionCal.get(java.util.Calendar.YEAR) == currentYear) {
                        monthlyRuns++;
                    }
                    if (!activeToday && session.getTimestamp() >= currentCal.getTimeInMillis() - (24*60*60*1000L)) {
                        activeToday = true;
                    }
                }
                tvMonthlyRunsCount.setText(String.valueOf(monthlyRuns));
                if (tvStreakCount != null) {
                    tvStreakCount.setText(activeToday ? "1" : "0");
                }
            }
            
            ImageView imgAvatarProfile = findViewById(R.id.imgAvatarProfile);
            ImageView imgAvatarEdit = findViewById(R.id.imgAvatarEdit);
            String savedPhotoUri = prefs.getString("profile_photo_uri", null);
            if (savedPhotoUri != null) {
                try {
                    Uri uri = Uri.parse(savedPhotoUri);
                    if (imgAvatarProfile != null) {
                        imgAvatarProfile.setImageURI(uri);
                        imgAvatarProfile.setImageTintList(null);
                    }
                    if (imgAvatarEdit != null) {
                        imgAvatarEdit.setImageURI(uri);
                        imgAvatarEdit.setImageTintList(null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                if (imgAvatarProfile != null) {
                    imgAvatarProfile.setImageResource(android.R.drawable.ic_menu_myplaces);
                    imgAvatarProfile.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#B0BEC5")));
                }
                if (imgAvatarEdit != null) {
                    imgAvatarEdit.setImageResource(android.R.drawable.ic_menu_myplaces);
                    imgAvatarEdit.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#B0BEC5")));
                }
            }
        };

        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        
        mMapView = findViewById(R.id.mapView);
        if (mMapView != null) {
            mMapView.setTileSource(TileSourceFactory.MAPNIK);
            mMapView.setMultiTouchControls(true);
            mMapView.setBuiltInZoomControls(false);
            mMapView.getController().setZoom(15.0);
            
            // Apply dark mode filter
            TilesOverlay overlay = mMapView.getOverlayManager().getTilesOverlay();
            if (overlay != null) {
                overlay.setColorFilter(new ColorMatrixColorFilter(new float[] {
                    -1,  0,  0, 0, 255, // red
                     0, -1,  0, 0, 255, // green
                     0,  0, -1, 0, 255, // blue
                     0,  0,  0, 1,   0  // alpha
                }));
            }
            
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                    if (location != null) {
                        GeoPoint currentPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        mMapView.getController().setCenter(currentPoint);
                    }
                });
            }
        }

        ImageView btnShareMap = findViewById(R.id.btnShareMap);
        if (btnShareMap != null) {
            btnShareMap.setOnClickListener(v -> shareMapSnapshot());
        }

        View fabZoomIn = findViewById(R.id.fabZoomIn);
        View fabZoomOut = findViewById(R.id.fabZoomOut);
        View fabLocation = findViewById(R.id.fabLocation);

        if (fabZoomIn != null) {
            fabZoomIn.setOnClickListener(v -> {
                if (mMapView != null) mMapView.getController().zoomIn();
            });
        }
        if (fabZoomOut != null) {
            fabZoomOut.setOnClickListener(v -> {
                if (mMapView != null) mMapView.getController().zoomOut();
            });
        }
        if (fabLocation != null) {
            fabLocation.setOnClickListener(v -> {
                if (mMapView != null && androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.getLastLocation().addOnSuccessListener(this, loc -> {
                        if (loc != null) {
                            mMapView.getController().animateTo(new GeoPoint(loc.getLatitude(), loc.getLongitude()));
                        }
                    });
                }
            });
        }

        // Map feature views
        com.google.android.material.button.MaterialButton btnStartMap = findViewById(R.id.btnStartMap);
        tvMapPace = findViewById(R.id.tvMapPace);
        tvMapTime = findViewById(R.id.tvMapTime);
        tvMapCalories = findViewById(R.id.tvMapCalories);

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isSessionActive) {
                    long elapsedMs = System.currentTimeMillis() - sessionStartTimeMs;
                    int seconds = (int) (elapsedMs / 1000) % 60;
                    int minutes = (int) ((elapsedMs / (1000 * 60)) % 60);
                    int hours = (int) ((elapsedMs / (1000 * 60 * 60)) % 24);
                    
                    if (tvMapTime != null) {
                        tvMapTime.setText(String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds));
                    }
                    
                    int currentGlobalSteps = prefs.getInt("step_count", 0);
                    int sessionSteps = Math.max(0, currentGlobalSteps - sessionStartSteps);
                    float sessionDistanceKm = sessionSteps * 0.000762f;
                    int sessionCalories = sessionSteps / 20;
                    
                    if (tvMapCalories != null) {
                        tvMapCalories.setText(String.valueOf(sessionCalories));
                    }
                    
                    if (sessionDistanceKm > 0.01f) {
                        float totalMinutes = elapsedMs / 60000.0f;
                        float paceDecimal = totalMinutes / sessionDistanceKm;
                        int paceMinutes = (int) paceDecimal;
                        int paceSeconds = (int) ((paceDecimal - paceMinutes) * 60);
                        if (tvMapPace != null) {
                            tvMapPace.setText(String.format(java.util.Locale.US, "%d'%02d\"", paceMinutes, paceSeconds));
                        }
                    }
                    
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };

        if (btnStartMap != null) {
            btnStartMap.setOnClickListener(v -> {
                if (!isSessionActive) {
                    // Start Session
                    isSessionActive = true;
                    sessionStartTimeMs = System.currentTimeMillis();
                    sessionStartSteps = prefs.getInt("step_count", 0);
                    routePoints.clear();
                    if (mMapView != null) mMapView.getOverlays().removeIf(o -> o instanceof Polyline);
                    
                    LocationRequest locationRequest = LocationRequest.create();
                    locationRequest.setInterval(5000);
                    locationRequest.setFastestInterval(2000);
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

                    locationCallback = new LocationCallback() {
                        @Override
                        public void onLocationResult(@NonNull LocationResult locationResult) {
                            for (android.location.Location location : locationResult.getLocations()) {
                                GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                                routePoints.add(geoPoint);
                                if (mMapView != null) {
                                    mMapView.getOverlays().removeIf(o -> o instanceof Polyline);
                                    Polyline line = new Polyline();
                                    line.setPoints(routePoints);
                                    line.getOutlinePaint().setColor(android.graphics.Color.parseColor("#00D4FF"));
                                    line.getOutlinePaint().setStrokeWidth(12f);
                                    mMapView.getOverlays().add(line);
                                    mMapView.getController().animateTo(geoPoint);
                                    mMapView.invalidate();
                                }
                            }
                        }
                    };
                    if (androidx.core.content.ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
                    }

                    btnStartMap.setText("FINISH");
                    btnStartMap.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#B71C1C"))); // Red
                    
                    if (tvMapPace != null) tvMapPace.setText("0'00\"");
                    if (tvMapCalories != null) tvMapCalories.setText("0");
                    if (tvMapTime != null) tvMapTime.setText("00:00:00");
                    
                    timerHandler.post(timerRunnable);
                } else {
                    // Stop Session
                    isSessionActive = false;
                    timerHandler.removeCallbacks(timerRunnable);
                    if (locationCallback != null) {
                        fusedLocationClient.removeLocationUpdates(locationCallback);
                    }
                    
                    int currentGlobalSteps = prefs.getInt("step_count", 0);
                    int sessionSteps = Math.max(0, currentGlobalSteps - sessionStartSteps);
                    float sessionDistanceKm = sessionSteps * 0.000762f;
                    int sessionCalories = sessionSteps / 20;
                    long elapsedMs = System.currentTimeMillis() - sessionStartTimeMs;
                    
                    if (sessionSteps > 0 || elapsedMs > 60000) {
                        String sessionId = java.util.UUID.randomUUID().toString();
                        String title = "Activity Session";
                        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
                        if (hour >= 5 && hour < 12) title = "Morning Run";
                        else if (hour >= 12 && hour < 17) title = "Afternoon Walk";
                        else if (hour >= 17 && hour < 21) title = "Evening Run";
                        else title = "Night Walk";
                        
                        String snapshotPath = null;
                        if (mMapView != null) {
                            try {
                                Bitmap bitmap = Bitmap.createBitmap(mMapView.getWidth(), mMapView.getHeight(), Bitmap.Config.ARGB_8888);
                                android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
                                mMapView.draw(canvas);
                                
                                File cachePath = new File(getCacheDir(), "images");
                                cachePath.mkdirs();
                                File imagePath = new File(cachePath, "route_snapshot_" + sessionId + ".png");
                                FileOutputStream stream = new FileOutputStream(imagePath);
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                                stream.close();
                                snapshotPath = imagePath.getAbsolutePath();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        
                        // Clear the map data after taking snapshot
                        routePoints.clear();
                        if (mMapView != null) {
                            mMapView.getOverlays().removeIf(o -> o instanceof Polyline);
                            mMapView.invalidate();
                        }
                        
                        com.faris.strideai.models.ActivitySession session = new com.faris.strideai.models.ActivitySession(
                            sessionId, title, sessionStartTimeMs, sessionDistanceKm, sessionSteps, sessionCalories, elapsedMs, snapshotPath
                        );
                        com.faris.strideai.utils.SessionManager.saveSession(MainActivity.this, session);
                        Toast.makeText(MainActivity.this, "Session saved to History", Toast.LENGTH_SHORT).show();
                        
                        // Automatically open History screen
                        startActivity(new Intent(MainActivity.this, HistoryActivity.class));
                    } else {
                        // Also clear the map data if session is discarded
                        routePoints.clear();
                        if (mMapView != null) {
                            mMapView.getOverlays().removeIf(o -> o instanceof Polyline);
                            mMapView.invalidate();
                        }
                        Toast.makeText(MainActivity.this, "Session too short to be saved.", Toast.LENGTH_SHORT).show();
                    }

                    btnStartMap.setText("START");
                    btnStartMap.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#000080"))); // Blue
                }
            });
        }

        // Initialize display
        updateProfileUI.run();

        View.OnClickListener saveListener = v -> {
            if (etEditHeight != null && etEditWeight != null && etEditName != null) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("name", etEditName.getText().toString());
                editor.putString("height", etEditHeight.getText().toString());
                editor.putString("weight", etEditWeight.getText().toString());
                editor.putString("email", etEditEmail.getText().toString());
                editor.putString("age", etEditAge.getText().toString());
                editor.apply();
                
                updateProfileUI.run();
                Toast.makeText(this, "Profile metrics synchronized", Toast.LENGTH_SHORT).show();
            }
            editProfileLayout.setVisibility(View.GONE);
            profileLayout.setVisibility(View.VISIBLE);
        };
        
        if (btnSaveChanges != null) btnSaveChanges.setOnClickListener(saveListener);
        if (btnSaveTop != null) btnSaveTop.setOnClickListener(saveListener);

        btnEditProfile.setOnClickListener(v -> {
            updateProfileUI.run();
            profileLayout.setVisibility(View.GONE);
            editProfileLayout.setVisibility(View.VISIBLE);
        });

        btnBackEdit.setOnClickListener(v -> {
            editProfileLayout.setVisibility(View.GONE);
            profileLayout.setVisibility(View.VISIBLE);
        });

        View btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();
                GoogleSignIn.getClient(MainActivity.this, gso).signOut().addOnCompleteListener(task -> {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            });
        }

        View.OnClickListener historyListener = v -> {
            startActivity(new Intent(MainActivity.this, HistoryActivity.class));
        };
        View btnHistory = findViewById(R.id.btnHistory);
        View btnHistoryProfile = findViewById(R.id.btnHistoryProfile);
        View btnHistoryMap = findViewById(R.id.btnHistoryMap);
        if (btnHistory != null) btnHistory.setOnClickListener(historyListener);
        if (btnHistoryProfile != null) btnHistoryProfile.setOnClickListener(historyListener);
        if (btnHistoryMap != null) btnHistoryMap.setOnClickListener(historyListener);

        // Initialize Gemini AI
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", BuildConfig.GEMINI_API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        Executor executor = Executors.newSingleThreadExecutor();

        fabAI.setOnClickListener(v -> {
            ChatFutures chat = model.startChat();
            
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(MainActivity.this);
            bottomSheetDialog.setContentView(R.layout.bottom_sheet_coach);
            bottomSheetDialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            
            bottomSheetDialog.setOnShowListener(dialog -> {
                com.google.android.material.bottomsheet.BottomSheetDialog d = (com.google.android.material.bottomsheet.BottomSheetDialog) dialog;
                android.widget.FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (bottomSheet != null) {
                    com.google.android.material.bottomsheet.BottomSheetBehavior<android.widget.FrameLayout> behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
                    behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
                    behavior.setSkipCollapsed(true);
                }
            });
            
            TextView tvCoachMessage = bottomSheetDialog.findViewById(R.id.tvCoachMessage);
            EditText etCoachInput = bottomSheetDialog.findViewById(R.id.etCoachInput);
            View btnSendCoach = bottomSheetDialog.findViewById(R.id.btnSendCoach);
            TextView chipCoach1 = bottomSheetDialog.findViewById(R.id.chipCoach1);
            TextView chipCoach2 = bottomSheetDialog.findViewById(R.id.chipCoach2);
            TextView chipCoach3 = bottomSheetDialog.findViewById(R.id.chipCoach3);

            if (tvCoachMessage != null) {
                tvCoachMessage.setMovementMethod(new android.text.method.ScrollingMovementMethod());
            }

            if (tvCoachMessage != null) {
                String name = prefs.getString("name", "Athlete");
                int steps = prefs.getInt("step_count", 0);
                int goal = prefs.getInt("step_goal", 10000);
                int percent = (goal > 0) ? (steps * 100 / goal) : 0;
                
                String timeOfDay = "Morning";
                int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
                if (hour >= 12 && hour < 17) timeOfDay = "Afternoon";
                else if (hour >= 17) timeOfDay = "Evening";

                String message = "Good " + timeOfDay + ", " + name.split(" ")[0] + "! You've reached <font color=\"#00D4FF\">" + percent + "%</font> of your step goal today.<br><br>Would you like me to analyze your weekly consistency or help you calculate your target for today's workout?";
                tvCoachMessage.setText(Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY));
            }
            
            View bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }

            Runnable sendPrompt = () -> {
                if (etCoachInput == null || tvCoachMessage == null) return;
                String prompt = etCoachInput.getText().toString().trim();
                if (prompt.isEmpty()) return;

                int currentGoal = prefs.getInt("step_goal", 10000);
                String hiddenInstruction = "\n\n[SYSTEM INSTRUCTION: You are StrideAI Coach. The user's current step goal is " + currentGoal + ". 1 calorie burned equals roughly 20 steps. If the user asks how many steps to burn Y calories, calculate it, suggest the new step goal, and ask if they want to set it as their daily goal. If the user agrees to update their goal to Z steps, you MUST append the exact string [SET_GOAL: Z] at the very end of your response. Use Indonesian language.]";
                
                String fullPrompt = prompt + hiddenInstruction;
                
                etCoachInput.setText("");
                tvCoachMessage.setText("Thinking...");

                Content content = new Content.Builder().addText(fullPrompt).build();
                ListenableFuture<GenerateContentResponse> response = chat.sendMessage(content);
                
                Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                    @Override
                    public void onSuccess(GenerateContentResponse result) {
                        runOnUiThread(() -> {
                            String responseText = result.getText();
                            if (responseText != null) {
                                Pattern pattern = Pattern.compile("\\[SET_GOAL:\\s*(\\d+)\\]");
                                Matcher matcher = pattern.matcher(responseText);
                                if (matcher.find()) {
                                    int newGoal = Integer.parseInt(matcher.group(1));
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putInt("step_goal", newGoal);
                                    editor.apply();
                                    
                                    updateProfileUI.run();
                                    Toast.makeText(MainActivity.this, "Target langkah diperbarui ke " + newGoal + "!", Toast.LENGTH_SHORT).show();
                                    
                                    responseText = responseText.replaceAll("\\[SET_GOAL:\\s*\\d+\\]", "").trim();
                                }
                                tvCoachMessage.setText(responseText);
                            }
                        });
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        runOnUiThread(() -> {
                            tvCoachMessage.setText("Error: " + t.getMessage());
                        });
                    }
                }, executor);
            };

            if (btnSendCoach != null) {
                btnSendCoach.setOnClickListener(view -> sendPrompt.run());
            }

            View.OnClickListener chipListener = view -> {
                if (view instanceof TextView && etCoachInput != null) {
                    etCoachInput.setText(((TextView) view).getText());
                    sendPrompt.run();
                }
            };

            if (chipCoach1 != null) chipCoach1.setOnClickListener(chipListener);
            if (chipCoach2 != null) chipCoach2.setOnClickListener(chipListener);
            if (chipCoach3 != null) chipCoach3.setOnClickListener(chipListener);
            
            bottomSheetDialog.show();
        });

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                dashboardLayout.setVisibility(View.VISIBLE);
                fabAI.setVisibility(View.VISIBLE);
                mapLayout.setVisibility(View.GONE);
                profileLayout.setVisibility(View.GONE);
                editProfileLayout.setVisibility(View.GONE);
                return true;
            } else if (id == R.id.nav_map) {
                dashboardLayout.setVisibility(View.GONE);
                fabAI.setVisibility(View.GONE);
                mapLayout.setVisibility(View.VISIBLE);
                profileLayout.setVisibility(View.GONE);
                editProfileLayout.setVisibility(View.GONE);
                return true;
            } else if (id == R.id.nav_profile) {
                dashboardLayout.setVisibility(View.GONE);
                fabAI.setVisibility(View.GONE);
                mapLayout.setVisibility(View.GONE);
                profileLayout.setVisibility(View.VISIBLE);
                editProfileLayout.setVisibility(View.GONE);
                return true;
            }
            return false;
        });

        // Initialize Sensor
        sensorManager = (android.hardware.SensorManager) getSystemService(SENSOR_SERVICE);
        stepCounterSensor = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_STEP_COUNTER);

        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACTIVITY_RECOGNITION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACTIVITY_RECOGNITION}, 1);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, android.hardware.SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(android.hardware.SensorEvent event) {
        if (event.sensor.getType() == android.hardware.Sensor.TYPE_STEP_COUNTER) {
            int currentDeviceSteps = (int) event.values[0];
            int previousDeviceSteps = prefs.getInt("previous_device_steps", -1);
            
            if (previousDeviceSteps == -1) {
                prefs.edit().putInt("previous_device_steps", currentDeviceSteps).apply();
                return;
            }
            
            int stepsTaken = currentDeviceSteps - previousDeviceSteps;
            if (stepsTaken > 0) {
                int currentAppSteps = prefs.getInt("step_count", 0);
                int newAppSteps = currentAppSteps + stepsTaken;
                
                int currentCalories = newAppSteps / 20;
                float currentDistance = newAppSteps * 0.000762f;
                
                prefs.edit()
                    .putInt("step_count", newAppSteps)
                    .putInt("previous_device_steps", currentDeviceSteps)
                    .putInt("calories", currentCalories)
                    .putFloat("distance", currentDistance)
                    .apply();
                
                if (updateProfileUI != null) {
                    updateProfileUI.run();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {
    }

    private void fetchLocation() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    try {
                        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            Address address = addresses.get(0);
                            String city = address.getLocality();
                            String country = address.getCountryCode();
                            String locationText = (city != null ? city.toUpperCase() : "") + (country != null ? ", " + country : "");
                            if (tvLocation != null) tvLocation.setText(locationText);
                            
                            // Set App Language based on Country
                            setAppLanguage(address.getCountryCode());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void setAppLanguage(String countryCode) {
        if (countryCode == null) return;
        Locale locale;
        if (countryCode.equalsIgnoreCase("ID")) {
            locale = new Locale("in", "ID");
        } else {
            locale = Locale.US;
        }
        
        Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        
        // Refresh UI components that use localized strings or date formats
        if (updateProfileUI != null) updateProfileUI.run();
    }

    private void shareMapSnapshot() {
        if (mMapView == null) {
            Toast.makeText(this, "Map not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Bitmap bitmap = Bitmap.createBitmap(mMapView.getWidth(), mMapView.getHeight(), Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
            mMapView.draw(canvas);
            
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File imagePath = new File(cachePath, "route_snapshot.png");
            FileOutputStream stream = new FileOutputStream(imagePath);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", imagePath);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share your route"));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to share map", Toast.LENGTH_SHORT).show();
        }
    }
}