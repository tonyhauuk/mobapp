package com.example.mobapp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FragmentOtherInfo extends Fragment {
	View content;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		content = inflater.inflate(R.layout.fragment_otherinfo, null);
		//initView();

		return content;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}
	
	private void initView() {
		// TODO Auto-generated method stub
		
	}
}
