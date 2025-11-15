package com.example;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class InverseSpellbookFilterPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(InverseSpellbookFilterPlugin.class);
		RuneLite.main(args);
	}
}