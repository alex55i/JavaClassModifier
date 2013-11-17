package ru.alex55i.jclassmodifier.gui;

import it.sauronsoftware.junique.AlreadyLockedException;
import it.sauronsoftware.junique.JUnique;
import it.sauronsoftware.junique.MessageHandler;
import japa.parser.JavaParser;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import jsyntaxpane.DefaultSyntaxKit;
import ru.alex55i.jclassmodifier.Config;

public class MainFrame extends JFrame
{
	private static final long serialVersionUID = 1L;
	private static final Config config = new Config();
	private ContentPanel contentPanel;
	private static MessageHandler handler;
	private static String[] programArgs;

	public static void main(String[] args)
	{
		programArgs = args;
		String appId = MainFrame.class.getName();
		boolean alreadyRunning;
		try
		{
			JUnique.acquireLock(appId, new MessageHandler()
			{
				public String handle(String message)
				{
					if (handler != null)
						handler.handle(message);
					return null;
				}
			});
			alreadyRunning = false;
		} catch (AlreadyLockedException e)
		{
			alreadyRunning = true;
		}
		if (alreadyRunning)
		{
			for (int i = 0; i < args.length; i++)
			{
				JUnique.sendMessage(appId, args[0]);
			}
			System.exit(0);
		}

		config.load();
		JavaParser.setCacheParser(false); // Parser will be used by multiple threads
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				DefaultSyntaxKit.initKit();
				new MainFrame().setVisible(true);
			}
		});
	}

	public MainFrame()
	{
		setPreferredSize(new Dimension(1000, 700));
		setTitle("Java Class Modifier");
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		contentPanel = new ContentPanel();
		setContentPane(contentPanel);

		pack();
		setLocationRelativeTo(null);
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				System.exit(0);
			}
		});
	}

	public static Config getConfig()
	{
		return config;
	}

	public static void setHandler(MessageHandler handler)
	{
		MainFrame.handler = handler;
	}

	public static String[] getProgramArgs()
	{
		return programArgs;
	}
}
