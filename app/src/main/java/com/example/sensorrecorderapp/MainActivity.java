package com.example.sensorrecorderapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.content.SharedPreferences;
import android.content.DialogInterface;
import android.widget.ToggleButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManagers;
    private SensorManager sensorManagerG;
    private Sensor senAcc;
    private Sensor senGyro;
    private TextView GPSx;
    private TextView GPSy;
    private TextView GPS_loc;

    private FusedLocationProviderClient MyFusedLocClient;
    private LinearLayout llButtons;
    private Button addButton;
    private SharedPreferences prefs;
    private SharedPreferences idxPrefs;

    private long lastUpdate = 0;
    int count=0;
    private int locationRequestCode = 1000;
    private double wayLatitude = 0.0, wayLongitude = 0.0;
    private long lastUpdate_gyro = 0;
    public int i = 0;
    private int idxTemp;
    public String fileString = "";
    private static final int REQUEST_CODE_CSV = 100;
    private String csvFilePath="";

    ArrayList<String> activitySelected = new ArrayList<>();;

    private Uri selectedFile;

    public class ButtonData {
        private int id;
        private String label;
        private boolean state;

        private String csvFilePath="";

        public ButtonData(int id, String label, boolean state) {
            this.id = id;
            this.label = label;
            this.state = state;
        }

        public int getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public boolean getState() {
            return state;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        selectedFile = null;

        llButtons=findViewById(R.id.tag_layout);

        MyFusedLocClient = LocationServices.getFusedLocationProviderClient(this);
        sensorManagers = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManagerG = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        assert sensorManagers != null;
        senAcc = sensorManagers.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senGyro = sensorManagerG.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE}, locationRequestCode);
        } else {
            Toast T = Toast.makeText(getApplicationContext(), "Location & file access Permission Granted", Toast.LENGTH_SHORT);
            T.show();
        }

        Button dropdownButton = findViewById(R.id.dropdown_button);
        dropdownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                builder.setTitle("Select a Device");
                builder.setMessage("Please select a Device:");

                final RadioButton radioButton = new RadioButton(MainActivity.this);
                radioButton.setText("Phone");
                radioButton.setChecked(true);

                builder.setView(radioButton);

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (radioButton.isChecked()) {
                            Toast.makeText(MainActivity.this, "Phone is selected", Toast.LENGTH_SHORT).show();
                        } else {
                        }
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        switchAppearance();
        Switch toggle = (Switch) findViewById(R.id.sw);
        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                i = 1;
                onResume();
            } else {
                i = 0;
                onPause();
                SharedPreferences.Editor editor = idxPrefs.edit();
                editor.putInt("LAST_INDEX", idxTemp+1);
                editor.apply();

            }
        });
        llButtons = findViewById(R.id.tag_layout);
        addButton = findViewById(R.id.add_button);
        prefs = getPreferences(Context.MODE_PRIVATE);
        loadButtonsFromPrefs();

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Enter Button Label");

                final EditText input = new EditText(MainActivity.this);
                builder.setView(input);

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String label = input.getText().toString();
                        addToggleButton(label);
                        saveButtonsToPrefs();
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });
        Button selectCSVFileButton = findViewById(R.id.selectButton);
        selectCSVFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(Intent.createChooser(intent, "Select CSV File"), REQUEST_CODE_CSV);
            }
        });
        Button submitButton = findViewById(R.id.submitButton);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText etFileName = findViewById(R.id.fileNameTextView);
                String fileName = etFileName.getText().toString();
                if (!fileName.isEmpty()) {
                    Intent intent = new Intent(MainActivity.this, GraphActivity.class);
                    intent.putExtra("filePath", csvFilePath);
                    intent.putExtra("activitySelected", activitySelected);
                    startActivity(intent);

                } else {
                    Toast.makeText(MainActivity.this, "Please select a file", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                GetLatitudeLongitude();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;
        Sensor GSensor = sensorEvent.sensor;

        if (GSensor.getType() == Sensor.TYPE_GYROSCOPE){
            float gx = Math.round(sensorEvent.values[0] *  100000f) / 100000f;
            float gy = Math.round(sensorEvent.values[1] *  100000f) / 100000f;
            float gz = Math.round(sensorEvent.values[2] *  100000f) / 100000f;

            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastUpdate_gyro > 300)) {

                lastUpdate_gyro = currentTime;

                GetLatitudeLongitude();

                String sX = Float.toString(gx);
                TextView text = (TextView) findViewById(R.id.gyrox);
                text.setText(sX);

                String sY = Float.toString(gy);
                text = (TextView) findViewById(R.id.gyroy);
                text.setText(sY);

                String sZ = Float.toString(gz);
                text = (TextView) findViewById(R.id.gyroz);
                text.setText(sZ);

                fileString = fileString + sX + ", " + sY + ", " + sZ + ", ";

            }
            FileWriters(fileString);
        }

        if(mySensor.getType() == Sensor.TYPE_ACCELEROMETER){
            float x = Math.round(sensorEvent.values[0] *  100000f) / 100000f;
            float y = Math.round(sensorEvent.values[1] *  100000f) / 100000f;
            float z = Math.round(sensorEvent.values[2] *  100000f) / 100000f;

            long currentTime = System.currentTimeMillis();

            if ((currentTime - lastUpdate) > 300){
                long timeDiff = currentTime - lastUpdate;
                lastUpdate = currentTime;

                String sX = Float.toString(x);
                TextView text = (TextView) findViewById(R.id.accelx);
                text.setText(sX);

                String sY = Float.toString(y);
                text = (TextView) findViewById(R.id.accely);
                text.setText(sY);

                String sZ = Float.toString(z);
                text = (TextView) findViewById(R.id.accelz);
                text.setText(sZ);

                fileString = fileString + sX + ", " + sY + ", " + sZ + ", ";
                for (int i=0; i<activitySelected.size()-1; i++)
                {
                    fileString = fileString + activitySelected.get(i) + ", ";
                }
                fileString = fileString + activitySelected.get(activitySelected.size()-1) + ", ";
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                Date now = new Date();

                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

                String currentTimeString = formatter.format(now);

                fileString=fileString+currentTimeString+"\n";
            }
            FileWriters(fileString);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CSV && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            String filePath = null;
            if (uri != null) {
                if ("content".equals(uri.getScheme())) {
                    Cursor cursor = null;
                    try {
                        cursor = getContentResolver().query(uri, new String[]{MediaStore.Files.FileColumns.DISPLAY_NAME}, null, null, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            int index = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME);
                            if (index >= 0) {
                                filePath = cursor.getString(index);
                            }
                        }
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                } else if ("file".equals(uri.getScheme())) {
                    filePath = uri.getPath();
                }
            }

            csvFilePath=filePath;
            String fileName = filePath != null ? filePath.substring(filePath.lastIndexOf("/") + 1) : "";
            EditText etFileName = findViewById(R.id.fileNameTextView);
            if (etFileName != null) {
                etFileName.setText(fileName);
            }
        }
    }

    private void addToggleButton(String label) {
        ToggleButton toggleButton = new ToggleButton(this);
        toggleButton.setText(label);
        toggleButton.setTextOn(label);
        toggleButton.setTextOff(label);

        int id = View.generateViewId();
        toggleButton.setId(id);

        llButtons.addView(toggleButton);

        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean state = ((ToggleButton) v).isChecked();
                switchAppearance();
            }
        });
    }

    private void loadButtonsFromPrefs() {
        SharedPreferences prefs = getSharedPreferences("BUTTONS_PREFS", MODE_PRIVATE);
        String buttonDataString = prefs.getString("BUTTON_DATA", "");
        if (!buttonDataString.isEmpty()) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<ButtonData>>(){}.getType();
            ArrayList<ButtonData> buttonDataList = gson.fromJson(buttonDataString, type);
            for (ButtonData buttonData : buttonDataList) {
                ToggleButton button = new ToggleButton(this);
                button.setId(buttonData.getId());
                button.setTextOn(buttonData.getLabel());
                button.setTextOff(buttonData.getLabel());
                button.setChecked(false);
                // Set width and height
                int width = ViewGroup.LayoutParams.WRAP_CONTENT; // or a specific width in pixels
                int height = ViewGroup.LayoutParams.WRAP_CONTENT; // or a specific height in pixels

                // Create LayoutParams object and set margins
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
                params.setMargins(16, 16, 16, 16); // left, top, right, bottom margins in pixels
                // Create a drawable for rounded edges programmatically
                GradientDrawable roundedDrawable = new GradientDrawable();
                roundedDrawable.setShape(GradientDrawable.RECTANGLE);
                roundedDrawable.setCornerRadius(24f);
                // Apply the LayoutParams to the button
                button.setLayoutParams(params);

                llButtons.addView(button);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean state = ((ToggleButton) v).isChecked();
                        switchAppearance();
                    }
                });
            }
        }
    }

    private void saveButtonsToPrefs() {
        ArrayList<ButtonData> buttonDataList = new ArrayList<>();
        for (int i = 0; i < llButtons.getChildCount(); i++) {
            View view = llButtons.getChildAt(i);
            if (view instanceof ToggleButton) {
                ToggleButton button = (ToggleButton) view;
                int id = button.getId();
                String label = button.getTextOn().toString();
                boolean state = button.isChecked();
                ButtonData buttonData = new ButtonData(id, label, state);
                buttonDataList.add(buttonData);
            }
        }
        Gson gson = new Gson();
        String buttonDataString = gson.toJson(buttonDataList);
        SharedPreferences.Editor editor = getSharedPreferences("BUTTONS_PREFS", MODE_PRIVATE).edit();
        editor.putString("BUTTON_DATA", buttonDataString);
        editor.apply();
    }

    public void FileWriters(String str) {
        SimpleDateFormat dateObj = new SimpleDateFormat("dd/MM/yyyy");
        Calendar calendar = Calendar.getInstance();
        String date = dateObj.format(calendar.getTime());

        idxPrefs = getPreferences(Context.MODE_PRIVATE);

        idxTemp = idxPrefs.getInt("LAST_INDEX", 0);
        File folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);


        final File file = new File(folder, "Data " + idxTemp + ".csv");
        try {
            if(!file.exists()){
                file.createNewFile();
                FileOutputStream fOut = new FileOutputStream(file, true);
                String temporary="AX, AY, AZ, GX, GY, GZ";
                int len = activitySelected.size();
                for (int i=0; i<len; i++)
                {
                    temporary=temporary+", Activity"+(i+1);
                }
                temporary=temporary+", Timestamp";
                temporary=temporary+"\n";
                fOut.write(temporary.getBytes());
                fOut.close();
            }
            FileOutputStream fOut = new FileOutputStream(file, true);
            fOut.write(str.getBytes());
            fOut.flush();
            fOut.close();

        } catch (IOException e){
            Log.e("Exception", "File write failed: " + e.getMessage());
        }
        fileString = "";
    }

    public static void removeRowFromCsvFile(File file, int rowIndex) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
            br.close();

            if ((rowIndex >= 0 && rowIndex < lines.size()) && lines.size()>1) {
                lines.remove(rowIndex);
            }
            if (lines.size()>1)
            {
                lines.remove(lines.size()-1);
            }

            BufferedWriter bw = new BufferedWriter(new FileWriter(file, false));
            for (String s : lines) {
                bw.write(s);
                bw.newLine();
            }
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void removeNoise(File file, int rowIndex) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
            br.close();

            if ((rowIndex >= 0 && rowIndex < lines.size()) && lines.size()>1) {
                lines.remove(rowIndex);
            }
            if (lines.size()>1)
            {
                lines.remove(lines.size()-1);
            }

            BufferedWriter bw = new BufferedWriter(new FileWriter(file, false));
            for (String s : lines) {
                bw.write(s);
                bw.newLine();
            }
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void onPause() {
        super.onPause();
        sensorManagers.unregisterListener(this);
        File folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        final File file = new File(folder, "Data " + idxTemp + ".csv");
        if (file.exists())
        {
            removeRowFromCsvFile(file,1);
            removeNoise(file,1);
        }
    }

    protected void onResume() {
        super.onResume();
        if (i == 1) {
            sensorManagers.registerListener(this, senAcc, SensorManager.SENSOR_DELAY_UI );
            sensorManagers.registerListener(this, senGyro, SensorManager.SENSOR_DELAY_UI );
        }
    }

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 123;

    public void GetLatitudeLongitude() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            MyFusedLocClient.flushLocations();
            MyFusedLocClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    wayLatitude = Math.round(location.getLatitude() *  100000f) / 100000f;
                    wayLongitude = Math.round(location.getLongitude() *  100000f) / 100000f;

                    GPSx = (TextView) findViewById(R.id.gpsx);
                    GPSx.setText("" + wayLatitude);

                    GPSy = (TextView) findViewById(R.id.gpsy);
                    GPSy.setText("" + wayLongitude);

                    GPS_loc = (TextView) findViewById(R.id.loc);
                    GPS_loc.setText(String.format(Locale.US, "%s - %s", wayLatitude, wayLongitude));
                }
            });
        }
    }

    public void switchAppearance()
    {
        ArrayList<String> tempActivities = new ArrayList<>();;
        Switch mySwitch=findViewById(R.id.sw);
        int cnt = 0;
        if (llButtons!=null)
        {
            for (int i = 0; i < llButtons.getChildCount(); i++) {
                View view = llButtons.getChildAt(i);
                if (view instanceof ToggleButton) {
                    ToggleButton toggleButton = (ToggleButton) view;
                    if (toggleButton.isChecked()) {
                        cnt++;
                    }
                }
            }
        }

        if (cnt>=1)
        {
            mySwitch.setVisibility(View.VISIBLE);
            String tempActivity="";
            for (int i = 0; i < llButtons.getChildCount(); i++) {
                View view = llButtons.getChildAt(i);
                if (view instanceof ToggleButton) {
                    ToggleButton toggleButton = (ToggleButton) view;
                    if (toggleButton.isChecked()) {
                        tempActivity = toggleButton.getText().toString();
                        tempActivities.add(tempActivity);
                    }
                }
            }
            activitySelected=tempActivities;
        }
        else
        {
            mySwitch.setChecked(false);
            mySwitch.setVisibility(View.GONE);
        }
    }

}