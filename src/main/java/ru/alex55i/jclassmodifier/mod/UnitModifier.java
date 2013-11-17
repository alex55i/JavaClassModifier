package ru.alex55i.jclassmodifier.mod;

import japa.parser.ASTHelper;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.Node;
import japa.parser.ast.body.AnnotationDeclaration;
import japa.parser.ast.body.AnnotationMemberDeclaration;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.EmptyMemberDeclaration;
import japa.parser.ast.body.EmptyTypeDeclaration;
import japa.parser.ast.body.EnumConstantDeclaration;
import japa.parser.ast.body.EnumDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.InitializerDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.expr.ArrayInitializerExpr;
import japa.parser.ast.expr.BinaryExpr;
import japa.parser.ast.expr.BooleanLiteralExpr;
import japa.parser.ast.expr.CastExpr;
import japa.parser.ast.expr.CharLiteralExpr;
import japa.parser.ast.expr.ClassExpr;
import japa.parser.ast.expr.DoubleLiteralExpr;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.FieldAccessExpr;
import japa.parser.ast.expr.IntegerLiteralExpr;
import japa.parser.ast.expr.LongLiteralExpr;
import japa.parser.ast.expr.MemberValuePair;
import japa.parser.ast.expr.MethodCallExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.NormalAnnotationExpr;
import japa.parser.ast.expr.SingleMemberAnnotationExpr;
import japa.parser.ast.expr.StringLiteralExpr;
import japa.parser.ast.expr.UnaryExpr;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.PrimitiveType;
import japa.parser.ast.type.ReferenceType;
import japa.parser.ast.type.Type;
import japa.parser.ast.visitor.ModifierVisitorAdapter;

import java.util.ArrayList;
import java.util.List;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.ParameterAnnotationsAttribute;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.BooleanMemberValue;
import javassist.bytecode.annotation.ByteMemberValue;
import javassist.bytecode.annotation.CharMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;
import javassist.bytecode.annotation.DoubleMemberValue;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.bytecode.annotation.LongMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.ShortMemberValue;
import javassist.bytecode.annotation.StringMemberValue;

import org.apache.commons.lang.StringUtils;

public class UnitModifier
{
	ClassPool classPool;
	CompilationUnit unit;

	public UnitModifier(ClassPool pool, CompilationUnit unit)
	{
		classPool = pool;
		this.unit = unit;
	}

	public static class BodyModifierVisitorAdapter extends ModifierVisitorAdapter<Void>
	{
		public Node visit(BodyDeclaration n)
		{
			if (n instanceof MethodDeclaration)
			{
				return visit((MethodDeclaration) n, null);
			}
			else if (n instanceof FieldDeclaration)
			{
				return visit((FieldDeclaration) n, null);
			}
			else if (n instanceof ConstructorDeclaration)
			{
				return visit((ConstructorDeclaration) n, null);
			}
			else if (n instanceof InitializerDeclaration)
			{
				return visit((InitializerDeclaration) n, null);
			}
			else if (n instanceof EnumConstantDeclaration)
			{
				return visit((EnumConstantDeclaration) n, null);
			}
			else if (n instanceof EmptyMemberDeclaration)
			{
				return visit((EmptyMemberDeclaration) n, null);
			}
			else if (n instanceof AnnotationMemberDeclaration)
			{
				return visit((AnnotationMemberDeclaration) n, null);
			}
			else if (n instanceof ClassOrInterfaceDeclaration)
			{
				return visit((ClassOrInterfaceDeclaration) n, null);
			}
			else if (n instanceof EnumDeclaration)
			{
				return visit((EnumDeclaration) n, null);
			}
			else if (n instanceof AnnotationDeclaration)
			{
				return visit((AnnotationDeclaration) n, null);
			}
			else if (n instanceof EmptyTypeDeclaration)
			{
				return visit((EmptyTypeDeclaration) n, null);
			}
			return null;
		}
	}

	public class ClassNameResolveVisitor extends BodyModifierVisitorAdapter
	{
		public Node visit(ClassOrInterfaceType n, Void arg)
		{
			n.setName(getFullClassName(n.getName()));
			return super.visit(n, arg);
		};

