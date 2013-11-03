package ru.alex55i.jclassmodifier.mod;

import javassist.CtMethod;

public class ModMethod
{
	CtMethod method;
	String body;

	public ModMethod(CtMethod method, String body)
	{
		this.method = method;
		this.body = body;
	}

	public CtMethod getMethod()
	{
		return method;
	}

	public String getBody()
	{
		return body;
	}

}
