package com.example.finalpj;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class ListViewAdapter extends BaseAdapter {
    Context context;
    ArrayList<String> facName;
    public ListViewAdapter(Context context, ArrayList<String> facName){
        this.context=context;
        this.facName=facName;
    }
    @Override
    public int getCount() {
        return facName.size();
    }

    @Override
    public Object getItem(int i) {
        return facName.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = View.inflate(context, R.layout.lvitem, null);
        }
        TextView tv1=view.findViewById(R.id.textId);
        tv1.setText(facName.get(i));
        return view;
    }

}