		public Node visit(MethodCallExpr n, Void arg)
		{
			Expression scope = n.getScope();
			if (scope instanceof NameExpr)
			{
				NameExpr ne = (NameExpr) scope;
				ne.setName(getFullClassName(ne.getName()));
			}
			return super.visit(n, arg);
		};

		@Override
		public Node visit(FieldAccessExpr n, Void arg)
		{
			Expression scope = n.getScope();
			if (scope instanceof NameExpr)
			{
				NameExpr ne = (NameExpr) scope;
				ne.setName(getFullClassName(ne.getName()));
			}
			return super.visit(n, arg);
		}
	}

	public Object exprToObject(Expression expr)
	{
		if (expr instanceof IntegerLiteralExpr)
		{
			return Integer.valueOf(((IntegerLiteralExpr) expr).getValue());
		}
		else if (expr instanceof BooleanLiteralExpr)
		{
			return ((BooleanLiteralExpr) expr).getValue();
		}
		else if (expr instanceof DoubleLiteralExpr)
		{
			String l = ((DoubleLiteralExpr) expr).getValue();
			if (l.endsWith("F") || l.endsWith("f"))
			{
				return Float.valueOf(l.substring(0, l.length() - 1));
			}
			else
			{
				if (l.endsWith("d") || l.endsWith("D"))
					l = l.substring(0, l.length() - 1);

				return Double.valueOf(l);
			}
		}
		else if (expr instanceof LongLiteralExpr)
		{
			String l = ((LongLiteralExpr) expr).getValue();
			if (l.endsWith("L") || l.endsWith("l"))
				l = l.substring(0, l.length() - 1);

			return Long.valueOf(l);
		}
		else if (expr instanceof CharLiteralExpr)
		{
			return ((CharLiteralExpr) expr).getValue().charAt(0);
		}
		else if (expr instanceof StringLiteralExpr)
		{
			return ((StringLiteralExpr) expr).getValue();
		}
		else if (expr instanceof ClassExpr)
		{
			return new ClassMember(getFullClassName(((ClassExpr) expr).getType().toString()));
		}
		else if (expr instanceof CastExpr)
		{
			CastExpr ce = (CastExpr) expr;
			Object o = exprToObject(ce.getExpr());
			Type t = ce.getType();

			if (t instanceof PrimitiveType)
			{
				return convert(o, ((PrimitiveType) t).getType());
			}
		}
		else if (expr instanceof FieldAccessExpr)
		{
			FieldAccessExpr fe = (FieldAccessExpr) expr;
			Expression scope = fe.getScope();
			String accClazz = getFullClassName(scope.toString());
			String value = fe.getField();
			return new EnumMember(accClazz, value);
		}
		else if (expr instanceof ArrayInitializerExpr)
		{
			ArrayInitializerExpr ae = (ArrayInitializerExpr) expr;
			List<Object> elements = new ArrayList<Object>();
			for (Expression val : ae.getValues())
			{
				elements.add(exprToObject(val));
			}
			return elements.toArray();
		}
		else if (expr instanceof UnaryExpr)
		{
			UnaryExpr ue = (UnaryExpr) expr;
			UnaryExpr.Operator op = ue.getOperator();
			Object val = exprToObject(ue.getExpr());
			return applyUnaryOperator(val, op);
		}
		else if (expr instanceof BinaryExpr)
		{
			BinaryExpr be = (BinaryExpr) expr;
			BinaryExpr.Operator op = be.getOperator();
			Object val1 = exprToObject(be.getLeft());
			Object val2 = exprToObject(be.getRight());
			if (val1 instanceof Boolean)
				return applyBinaryBooleanOperator((Boolean) val1, (Boolean) val2, op);
			else
			{
				if (val1 instanceof Number)
				{
					if (val2 instanceof Number)
						return applyBinaryNumberOperator((Number) val1, (Number) val2, op);
					else if (val2 instanceof Character)
						return applyBinaryNumberOperator((Number) val1, Integer.valueOf(((Character) val2).charValue()), op);
				}
				else if (val1 instanceof Character)
				{
					Integer c = Integer.valueOf(((Character) val1).charValue());
					if (val2 instanceof Number)
						return applyBinaryNumberOperator(c, (Number) val2, op);
					else if (val2 instanceof Character)
						return applyBinaryNumberOperator(c, Integer.valueOf(((Character) val2).charValue()), op);
				}
			}
		}
		throw new RuntimeException("UNKNOWN EXPR: " + expr.getClass().getName() + " " + expr);
	}

