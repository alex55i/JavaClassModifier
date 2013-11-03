package ru.alex55i.jclassmodifier.decompiler;

import japa.parser.ast.CompilationUnit;

import java.io.File;

public abstract class Decompiler
{

	public abstract CompilationUnit[] decompileClassFiles(File[] files, File[] classFiles) throws Exception;

}
