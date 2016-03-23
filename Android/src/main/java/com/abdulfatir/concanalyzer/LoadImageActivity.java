package com.abdulfatir.concanalyzer;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.abdulfatir.concanalyzer.models.SampleModel;
import com.abdulfatir.concanalyzer.util.LinearFunction;
import com.abdulfatir.concanalyzer.util.Utils;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class LoadImageActivity extends AppCompatActivity {

    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_image);

        imageView = (ImageView) findViewById(R.id.imageView);
        final Dialog sample_data = new Dialog(this);
        sample_data.setContentView(R.layout.sample_data_dialog);
        sample_data.setCancelable(true);

        final EditText intensityET = (EditText)sample_data.findViewById(R.id.editText);
        final Spinner sampleTypeSp = (Spinner)sample_data.findViewById(R.id.spinner);
        final EditText concET = (EditText)sample_data.findViewById(R.id.editText3);
        final EditText idET = (EditText)sample_data.findViewById(R.id.editText4);
        Button save = (Button)sample_data.findViewById(R.id.button3);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int pos = sampleTypeSp.getSelectedItemPosition();
                int idx = Integer.parseInt(idET.getText().toString());

                if(pos == 2)
                    samples.get(idx).setDpt(SampleModel.DataPointType.UNKNOWN);

                else if(pos == 0){
                    samples.get(idx).setDpt(SampleModel.DataPointType.KNOWN);
                    double concen = Double.parseDouble(concET.getText().toString());
                    samples.get(idx).setConcentration(concen);
                }

                else if(pos == 1){
                    samples.get(idx).setDpt(SampleModel.DataPointType.QUALITY_CONTROL);
                    double concen = Double.parseDouble(concET.getText().toString());
                    samples.get(idx).setConcentration(concen);
                }

                sample_data.dismiss();
            }
        });

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if(action == MotionEvent.ACTION_UP)
                {
                    float ivX = event.getX();
                    float ivY = event.getY();
                    int[] imC = getProjection(ivX, ivY);
                    if(imC != null) {
                        for (int idx = 0; idx < rects.length; idx++) {
                            if (rects[idx].contains(new Point(imC[0], imC[1]))) {

                                SampleModel s = samples.get(idx);
                                intensityET.setText(""+s.getIntensity());
                                concET.setText(""+s.getConcentration());
                                idET.setText("" + idx);

                                int pos = 0;

                                switch (s.getDpt())
                                {
                                    case KNOWN:
                                        pos = 0;
                                        break;
                                    case QUALITY_CONTROL:
                                        pos = 1;
                                        break;
                                    case UNKNOWN:
                                        pos = 2;
                                }

                                sampleTypeSp.setSelection(pos);

                                sample_data.setTitle("Sample " + idx);
                                sample_data.show();
                            }
                        }
                    }
                }
                return true;
            }
        });

        startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_GET_CONTENT)
                .setType("image/*"), "Choose an image"), IMAGE_CODE);
    }


    private int[] getProjection(float x, float y){
        if(x<0 || y<0 || x > imageView.getWidth() || y > imageView.getHeight()){
            return null;
        }

        else{
            int projectedX = (int)((double)x * ((double)changedBitmap.getWidth()/(double)imageView.getWidth()));
            int projectedY = (int)((double)y * ((double)changedBitmap.getHeight()/(double)imageView.getHeight()));

            return new int[]{projectedX, projectedY};
        }
    }

    HashMap<Integer, SampleModel> samples;


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_CODE && resultCode == RESULT_OK && data != null) {

            String[] projection = { MediaStore.Images.Media.DATA , MediaStore.Images.Media.ORIENTATION };
            Uri selectedImage = data.getData();
            Cursor cursor;
            if(Build.VERSION.SDK_INT > 19)
            {
                // Will return "image:x*"
                String wholeID = DocumentsContract.getDocumentId(selectedImage);
                // Split at colon, use second item in the array
                String id = wholeID.split(":")[1];
                // where id is equal to
                String sel = MediaStore.Images.Media._ID + "=?";

                cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        projection, sel, new String[]{ id }, null);
            }

            //String[] filePathColumn = {MediaStore.Images.Media.DATA, };
            else
            {
                cursor = getContentResolver().query(selectedImage,
                        projection, null, null, null);
            }
            try {
                if(cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(projection[0]);
                    String picturePath = cursor.getString(columnIndex);
                    int orientation = -1;
                    orientation = cursor.getInt(cursor.getColumnIndex(projection[1]));

                    Matrix matrix = new Matrix();
                    matrix.reset();
                    matrix.postRotate(orientation);

                    cursor.close();

                    changedBitmap = BitmapFactory.decodeFile(picturePath);//Utils.cropToSquare(Utils.lessResolution(picturePath, 512, 512));
                    changedBitmap = Bitmap.createBitmap(changedBitmap, 0, 0, changedBitmap.getWidth(), changedBitmap.getHeight(), matrix, true);
                    imageView.setImageBitmap(changedBitmap);
                }

                else
                {
                    if(selectedImage.toString().startsWith("file://"))
                    {
                        String picturePath = selectedImage.getPath();
                        changedBitmap = BitmapFactory.decodeFile(picturePath); //Utils.cropToSquare(Utils.lessResolution(picturePath, 512, 512));
                        imageView.setImageBitmap(changedBitmap);
                    }
                    Log.d(getClass().getName(), "=> " + selectedImage.toString());
                }
            }
            catch(NullPointerException e)
            {
                Toast.makeText(this, "Failed to load image. Try loading from a different app or folder instead.", Toast.LENGTH_LONG).show();
            }
        }

    }

    private final int IMAGE_CODE = 7323;
    private Bitmap changedBitmap;

    static
    {
        OpenCVLoader.initDebug();
    }

    int H_MIN = 0;
    int H_MAX = 169;
    int S_MIN = 0;
    int S_MAX = 255;
    int V_MIN = 0;
    int V_MAX = 255;

    ProgressDialog dlg;

    public void analyze(View view) {
        if(((Button)view).getText().equals("Analyze")){
            dlg = ProgressDialog.show(this, "Analyzing", "Please wait...",true, false);
            new AnalyzeImageTask().execute();
            ((Button)view).setText("Results");
        }
        else if(((Button)view).getText().equals("Results"))
        {
            SimpleRegression reg=new SimpleRegression();
            String result="";
            boolean incom = false;
            for(int i=0;i<8;i++)
            {
                SampleModel s = samples.get(i);
                if(s.getDpt() == SampleModel.DataPointType.NONE)
                {
                    //Log.d("Results", "Incomplete Data");
                    incom = true;
                    break;
                }
                else if(s.getDpt() == SampleModel.DataPointType.KNOWN)
                {
                    reg.addData(s.getIntensity(), s.getConcentration());
                    //Log.d("Results",s.getIntensity() +","+s.getConcentration() +"," + s.getDpt().toString());
                }
            }
            double slope = reg.getSlope();
            double inter = reg.getIntercept();

            LinearFunction linearFunction=new LinearFunction(inter, slope);
            LinearFunction standardCurve = linearFunction.swapAxes();
            standardCurve.setIntercept(standardCurve.getIntercept()+255);
            result += ("<b>Equation</b></br>Conc = " + String.format("%.2f", slope) +"B + " + String.format("%.2f", inter) + "</br>B : Brightness Intensity</br>");

            int qc=0;
            for(int i=0;i<8;i++)
            {
                SampleModel s = samples.get(i);
                if(s.getDpt() == SampleModel.DataPointType.QUALITY_CONTROL) {
                    double calculated_conc = slope*s.getIntensity() + inter;
                    double error = Math.abs(calculated_conc - s.getConcentration())/s.getConcentration();

                    error *= 100;
                    qc++;

                    result += ("<b>Quality Control "+qc+".</b></br>"+"&nbsp;&nbsp;Calculated: "+ String.format("%.2f", calculated_conc) +"</br>&nbsp;&nbsp;Error: " + String.format("%.2f", error) + "%</br>");
                }

                else if(s.getDpt() == SampleModel.DataPointType.UNKNOWN) {
                    double calculated_conc = slope*s.getIntensity() + inter;

                    result += ("<b>Unknown Sample</b></br>"+"&nbsp;&nbsp;Calculated: "+String.format("%.2f", calculated_conc)+"</br>");
                }
            }

            final Dialog result_dialog = new Dialog(this);
            result_dialog.setContentView(R.layout.result_dialog);

            if(incom)
                result = "<b>Incomplete Data</b></br>";

            WebView resultTv = (WebView)result_dialog.findViewById(R.id.resultTV);
            resultTv.setVerticalScrollBarEnabled(false);
            resultTv.setBackgroundColor(0x00000000);
            resultTv.loadData(result, "text/html", "utf-8");

            Button Ok = (Button)result_dialog.findViewById(R.id.OK);
            Ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    result_dialog.dismiss();
                }
            });

            result_dialog.setCancelable(false);
            result_dialog.setTitle("Results");
            result_dialog.show();

        }
    }

    public void segmentImage()
    {
        if(changedBitmap != null)
        {
            samples = new HashMap<>();

            Mat raw=new Mat();
            Mat hsv=new Mat();
            org.opencv.android.Utils.bitmapToMat(changedBitmap, raw);
            Imgproc.cvtColor(raw, hsv, Imgproc.COLOR_RGB2HSV);
            Imgproc.cvtColor(raw, raw, Imgproc.COLOR_RGB2GRAY);
            double maxH = 0;
            Mat thresh = new Mat(raw.size(), CvType.CV_8UC1);
            for(int x=0;x<raw.cols();x++)
            {
                for(int y=0;y<raw.rows();y++)
                {
                    double[] data = hsv.get(y,x);
                    double H = data[0];
                    if(H>maxH) maxH=H;
                    double S = data[1];
                    double V = data[2];
                    if(H_MIN<=H && H<=H_MAX && S_MIN<=S && S<=S_MAX && V_MIN<=V && V<=V_MAX) {
                        data = new double[] {0};
                        thresh.put(y,x, data);
                    }
                    else
                    {
                        data = new double[] {255};
                        thresh.put(y,x, data);
                    }
                }
            }

            Imgproc.medianBlur(thresh, thresh, 5);
            //org.opencv.android.Utils.matToBitmap(thresh, changedBitmap);

            List<MatOfPoint> conts = new ArrayList<>();
            Mat hieh = new Mat();

            Imgproc.findContours(thresh, conts, hieh, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
            hieh.release();
            Collections.sort(conts, new Comparator<MatOfPoint>() {
                @Override
                public int compare(MatOfPoint lhs, MatOfPoint rhs) {
                    double i = Imgproc.contourArea(lhs);
                    double j = Imgproc.contourArea(rhs);
                    if (i < j)
                        return 1;
                    if (i > j)
                        return -1;
                    return 0;
                }
            });

            double intensities[] = new double[8];
            rects = new Rect[8];

            for(int idx=0;idx<8;idx++)
            {
                Rect r = Imgproc.boundingRect(conts.get(idx));
                rects[idx] = r;
                double intensity_sum = 0;
                int n = 0;
                for(int x = r.x; x<=r.x + r.width; x++)
                {
                    for(int y = r.y; y<=r.y + r.height; y++)
                    {
                        double B = thresh.get(y,x)[0];

                        if(B > 0)
                        {
                            double I = raw.get(y,x)[0];
                            intensity_sum += I;
                            n += 1;
                        }
                    }
                }

                double intensity = intensity_sum / n;
                //Log.d("opencv",intensity+"");
                intensities[idx] = intensity;
                SampleModel sample = new SampleModel(intensity);
                samples.put(idx,sample);
            }
            thresh.release();
            raw.release();
            hsv.release();

            hsv = null;
            thresh = null;
            conts.clear();
            conts = null;

            raw = new Mat();
            org.opencv.android.Utils.bitmapToMat(changedBitmap, raw);

            for(int idx=0;idx<8;idx++) {
                Rect r = rects[idx];
                Imgproc.rectangle(raw, new Point(r.x, r.y), new Point(r.x + r.width, r.y + r.height), new Scalar(255, 0, 0),3);
                Imgproc.putText(raw, Integer.toString(idx), new Point(r.x, r.y), 3, 2, new Scalar(255, 0, 0),2);
            }

            org.opencv.android.Utils.matToBitmap(raw, changedBitmap);

        }
    }

    private Rect[] rects;

    private class AnalyzeImageTask extends AsyncTask<Void,Void,Void>
    {

        @Override
        protected Void doInBackground(Void... params) {
            segmentImage();
            return null;
        }

        @Override
        protected void onPostExecute(Void v)
        {
            imageView.setImageBitmap(changedBitmap);
            dlg.dismiss();
        }
    }
}

