package ru.alex55i.jclassmodifier.mod;

import japa.parser.ast.BlockComment;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.Node;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.stmt.BlockStmt;
import japa.parser.ast.visitor.ModifierVisitorAdapter;

import java.io.File;
import java.io.IOException;

import ru.alex55i.jclassmodifier.ClassContainer;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

public class DecompiledFile
{
	private CompilationUnit unit;
	private ClassContainer container;

	public DecompiledFile(CompilationUnit unit, ClassContainer container)
	{
		this.unit = unit;
		this.container = container;
	}

	public CompilationUnit getUnit()
	{
		return unit;
	}

	public ClassContainer getContainer()
	{
		return container;
	}

	public void saveToFile(File file) throws IOException
	{
		Files.write(toString(), file, Charsets.UTF_8);
	}

	public String toString()
	{
		return unit.toString();
	}

	public void recomputeChecksum()
	{
		new ModifierVisitorAdapter<String>()
		{
			public Node visit(MethodDeclaration n, String arg)
			{
				BlockStmt body = n.getBody();
				if (body != null)
				{
					n.setComment(new BlockComment(hash(n)));
				}
				return n;
			};
		}.visit(unit, null);
	}

	public static String hash(MethodDeclaration md)
	{
		if (md.getBody() != null)
		{
			return Hashing.adler32().hashString(md.getBody().toString(), Charsets.UTF_8).toString();
		} else
		{
			return "null";
		}
	}
}
