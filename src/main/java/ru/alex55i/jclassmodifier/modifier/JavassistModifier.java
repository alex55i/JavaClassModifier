package ru.alex55i.jclassmodifier.modifier;

import static com.google.common.collect.ObjectArrays.concat;
import static javassist.bytecode.AccessFlag.ENUM;
import static javassist.bytecode.AccessFlag.FINAL;
import static javassist.bytecode.AccessFlag.PUBLIC;
import static javassist.bytecode.AccessFlag.STATIC;
import static javassist.bytecode.AccessFlag.VARARGS;
import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.Node;
import japa.parser.ast.PackageDeclaration;
import japa.parser.ast.body.AnnotationDeclaration;
import japa.parser.ast.body.AnnotationMemberDeclaration;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.EnumConstantDeclaration;
import japa.parser.ast.body.EnumDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.InitializerDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.expr.ArrayCreationExpr;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.ObjectCreationExpr;
import japa.parser.ast.stmt.BlockStmt;
import japa.parser.ast.stmt.ExplicitConstructorInvocationStmt;
import japa.parser.ast.stmt.Statement;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.visitor.ModifierVisitorAdapter;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtField.Initializer;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationDefaultAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import ru.alex55i.jclassmodifier.mod.JavaClassFile;
import ru.alex55i.jclassmodifier.mod.ModType;
import ru.alex55i.jclassmodifier.mod.UnitModifier;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class JavassistModifier extends ClassModifier
{
	public JavassistModifier(ClassPool classPool, CompilationUnit[] units)
	{
		super(classPool, units);
	}

	@Override
	public CtClass[] modify() throws NotFoundException, CannotCompileException
	{
		List<ModType> list = Lists.newArrayList();
		for (CompilationUnit unit : units)
		{
			try
			{
				// Should format the code
				unit = JavaParser.parse(new StringReader(unit.toString()));
			} catch (ParseException e)
			{
				throw new CannotCompileException(e);
			}

			UnitModifier modifier = new UnitModifier(classPool, unit);
			for (TypeDeclaration td : unit.getTypes())
			{
				ModType modType = typeDeclToMod(modifier, td);
				list.add(modType);
			}
		}

		for (ModType modType : list)
		{
			if (modType.getConstructors().size() == 0)
				modType.newConstructor(new CtClass[0], null);
			modType.addMethodDescriptors();
		}
		for (ModType modType : list)
		{
			modType.addFieldsWithInit();
		}
		for (ModType modType : list)
		{
			modType.addMethodsBody();
		}

		CtClass[] arr = new CtClass[list.size()];
		int i = 0;
		for (ModType modType : list)
		{
			arr[i++] = modType.getClazz();
		}
		return arr;
	}

	private ModType typeDeclToMod(UnitModifier unitMod, TypeDeclaration td) throws NotFoundException
	{
		CtClass stringClass = classPool.get("java.lang.String");
		String classname = td.getName();
		PackageDeclaration pkg = unitMod.getUnit().getPackage();
		if (pkg != null)
		{
			classname = pkg.getName().toString() + "." + classname;
		}

		CtClass origClazz = classPool.getOrNull(classname);

		String superclazzname = "java.lang.Object";
		List<ClassOrInterfaceType> impls = null;
		List<BodyDeclaration> members = null;
		if (td instanceof ClassOrInterfaceDeclaration)
		{
			ClassOrInterfaceDeclaration cd = (ClassOrInterfaceDeclaration) td;
			if (cd.getExtends() != null && cd.getExtends().size() > 0)
				superclazzname = cd.getExtends().get(0).getName();
			impls = cd.getImplements();
			members = cd.getMembers();
		}
		else if (td instanceof EnumDeclaration)
		{
			EnumDeclaration cd = (EnumDeclaration) td;
			superclazzname = "java.lang.Enum";
			impls = cd.getImplements();
			members = cd.getMembers();
		}
		else if (td instanceof AnnotationDeclaration)
		{
			AnnotationDeclaration cd = (AnnotationDeclaration) td;
			members = cd.getMembers();
		}
		CtClass superclazz = unitMod.findImportedClass(superclazzname);

		// Make class
		System.out.println("Making class " + classname);
		CtClass clazz = classPool.makeClass(classname, superclazz);
		clazz.setModifiers(td.getModifiers());
		ModType modType = new ModType(clazz);

		ClassFile clazzFile = clazz.getClassFile();
		ConstPool clazzConstPool = clazzFile.getConstPool();
		clazzFile.addAttribute(unitMod.makeAnnotationAttribute(clazzConstPool, td));

		if (td instanceof EnumDeclaration)
		{
			clazz.setModifiers(clazz.getModifiers() | ENUM | FINAL);
			EnumDeclaration ed = (EnumDeclaration) td;
			List<EnumConstantDeclaration> entries = ed.getEntries();
			String clazzArray = classname + "[]";
			CtClass ctClazzArray = unitMod.findImportedClass(clazzArray);
			List<String> enumFields = Lists.newArrayList();
			int j = 0;
			for (EnumConstantDeclaration ecd : entries)
			{
				enumFields.add(ecd.getName());

				String[] stringParams = new String[ecd.getArgs() == null ? 2 : (ecd.getArgs().size() + 2)];
				int i = 0;
				stringParams[i++] = "\"" + ecd.getName() + "\"";
				stringParams[i++] = Integer.toString(j);
				if (ecd.getArgs() != null)
				{
					for (Expression stmt : ecd.getArgs())
					{
						stringParams[i++] = stmt.toString();
					}
				}
				Initializer init = Initializer.byExpr("new " + classname + "(" + Joiner.on(',').join(stringParams) + ");");
				CtField fd = modType.newField(clazz, ecd.getName(), init);
				fd.setModifiers(PUBLIC | STATIC | FINAL | ENUM);
				fd.getFieldInfo().addAttribute(unitMod.makeAnnotationAttribute(clazzConstPool, ecd));
				j++;
			}
			Initializer init = Initializer.byExpr("new " + clazzArray + " {" + Joiner.on(',').join(enumFields) + "};");
			CtField fd = modType.newField(ctClazzArray, "ENUM$VALUES", init);
			fd.setModifiers(Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL);

			String body = "{return (" + clazzArray + ")ENUM$VALUES.clone();}";
			CtMethod valuesMtd = modType.newMethod(ctClazzArray, "values", new CtClass[0], body);
			valuesMtd.setModifiers(PUBLIC | STATIC);

			String body1 = "{return (" + clazz.getName() + ")Enum.valueOf(" + clazz.getName() + ".class, $1);}";
			CtMethod valueOfMtd = modType.newMethod(clazz, "valueOf", new CtClass[] { stringClass }, body1);
			valueOfMtd.setModifiers(PUBLIC | STATIC);
		}
		else if (td instanceof AnnotationDeclaration)
		{
			clazz.setModifiers(clazz.getModifiers() | Modifier.ABSTRACT | Modifier.INTERFACE | Modifier.ANNOTATION);
		}

		// Add interfaces
		if (impls != null && impls.size() > 0)
		{
			CtClass[] infs = new CtClass[impls.size()];
			int i = 0;
			for (ClassOrInterfaceType t : impls)
			{
				infs[i++] = unitMod.findImportedClass(t.getName());
			}
			clazz.setInterfaces(infs);
		}

		// Add members
		if (members != null)
		{
			for (BodyDeclaration d : members)
			{
				if (d instanceof InitializerDeclaration)
				{
					InitializerDeclaration id = (InitializerDeclaration) d;
					if (id.isStatic())
					{
						BlockStmt block = id.getBlock();
						unitMod.new ClassNameResolveVisitor().visit(block, null);
						modType.setClassInitializer(block.toString());
					}
				}
				else if (d instanceof AnnotationMemberDeclaration)
				{
					AnnotationMemberDeclaration amd = (AnnotationMemberDeclaration) d;
					CtMethod mtd = modType.newMethod(unitMod.getCtClassFromType(amd.getType()), amd.getName(), unitMod.getParameterTypes(amd), null);
					mtd.setModifiers(amd.getModifiers() | Modifier.PUBLIC | Modifier.ABSTRACT | Modifier.ANNOTATION);
					MethodInfo minfo = mtd.getMethodInfo();
					AnnotationDefaultAttribute ada = new AnnotationDefaultAttribute(clazzConstPool);
					ada.setDefaultValue(unitMod.objectToMemberValue(clazzConstPool, unitMod.exprToObject(amd.getDefaultValue())));
					minfo.addAttribute(ada);
				}
				else if (d instanceof FieldDeclaration)
				{
					FieldDeclaration fd = (FieldDeclaration) d;
					CtClass type = unitMod.getCtClassFromType(fd.getType());
					List<VariableDeclarator> vars = fd.getVariables();
					for (VariableDeclarator vard : vars)
					{
						Expression init = vard.getInit();
						Initializer iinit = null;
						if (init != null)
						{
							if (init instanceof ObjectCreationExpr)
							{
								unitMod.new ClassNameResolveVisitor().visit((ObjectCreationExpr) init, null);
							}
							else if (init instanceof ArrayCreationExpr)
							{
								unitMod.new ClassNameResolveVisitor().visit((ArrayCreationExpr) init, null);
							}
							iinit = Initializer.byExpr(init.toString());
						}

						String fname = vard.getId().getName();
						CtField newFld = modType.newField(type, fname, iinit);
						newFld.setModifiers(fd.getModifiers());
						newFld.getFieldInfo().addAttribute(unitMod.makeAnnotationAttribute(clazzConstPool, d));
					}
				}
				else if ((d instanceof ConstructorDeclaration) || (d instanceof MethodDeclaration))
				{
					boolean needRecompile = true;
					boolean isConstructor = (d instanceof ConstructorDeclaration);
					int mdModifers = 0;
					String mdName = null;
					String mdDescr = null;
					String mdType = null;
					CtClass[] paramz = unitMod.getParameterTypes(d);
					CtClass[] throwz = unitMod.getThrows(d);

					if (isConstructor)
					{
						ConstructorDeclaration cd = (ConstructorDeclaration) d;
						// Always recompile constructors
						needRecompile = true;

						mdModifers = cd.getModifiers();
						mdName = cd.getName();
						mdDescr = unitMod.getJVMDescr(cd);
						mdType = "void";
					}
					else
					{
						MethodDeclaration md = (MethodDeclaration) d;
						mdModifers = md.getModifiers();
						mdName = md.getName();
						mdDescr = unitMod.getJVMDescr(md);
						mdType = md.getType().toString();

						if (origClazz != null)
						{
							try
							{
								origClazz.getDeclaredMethod(mdName, paramz);
								if (md.getComment() != null && md.getComment().getContent().trim().equals(JavaClassFile.hash(md)))
								{
									needRecompile = false;
								}
							} catch (NotFoundException e)
							{}
						}
					}

					if (needRecompile)
					{

						CtClass returnType = unitMod.findImportedClass(mdType);

						// Replace all parameters
						unitMod.replaceParameters(d);
						// Resolve classnames
						unitMod.new ClassNameResolveVisitor().visit(d);
						System.out.println("Method modifed: " + mdName + " " + mdDescr);

						// Add the new constructor/method
						if (isConstructor)
						{
							BlockStmt bodyBlock = ((ConstructorDeclaration) d).getBlock();
							if (td instanceof EnumDeclaration)
							{
								List<Statement> stmts = bodyBlock.getStmts();
								if (stmts != null && stmts.size() > 0)
								{
									Statement stmt = stmts.get(0);
									if (stmt instanceof ExplicitConstructorInvocationStmt)
									{
										ExplicitConstructorInvocationStmt est = (ExplicitConstructorInvocationStmt) stmt;
										System.out.println("Removing " + est);
										stmts.remove(stmt);
									}
								}
								// Increment all param references by 2
								final Pattern patt = Pattern.compile("\\$(\\d*)");
								new ModifierVisitorAdapter<Void>()
								{
									public Node visit(NameExpr n, Void arg)
									{
										Matcher matcher = patt.matcher(n.getName());
										if (matcher.matches())
										{
											int i = Integer.parseInt(matcher.group(1)) + 2;
											n.setName("$" + i);
										}
										return n;
									};
								}.visit(bodyBlock, null);
								List<Expression> args = new ArrayList<Expression>();
								args.add(new NameExpr("$1"));
								args.add(new NameExpr("$2"));

								if (stmts == null)
									stmts = new ArrayList<Statement>();
								stmts.add(0, new ExplicitConstructorInvocationStmt(false, null, args));
								bodyBlock.setStmts(stmts);
								// Insert before parameters
								paramz = concat(stringClass, concat(CtClass.intType, paramz));
							}
							CtConstructor newConstr = modType.newConstructor(paramz, bodyBlock.toString());
							newConstr.setExceptionTypes(throwz);
							newConstr.setModifiers(mdModifers);
							MethodInfo mi = newConstr.getMethodInfo();
							mi.addAttribute(unitMod.makeParameterAnnotations(clazzConstPool, d));
							mi.addAttribute(unitMod.makeAnnotationAttribute(clazzConstPool, d));
						}
						else
						{
							MethodDeclaration md = ((MethodDeclaration) d);
							String mdBody = null;
							if (!Modifier.isAbstract(mdModifers))
							{
								BlockStmt body = md.getBody();
								mdBody = body != null ? body.toString() : null;
							}
							CtMethod newMtd = modType.newMethod(returnType, mdName, paramz, mdBody);
							newMtd.setExceptionTypes(throwz);

							// Add VARARGS modifier
							if (unitMod.isVarargs(md))
								newMtd.setModifiers(mdModifers | VARARGS);
							else
								newMtd.setModifiers(mdModifers);

							MethodInfo mi = newMtd.getMethodInfo();
							//TODO: Add LocalVariableAttribute
							mi.addAttribute(unitMod.makeParameterAnnotations(clazzConstPool, d));
							mi.addAttribute(unitMod.makeAnnotationAttribute(clazzConstPool, d));
						}
					}
					else
					{
						if (isConstructor)
						{
							CtConstructor origConstr = origClazz.getDeclaredConstructor(paramz);
							CtConstructor newConstr = modType.copyConstructor(origConstr);
							newConstr.setModifiers(mdModifers);
							MethodInfo mi = newConstr.getMethodInfo();
							mi.addAttribute(unitMod.makeParameterAnnotations(clazzConstPool, d));
							mi.addAttribute(unitMod.makeAnnotationAttribute(clazzConstPool, d));
						}
						else
						{
							CtMethod origMtd = origClazz.getDeclaredMethod(mdName, paramz);
							CtMethod newMtd = modType.copyMethod(origMtd);
							newMtd.setModifiers(mdModifers);
							MethodInfo mi = newMtd.getMethodInfo();
							mi.addAttribute(unitMod.makeParameterAnnotations(clazzConstPool, d));
							mi.addAttribute(unitMod.makeAnnotationAttribute(clazzConstPool, d));
						}
					}
				}
			}
		}
		return modType;
	}
}
