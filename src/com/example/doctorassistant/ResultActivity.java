package com.example.doctorassistant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.List;
import java.util.Map;

public class ResultActivity extends Activity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.popup);
		ListView listView = (ListView) findViewById(R.id.show);
		Intent intent = getIntent();
		Bundle data = intent.getExtras();
		@SuppressWarnings("unchecked")
		List<Map<String, String>> list = (List<Map<String, String>>) 
				data.getSerializable("data");
		String name=data.getString("name", "***");
		int rank=data.getInt("rank");
		if(name.isEmpty()){name="患者"+rank;}
		setTitle("历史纪录"+"                                                                 ("+name+")");
		SimpleAdapter adapter = new SimpleAdapter(ResultActivity.this
				, list,
				R.layout.result_line, new String[] { "time","mlmin", "mlmax","xymin", "xymax"  }
				, new int[] {R.id.time,R.id.mlmin, R.id.mlmax ,R.id.xymin,R.id.xymax});
		listView.setAdapter(adapter);
	}
}