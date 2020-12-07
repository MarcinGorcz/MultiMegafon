package com.example.multimegafon3;

import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class OdbieranieActivity extends AppCompatActivity {
    private static String TAG = "Odbieranie";
    private boolean isReceivingAudio = false;

    private String serversAddress = null;
    private int port = 4003;
    private MulticastSocket receivingSocket;

    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL = AudioFormat.CHANNEL_OUT_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, FORMAT);
    int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_odbieranie);
        serversAddress = "239.168.0.60";
        receiveAndPlayAudioStream();
    }

    protected void onDestroy() {
        super.onDestroy();
        isReceivingAudio = false;
    }

    private void receiveAndPlayAudioStream(){
        isReceivingAudio = true;
        Log.d(TAG, "Startowanie odbioru!");
        receiveAndPlayAudio rapa = new receiveAndPlayAudio();
        new Thread(rapa).start();
    }

    private class receiveAndPlayAudio implements  Runnable{
        public void run(){
            Log.d(TAG, "Jestem w threadzie odbierania");
            try {
                InetAddress serverAddr = InetAddress.getByName(serversAddress);
                receivingSocket = new MulticastSocket(port);
                receivingSocket.joinGroup(serverAddr);
                byte [] audioBuffer = new byte[4096];
                DatagramPacket recvDatagram = new DatagramPacket(audioBuffer, audioBuffer.length);
                //Does not compile -> Problem with configuration.
                AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
                while(isReceivingAudio){
                    Log.d(TAG, "Czekam na wiadomosc");
                    receivingSocket.receive(recvDatagram);
                    Log.d(TAG, "Otrzymalem wiadomosc");
                    Log.d(TAG, "odbieranko" + recvDatagram.getData());
                    audioTrack.play();
                    audioTrack.write(audioBuffer, 0, audioBuffer.length);
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
