package com.example.snapcam;

public enum Commands {
	snap(-1),
	flashon(-1),
	flashoff(-1),
	front(-1),
	back(-1),
	three(3),
	four(4),
	five(5),
	six(6),
	seven(7),
	eight(8),
	nine(9),
	ten(10);
	
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
