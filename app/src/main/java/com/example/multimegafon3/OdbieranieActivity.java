package com.example.multimegafon3;

import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class OdbieranieActivity extends AppCompatActivity {
    private static String TAG = "Odbieranie";
    private boolean isReceivingAudio = false;
    private boolean isVerifyingQuality = false;
    private String serversMulticastAddress = null;
    private String serversIpAddress = null;

    private int port = 4003;
    private int portForQoS = 4004;
    private int portForQoSSend = 4005;
    private MulticastSocket receivingSocket;

    private static final int SAMPLE_RATE = 8000;
    private static final int CHANNEL = AudioFormat.CHANNEL_OUT_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, 1, FORMAT);
    private String chosenServer = null;

    Toast toast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        chosenServer = getIntent().getStringExtra("ChosenServer");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_odbieranie);
        //serversMulticastAddress = "239.168.0.192";
        toast = Toast.makeText(this, "Problem z jakoscia - Polaczenie zerwane", Toast.LENGTH_LONG);

        setServersAddress();
        receiveAndPlayAudioStream();
        startQualityVerification();
    }

    protected void onDestroy() {
        super.onDestroy();
        isReceivingAudio = false;
        isVerifyingQuality = false;
    }

    private void setServersAddress(){
        String[] addressesSplitted = chosenServer.split(";");
        serversMulticastAddress = addressesSplitted[0];
        serversIpAddress = addressesSplitted[1];
    }

    private void receiveAndPlayAudioStream(){
        isReceivingAudio = true;
        Log.d(TAG, "Startowanie odbioru!");
        receiveAndPlayAudio rapa = new receiveAndPlayAudio();
        new Thread(rapa).start();
    }

    private void startQualityVerification(){
        isVerifyingQuality = true;
        Log.d(TAG, "Startowanie weryfikacji jakości!");
        checkConnectionQuality CQ = new checkConnectionQuality();
        new Thread(CQ).start();
    }

    private class checkConnectionQuality implements Runnable {
        //Realnie idzie 173 wiadomosci o dlugosci 640 bytów w ciągu 6.9 sekund czyli ~128 kbps ( zmierzone 125,36 kbps )

        int somethingsWrongCounter = 0;

        public void run(){
            try {
                if(isVerifyingQuality) {
                    //DatagramSocket socketToServer = new DatagramSocket(portForQoSSend);
                    DatagramSocket socketToServer = new DatagramSocket(portForQoS);
                    InetAddress serverInetAddress = InetAddress.getByName(serversIpAddress);
                    byte[] startSeqByte = "START".getBytes();
                    DatagramPacket packetStart = new DatagramPacket(startSeqByte, startSeqByte.length, serverInetAddress, portForQoS);
                    //DatagramSocket fromServerSocket = new DatagramSocket(portForQoS);

                    byte[] recievedMessageFromServer = new byte[640];
                    DatagramPacket recievedMessageFromServerPacket = new DatagramPacket(recievedMessageFromServer, recievedMessageFromServer.length);
                    while(isVerifyingQuality){
                        byte[][] messageFromServer = new byte[25][640];
                        long[] timestamps = new long[25];
                        //Log.d(TAG,"123 Czekam...");
                        Thread.sleep(3000);
                        socketToServer.send(packetStart);
                        Log.d(TAG,"123 Wyslano do Servera");
                        //TODO
                        //fromServerSocket.setSoTimeout(100);
                        socketToServer.setSoTimeout(100);
                        for(int i=0;i<25;i++) {
                            Log.d(TAG,"123 Oczekuje na serwer " + i);
                            //fromServerSocket.receive(recievedMessageFromServerPacket);
                            socketToServer.receive(recievedMessageFromServerPacket);
                            messageFromServer[i] = Arrays.copyOfRange(recievedMessageFromServer,0,640);
                            Date date = new Date();
                            timestamps[i] = date.getTime();
                        }

                        checkQuality(messageFromServer, timestamps);
                        if(somethingsWrongCounter>=3){
                            Log.d(TAG,"123 Problem z jakoscia");
                            isReceivingAudio=false;
                            toast.show();
                            Thread.sleep(1000);
                            isVerifyingQuality=false;
                        }
                    }
                    socketToServer.close();
                    //fromServerSocket.close();
                }

            } catch (UnknownHostException | SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void checkQuality(byte[][] recievedDatagramBytes, long[] timeArray){
            boolean isJitterBig = false;
            boolean isOrderWrong = false;
            boolean isHighErrorRate = false;

            //Calculate jitter
            long jitter = 0;
            long avg_delta_temp=0;
            for(int j=1;j<25;j++){
                avg_delta_temp += timeArray[j]-timeArray[j-1];
            }
            jitter = avg_delta_temp/24;
            if(jitter>10){
                isJitterBig = true;
            }

            //Check data order
            int iteration = 0;
            for(byte[] message : recievedDatagramBytes){
                String s = new String(message);
                String[] messageSplitted =  s.split(";");
                try {
                    int firstInt = Integer.parseInt((messageSplitted[0]));
                    if (firstInt == iteration) {
                        iteration++;
                        continue;
                    } else {
                        isOrderWrong = true;
                        break;
                    }
                }
                catch(NullPointerException e){
                    e.printStackTrace();
                    Log.d(TAG,"123 ProblemWithGettingOrder");
                }
            }

            //Check error rate
            byte[][] templateArray = templateArray();
            double errorRatePart = 0;
            for(int e=0;e<25;e++){
                int n = Math.min(recievedDatagramBytes[e].length, templateArray[e].length), nLarge = Math.max(recievedDatagramBytes[e].length, templateArray[e].length);
                int unequalCount = nLarge - n;
                for (int i=0; i<n; i++)
                    if (recievedDatagramBytes[e][i] != templateArray[e][i]) unequalCount++;
                errorRatePart += unequalCount * 100.0 / nLarge;
            }
            double errorRate = errorRatePart/25;

            if(errorRate>5.0){
                Log.d(TAG,"123: " + errorRate);
                isHighErrorRate=true;
            }

            if(isHighErrorRate || isJitterBig || isOrderWrong){
                Log.d(TAG,"123: " + isHighErrorRate + " " + isJitterBig + " " + isOrderWrong);
                somethingsWrongCounter++;
            }
            else
            {
                somethingsWrongCounter=0;
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

    private class receiveAndPlayAudio implements  Runnable{
        public void run(){
            //Log.d(TAG, "Jestem w threadzie odbierania");
            try {
                InetAddress serverAddr = InetAddress.getByName(serversMulticastAddress);
                receivingSocket = new MulticastSocket(port);
                receivingSocket.joinGroup(serverAddr);
                byte [] audioBuffer = new byte[BUFFER_SIZE];
                DatagramPacket recvDatagram = new DatagramPacket(audioBuffer, audioBuffer.length);

                //CHANNEL_CONFIGURATION_MONO
                //AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 640, AudioTrack.MODE_STREAM);
                AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, CHANNEL,FORMAT, BUFFER_SIZE, AudioTrack.MODE_STREAM);
                audioTrack.play();

                while(isReceivingAudio){
                    //Log.d(TAG, "Czekam na wiadomosc");
                    receivingSocket.receive(recvDatagram);
                    recvDatagram.getData();
                    //Log.d(TAG, "odbieranko " + new String(recvDatagram.getData()));
                    audioTrack.write(recvDatagram.getData(), 0, audioBuffer.length);
                }


            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