	public MemberValue objectToMemberValue(ConstPool cp, Object o)
	{
		if (o instanceof Integer)
			return new IntegerMemberValue(cp, (Integer) o);
		else if (o instanceof Character)
			return new CharMemberValue((Character) o, cp);
		else if (o instanceof Byte)
			return new ByteMemberValue((Byte) o, cp);
		else if (o instanceof Short)
			return new ShortMemberValue((Short) o, cp);
		else if (o instanceof Long)
			return new LongMemberValue((Long) o, cp);
		else if (o instanceof Float)
			return new DoubleMemberValue((Float) o, cp);
		else if (o instanceof Double)
			return new DoubleMemberValue((Double) o, cp);
		else if (o instanceof Boolean)
			return new BooleanMemberValue((Boolean) o, cp);
		else if (o instanceof String)
			return new StringMemberValue((String) o, cp);
		else if (o instanceof ClassMember)
			return new ClassMemberValue(((ClassMember) o).getClassname(), cp);
		else if (o instanceof Object[])
		{
			Object[] arr = (Object[]) o;
			ArrayMemberValue mv = new ArrayMemberValue(cp);
			MemberValue[] elements = new MemberValue[arr.length];
			for (int i = 0; i < arr.length; i++)
			{
				elements[i] = objectToMemberValue(cp, arr[i]);
			}
			mv.setValue(elements);
			return mv;
		}
		else if (o instanceof EnumMember)
		{
			EnumMemberValue mv = new EnumMemberValue(cp);
			EnumMember em = (EnumMember) o;
			mv.setType(em.getType());
			mv.setValue(em.getValue());
			return mv;
		}
		throw new RuntimeException("UNKNOWN TYPE: " + o.getClass().getName() + " " + o);
	}

	public Object convert(Object o, PrimitiveType.Primitive type)
	{
		if (o instanceof Character)
			o = Integer.valueOf(((Character) o).charValue());

		if (o instanceof Number)
		{
			Number n = (Number) o;
			switch (type)
			{
				case Byte:
					return n.byteValue();
				case Char:
					return (char) n.intValue();
				case Int:
					return n.intValue();
				case Long:
					return n.longValue();
				case Short:
					return n.shortValue();
				case Double:
					return n.doubleValue();
				case Float:
					return n.floatValue();
			}
		}
		throw new RuntimeException("UNKNOWN CAST TYPE");
	}

	private Object applyUnaryOperator(Object val, UnaryExpr.Operator op)
	{
		System.err.println("UNARY OP: " + val.getClass().getName() + " " + op + " " + val);
		switch (op)
		{
			case not:
				if (val instanceof Boolean)
					return !((Boolean) val);
			case inverse:
				if (val instanceof Byte)
					return -((Byte) val);
				if (val instanceof Short)
					return -((Short) val);
				if (val instanceof Integer)
					return -((Integer) val);
				if (val instanceof Character)
					return -((Character) val);
				if (val instanceof Long)
					return -((Long) val);
				if (val instanceof Float)
					return -((Float) val);
				if (val instanceof Double)
					return -((Double) val);
			default:
				break;
		}
		throw new RuntimeException("UNKNOWN MEMBER TYPE: " + val.getClass().getName() + " " + val.toString());
	}

	public Boolean applyBinaryBooleanOperator(Boolean v1, Boolean v2, BinaryExpr.Operator op)
	{
		System.err.println("BINARY BOOL OP: " + v1.getClass().getName() + " " + v1 + " " + op + " " + v2.getClass().getName() + " " + v2);
		switch (op)
		{
			case or:
				return v1 || v2;
			case and:
				return v1 && v2;
			case equals:
				return v1 == v2;
			case notEquals:
				return v1 != v2;
		}
		return false;
	}

