package com.capstoneclanbingo;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import okhttp3.OkHttpClient;

@PluginDescriptor(
		name = "Capstone Clan Bingo"
)
public class CapstoneClanBingoPlugin extends Plugin
{
	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private CapstoneClanBingoConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private Gson gson;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	private CapstoneClanBingoPanel panel;
	private NavigationButton navButton;

	@Override
	protected void startUp()
	{
		CapstoneClanBingoApiClient apiClient =
				new CapstoneClanBingoApiClient(
						okHttpClient,
						gson
				);

		CapstoneClanBingoPlayerProvider playerProvider =
				new CapstoneClanBingoPlayerProvider(
						client,
						clientThread
				);

		panel =
				new CapstoneClanBingoPanel(
						apiClient,
						playerProvider,
						config,
						configManager
				);

		BufferedImage icon =
				ImageUtil.loadImageResource(
						getClass(),
						"board.png"
				);

		navButton =
				NavigationButton.builder()
						.tooltip("Capstone Clan Bingo")
						.icon(icon)
						.priority(5)
						.panel(panel)
						.build();

		clientToolbar.addNavigation(
				navButton
		);
	}

	@Override
	protected void shutDown()
	{
		if (panel != null)
		{
			panel.closeBoard();
		}

		if (navButton != null)
		{
			clientToolbar.removeNavigation(
					navButton
			);
		}

		panel = null;
		navButton = null;
	}

	@Provides
	CapstoneClanBingoConfig provideConfig(
			ConfigManager configManager
	)
	{
		return configManager.getConfig(
				CapstoneClanBingoConfig.class
		);
	}
}