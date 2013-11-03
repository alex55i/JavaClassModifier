package ru.alex55i.jclassmodifier.gui.tab;

import japa.parser.JavaParser;
import japa.parser.ParseException;

import java.awt.BorderLayout;
import java.io.StringReader;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import ru.alex55i.jclassmodifier.mod.JavaClassFile;


public class ClassFileTab extends JPanel
{
	private static final long serialVersionUID = 1L;

	private JEditorPane editArea;
	private JavaClassFile source;

	public ClassFileTab(JavaClassFile source)
	{
		this.source = source;
		setLayout(new BorderLayout());

		editArea = new JEditorPane();
		JScrollPane scroll = new JScrollPane(editArea);
		add(scroll);
		editArea.setContentType("text/java");
		editArea.setText(source.getUnit().toString());
	}

	public JavaClassFile getClassFile() throws ParseException
	{
		return new JavaClassFile(JavaParser.parse(new StringReader(getText())));
	}

	public String getText()
	{
		return editArea.getText();
	}

	@Override
	public String toString()
	{
		return source.getUnit().getTypes().get(0).getName();
	}
}