	public Object applyBinaryNumberOperator(Number v1, Number v2, BinaryExpr.Operator op)
	{
		System.err.println("BINARY NUMB OP: " + v1.getClass().getName() + " " + v1 + " " + op + " " + v2.getClass().getName() + " " + v2);
		switch (op)
		{
			case binOr:
				if (v2 instanceof Integer)
					return v1.intValue() | v2.intValue();
				if (v2 instanceof Short)
					return v1.shortValue() | v2.shortValue();
				if (v2 instanceof Long)
					return v1.longValue() | v2.longValue();
				if (v2 instanceof Byte)
					return v1.byteValue() | v2.byteValue();
				break;
			case binAnd:
				if (v2 instanceof Integer)
					return v1.intValue() & v2.intValue();
				if (v2 instanceof Short)
					return v1.shortValue() & v2.shortValue();
				if (v2 instanceof Long)
					return v1.longValue() & v2.longValue();
				if (v2 instanceof Byte)
					return v1.byteValue() & v2.byteValue();
				break;
			case equals:
				return v1.equals(v2);
			case notEquals:
				return !v1.equals(v2);
			case less:
				return compare(v1, v2) < 0;
			case greater:
				return compare(v1, v2) > 0;
			case lessEquals:
				return compare(v1, v2) <= 0;
			case greaterEquals:
				return compare(v1, v2) >= 0;
			case lShift:
				if (v2 instanceof Integer)
					return v1.intValue() << v2.intValue();
				if (v2 instanceof Short)
					return v1.shortValue() << v2.shortValue();
				if (v2 instanceof Long)
					return v1.longValue() << v2.longValue();
				if (v2 instanceof Byte)
					return v1.byteValue() << v2.byteValue();
				break;
			case rUnsignedShift:
				if (v2 instanceof Integer)
					return v1.intValue() >> v2.intValue();
				if (v2 instanceof Short)
					return v1.shortValue() >> v2.shortValue();
				if (v2 instanceof Long)
					return v1.longValue() >> v2.longValue();
				if (v2 instanceof Byte)
					return v1.byteValue() >> v2.byteValue();
				break;
			case rSignedShift:
				if (v2 instanceof Integer)
					return v1.intValue() >>> v2.intValue();
				if (v2 instanceof Short)
					return v1.shortValue() >>> v2.shortValue();
				if (v2 instanceof Long)
					return v1.longValue() >>> v2.longValue();
				if (v2 instanceof Byte)
					return v1.byteValue() >>> v2.byteValue();
				break;
			case plus:
				if (v2 instanceof Integer)
					return v1.intValue() + v2.intValue();
				if (v2 instanceof Short)
					return v1.shortValue() + v2.shortValue();
				if (v2 instanceof Long)
					return v1.longValue() + v2.longValue();
				if (v2 instanceof Byte)
					return v1.byteValue() + v2.byteValue();
				break;
			case minus:
				if (v2 instanceof Integer)
					return v1.intValue() - v2.intValue();
				if (v2 instanceof Short)
					return v1.shortValue() - v2.shortValue();
				if (v2 instanceof Long)
					return v1.longValue() - v2.longValue();
				if (v2 instanceof Byte)
					return v1.byteValue() - v2.byteValue();
				if (v2 instanceof Float)
					return v1.floatValue() - v2.floatValue();
				if (v2 instanceof Double)
					return v1.floatValue() - v2.floatValue();
				break;
			case times:
				if (v2 instanceof Integer)
					return v1.intValue() * v2.intValue();
				if (v2 instanceof Short)
					return v1.shortValue() * v2.shortValue();
				if (v2 instanceof Long)
					return v1.longValue() * v2.longValue();
				if (v2 instanceof Byte)
					return v1.byteValue() * v2.byteValue();
				if (v2 instanceof Float)
					return v1.floatValue() * v2.floatValue();
				if (v2 instanceof Double)
					return v1.floatValue() * v2.floatValue();
				break;
			case divide:
				if (v2 instanceof Integer)
					return v1.intValue() / v2.intValue();
				if (v2 instanceof Short)
					return v1.shortValue() / v2.shortValue();
				if (v2 instanceof Long)
					return v1.longValue() / v2.longValue();
				if (v2 instanceof Byte)
					return v1.byteValue() / v2.byteValue();
				if (v2 instanceof Float)
					return v1.floatValue() / v2.floatValue();
				if (v2 instanceof Double)
					return v1.floatValue() / v2.floatValue();
				break;
			case remainder:
				if (v2 instanceof Integer)
					return v1.intValue() % v2.intValue();
				if (v2 instanceof Short)
					return v1.shortValue() % v2.shortValue();
				if (v2 instanceof Long)
					return v1.longValue() % v2.longValue();
				if (v2 instanceof Byte)
					return v1.byteValue() % v2.byteValue();
				if (v2 instanceof Float)
					return v1.floatValue() % v2.floatValue();
				if (v2 instanceof Double)
					return v1.floatValue() % v2.floatValue();
				break;
		}
		throw new RuntimeException("UNKNOWN VAL1 TYPE: " + v1.getClass().getName() + " " + v1.toString() + "  op: " + op + " val2: " + v2.getClass().getName() + " " + v2);
	}

