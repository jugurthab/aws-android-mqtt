package com.example.lisa_tablet;

import android.os.StrictMode;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPolicyRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    AWSIotMqttManager awsMqttManager;
    String clientId;

    CognitoCachingCredentialsProvider credentialsProvider;

    private static final String MY_ENDPOINT = "YOUR_END_POINT";
    // You need to enable unauthorized access (in cognito) for this demo to work
    private static final String COGNITO_FEDERAL_POOL_ID = "YOUR_COGNITO_FEDERATED_POOL";
    // Select your region
    private static final Regions WORKING_REGION = Regions.US_EAST_1;

    Button connectButton;
    Button disconnectButton;
    Button subscribeButuon;

    EditText topicToSubscribe;
    TextView connectionStatus;
    TextView textToDisplay;

    View someView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setContentView(R.layout.activity_main);

        connectButton = (Button) findViewById(R.id.BUTTON_CONNECT);
        disconnectButton = (Button) findViewById(R.id.BUTTON_DISCONNECT);
        subscribeButuon = (Button) findViewById(R.id.BUTTON_SUBSCRIBE);

        topicToSubscribe = (EditText) findViewById(R.id.editText);

        connectionStatus = (TextView) findViewById(R.id.TEXT_STATUS);
        textToDisplay = (TextView) findViewById(R.id.TEXT_TO_DISPLAY);

        someView = (View) findViewById(R.id.view);

        disconnectButton.setEnabled(false);

        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(), // context
                COGNITO_FEDERAL_POOL_ID, // Identity Pool ID
                WORKING_REGION // Region
        );

        AttachPolicyRequest attachPolicyReq = new AttachPolicyRequest();
        attachPolicyReq.setPolicyName("YOUR_PUB_SUB_POLICY"); // name of your IOT AWS policy
        attachPolicyReq.setTarget(credentialsProvider.getIdentityPoolId());

        AWSIotClient mIotAndroidClient = new AWSIotClient(credentialsProvider);
        mIotAndroidClient.setRegion(Region.getRegion("YOUR_REGION_HERE")); // name of your IoT Region such as "us-east-1"
        mIotAndroidClient.attachPolicy(attachPolicyReq);

        clientId = UUID.randomUUID().toString();


        awsMqttManager = new AWSIotMqttManager(clientId, MY_ENDPOINT);
    }

    public void connectToAws(View view) {
        try {
            awsMqttManager.connect(credentialsProvider, new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status, final Throwable throwable) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            disconnectButton.setEnabled(false);
                            connectButton.setEnabled(false);
                            if (status == AWSIotMqttClientStatus.Connecting) {
                                connectionStatus.setText("Connecting...");

                            } else if (status == AWSIotMqttClientStatus.Connected) {
                                connectionStatus.setText("Connected");
                                disconnectButton.setEnabled(true);
                            } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                                if (throwable != null) {
                                    Toast.makeText(getApplicationContext(), "Connection error!", Toast.LENGTH_LONG).show();
                                }
                                connectionStatus.setText("Reconnecting");
                            } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                                if (throwable != null) {
                                    Toast.makeText(getApplicationContext(), "Connection error!", Toast.LENGTH_LONG).show();
                                    throwable.printStackTrace();
                                }
                                connectionStatus.setText("Disconnected");
                                disconnectButton.setEnabled(false);
                            } else {
                                connectionStatus.setText("Disconnected");
                                disconnectButton.setEnabled(false);
                            }
                        }
                    });
                }
            });
        } catch (final Exception e) {
            connectionStatus.setText("Error! " + e.getMessage());
        }
    }

    public void disconnectFromAWS(View view) {

        try {
            awsMqttManager.disconnect();
            disconnectButton.setEnabled(false);
            connectButton.setEnabled(true);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Disconnect error : " + e, Toast.LENGTH_LONG).show();
        }
    }

    public void subscribeToAws(View view) {
        final String topic = topicToSubscribe.getText().toString();
        try {
            awsMqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
                    new AWSIotMqttNewMessageCallback() {
                        @Override
                        public void onMessageArrived(final String topic, final byte[] data) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        String message = new String(data, "UTF-8");
                                        textToDisplay.setText(message);
                                        JSONObject mainObject = new JSONObject(message);
                                        JSONObject uniObject = mainObject.getJSONObject("state");
                                        JSONObject uniObject2 = uniObject.getJSONObject("desired");
                                        String  uniName = uniObject2.getString("background");



                                        Toast.makeText(getApplicationContext(), uniName, Toast.LENGTH_LONG).show();

                                        switch(uniName){
                                            case "red" :
                                                someView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), android.R.color.holo_red_dark));
                                                break;

                                            case "green" :
                                                someView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), android.R.color.holo_green_dark));
                                                break;

                                            case "blue" :
                                                someView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), android.R.color.holo_blue_bright));
                                                break;
                                        }

                                    } catch (UnsupportedEncodingException e) {
                                        Toast.makeText(getApplicationContext(), "Message encoding error." + e, Toast.LENGTH_LONG).show();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    });
            textToDisplay.setText("Subscribed to topic!");
        } catch (Exception e) {
            textToDisplay.setText("Subscription error.");
        }
    }
}
