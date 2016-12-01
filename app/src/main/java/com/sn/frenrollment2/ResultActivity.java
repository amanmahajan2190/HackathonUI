package com.sn.frenrollment2;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by amanmahajan on 11/12/2016.
 */

public class ResultActivity extends AppCompatActivity {


    private String xmlPostRegisterUserDevices = null;
    private String userName = "";
    private String adminCredentials = "restapi1:restapi1";
    private final String tenantCode = "EUVA+pG7oEHMro4rrtLx1QAo14O77EDlGGZBNGMLuFA=";
    private final String apiURL = "https://v90.airwlab.com/api/system/users/registeruserdevices";
    private final String awAgentBaseURL = "https://awagent.com?serverurl=";
    private String enrollmentToken = "";
    private final String enrollmentUrl = "v90.airwlab.com";
    @Override
    @TargetApi(Build.VERSION_CODES.M)
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result);
        TextView textView = (TextView) findViewById(R.id.message);
        TextView  tryAgain = (TextView)findViewById(R.id.textTryAgain);
        tryAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ResultActivity.this,MainActivity.class);
                startActivity(intent);
            }
        });

        Intent intent = getIntent();
        String msg ="";
        if(intent !=null){
            boolean isIdentical = intent.getBooleanExtra("IsIdentical",false);


            if(isIdentical){
                String name = intent.getStringExtra("UserName");
                msg = "Congrats " + name +". Let's start your Enrollment";
                userName=name;
                String threshhold = "Confidence Level " +intent.getStringExtra("Confidence") +" %";
           //     Toast.makeText(this,threshhold,Toast.LENGTH_LONG).show();
                tryAgain.setVisibility(View.GONE);
                try {
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            try {
                                 String apiResponse = new SendPostReqAsyncTaskToRegisterUserDevices(apiURL).execute().get();
                                enrollmentToken = getEnrollmentToken(apiResponse);
                                Log.i("AWAutoenroll", "The enrollment token is: " + enrollmentToken);
                                // Build the URL supported by the URL scheme from the results of the GET
                                // API request
                                String awAgentURL = awAgentBaseURL + enrollmentUrl + "&gid="
                                        + enrollmentToken;

                                // Launch the AirWatch agent using the URL scheme
                                launchAWagentForEnrollment(awAgentURL);

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            }


                            // run AsyncTask here.


                        }
                    }, 2000);

            }catch (Exception ex){
                    Log.d("Timer task","Problem");
                }
            }else{
                msg = "User is not Authenticated";

            }
        }
        textView.setText(msg);

    }



    private String getEnrollmentToken(String response) {

        String token = "";

        XmlPullParserFactory pullParserFactory;

        try {
            pullParserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = pullParserFactory.newPullParser();

            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            InputStream stream = new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8));
            parser.setInput(stream, null);
            token = parseXML(parser);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return token;
    }

    private String parseXML(XmlPullParser parser) throws XmlPullParserException, IOException {

        String token = "";
        int eventType = parser.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            String name = null;

            switch (eventType) {
                case XmlPullParser.START_TAG:
                    name = parser.getName();

                    if (name.equals("EnrollmentToken")) {
                        token = parser.nextText();
                    }

                    break;

                case XmlPullParser.END_TAG:
                    break;
            }

            eventType = parser.next();
        }
        return token;
    }

    private void launchAWagentForEnrollment(String awLaunchURL) {

        // Create an intent with supporting URL scheme
        Intent url_intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(awLaunchURL));
        url_intent.addCategory(Intent.CATEGORY_BROWSABLE);

        // Start an activity to launch the AW agent for enrollment
        startActivity(url_intent);

    }

    private String getXmlBody(String userName) {
        String xmlBody = null;

        xmlBody = "<ArrayOfDeviceUserDetails>\n" +
                "        <DeviceUserDetails>\n" +
                "               <UserName xmlns=\"http://www.air-watch.com/servicemodel/resources\">" + userName + "</UserName>\n" +
                "               <Password xmlns=\"http://www.air-watch.com/servicemodel/resources\">awtest</Password>\n" +
                "               <FirstName xmlns=\"http://www.air-watch.com/servicemodel/resources\">" + userName + "</FirstName>\n" +
                "               <LastName xmlns=\"http://www.air-watch.com/servicemodel/resources\">hackathon</LastName>\n" +
                "               <Email xmlns=\"http://www.air-watch.com/servicemodel/resources\">" + userName + "@noreply.com</Email>\n" +
                "               <TokenType xmlns=\"http://www.air-watch.com/servicemodel/resources\">2</TokenType>\n" +
                " </DeviceUserDetails>\n" +
                "</ArrayOfDeviceUserDetails>";
        return xmlBody;
    }


    class SendPostReqAsyncTaskToRegisterUserDevices extends AsyncTask<String, Void, String> {

        String urlString;


        public SendPostReqAsyncTaskToRegisterUserDevices(String urlString) {
            this.urlString = urlString;
        }

        protected String doInBackground(String... params) {

            String basicAuth = "Basic " + new String(Base64.encode(adminCredentials.getBytes(), Base64.DEFAULT));
            String response = "";
            xmlPostRegisterUserDevices = getXmlBody(userName);


            try {
                URL url = new URL(this.urlString);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();


                // Set up generic Connection Properties

                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);

                // Set up AW Specific Connection Properties

                conn.setRequestProperty("authorization", basicAuth);
                conn.setRequestProperty("aw-tenant-code", tenantCode);
                conn.setRequestProperty("content-type", "application/xml");

                // Set up post api call body

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(xmlPostRegisterUserDevices);
                writer.flush();
                writer.close();
                os.close();

                conn.connect();

                if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                    String line;
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    while ((line = br.readLine()) != null) {
                        response += line;
                    }
                } else {
                    response = "";
                }

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return response;
        }
    }




}
