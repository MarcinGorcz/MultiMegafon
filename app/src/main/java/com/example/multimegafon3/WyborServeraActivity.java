package com.example.multimegafon3;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.GenericArrayType;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.List;

public class WyborServeraActivity extends AppCompatActivity {
    private static String TAG = "Wybor";
    private ArrayList<String> detectedServers = new ArrayList<>();
    private String defaultMulticastAddress = "238.238.238.238";
    private int port = 4003;
    private boolean isListeningForServers = false;
    private String chosenServer = null;
    private RecyclerView serverRecyclerView;
    private ListAdapter recyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wybor_servera);

        startListiningForservers();
        drawRecycleList();
        getOnlyMulticastView();
    }

    public void RefreshList(View view){
        drawRecycleList();
    }

    public void drawRecycleList(){
        serverRecyclerView = (RecyclerView)findViewById(R.id.detectedServersRecyclerView);

        LinearLayoutManager recyclerLayoutManager = new LinearLayoutManager(this);
        serverRecyclerView.setLayoutManager(recyclerLayoutManager);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(serverRecyclerView.getContext(), recyclerLayoutManager.getOrientation());
        serverRecyclerView.addItemDecoration(dividerItemDecoration);

        //ListAdapter recyclerViewAdapter = new ListAdapter(testList(),this);
        //ListAdapter recyclerViewAdapter = new ListAdapter(detectedServers,this);
        recyclerViewAdapter = new ListAdapter(getOnlyMulticastView(),this);
        serverRecyclerView.setAdapter(recyclerViewAdapter);
    }

    private ArrayList<String> testList(){
        ArrayList<String> testList = new ArrayList<>();
        testList.add("Server1");
        testList.add("Server2");
        testList.add("Server3");
        testList.add("Server4");
        testList.add("Server5");
        testList.add("Server6");
        testList.add("Server7");
        testList.add("Server8");
        return testList;
    }

    protected ArrayList<String> getOnlyMulticastView(){
        int i = 0;
        ArrayList<String> onlyMulticastView = new ArrayList<>();
        for (String server : detectedServers)
        {
            i++;
            String[] arrayOfServerIps = server.split(";", 2);
            onlyMulticastView.add("Serwer " + i + ": " + arrayOfServerIps[0]);
            Log.d(TAG,"Serwer " + i + ": " + arrayOfServerIps[0]);
        }
        return onlyMulticastView;
    }

    protected void onDestroy() {
        super.onDestroy();
        isListeningForServers = false;
    }

    public void moveToOdbieranie(View view) {

        if(recyclerViewAdapter.getLastSelectedPosition()>-1){
            chosenServer = detectedServers.get(recyclerViewAdapter.getLastSelectedPosition());
            Log.d(TAG,"Wybrany serwer to: " + chosenServer);
        }
        else{
            Toast.makeText(getApplicationContext(), "Proszę wybrać serwer", Toast.LENGTH_SHORT).show();
        }


        if(chosenServer!=null){
            Intent intent = new Intent(this, OdbieranieActivity.class);
            intent.putExtra("ChosenServer", chosenServer);
            startActivity(intent);
        }
    }

    public void startListiningForservers(){
        isListeningForServers = true;
        listenForBroadcastingServers serverListener = new listenForBroadcastingServers();
        new Thread(serverListener).start();
    }

    private class listenForBroadcastingServers implements  Runnable{
        byte[] buffer=new byte[1024];
        public void run(){
            while(isListeningForServers){
                DatagramPacket packet=new DatagramPacket(buffer, buffer.length);
                try {
                    MulticastSocket socket = new MulticastSocket(port);
                    InetAddress defaultMulticastIp = InetAddress.getByName(defaultMulticastAddress);
                    socket.joinGroup(defaultMulticastIp);
                    socket.receive(packet);
                    String detectedServerMulticastIps=new String(packet.getData(), packet.getOffset(),packet.getLength());
                    if(!detectedServers.contains(detectedServerMulticastIps)) {
                        detectedServers.add(detectedServerMulticastIps);
                        Log.d(TAG, "Server added to list : " +  detectedServerMulticastIps);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

}