	public int compare(Number val1, Number val2)
	{
		if (val1 instanceof Long || val2 instanceof Long)
		{
			return longCompare(val1.longValue(), val2.longValue());
		}
		else if (val1 instanceof Double || val2 instanceof Double)
		{
			return Double.compare(val1.doubleValue(), val2.doubleValue());
		}
		else if (val1 instanceof Float || val2 instanceof Float)
		{
			return Float.compare(val1.floatValue(), val2.floatValue());
		}
		else
		{
			return intCompare(val1.intValue(), val2.intValue());
		}
	}

	public static int longCompare(long x, long y)
	{
		return (x < y) ? -1 : ((x == y) ? 0 : 1);
	}

	public static int intCompare(int x, int y)
	{
		return (x < y) ? -1 : ((x == y) ? 0 : 1);
	}

	public void addAnnotationAttributes(MethodInfo info, ConstPool constpool, BodyDeclaration d) throws NotFoundException
	{
		AnnotationsAttribute annsAttr = makeAnnotationAttribute(constpool, d);
		if (annsAttr != null)
			info.addAttribute(annsAttr);

		ParameterAnnotationsAttribute paramAnnsAttr = makeParameterAnnotationsAttribute(constpool, d);
		if (paramAnnsAttr != null)
			info.addAttribute(paramAnnsAttr);
	}

	public void addAnnotationAttributes(FieldInfo info, ConstPool constpool, BodyDeclaration d) throws NotFoundException
	{
		AnnotationsAttribute annsAttr = makeAnnotationAttribute(constpool, d);
		if (annsAttr != null)
			info.addAttribute(annsAttr);
	}
	
	public void addAnnotationAttributes(ClassFile classFile, ConstPool constpool, BodyDeclaration d) throws NotFoundException
	{
		AnnotationsAttribute annsAttr = makeAnnotationAttribute(constpool, d);
		if (annsAttr != null)
			classFile.addAttribute(annsAttr);
	}

	private ParameterAnnotationsAttribute makeParameterAnnotationsAttribute(ConstPool constpool, BodyDeclaration d) throws NotFoundException
	{
		List<Parameter> mdParams = null;
		if (d instanceof MethodDeclaration)
		{
			mdParams = ((MethodDeclaration) d).getParameters();
		}
		else if (d instanceof ConstructorDeclaration)
		{
			mdParams = ((ConstructorDeclaration) d).getParameters();
		}
		if (mdParams != null)
		{
			ParameterAnnotationsAttribute attr = new ParameterAnnotationsAttribute(constpool, ParameterAnnotationsAttribute.visibleTag);
			Annotation[][] javassistAnns = new Annotation[mdParams.size()][];
			int i = 0;
			// Counter for params without annotations
			int annotationless = 0;
			for (Parameter param : mdParams)
			{
				if (param.getAnnotations() != null)
				{
					List<Annotation> annotations = makeAnnotations(constpool, param.getAnnotations());
					javassistAnns[i] = new Annotation[annotations.size()];
					int j = 0;
					for (Annotation ann : annotations)
					{
						javassistAnns[i][j] = ann;
						j++;
					}
				}
				else
				{
					javassistAnns[i] = new Annotation[0];
					annotationless++;
				}
				i++;
			}
			if (mdParams.size() == annotationless)
				return null;

			attr.setAnnotations(javassistAnns);
			return attr;
		}
		return null;
	}

