package ru.alex55i.jclassmodifier;

import japa.parser.ast.CompilationUnit;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javassist.ClassPool;
import javassist.CtClass;
import ru.alex55i.jclassmodifier.decompiler.Decompiler;
import ru.alex55i.jclassmodifier.mod.DecompiledFile;
import ru.alex55i.jclassmodifier.modifier.ClassModifier;
import ru.alex55i.jclassmodifier.modifier.JavassistModifier;
import ru.alex55i.jclassmodifier.util.UrlClassPath;

import com.google.common.io.Files;

public class Helper
{

	public static DecompiledFile decompile(Decompiler decompiler, Collection<File> classpath, ClassContainer container) throws Exception
	{
		List<File> classFiles = new ArrayList<File>();

		byte[] bytes = container.getBytes();
		File tempDir = Files.createTempDir();
		File classFile = new File(tempDir, container.getSimpleName());
		Files.write(bytes, classFile);
		classFiles.add(classFile);

		System.out.println("Decompiling..");
		CompilationUnit[] units = decompiler.decompileClassFiles(
				classpath.toArray(new File[classpath.size()]),
				classFiles.toArray(new File[classFiles.size()]));
		System.out.println("Decompiled!");
		assert units.length == 1 : units.length;
		for (final CompilationUnit unit : units)
		{
			final DecompiledFile srcClass = new DecompiledFile(unit, container);
			srcClass.recomputeChecksum();
			return srcClass;
		}
		throw new RuntimeException("No decompiled files");
	}

	public static void compileAndUpdate(Collection<File> classpath, DecompiledFile classFile) throws Exception
	{
		ClassPool pool = new ClassPool(true);
		for (File cf : classpath)
		{
			pool.appendClassPath(new UrlClassPath(cf.toURI().toURL()));
		}
		System.out.println("Compiling");
		CompilationUnit unit = classFile.getUnit();
		ClassModifier modifer = new JavassistModifier(pool, new CompilationUnit[] { unit });
		CtClass[] outClasses = modifer.modify();
		assert outClasses.length == 1 : outClasses.length;
		classFile.getContainer().updateClass(outClasses[0]);
	}
}
