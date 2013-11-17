package ru.alex55i.jclassmodifier.gui;

import java.io.File;

import ru.alex55i.jclassmodifier.decompiler.Decompiler;
import ru.alex55i.jclassmodifier.mod.DecompiledFile;


public class Modificable
{
	public File[] originalClasses;
	public Decompiler decompiler;
	public DecompiledFile source;

	public Modificable(File[] originalClasses, Decompiler decompiler, DecompiledFile source)
	{
		this.originalClasses = originalClasses;
		this.decompiler = decompiler;
		this.source = source;
	}

}