	private AnnotationsAttribute makeAnnotationAttribute(ConstPool constpool, BodyDeclaration td)
	{
		AnnotationsAttribute attr = null;
		if (td.getAnnotations() != null)
		{
			attr = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
			List<Annotation> annotations = makeAnnotations(constpool, td.getAnnotations());
			for (Annotation ann : annotations)
			{
				attr.addAnnotation(ann);
			}
		}
		return attr;
	}

	public List<Annotation> makeAnnotations(ConstPool constpool, List<AnnotationExpr> annotationExprs)
	{
		List<Annotation> annotations = new ArrayList<Annotation>();
		for (AnnotationExpr annotation : annotationExprs)
		{
			Annotation ann = new Annotation(getFullClassName(annotation.getName().toString()), constpool);
			if (annotation instanceof NormalAnnotationExpr)
			{
				NormalAnnotationExpr expr = (NormalAnnotationExpr) annotation;
				for (MemberValuePair pair : expr.getPairs())
				{
					Expression value = pair.getValue();
					MemberValue memberValue = objectToMemberValue(constpool, exprToObject(value));
					if (memberValue != null)
					{
						ann.addMemberValue(pair.getName(), memberValue);
					}
				}
			}
			else if (annotation instanceof SingleMemberAnnotationExpr)
			{
				SingleMemberAnnotationExpr expr = (SingleMemberAnnotationExpr) annotation;
				Expression value = expr.getMemberValue();
				MemberValue memberValue = objectToMemberValue(constpool, exprToObject(value));
				if (memberValue != null)
				{
					ann.addMemberValue("value", memberValue);
				}
			}
			annotations.add(ann);
		}
		return annotations;
	}

	public CtClass[] getThrows(BodyDeclaration d) throws NotFoundException
	{
		List<NameExpr> mdThrows = null;
		if (d instanceof MethodDeclaration)
		{
			mdThrows = ((MethodDeclaration) d).getThrows();
		}
		else if (d instanceof ConstructorDeclaration)
		{
			mdThrows = ((ConstructorDeclaration) d).getThrows();
		}
		CtClass[] throwz;
		if (mdThrows != null)
		{
			throwz = new CtClass[mdThrows.size()];
			int i = 0;
			for (NameExpr p : mdThrows)
			{
				throwz[i] = findImportedClass(p.getName());
				i++;
			}
		}
		else
		{
			throwz = new CtClass[0];
		}
		return throwz;
	}

	public CtClass[] getParameterTypes(BodyDeclaration d) throws NotFoundException
	{
		List<Parameter> mdParams = null;
		if (d instanceof MethodDeclaration)
		{
			mdParams = ((MethodDeclaration) d).getParameters();
		}
		else if (d instanceof ConstructorDeclaration)
		{
			mdParams = ((ConstructorDeclaration) d).getParameters();
		}
		CtClass[] paramz;
		if (mdParams != null)
		{
			paramz = new CtClass[mdParams.size()];
			int i = 0;
			for (Parameter p : mdParams)
			{
				paramz[i++] = getParameterType(p);
			}
		}
		else
		{
			paramz = new CtClass[0];
		}
		return paramz;
	}

	public boolean isVarargs(MethodDeclaration md)
	{
		List<Parameter> params = md.getParameters();
		if (params != null)
		{
			return params.get(params.size() - 1).isVarArgs();
		}
		return false;
	}

	public void replaceParameters(BodyDeclaration d)
	{
		List<Parameter> mdParams = null;
		if (d instanceof MethodDeclaration)
		{
			mdParams = ((MethodDeclaration) d).getParameters();
		}
		else if (d instanceof ConstructorDeclaration)
		{
			mdParams = ((ConstructorDeclaration) d).getParameters();
		}
		if (mdParams != null)
		{
			int i = 0;
			final String[] names = new String[mdParams.size()];
			for (Parameter p : mdParams)
			{
				names[i] = p.getId().getName();
				p.getId().setName("$" + (i + 1));
				i++;
			}
			new BodyModifierVisitorAdapter()
			{
				@Override
				public Node visit(NameExpr n, Void arg)
				{
					for (int i = 0; i < names.length; i++)
					{
						String name = names[i];
						if (name.equals(n.getName()))
						{
							n.setName("$" + (i + 1));
						}
					}
					return super.visit(n, arg);
				}

			}.visit(d);
		}
	}

