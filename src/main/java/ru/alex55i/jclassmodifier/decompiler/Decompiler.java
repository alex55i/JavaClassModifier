package ru.alex55i.jclassmodifier.decompiler;

import japa.parser.ast.CompilationUnit;

import java.io.File;

public interface Decompiler
{
	public CompilationUnit[] decompileClassFiles(File[] files, File[] classFiles) throws Exception;
}
