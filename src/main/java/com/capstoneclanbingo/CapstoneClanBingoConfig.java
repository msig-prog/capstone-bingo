package com.capstoneclanbingo;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(CapstoneClanBingoConfig.GROUP)
public interface CapstoneClanBingoConfig extends Config
{
	String GROUP = "capstoneclanbingo";

	String TEAM_CODE_KEY = "teamCode";
	String WARNING_ACCEPTED_KEY = "thirdPartyWarningAccepted";

	@ConfigItem(
			keyName = TEAM_CODE_KEY,
			name = "Team Code",
			description = "Saved Capstone Clan Bingo team code.",
			hidden = true
	)
	default String teamCode()
	{
		return "";
	}

	@ConfigItem(
			keyName = WARNING_ACCEPTED_KEY,
			name = "Third-Party Warning Accepted",
			description = "Whether the third-party server disclosure has been accepted.",
			hidden = true
	)
	default boolean thirdPartyWarningAccepted()
	{
		return false;
	}
}