package com.example.multimegafon3;

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
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

public class NadawanieActivity extends AppCompatActivity {
    private static String TAG = "Nadawanie";
    private boolean isMulticastingReadiness = false;
    private boolean isStreamingAudio = false;
    private String myMulticastAddress = "";
    private String myIpAddress ="";
    private String defaultMulticastAddress = "238.238.238.238";
    private int port = 4003;
    private int portForQoS = 4004;
    private int portForQoSSend = 4005;
    InetAddress groupAnnounce = null;
    MulticastSocket multicastAnnounceSocket = null;
    InetAddress groupStream = null;
    MulticastSocket multicastStreamSocket = null;
    private static final int RECORDING_RATE = 8000;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    //private static final int FORMAT = AudioFormat.ENCODING_PCM_8BIT;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static int BUFFER_SIZE = AudioRecord.getMinBufferSize(RECORDING_RATE, CHANNEL, FORMAT);
    private AudioRecord recorder;
    boolean isRespondingToQosSequence = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nadawanie);
        Log.d(TAG, "NadawanieCreationStarted");
        askForAccessToMicrophone();
        askForAccessToWifi();
        getMulticastAddress();
        getIpAddress();
        try {
            startAnnouncingMulticastReadiness();
        } catch (IOException e) {
            e.printStackTrace();
        }
        startStreamingAudio();
        startRespondingToQoSSequence();
    }

    protected void onDestroy() {
        super.onDestroy();
        isMulticastingReadiness = false;
        isStreamingAudio = false;
        isRespondingToQosSequence = false;
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

    private void getIpAddress(){
        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        String address = Formatter.formatIpAddress(info.getIpAddress());
        myIpAddress = address;
    }

    private void startStreamingAudio(){
        isStreamingAudio = true;
        streamingThread sa = new streamingThread();
        new Thread(sa).start();
    }

    private void startAnnouncingMulticastReadiness() throws IOException {
        isMulticastingReadiness = true;
        announceThread at = new announceThread();
        new Thread(at).start();
    }

    private void startRespondingToQoSSequence(){
        isRespondingToQosSequence = true;
        receiveQoSSeqStarter respondSeq = new receiveQoSSeqStarter();
        new Thread(respondSeq).start();
    }

    class announceThread implements Runnable {
        public void run() {
            try {
                groupAnnounce = InetAddress.getByName(defaultMulticastAddress);
                multicastAnnounceSocket = new MulticastSocket(port);
                multicastAnnounceSocket.joinGroup(groupAnnounce);
                String announcingDatagramMsg = myMulticastAddress + ";" + myIpAddress;
                byte[] announcingDatagramByte = announcingDatagramMsg.getBytes();
                DatagramPacket announcingDatagram = new DatagramPacket(announcingDatagramByte,announcingDatagramByte.length,groupAnnounce,port);

                while(isMulticastingReadiness) {
                    try {
                        Thread.sleep(100);
                        multicastAnnounceSocket.send(announcingDatagram);
                        //Log.d(TAG, "AnnouncingMessageSent: " + announcingDatagramMsg);
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
                    DatagramPacket packet;
                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDING_RATE, CHANNEL, FORMAT, BUFFER_SIZE);
                    byte[] audioBuffer = new byte[BUFFER_SIZE];
                    recorder.startRecording();
                    int readStatus = 0;
                    while(isStreamingAudio){
                        readStatus = recorder.read(audioBuffer,0,audioBuffer.length);
                        //Log.d(TAG,"ResultOfRead: " + readStatus);
                        packet = new DatagramPacket(audioBuffer, readStatus,  groupStream, port);
                        multicastStreamSocket.send(packet);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            recorder.stop();
            recorder.release();
        }
    }

    class receiveQoSSeqStarter implements Runnable {
        public void run() {
            try {
                if(isRespondingToQosSequence) {
                    byte[][] templateArray= templateArray();
                    DatagramSocket fromClientSocket = new DatagramSocket(portForQoS);
                    byte[] recievedSeqStartMessageByte = new byte["START".getBytes().length];
                    DatagramPacket recievedSeqStartMessagePacket = new DatagramPacket(recievedSeqStartMessageByte, recievedSeqStartMessageByte.length);
                    // clientInetAddress;
                    while(isRespondingToQosSequence){
                        fromClientSocket.receive(recievedSeqStartMessagePacket);
                        InetAddress clientInetAddress=recievedSeqStartMessagePacket.getAddress();
                        Log.d(TAG,"123 Otrzymano od klienta " + clientInetAddress);
                        String text = new String(recievedSeqStartMessageByte, 0, recievedSeqStartMessagePacket.getLength());
                        if(text.equals("START")){
                            //TODO new respondToQoSSeqStarter(socket).start(); ???


                            //DatagramSocket toClientSocket = new DatagramSocket(portForQoSSend);
                            for(int i=0;i<25;i++){
                                //byte[] startSeqByte = "STOP".getBytes();
                                byte[] startSeqByte = templateArray[i];
                                DatagramPacket packetStart = new DatagramPacket(startSeqByte, startSeqByte.length, clientInetAddress, portForQoS);
                                //TODO
                                //toClientSocket.send(packetStart);
                                fromClientSocket.send(packetStart);
                                //String s = new String(startSeqByte);
                                //Log.d(TAG,"123 Wyslano do klienta " + clientInetAddress + " " + s + " "+ i);
                                //Log.d(TAG,"123 Wyslano do klienta " + clientInetAddress + " " + i);
                            }
                            Log.d(TAG,"123 Wyslano do klienta " + clientInetAddress);
                        }
                    }
                    fromClientSocket.close();
                    //toClientSocket.close();
                }
            } catch (UnknownHostException | SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private byte[][] templateArray(){
            byte[][] templateArrayList = new byte[25][640];
            String sampleText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin nibh augue, suscipit a, scelerisque sed, lacinia in," +
                    " mi. Cras vel lorem. Etiam pellentesque aliquet tellus. Phasellus pharetra nulla ac diam. Quisque semper justo at risus. Donec venenatis," +
                    " turpis vel hendrerit interdum, dui ligula ultricies purus, sed posuere libero dui id orci. Nam congue, ped";
            String tempString;
              for(int i=0;i<25;i++){
                tempString = i + ";" + sampleText;
                byte[] byteSampleText = tempString.getBytes();
                templateArrayList[i] = Arrays.copyOfRange(byteSampleText,0,640);
            }
            return templateArrayList;
        }
    }

    public class respondToQoSSeqStarter extends Thread {
        private DatagramSocket socket;
        private boolean running;
        private byte[] buf = new byte[256];

        public respondToQoSSeqStarter(InetAddress clientip,int clientport){
            //socket = new DatagramPacket(buf, buf.length, address, port);
        }

        public void run() {
            running = true;

            while (running) {
                DatagramPacket packet
                        = new DatagramPacket(buf, buf.length);
                //socket.receive(packet);

                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                packet = new DatagramPacket(buf, buf.length, address, port);
                String received
                        = new String(packet.getData(), 0, packet.getLength());

                if (received.equals("end")) {
                    running = false;
                    continue;
                }
                //socket.send(packet);
            }
            socket.close();
        }
    }

}