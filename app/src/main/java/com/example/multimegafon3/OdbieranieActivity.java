package com.example.multimegafon3;

import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

public class OdbieranieActivity extends AppCompatActivity {
    private static String TAG = "Odbieranie";
    private boolean isReceivingAudio = false;

    private String serversMulticastAddress = null;
    private String serversIpAddress = null;

    private int port = 4003;
    private MulticastSocket receivingSocket;

    private static final int SAMPLE_RATE = 8000;
    private static final int CHANNEL = AudioFormat.CHANNEL_OUT_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, 1, FORMAT);
    private String chosenServer = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        chosenServer = getIntent().getStringExtra("ChosenServer");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_odbieranie);
        //serversMulticastAddress = "239.168.0.192";
        setServersAddress();
        receiveAndPlayAudioStream();
    }

    protected void onDestroy() {
        super.onDestroy();
        isReceivingAudio = false;
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

    }

    private class checkConnectionQuality implements  Runnable {
        public void run(){

        }
    }

    private class receiveAndPlayAudio implements  Runnable{
        public void run(){
            Log.d(TAG, "Jestem w threadzie odbierania");
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
                    Log.d(TAG, "Czekam na wiadomosc");
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
