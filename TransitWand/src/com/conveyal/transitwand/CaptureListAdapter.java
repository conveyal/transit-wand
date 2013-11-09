package com.conveyal.transitwand;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class CaptureListAdapter extends ArrayAdapter<RouteCapture> {
    private ArrayList<RouteCapture> routes;

    
    private int listItemLayoutResId;
    
    public CaptureListAdapter(Context context, ArrayList<RouteCapture> routes) {
        super(context, R.layout.capture_list_item_layout, routes);
        this.routes = routes;
        listItemLayoutResId = android.R.layout.two_line_list_item;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
    	
        View v = convertView;
        
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            
            v = vi.inflate(listItemLayoutResId, parent, false);
        }
            
        RouteCapture route = routes.get(position);
        
        if (route != null) {
            TextView routeName = (TextView) v.findViewById(android.R.id.text1);
            TextView details = (TextView) v.findViewById(android.R.id.text2);

            routeName.setText(route.name);
            details.setText("Stops: " + route.stops.size() + " Points: " + route.points.size());
        }
        return v;
    }
}