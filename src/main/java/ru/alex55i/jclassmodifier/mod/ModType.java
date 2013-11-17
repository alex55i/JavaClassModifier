package ru.alex55i.jclassmodifier.mod;

import java.util.ArrayList;
import java.util.List;

import javassist.CannotCompileException;
import javassist.ClassMap;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtField.Initializer;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.StackMapTable;

public class ModType
{
	CtClass clazz;
	ModConstructor staticInitializer;
	List<ModConstructor> constructors = new ArrayList<ModConstructor>();
	List<ModMethod> methods = new ArrayList<ModMethod>();
	List<ModField> fields = new ArrayList<ModField>();

	public ModType(CtClass clazz)
	{
		this.clazz = clazz;
		try
		{
			staticInitializer = new ModConstructor(clazz.makeClassInitializer(), "{}");
		} catch (CannotCompileException e)
		{
			e.printStackTrace();
		}
	}

	public void addMethodDescriptors() throws CannotCompileException
	{
		for (ModConstructor c : constructors)
		{
			clazz.addConstructor(c.constructor);
		}
		for (ModMethod m : methods)
		{
			clazz.addMethod(m.method);
		}
	}

	public void addFieldsWithInit() throws CannotCompileException
	{
		for (ModField f : fields)
		{
			clazz.addField(f.field, f.initializer);
		}
	}

	public void addMethodsBody() throws CannotCompileException, NotFoundException
	{
		if (staticInitializer != null)
			staticInitializer.constructor.setBody(staticInitializer.body);

		for (ModMethod m : methods)
		{
			if (m.body != null && !Modifier.isAbstract(m.method.getModifiers()))
			{
				System.out.println("Setting body: " + m.getMethod().getName() + " " + m.body);
				m.method.setBody(m.body);
			}
		}
		for (ModConstructor c : constructors)
		{
			if (c.body != null)
			{
				c.constructor.setBody(c.body);
			}
		}
	}

	public void rebuildStackMaps() throws BadBytecode
	{
		for (ModMethod m : methods)
		{
			MethodInfo info = m.method.getMethodInfo();
			if (info.getCodeAttribute() != null)
			{
				info.getCodeAttribute().setAttribute((StackMapTable)null);
			}
			//info.rebuildStackMap(clazz.getClassPool());
		}
		for (ModConstructor c : constructors)
		{
			MethodInfo info = c.constructor.getMethodInfo();
			if (info.getCodeAttribute() != null)
			{
				info.getCodeAttribute().setAttribute((StackMapTable)null);
			}
			//info.rebuildStackMap(clazz.getClassPool());
		}
	}

	public void setClassInitializer(String body)
	{
		staticInitializer.body = body;
	}

	public CtMethod newMethod(CtClass returnType, String mname, CtClass[] parameters, String body)
	{
		CtMethod m = new CtMethod(returnType, mname, parameters, clazz);
		methods.add(new ModMethod(m, body));
		return m;
	}

	public CtConstructor newConstructor(CtClass[] parameters, String body)
	{
		CtConstructor c = new CtConstructor(parameters, clazz);
		constructors.add(new ModConstructor(c, body));
		return c;
	}

	public CtField newField(CtClass type, String name, Initializer init)
	{
		try
		{
			CtField f = new CtField(type, name, clazz);
			fields.add(new ModField(f, init));
			return f;
		} catch (CannotCompileException e)
		{
			throw new RuntimeException(e);
		}
	}

	public CtMethod copyMethod(CtMethod method)
	{
		try
		{
			CtMethod m = CtNewMethod.copy(method, clazz, null);
			methods.add(new ModMethod(m, null));
			return m;
		} catch (CannotCompileException e)
		{
			throw new RuntimeException(e);
		}
	}

	public CtConstructor copyConstructor(CtConstructor constructor)
	{
		try
		{
			CtConstructor c = CtNewConstructor.copy(constructor, clazz, null);
			constructors.add(new ModConstructor(c, null));
			return c;
		} catch (CannotCompileException e)
		{
			throw new RuntimeException(e);
		}
	}

	public ModConstructor getStaticInitializer()
	{
		return staticInitializer;
	}

	public List<ModConstructor> getConstructors()
	{
		return constructors;
	}

	public List<ModMethod> getMethods()
	{
		return methods;
	}

	public List<ModField> getFields()
	{
		return fields;
	}

	public CtClass getClazz()
	{
		return clazz;
	}
}
