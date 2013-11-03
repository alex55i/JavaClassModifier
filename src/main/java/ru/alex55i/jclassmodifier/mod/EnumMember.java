package ru.alex55i.jclassmodifier.mod;

public class EnumMember
{
	private String type;
	private String value;

	public EnumMember(String type, String value)
	{
		this.type = type;
		this.value = value;
	}

	public String getType()
	{
		return type;
	}

	public String getValue()
	{
		return value;
	}

}
