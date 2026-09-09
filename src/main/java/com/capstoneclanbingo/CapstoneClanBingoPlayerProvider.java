package com.capstoneclanbingo;

import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.callback.ClientThread;

public class CapstoneClanBingoPlayerProvider
{
    private final Client client;
    private final ClientThread clientThread;

    public CapstoneClanBingoPlayerProvider(
            Client client,
            ClientThread clientThread
    )
    {
        this.client = client;
        this.clientThread = clientThread;
    }

    public void getCurrentPlayerName(
            PlayerNameCallback callback
    )
    {
        clientThread.invokeLater(() ->
        {
            Player player =
                    client.getLocalPlayer();

            String playerName = null;

            if (player != null)
            {
                playerName =
                        player.getName();
            }

            if (playerName != null)
            {
                playerName =
                        playerName.trim();
            }

            final String result =
                    playerName;

            SwingUtilities.invokeLater(() ->
            {
                if (
                        result == null
                                || result.isEmpty()
                )
                {
                    callback.onResult(
                            false,
                            null,
                            "Log into Old School RuneScape before opening the bingo board."
                    );

                    return;
                }

                callback.onResult(
                        true,
                        result,
                        "Player found."
                );
            });
        });
    }

    public interface PlayerNameCallback
    {
        void onResult(
                boolean success,
                String playerName,
                String message
        );
    }
}