package ru.alex55i.jclassmodifier.decompiler;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.EnumConstantDeclaration;
import japa.parser.ast.body.EnumDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.PrimitiveType;
import japa.parser.ast.type.ReferenceType;
import japa.parser.ast.type.Type;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import joptsimple.internal.Strings;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;

import ru.alex55i.jclassmodifier.util.Utils;

import com.google.common.io.Files;


public class FernflowerDecompiler extends Decompiler
{

	public CompilationUnit[] decompileClassFiles(File[] classpath, File[] files) throws IOException, InterruptedException, ParseException
	{
		File tempdir = Files.createTempDir();
		if (tempdir.exists())
		{
			FileUtils.cleanDirectory(tempdir);
		}

		List<String> cmd = new ArrayList<String>();
		cmd.add("java");
		cmd.add("-jar");
		cmd.add(new File("fernflower.jar").getAbsolutePath());
		cmd.add("-din=0");
		cmd.add("-rbr=0");
		for (File f : files)
		{
			cmd.add(f.getAbsolutePath());
		}
		for (File f : classpath)
		{
			if (!ArrayUtils.contains(files, f))
				cmd.add("-e=" + f.getAbsolutePath());
		}
		cmd.add(tempdir.getAbsolutePath());

		System.out.println("Executing: " + Strings.join(cmd, " "));

		Process proc = new ProcessBuilder(cmd).start();
		Utils.printProcessOutput(proc);
		proc.waitFor();

		List<CompilationUnit> units = new ArrayList<CompilationUnit>();
		Collection<File> list = FileUtils.listFiles(tempdir, null, true);
		for (File f : list)
		{
			if (f == null)
				continue;
			if (f.getName().endsWith(".java"))
			{
				CompilationUnit unit = JavaParser.parse(f);
				units.add(unit);
				for (TypeDeclaration td : unit.getTypes())
				{
					if (td instanceof EnumDeclaration)
					{
						// Remove first two args from enum values and constructors
						List<EnumConstantDeclaration> entries = ((EnumDeclaration) td).getEntries();
						for (EnumConstantDeclaration entry : entries)
						{
							entry.getArgs().remove(1);
							entry.getArgs().remove(0);
						}
						BodyDeclaration toRemove = null;
						for (BodyDeclaration bd : td.getMembers())
						{
							if (bd instanceof FieldDeclaration && ((FieldDeclaration) bd).getVariables().get(0).getId().getName().endsWith("$VALUES"))
							{
								toRemove = bd;
							}
							else if (bd instanceof ConstructorDeclaration)
							{
								List<Parameter> params = ((ConstructorDeclaration) bd).getParameters();
								if (params.size() >= 2)
								{
									Type t0 = params.get(0).getType();
									Type t1 = params.get(1).getType();
									if (((t0 instanceof ClassOrInterfaceType) || (t0 instanceof ReferenceType)) && (t1 instanceof PrimitiveType))
									{
										if ("String".equals(t0.toString()) || "java.lang.String".equals(t0.toString()) && "int".equals(t1.toString()))
										{
											params.remove(1);
											params.remove(0);
										}
									}
								}
							}
						}
						if (toRemove != null)
							td.getMembers().remove(toRemove);
					}
					else if (td instanceof ClassOrInterfaceDeclaration)
					{
						for (MethodDeclaration md : findBridgeMethods(td))
						{
							td.getMembers().remove(md);
						}
					}
				}
			}
		}

		return units.toArray(new CompilationUnit[units.size()]);
	}

	private List<MethodDeclaration> findBridgeMethods(TypeDeclaration td)
	{
		List<MethodDeclaration> bridges = new ArrayList<MethodDeclaration>();
		for (BodyDeclaration bd : td.getMembers())
		{
			if (bd instanceof MethodDeclaration)
			{
				MethodDeclaration md = (MethodDeclaration) bd;
				Type t = md.getType();
				if (t instanceof ReferenceType)
				{
					ReferenceType rt = (ReferenceType) t;
					if (rt.getArrayCount() == 0)
					{
						t = rt.getType();
					}
				}

				if (t instanceof ClassOrInterfaceType)
				{
					ClassOrInterfaceType coi = (ClassOrInterfaceType) t;
					String returnType = coi.getName();
					if ("Object".equals(returnType) || "java.lang.Object".equals(returnType))
					{
						for (BodyDeclaration bd2 : td.getMembers())
						{
							if (bd2 instanceof MethodDeclaration)
							{
								MethodDeclaration md2 = (MethodDeclaration) bd2;
								if (md2.getName().equals(md.getName()))
								{
									bridges.add(md);
									break;
								}
							}
						}
					}
				}
			}
		}
		return bridges;
	}

}