	public String getFullClassName(String name)
	{
		try
		{
			CtClass clazz = findImportedClass(name);
			return clazz.getName();
		} catch (NotFoundException e)
		{}
		return name;
	}

	public String getJVMDescr(MethodDeclaration md) throws NotFoundException
	{
		StringBuffer buf = new StringBuffer();
		buf.append('(');
		List<Parameter> params = md.getParameters();
		if (params != null)
		{
			for (Parameter p : params)
			{
				buf.append(getJVMType(p.getType()));
			}
		}
		buf.append(')');
		buf.append(getJVMType(md.getType()));
		return buf.toString();
	}

	public String getJVMDescr(ConstructorDeclaration cd) throws NotFoundException
	{
		StringBuffer buf = new StringBuffer();
		buf.append('(');
		List<Parameter> params = cd.getParameters();
		if (params != null)
		{
			for (Parameter p : params)
			{
				buf.append(getJVMType(p.getType()));
			}
		}
		buf.append(')');
		buf.append('V');
		return buf.toString();
	}

	public String getJVMType(Type t) throws NotFoundException
	{
		return getJVMType(t, new StringBuffer());
	}

	public String getJVMType(Type t, StringBuffer buffer) throws NotFoundException
	{
		if (t instanceof ClassOrInterfaceType)
		{
			ClassOrInterfaceType cit = (ClassOrInterfaceType) t;
			buffer.append("L" + findImportedClass(cit.getName()).getName().replace('.', '/') + ";");
		}
		else if (t instanceof ReferenceType)
		{
			ReferenceType rt = (ReferenceType) t;
			for (int i = 0; i < rt.getArrayCount(); i++)
				buffer.append('[');
			getJVMType(rt.getType(), buffer);
		}
		else if (ASTHelper.BYTE_TYPE.equals(t))
			buffer.append("B");
		else if (ASTHelper.SHORT_TYPE.equals(t))
			buffer.append("S");
		else if (ASTHelper.INT_TYPE.equals(t))
			buffer.append("I");
		else if (ASTHelper.LONG_TYPE.equals(t))
			buffer.append("J");
		else if (ASTHelper.FLOAT_TYPE.equals(t))
			buffer.append("F");
		else if (ASTHelper.DOUBLE_TYPE.equals(t))
			buffer.append("D");
		else if (ASTHelper.BOOLEAN_TYPE.equals(t))
			buffer.append("Z");
		else if (ASTHelper.CHAR_TYPE.equals(t))
			buffer.append("C");
		else if (ASTHelper.VOID_TYPE.equals(t))
			buffer.append("V");

		return buffer.toString();
	}

	public CtClass getParameterType(Parameter p) throws NotFoundException
	{
		String s = p.getType().toString();
		if (p.isVarArgs())
			s += "[]";
		return findImportedClass(s);
	}

	public CtClass getCtClassFromType(Type t) throws NotFoundException
	{
		return findImportedClass(t.toString());
	}

	public CtClass findImportedClass(String name) throws NotFoundException
	{
		CtClass clazz = classPool.getOrNull(name);
		if (clazz != null)
			return clazz;

		clazz = classPool.getOrNull("java.lang." + name);
		if (clazz != null)
			return clazz;

		String classname;
		for (ImportDeclaration id : getImports())
		{
			String importName = id.getName().toString();
			if (importName.endsWith(".*"))
				// Wildcard imports
				classname = importName.substring(0, importName.length() - 1) + name;
			else
				// Single-class imports
				classname = StringUtils.substringBeforeLast(importName, ".") + "." + name;

			int ix = -1;
			do
			{
				if (ix != -1)
					classname = new StringBuffer(classname).replace(ix, ix + 1, "$").toString();

				clazz = classPool.getOrNull(classname);
				if (clazz != null)
					return clazz;

				// Find next dot
			} while (-1 != (ix = classname.lastIndexOf('.')));
		}
		throw new NotFoundException("Class not found: " + name);
	}

	public List<ImportDeclaration> getImports()
	{
		List<ImportDeclaration> list = unit.getImports();
		if (list != null)
			return list;
		else
			return new ArrayList<ImportDeclaration>();
	}

	public CompilationUnit getUnit()
	{
		return unit;
	}

	public ClassPool getClassPool()
	{
		return classPool;
	}

}
