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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
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

    private static final int SAMPLE_RATE = 8000;
    private static final int CHANNEL = AudioFormat.CHANNEL_OUT_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, 1, FORMAT);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_odbieranie);
        serversAddress = "239.168.0.192";
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
        /*/TAKE 2.0/
        public void run(){
            try{
                InetAddress serverAddr = InetAddress.getByName(serversAddress);
                receivingSocket = new MulticastSocket(port);
                receivingSocket.joinGroup(serverAddr);

                byte [] audioBuffer = new byte[BUFFER_SIZE];
                BufferedInputStream myBis = new BufferedInputStream();
                DataInputStream myDis = new DataInputStream(myBis);
            }
            catch(UnknownHostException eUHE){
                eUHE.printStackTrace();
            }
            catch(IOException eIOE ){
                eIOE.printStackTrace();
            }
        }
        */
        public void run(){
            Log.d(TAG, "Jestem w threadzie odbierania");
            try {
                InetAddress serverAddr = InetAddress.getByName(serversAddress);
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
