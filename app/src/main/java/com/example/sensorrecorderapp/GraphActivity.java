package com.example.sensorrecorderapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class GraphActivity extends AppCompatActivity {

    private static final Log log = LogFactory.getLog(GraphActivity.class);
    private ArrayList<Double> accelxList, accelyList, accelzList, gyroxList, gyroyList, gyrozList;
    private ArrayList<String> latList, longList;
    private GraphView graph1, graph2;
    private int rowCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        Intent intent = getIntent();
        String fileName = intent.getStringExtra("filePath");
        ArrayList<String> activities = intent.getStringArrayListExtra("activitySelected");

        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + fileName;

        try {
            CSVReader reader = new CSVReader(new FileReader(filePath));
            List<String[]> csvData = reader.readAll();

            GraphView graph1 = findViewById(R.id.graph1);
            graph1.getGridLabelRenderer().setHorizontalAxisTitle("Sample");
            graph1.getGridLabelRenderer().setVerticalAxisTitle("Acceleration");
            graph1.getGridLabelRenderer().setHorizontalAxisTitleColor(Color.BLACK);
            graph1.getGridLabelRenderer().setVerticalAxisTitleColor(Color.BLACK);
            graph1.getViewport().setScrollable(true);
            graph1.getViewport().setScalable(true);
            graph1.setBackgroundColor(Color.WHITE);

            GraphView graph2 = findViewById(R.id.graph2);
            graph2.getGridLabelRenderer().setHorizontalAxisTitle("Sample");
            graph2.getGridLabelRenderer().setVerticalAxisTitle("Gyro");
            graph2.getGridLabelRenderer().setHorizontalAxisTitleColor(Color.BLACK);
            graph2.getGridLabelRenderer().setVerticalAxisTitleColor(Color.BLACK);
            graph2.getViewport().setScrollable(true);
            graph2.getViewport().setScalable(true);
            graph2.setBackgroundColor(Color.WHITE);

            LineGraphSeries<DataPoint> accelxSeries = new LineGraphSeries<>();
            LineGraphSeries<DataPoint> accelySeries = new LineGraphSeries<>();
            LineGraphSeries<DataPoint> accelzSeries = new LineGraphSeries<>();

            LineGraphSeries<DataPoint> gyroxSeries = new LineGraphSeries<>();
            LineGraphSeries<DataPoint> gyroySeries = new LineGraphSeries<>();
            LineGraphSeries<DataPoint> gyrozSeries = new LineGraphSeries<>();
            String tempActivityName="";
            String[] tempRow=csvData.get(0);
            int gz1=0,timestamp=0;
            for (int i=0; i<tempRow.length; i++)
            {
                if (tempRow[i]=="GZ ")
                {
                    android.util.Log.d("GraphActivity", "onCreate: " + i);
                    gz1=i;
                }
                else if (tempRow[i]=="Timestamp ")
                {
                    timestamp=i;
                }
            }
            int number=timestamp-gz1-1;

            for (int i = 1; i < csvData.size(); i++) {
                String[] row = csvData.get(i);
                String temp="";
                int rowLength = row.length;
                if (rowLength<7)
                {
                    continue;
                }
                double x = (double) i;

                double accelx = Double.parseDouble(row[1]);
                double accely = Double.parseDouble(row[2]);
                double accelz = Double.parseDouble(row[3]);
                accelxSeries.appendData(new DataPoint(x, accelx), true, csvData.size());
                accelySeries.appendData(new DataPoint(x, accely), true, csvData.size());
                accelzSeries.appendData(new DataPoint(x, accelz), true, csvData.size());

                double gyrox = Double.parseDouble(row[4]);
                double gyroy = Double.parseDouble(row[5]);
                double gyroz = Double.parseDouble(row[6]);
                gyroxSeries.appendData(new DataPoint(x, gyrox), true, csvData.size());
                gyroySeries.appendData(new DataPoint(x, gyroy), true, csvData.size());
                gyrozSeries.appendData(new DataPoint(x, gyroz), true, csvData.size());
                for (int j=7; j<row.length-1; j++)
                {
                    temp=temp+row[j]+", ";
                }
                temp=temp+row[row.length-1];
                tempActivityName=temp;
            }
            String activityName="";
            activityName=activityName+tempActivityName;

            accelxSeries.setTitle("Ax");
            accelxSeries.setColor(Color.BLUE);
            graph1.addSeries(accelxSeries);
            accelySeries.setTitle("Ay");
            accelySeries.setColor(Color.GREEN);
            graph1.addSeries(accelySeries);
            accelzSeries.setTitle("Az");
            accelzSeries.setColor(Color.RED);
            graph1.addSeries(accelzSeries);
            graph1.setTitle("Activities: " + activityName);
            graph1.setTitleColor(Color.BLACK);

            gyroxSeries.setTitle("Gx");
            gyroxSeries.setColor(Color.BLUE);
            graph2.addSeries(gyroxSeries);
            gyroySeries.setTitle("Gy");
            gyroySeries.setColor(Color.GREEN);
            graph2.addSeries(gyroySeries);
            gyrozSeries.setTitle("Gz");
            gyrozSeries.setColor(Color.RED);
            graph2.addSeries(gyrozSeries);
            graph2.setTitle("Activities: " + activityName);
            graph2.setTitleColor(Color.BLACK);

            Button shareButton = findViewById(R.id.shareButton);
            shareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bitmap graph1Bitmap = graph1.takeSnapshot();
                    Bitmap graph2Bitmap = graph2.takeSnapshot();

                    ArrayList<Uri> imageUris = new ArrayList<>();
                    imageUris.add(getImgUri(getApplicationContext(), graph1Bitmap));
                    imageUris.add(getImgUri(getApplicationContext(), graph2Bitmap));

                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);

                    shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);

                    shareIntent.setType("image/*");
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, "Share graphs"));
                }
            });

            graph1.getLegendRenderer().setVisible(true);
            graph2.getLegendRenderer().setVisible(true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (CsvException e) {
            throw new RuntimeException(e);
        }

    }

    public static Uri getImgUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage,"IMG_" + Calendar.getInstance().getTime(), null);
        return Uri.parse(path);
    }
}
