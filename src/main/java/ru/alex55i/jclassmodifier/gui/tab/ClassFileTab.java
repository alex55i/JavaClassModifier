package ru.alex55i.jclassmodifier.gui.tab;

import japa.parser.JavaParser;
import japa.parser.ParseException;

import java.awt.BorderLayout;
import java.io.StringReader;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import ru.alex55i.jclassmodifier.mod.DecompiledFile;

public class ClassFileTab extends JPanel
{
	private static final long serialVersionUID = 1L;

	private JEditorPane editArea;
	private DecompiledFile source;

	public ClassFileTab(DecompiledFile source)
	{
		this.source = source;
		setLayout(new BorderLayout());

		editArea = new JEditorPane();
		JScrollPane scroll = new JScrollPane(editArea);
		add(scroll);
		editArea.setContentType("text/java");
		String s = source.getUnit().toString();
		editArea.setText(s);
	}

	public DecompiledFile constructClassFile() throws ParseException
	{
		return new DecompiledFile(JavaParser.parse(new StringReader(getText())), source.getContainer());
	}

	public String getText()
	{
		return editArea.getText();
	}

	public void setUnchanged() throws ParseException
	{
		source = constructClassFile();
	}

	public boolean isChanged()
	{
		return !getText().equals(source.getUnit().toString());
	}

	@Override
	public String toString()
	{
		return source.getUnit().getTypes().get(0).getName();
	}
}
