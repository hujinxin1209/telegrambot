package org.telegram;

public enum LogisticsEnum {
	EMS("EMS", "EMS"),
	SF("顺丰", "SF"),
	YTO("圆通","YTO"),
	HTKY("百世快递","HTKY"),
	ZTO("中通","ZTO"),
	YD("韵达","YD"),
	STO("申通","STO"),
	DBL("德邦","DBL"),
	JD("京东","JD"),
	ZJS("宅急送","ZJS"),
	HHTT("天天","HHTT"),
	YZPY("邮政快递包裹","YZPY");
	
	private String value;
	private String code;
	
	LogisticsEnum(String value, String code){
		this.value = value;
		this.code = code;
	}
	
	public String getValue(){
		return value;
	}
	
	public String getCode() {
		return code;
	}
}
