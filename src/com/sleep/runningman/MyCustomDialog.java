package com.sleep.runningman;

import com.example.doctorassistant.R;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class MyCustomDialog extends Dialog {
	// 定义回调事件，用于dialog的点击事件
	public interface OnCustomDialogListener {
		public void callback();
	}

	// private String textTitle;
	private TextView message;
	private String textMessage;
	private Button button;
	private String textButton;
	private boolean hideButton = false;
	private OnCustomDialogListener customDialogListener;

	/**
	 * 自定义dialog，可修改显示的内容，可隐藏按钮，可修改按钮中的文字，可自定义按钮触发的事件
	 * 
	 * @param context
	 *            上下文
	 * @param textMessage
	 *            dialog提示的内容
	 * @param hideButton
	 *            true：显示按钮，且点击外部可以取消对话框，false：隐藏按钮，且点击外部不能取消对话框
	 * @param textButton
	 *            按钮中的文字
	 * @param customDialogListener
	 *            按钮的点击事件监听器
	 */
	public MyCustomDialog(Context context, String textMessage, boolean hideButton, String textButton,
			OnCustomDialogListener customDialogListener) {
		super(context);
		this.textMessage = textMessage;
		this.hideButton = hideButton;
		this.textButton = textButton;
		this.customDialogListener = customDialogListener;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 设置去除屏幕顶端的标题
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		// 设置动画效果
		getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
		// 设置全屏
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
		// 设置背景图
		getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		// 设置点击dialog外部时，是否可以取消dialog
		setCanceledOnTouchOutside(hideButton);
		setCancelable(hideButton);

		if (hideButton) {
			setContentView(R.layout.dialog_custom);
			button = (Button) findViewById(R.id.positive_button);
			// 设置按钮的文字内容
			if (textButton != null) {
				button.setText(textButton);
			}
			// 设置按钮的点击事件监听器
			button.setOnClickListener(clickListener);
		} else {
			setContentView(R.layout.dialog_custom_no_button);
		}
		// //设置dialog的标题
		// setTitle(textTitle);
		// 设置dialog的文字内容
		message = (TextView) findViewById(R.id.message);
		message.setText(textMessage);
	}

	private View.OnClickListener clickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			customDialogListener.callback();
			MyCustomDialog.this.dismiss();
		}
	};

}