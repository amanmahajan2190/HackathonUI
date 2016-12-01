package com.sn.frenrollment2;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;

    ImageView mImageView;
    ImageView takePhoto;
    String pictureImagePath = "";


    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = timeStamp + ".jpg";
            File storageDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES);
            pictureImagePath = storageDir.getAbsolutePath() + "/" + imageFileName;
            File file = new File(pictureImagePath);
            Uri outputFileUri = Uri.fromFile(file);


            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);

        }
    }





    @Override
    @TargetApi(Build.VERSION_CODES.M)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageView = (ImageView) findViewById(R.id.imgUserPhoto);
        takePhoto =(ImageView) findViewById(R.id.imageTakePhoto);
        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });





        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Explain to the user why we need to read the contacts
                Log.d("Some problem", "Need usage Accesss");
            }

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);

            // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
            // app-defined int constant

            return;
        }



        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }




    public void grabImage(ImageView imageView) {
        File imgFile = new File(pictureImagePath);
        if (imgFile.exists()) {
            try {


                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                Bitmap new_bitmap = getResizedBitmap(myBitmap, 480, 360);
                mImageView.setImageBitmap(myBitmap);
                String image = getBase64Encode(new_bitmap);
                JSONObject object = new JSONObject();
                try {
                    object.put("FileName", "Face.jpg");
                    object.put("FileData", image);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                new SendPostReqAsyncTask(object).execute();


                // new SendPostImageReqAsyncTask(pictureImagePath).execute();

            } catch (Exception ex) {
                ex.printStackTrace();
            }


        }
    }

    private String getBase64Encode(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);

    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE /*&& resultCode == RESULT_OK*/) {


            //... some code to inflate/create/find appropriate ImageView to place grabbed image
            this.grabImage(mImageView);

        }
    }

    class SendPostReqAsyncTask extends AsyncTask<JSONObject, Void, String> {
        JSONObject object;
        String response = "";

        private SendPostReqAsyncTask(JSONObject o) {
            this.object = o;
        }


        @Override
        protected String doInBackground(JSONObject... jsonObjects) {
            URL url = null;
            try {
                url = new URL("http://visageapp.azurewebsites.net/api/image");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setFixedLengthStreamingMode(object.toString().length());
                // conn.setRequestProperty("content-length",object.g;
                conn.setRequestProperty("content-type", "application/json");

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(object.toString());
                writer.flush();
                writer.close();
                os.close();
                if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                    String line;
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    while ((line = br.readLine()) != null) {
                        response += line;
                        Log.e("Some", response);
                    }
                    JSONObject object = new JSONObject(response);
                    JSONObject value_object = object.getJSONObject("Data");
                    boolean isIDentical = value_object.getBoolean("isIdentical");
                    double confidence = value_object.getDouble("confidence");
                    NumberFormat formatter = new DecimalFormat("#0.00");
                    confidence =confidence*100;
                    String confidence_val = formatter.format(confidence);
                    String user = value_object.getString("userName");
                    Intent intent = new Intent(MainActivity.this,ResultActivity.class);
                    intent.putExtra("IsIdentical",isIDentical);
                    intent.putExtra("UserName",user);
                    intent.putExtra("Confidence",confidence_val);
                    startActivity(intent);




                } else {
                    response = "";
                    Log.e("Not getting", response);
                }

                conn.disconnect();


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return response;


        }
    }






    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        //     bm.recycle();
        return resizedBitmap;
    }





}
