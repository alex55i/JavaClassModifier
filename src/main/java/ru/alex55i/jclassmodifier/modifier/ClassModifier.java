package ru.alex55i.jclassmodifier.modifier;

import japa.parser.ast.CompilationUnit;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;

public abstract class ClassModifier
{
	protected ClassPool classPool;
	protected CompilationUnit[] units;

	public ClassModifier(ClassPool classPool, CompilationUnit[] units)
	{
		this.classPool = classPool;
		this.units = units;
	}

	public abstract CtClass[] modify() throws NotFoundException, CannotCompileException, BadBytecode;

}
