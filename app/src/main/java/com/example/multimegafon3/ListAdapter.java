package com.example.multimegafon3;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;


public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {
    private ArrayList<String> detectedServersList;
    private Context context;
    private int lastSelectedPosition = -1;
    private static String TAG = "ListAdapter";


    public ListAdapter(ArrayList<String> detectedServers, Context ctx) {
        detectedServersList = detectedServers;
        context = ctx;
    }

    @Override
    public ListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row, parent, false);
        ListAdapter.ViewHolder viewHolder = new ListAdapter.ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ListAdapter.ViewHolder holder, int position) {
        String selectedServer = detectedServersList.get(position);
        holder.serverItem.setText(selectedServer);
        holder.selectionState.setChecked(lastSelectedPosition == position);
        Log.d(TAG, "Adding element: " + position );
    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "SizeOFList : " + detectedServersList.size() );
        return detectedServersList.size();
    }

    public String getSelectedServer(){
        return detectedServersList.get(lastSelectedPosition);
    }

    public int getLastSelectedPosition(){
        return lastSelectedPosition;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView serverItem;
        public RadioButton selectionState;

        public ViewHolder(View view) {
            super(view);
            serverItem = (TextView)view.findViewById(R.id.server_ip);
            selectionState = (RadioButton)view.findViewById(R.id.server_select);

            selectionState.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    lastSelectedPosition = getAdapterPosition();
                    notifyDataSetChanged();

                    Toast.makeText(ListAdapter.this.context,
                            "Wybrany serwer to: " + serverItem.getText(),
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
