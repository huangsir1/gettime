package com.example.doctorassistant;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDatabaseHelper extends SQLiteOpenHelper
{
	final String CREATE_TABLE_SQL =
			"create table dictt(_id integer primary " +
					"key autoincrement , name,number , history_number"
					+ ",time, mlmin , mlmax , xymin , xymax"
					+ ",time1, mlmin1 , mlmax1 , xymin1 , xymax1"
					+ ",time2, mlmin2 , mlmax2 , xymin2 , xymax2"
					+ ",time3, mlmin3 , mlmax3 , xymin3 , xymax3)";
	public MyDatabaseHelper(Context context, String name, int version)
	{
		super(context, name, null, version);
	}
	@Override
	public void onCreate(SQLiteDatabase db)
	{
		db.execSQL(CREATE_TABLE_SQL);
	}
	@Override
	public void onUpgrade(SQLiteDatabase db
			, int oldVersion, int newVersion)
	{
		System.out.println("--------onUpdate Called--------"
				+ oldVersion + "--->" + newVersion);
	}
}
