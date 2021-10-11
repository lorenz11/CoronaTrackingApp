package com.hello.coronatrackingapp.screens;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.core.content.ContextCompat;

import com.hello.coronatrackingapp.R;

import java.util.ArrayList;

/**
 * this class is an ArrayAdapter that is used in
 * @see LogScreen to configure the ListView, specifically color code
 * its entries.
 */
public class OperationsListAdapter extends ArrayAdapter {
    Context context;

    public OperationsListAdapter(Context context, int item, ArrayList<String> list) {
        super(context, item, list);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        if (getItem(position).toString().substring(0,1).equals("G")) {
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.GO));
        } else if(getItem(position).toString().substring(0,1).equals("B")){
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.BLE));
        } else if(getItem(position).toString().substring(0,1).equals("S")){
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.SC));
        } else if(getItem(position).toString().substring(0,1).equals("D")){
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.DB));
        } else if(getItem(position).toString().substring(0,1).equals("E")){
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.EN));
        }
        return view;
    }
}
