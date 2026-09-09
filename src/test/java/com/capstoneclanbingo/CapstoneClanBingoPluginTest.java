package com.capstoneclanbingo;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CapstoneClanBingoPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CapstoneClanBingoPlugin.class);
		RuneLite.main(args);
	}
}