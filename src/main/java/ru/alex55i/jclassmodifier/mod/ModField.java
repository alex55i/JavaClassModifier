package ru.alex55i.jclassmodifier.mod;

import javassist.CtField;
import javassist.CtField.Initializer;

public class ModField
{
	CtField field;
	Initializer initializer;

	public ModField(CtField field, Initializer initializer)
	{
		this.field = field;
		this.initializer = initializer;
	}

	public CtField getField()
	{
		return field;
	}

	public Initializer getInitializer()
	{
		return initializer;
	}

}
