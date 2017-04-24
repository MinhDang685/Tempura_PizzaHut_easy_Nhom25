package com.onedictprojects.soundrecorder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by kiencbui on 21/04/2017.
 */

public class CustomListAdapter extends BaseAdapter {

    private List<AudioItem> listData;
    private LayoutInflater layoutInflater;
    private Context context;

    public CustomListAdapter(List<AudioItem> listData, Context context) {
        this.listData = listData;
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return listData.size();
    }

    @Override
    public Object getItem(int position) {
        return listData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView==null) {
            convertView = layoutInflater.inflate(R.layout.list_item_layout,null);
            holder=new ViewHolder();
            holder.fileType=(ImageView) convertView.findViewById(R.id.imageView_filetype);
            holder.fileName=(TextView) convertView.findViewById(R.id.textView_filename);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }

        AudioItem item = this.listData.get(position);
        holder.fileName.setText(item.getFilename());

        int imageId = this.getMipmapResById(item.getFileType());
        holder.fileType.setImageResource(imageId);

        return convertView;
    }

    public int getMipmapResById(String resName) {
        String pkgName = context.getPackageName();

        int resId= context.getResources().getIdentifier(resName,"mipmap",pkgName);
        return resId;
    }


    static class ViewHolder {
        ImageView fileType;
        TextView fileName;
    }
}
