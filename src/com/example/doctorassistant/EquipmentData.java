package com.example.doctorassistant;

import java.util.Date;

public class EquipmentData {

	private Byte Head1; 		// 数据包头1
	private Byte Head2; 		// 数据包头2
	private Integer ml; 		// 平均后的脉率
	private Integer mlr; 		// 实时脉率
	private Integer xy; 		// 平均后的血氧
	private Integer xyr; 		// 实时血氧
	private Integer PPG; 		// PPG波
	private Integer PI; 		// PI强度
	private Byte Status; 		// 血氧状态位
	private Integer Battery; 	// 电池剩余量
	private Integer RR; 		// RR间期
	private Integer Index; 		// 数据包目录
	private Byte checkSum;		// 校验字节
	private Date time; 			// 当前数据解析出来的时间
	private long numTime; 		// 当前数据解析出来的时间
	// private Integer xybx;
	// private String jgbz = "";
	// private Byte Lowest;
	// private Byte Lower ;
	// private Byte Higher ;
	// private Byte Highest ;

	public boolean isValid() {
		boolean flag = false;
		if (xy != null && xy.intValue() != 0) {
			flag = true;
		} else if (ml != null && ml.intValue() != 0) {
			flag = true;
		}
		return flag;
	}

	public Byte getHead1() {
		return Head1;
	}

	public void setHead1(Byte head1) {
		Head1 = head1;
	}

	public Byte getHead2() {
		return Head2;
	}

	public void setHead2(Byte head2) {
		Head2 = head2;
	}

	public Integer getMl() {
		return ml;
	}

	public void setMl(Integer ml) {
		this.ml = ml;
	}

	public Integer getMlr() {
		return mlr;
	}

	public void setMlr(Integer mlr) {
		this.mlr = mlr;
	}

	public Integer getXy() {
		return xy;
	}

	public void setXy(Integer xy) {
		this.xy = xy;
	}

	public Integer getXyr() {
		return xyr;
	}

	public void setXyr(Integer xyr) {
		this.xyr = xyr;
	}

	public Integer getPPG() {
		return PPG;
	}

	public void setPPG(Integer pPG) {
		PPG = pPG;
	}

	public Integer getPI() {
		return PI;
	}

	public void setPI(Integer pI) {
		PI = pI;
	}

	public Byte getStatus() {
		return Status;
	}

	public void setStatus(Byte status) {
		Status = status;
	}

	public Integer getBattery() {
		return Battery;
	}

	public void setBattery(Integer battery) {
		Battery = battery;
	}

	public Integer getRR() {
		return RR;
	}

	public void setRR(Integer rR) {
		RR = rR;
	}

	public Integer getIndex() {
		return Index;
	}

	public void setIndex(Integer index) {
		Index = index;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public long getNumTime() {
		return numTime;
	}

	public void setNumTime(long numTime) {
		this.numTime = numTime;
	}

	public Byte getCheckSum() {
		return checkSum;
	}

	public void setCheckSum(Byte checkSum) {
		this.checkSum = checkSum;
	}


}
