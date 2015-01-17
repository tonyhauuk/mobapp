package com.stackbase.mobapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.stackbase.mobapp.activity.PreferencesActivity;
import com.stackbase.mobapp.activity.ThumbnailsActivity;
import com.stackbase.mobapp.utils.Constant;
import com.stackbase.mobapp.utils.Helper;

import java.io.File;
import java.util.ArrayList;

public class FragmentOtherInfo extends Fragment {
    Activity active;
    View content;
    ImageButton creditReportBtn;
    ImageButton marriageCertBtn;
    ImageButton contractBtn;

    private SharedPreferences prefs;

    private static ArrayList<View> getViewsByTag(ViewGroup root, String tag, Class<?> viewType) {
        ArrayList<View> views = new ArrayList<View>();
        final int childCount = root.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = root.getChildAt(i);
            if (child instanceof ViewGroup) {
                views.addAll(getViewsByTag((ViewGroup) child, tag, viewType));
            }

            final Object tagObj = child.getTag();
            if (tagObj != null && tagObj.equals(tag) && viewType.isInstance(child)) {
                views.add(child);
            }
        }
        return views;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        active = this.getActivity();
        content = inflater.inflate(R.layout.fragment_otherinfo, null);

        creditReportBtn = (ImageButton) content.findViewById(R.id.creditReportBtn);
        marriageCertBtn = (ImageButton) content.findViewById(R.id.marriageCertBtn);
        contractBtn = (ImageButton) content.findViewById(R.id.contractBtn);

        ImageButton.OnClickListener clickListener = new ImageButton.OnClickListener() {
            @Override
            public void onClick(View v) {
                String id = active.getIntent().getStringExtra(Constant.INTENT_KEY_ID);
                String name = active.getIntent().getStringExtra(Constant.INTENT_KEY_NAME);
                String label = String.valueOf(v.getId());

                // The folder name is generated by idnumber + borrower name + the id of picture type
                String subFolder = Helper.getMD5String(name + id + label);
                prefs = PreferenceManager.getDefaultSharedPreferences(active);
                String rootDir = prefs.getString(PreferencesActivity.KEY_STORAGE_DIR,
                        Constant.DEFAULT_STORAGE_DIR);
                File imageFolder = new File(rootDir + File.separator + subFolder);
                if (!imageFolder.exists()) {
                    imageFolder.mkdirs();
                }
                Intent intent = new Intent();
                intent.putExtra(Constant.INTENT_KEY_PIC_FOLDER, imageFolder.getAbsolutePath());
                intent.setClass(active, ThumbnailsActivity.class);
                startActivity(intent);


            }

        };

        creditReportBtn.setOnClickListener(clickListener);
        marriageCertBtn.setOnClickListener(clickListener);
        contractBtn.setOnClickListener(clickListener);

        return content;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }
}
