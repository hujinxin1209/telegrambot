package org.telegram;

import java.util.ArrayList;
import java.util.List;

public class BuildVars {
	public static final String pathToLogs = "./";
	public static final String linkDB = "jdbc:mysql://localhost:3306/telegram?useUnicode=true&characterEncoding=UTF-8";
	public static final String controllerDB = "com.mysql.cj.jdbc.Driver";
	public static final String userDB = "root";
	public static final String password = "123456";
	public static final List<Integer> ADMINS = new ArrayList<>();
	
	static {
		// init
	}
}
