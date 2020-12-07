package com.example.multimegafon3;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class NadawanieActivity extends AppCompatActivity {
    private static String TAG = "Nadawanie";
    private boolean isMulticastingReadiness = false;
    private boolean isStreamingAudio = false;
    private String myMulticastAddress = "";
    private String defaultMulticastAddress = "238.238.238.238";
    private int port = 4003;
    InetAddress groupAnnounce = null;
    MulticastSocket multicastAnnounceSocket = null;
    InetAddress groupStream = null;
    MulticastSocket multicastStreamSocket = null;
    private static final int RECORDING_RATE = 44100;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static int BUFFER_SIZE = AudioRecord.getMinBufferSize(RECORDING_RATE, CHANNEL, FORMAT);
    private AudioRecord recorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nadawanie);
        Log.d(TAG, "NadawanieCreationStarted");
        askForAccessToMicrophone();
        askForAccessToWifi();
        getMulticastAddress();
        try {
            announceMulticastReadiness();
        } catch (IOException e) {
            e.printStackTrace();
        }
        startStreamingAudio();
    }

    protected void onDestroy() {
        super.onDestroy();
        isMulticastingReadiness = false;
        isStreamingAudio = false;
    }


    private void askForAccessToMicrophone(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.RECORD_AUDIO },10);
        }
        else if(ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED){
            Log.d(TAG, "Dostep do mikrofonu przyznany przy wejsciu w aplikacje.");
        }
        else{
            Toast.makeText(getApplicationContext(), "Nie można rozpocząć nadawania - Brak dostepu do mikrofonu", Toast.LENGTH_SHORT).show();
        }
    }

    private void askForAccessToWifi(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CHANGE_WIFI_STATE },10);
        }
        else if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE)== PackageManager.PERMISSION_GRANTED){
            Log.d(TAG, "Dostep do Wifi przyznany przy wejsciu w aplikacje.");
        }
        else {
            Toast.makeText(getApplicationContext(), "Nie można rozpocząć nadawania - Brak dostępu do Wifi", Toast.LENGTH_SHORT).show();
        }
    }

    private void announceMulticastReadiness() throws IOException {
        isMulticastingReadiness = true;
        announceThread at = new announceThread();
        new Thread(at).start();
    }

    private void getMulticastAddress(){
        //224.0.0.0 to 239.255.255.255
        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        String address = Formatter.formatIpAddress(info.getIpAddress());
        String[] addressSplitted = address.split("\\.");
        String multicastAddress = "239." + addressSplitted[1] + "." + addressSplitted[2] + "." + addressSplitted[3];
        myMulticastAddress = multicastAddress;
        Log.d(TAG, "Multicast address set to: " + myMulticastAddress);
    }

    private void startStreamingAudio(){
        isStreamingAudio = true;
        streamingThread sa = new streamingThread();
        new Thread(sa).start();
    }

    class announceThread implements Runnable {
        public void run() {
            try {
                groupAnnounce = InetAddress.getByName(defaultMulticastAddress);
                multicastAnnounceSocket = new MulticastSocket(port);
                multicastAnnounceSocket.joinGroup(groupAnnounce);
                String announcingDatagramMsg = myMulticastAddress;
                byte[] announcingDatagramByte = announcingDatagramMsg.getBytes();
                DatagramPacket announcingDatagram = new DatagramPacket(announcingDatagramByte,announcingDatagramByte.length,groupAnnounce,port);

                while(isMulticastingReadiness) {
                    try {
                        Thread.sleep(100);
                        multicastAnnounceSocket.send(announcingDatagram);
                        Log.d(TAG, "AnnouncingMessageSent: " + announcingDatagramMsg);
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    class streamingThread implements Runnable {
        public void run() {
                try {
                    groupStream = InetAddress.getByName(myMulticastAddress);
                    multicastStreamSocket = new MulticastSocket(port);
                    multicastStreamSocket.joinGroup(groupStream);
                    byte[] buffer = new byte[BUFFER_SIZE];
                    DatagramPacket packet;
                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDING_RATE, CHANNEL, FORMAT, BUFFER_SIZE * 10);
                    recorder.startRecording();
                    Log.d(TAG, "I am streaming Audio.");
                    while(isStreamingAudio){
                        Log.d(TAG, "Nadaje");
                        int read = recorder.read(buffer, 0, buffer.length);
                        packet = new DatagramPacket(buffer, read,  groupStream, port);
                        multicastStreamSocket.send(packet);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

}