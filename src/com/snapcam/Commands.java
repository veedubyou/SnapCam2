package com.snapcam;

public enum Commands {
	snap(-1),
	flashon(-1),
	flashoff(-1),
	frontcamera(-1),
	backcamera(-1),
	help(-1),
	threeseconds(3),
	fourseconds(4),
	fiveseconds(5),
	sixseconds(6),
	sevenseconds(7),
	eightseconds(8),
	nineseconds(9),
	tenseconds(10);
	
	private int value;
	Commands(int value)
	{
		this.value = value;
	}
	
	public int getValue()
	{
		return value;
	}
}
