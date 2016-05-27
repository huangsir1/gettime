package com.example.doctorassistant;

import android.util.Log;

import java.util.Date;

public class MyDataUtil {
	/*
	 * Baudrate：115200，Parity：none，connect pair serial number：0000
	 * 
	 * Package length: 11 bytes
	 * Package transfer rate: N/A （maximum transfer time span is 1 second）
	 * 
	 * Package Format:
	 * Byte0：0xff				Head Mark1
	 * Byte1：0xaa 				Head Mark2
	 * Byte2：Package Index 		Package index series
	 * Byte3：Status Byte		SpO2 status info
	 * Byte4：PulseRate 	       	Average PulseRate
	 * Byte5：SpO2Sat  	       	Average SpO2Sat
	 * Byte6：PlethSignal	    Pulse infusion intensity
	 * Byte7：Battery	 		Battery power in percent
	 * Byte8：rrInterval 		Low Byte of rrInterval
	 * Byte9：rrInterval 		High Byte of rrInterval
	 * Byte10：CheckSum			Checksum of the whole package
	 * 
	 * Package Index:			Range (0~255), the index of the package
	 * Status Byte				0x01	SpO2 Sensor Off
	 * 							0x02	No finger
	 * 							0x04	No pulse signal
	 * 							0x08	Pulse Beat Tips （rrInterval update Tip）
	 * PulseRate				Range (25~250, invalid value=255) unit：bpm
	 * SpO2Sat 	       			Range (35~100, invalid value=127) unit：%
	 * PlethSignal				Range (1~200, invalid value=0) unit：‰
	 * Battery:					Range (0~100) unit：%
	 * rrInterval:				Range (20~300, invalid value=0), 
	 * 							Formula: PulseRate = 60 * 100 / rrInterval
	 * CheckSum					Range (0~255)
	 * 							Formula: CheckSum = (Byte0+Byte1+…+Byte9) % 256
	 */

	private static final boolean IsCheckSumSupport = true;		//wdl, 包校验的开关
	private static final int[] PKG_HEAD = new int[]{0xff, 0xaa};  //包头校验值
	private byte[] pkgData;  //解析数据临时存储数组
	private int pkgLength;  //数据包长度
	private int pkgIndexOld, pkgIndexNew;  //数据包新、旧index值，检验数据包是否连续
	private boolean IsCheckSumError;  //检验校验是否通过
	private boolean IsPackageLost;  //检验数据包是否丢失

	public NewDataNotify mNewDataNotify;

	public interface NewDataNotify {
		void notifyNewData(EquipmentData equipmentData);
		void notifyToomuchData(int flag);
		void notifypackagelost();
	}
	
	public MyDataUtil(NewDataNotify ndn) {
		mNewDataNotify = ndn;
		pkgLength = IsCheckSumSupport ? 11 : 10;
		pkgData = new byte[pkgLength];
		pkgIndexOld = -1;
		IsCheckSumError = false;
		IsPackageLost = false;
	}
	
	public void getNewWork(byte[] values, int len) {
		for (int i = 0; i < len; i++) {
			for (int j = 0; j < pkgLength - 1; j++) {
				pkgData[j] = pkgData[j + 1];
			}
			pkgData[pkgLength - 1] = values[i];
			if ((0xFF & pkgData[0]) == PKG_HEAD[0] && (0xFF & pkgData[1]) == PKG_HEAD[1]) {
				parsePackage();
			}
		}
	}

	private void parsePackage() {

		CheckIfPackageLost();

		CheckIfCheckSumError();
		
		if(IsPackageLost) mNewDataNotify.notifypackagelost();

		if (!IsCheckSumError) {
			EquipmentData equipmentData = new EquipmentData();
			equipmentData.setHead1(pkgData[0]);
			equipmentData.setHead2(pkgData[1]);
			equipmentData.setIndex(0xFF & pkgData[2]);
			equipmentData.setStatus(pkgData[3]);
			equipmentData.setMl(0xFF & pkgData[4]);
			equipmentData.setXy(0xFF & pkgData[5]);
			equipmentData.setPI(0xFF & pkgData[6]);
			equipmentData.setBattery(0xFF & pkgData[7]);
			equipmentData.setRR((0xFF & pkgData[8]) + ((0xFF & pkgData[9]) << 8));
			//Log.i("wdl", String.format("%d, %d, RR = %d", pkgData[10], pkgData[11], equipmentData.getRR()));
			if (IsCheckSumSupport)
				equipmentData.setCheckSum(pkgData[10]);
			else
				equipmentData.setCheckSum((byte) 0);
			equipmentData.setNumTime(System.currentTimeMillis());
			if (mNewDataNotify != null) mNewDataNotify.notifyNewData(equipmentData);
		}
	}

	private void CheckIfPackageLost() {
		
		pkgIndexNew = 0xFF & pkgData[2];
		
		IsPackageLost = false;
		if (pkgIndexOld != -1 && (pkgIndexOld + 1) % 256 != pkgIndexNew) {
			IsPackageLost = true;
			Log.i("wdl", String.format("Package lost, pkgIndexOld = %d, pkgIndexNew = %d", pkgIndexOld, pkgIndexNew));
			DumpPackage();
		}
		pkgIndexOld = pkgIndexNew;
	}

	private void CheckIfCheckSumError() {
		IsCheckSumError = false;
		if (IsCheckSumSupport) {
			byte checkSum;

			//wdl, 特别关注包尾最后2个字节与包头2个字节完全相同的情况，
			//wdl, 采用异或校验方式时，上述情况发生几率较大，会导致CheckIfPackageLost()发生错误判断，
			//wdl, 采用和校验方式时，上述情况发生几率较小，但仍需进一步观察
			if ((0xFF & pkgData[pkgLength - 2]) == PKG_HEAD[0] && (0xFF & pkgData[pkgLength - 1]) == PKG_HEAD[1]) {
				DumpPackage();
			}

			checkSum = CalcCheckSum();
			if (CalcCheckSum() != pkgData[pkgLength - 1]) {
				IsCheckSumError = true;
				Log.i("wdl", String.format("check sum failed, checkSum = 0x%02x, pkgData[%d] = 0x%02x",
						checkSum, pkgLength - 1, pkgData[pkgLength - 1]));
				DumpPackage();
			}
		}
	}

	private byte CalcCheckSum() {
		byte checkSum = 0;

		for (int i = 0; i < pkgLength - 1; i++) {
//			checkSum ^= pkgData[i];		//wdl, 异或校验
			checkSum += pkgData[i];		//wdl, 和校验
		}
		return checkSum;
	}

	private void DumpPackage() {
		String str = "pkgData = { ";

		for (int i = 0; i < pkgLength; i++) {
			str += String.format("0x%02x ", pkgData[i]);
		}
		str += "}";
		Log.i("wdl", str);
	}
}
