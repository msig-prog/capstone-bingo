package com.capstoneclanbingo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.image.BufferedImage;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.PluginPanel;

public class CapstoneClanBingoPanel extends PluginPanel
{
    private static final String DEV_TEAM_CODE = "205";
    private static final String DEV_PLAYER_NAME = "DevTester";

    private final CapstoneClanBingoApiClient apiClient;
    private final CapstoneClanBingoPlayerProvider playerProvider;
    private final CapstoneClanBingoConfig config;
    private final ConfigManager configManager;

    private CapstoneClanBingoBoardWindow boardWindow;

    private final JTextField teamCodeField;
    private final JButton joinTeamButton;
    private final JButton forgetTeamButton;
    private final JButton openBoardButton;
    private final JLabel teamStatusLabel;
    private final JLabel playerStatusLabel;

    private String joinedTeamCode;
    private String currentPlayerName;

    public CapstoneClanBingoPanel(
            CapstoneClanBingoApiClient apiClient,
            CapstoneClanBingoPlayerProvider playerProvider,
            CapstoneClanBingoConfig config,
            ConfigManager configManager
    )
    {
        this.apiClient = apiClient;
        this.playerProvider = playerProvider;
        this.config = config;
        this.configManager = configManager;

        setLayout(new BorderLayout(0, 12));

        JLabel title =
                new JLabel(
                        "Capstone Clan Bingo",
                        SwingConstants.CENTER
                );

        JLabel teamCodeLabel =
                new JLabel("Team Code");

        teamCodeField =
                new JTextField();

        teamCodeField.setToolTipText(
                "Enter the team code from Discord"
        );

        teamCodeField.setPreferredSize(
                new Dimension(180, 30)
        );

        joinTeamButton =
                new JButton("Join Team");

        forgetTeamButton =
                new JButton("Forget Team");

        openBoardButton =
                new JButton("Open Bingo Board");

        teamStatusLabel =
                new JLabel(
                        "",
                        SwingConstants.CENTER
                );

        playerStatusLabel =
                new JLabel(
                        "<html><center>"
                                + "Player: <b>Not detected yet</b>"
                                + "</center></html>",
                        SwingConstants.CENTER
                );

        joinTeamButton.addActionListener(e ->
                ensureThirdPartyWarningAccepted(
                        this::joinTeam
                )
        );

        teamCodeField.addActionListener(e ->
                ensureThirdPartyWarningAccepted(
                        this::joinTeam
                )
        );

        openBoardButton.addActionListener(e ->
                ensureThirdPartyWarningAccepted(
                        this::detectPlayerAndLoadEverything
                )
        );

        forgetTeamButton.addActionListener(e ->
                forgetTeam()
        );

        JPanel teamPanel =
                new JPanel(
                        new GridLayout(
                                0,
                                1,
                                0,
                                6
                        )
                );

        teamPanel.add(teamCodeLabel);
        teamPanel.add(teamCodeField);
        teamPanel.add(joinTeamButton);
        teamPanel.add(forgetTeamButton);
        teamPanel.add(teamStatusLabel);
        teamPanel.add(playerStatusLabel);

        JLabel instructions =
                new JLabel(
                        "<html><center>"
                                + "Left click: Claim → Complete → Clear"
                                + "<br>"
                                + "Aid tiles: left click opens roster"
                                + "<br>"
                                + "Right click: progress, ownership, aid"
                                + "</center></html>",
                        SwingConstants.CENTER
                );

        JPanel boardPanel =
                new JPanel(
                        new GridLayout(
                                0,
                                1,
                                0,
                                8
                        )
                );

        boardPanel.add(openBoardButton);
        boardPanel.add(instructions);

        JPanel centerPanel =
                new JPanel(
                        new BorderLayout(
                                0,
                                16
                        )
                );

        centerPanel.add(
                teamPanel,
                BorderLayout.NORTH
        );

        centerPanel.add(
                boardPanel,
                BorderLayout.CENTER
        );

        add(
                title,
                BorderLayout.NORTH
        );

        add(
                centerPanel,
                BorderLayout.CENTER
        );

        loadSavedTeam();
    }

    private boolean isDevTeamCode(
            String code
    )
    {
        return DEV_TEAM_CODE.equals(code);
    }

    private boolean isValidTeamCode(
            String code
    )
    {
        return isDevTeamCode(code)
                || code.matches("[A-Z0-9]{6}");
    }

    private void loadSavedTeam()
    {
        String savedCode =
                config.teamCode();

        if (savedCode != null)
        {
            savedCode =
                    savedCode.trim().toUpperCase();
        }

        if (
                savedCode != null
                        && isValidTeamCode(savedCode)
        )
        {
            joinedTeamCode = savedCode;
            teamCodeField.setText(savedCode);
            openBoardButton.setEnabled(true);
            forgetTeamButton.setEnabled(true);

            teamStatusLabel.setText(
                    "<html><center>"
                            + "Saved team:"
                            + "<br><b>"
                            + savedCode
                            + "</b>"
                            + (
                            isDevTeamCode(savedCode)
                                    ? "<br><span style='color:#d6a64c;'>DEV LOGIN BYPASS</span>"
                                    : ""
                    )
                            + "</center></html>"
            );
            return;
        }

        joinedTeamCode = null;
        openBoardButton.setEnabled(false);
        forgetTeamButton.setEnabled(false);

        teamStatusLabel.setText(
                "<html><center>"
                        + "Enter the team code you received on Discord."
                        + "</center></html>"
        );
    }

    private void ensureThirdPartyWarningAccepted(
            Runnable action
    )
    {
        if (config.thirdPartyWarningAccepted())
        {
            action.run();
            return;
        }

        String message =
                "<html>"
                        + "<div style='width: 360px;'>"
                        + "<b>Third-Party Server Notice</b>"
                        + "<br><br>"
                        + "Capstone Clan Bingo connects to a third-party "
                        + "Cloudflare service to load the current board artwork "
                        + "and synchronize team progress."
                        + "<br><br>"
                        + "The plugin sends your RuneScape display name, team "
                        + "code, and bingo-board activity."
                        + "<br><br>"
                        + "Your IP address is also transmitted as part of the "
                        + "internet connection."
                        + "<br><br>"
                        + "The service is <b>not controlled or verified by the "
                        + "RuneLite Developers</b>."
                        + "<br><br>"
                        + "The plugin does not send your RuneScape password, "
                        + "Jagex credentials, bank, inventory, or chat messages."
                        + "<br><br>"
                        + "Do you want to continue?"
                        + "</div>"
                        + "</html>";

        int result =
                JOptionPane.showConfirmDialog(
                        this,
                        message,
                        "Capstone Clan Bingo - Server Notice",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

        if (result != JOptionPane.YES_OPTION)
        {
            return;
        }

        configManager.setConfiguration(
                CapstoneClanBingoConfig.GROUP,
                CapstoneClanBingoConfig.WARNING_ACCEPTED_KEY,
                true
        );

        action.run();
    }

    private void joinTeam()
    {
        String code =
                teamCodeField
                        .getText()
                        .trim()
                        .toUpperCase();

        if (!isValidTeamCode(code))
        {
            restoreCurrentTeamCode();

            teamStatusLabel.setText(
                    "<html><center>"
                            + "<b>Invalid team code.</b>"
                            + "<br>Use exactly 6 letters/numbers."
                            + "</center></html>"
            );
            return;
        }

        setBusy(true);

        teamStatusLabel.setText(
                "<html><center>"
                        + "Checking team code..."
                        + "</center></html>"
        );

        apiClient.validateTeamCode(
                code,
                (success, message) ->
                {
                    setBusy(false);

                    if (!success)
                    {
                        restoreCurrentTeamCode();

                        teamStatusLabel.setText(
                                "<html><center><b>"
                                        + message
                                        + "</b></center></html>"
                        );
                        return;
                    }

                    joinedTeamCode = code;
                    currentPlayerName = null;

                    teamCodeField.setText(
                            joinedTeamCode
                    );

                    configManager.setConfiguration(
                            CapstoneClanBingoConfig.GROUP,
                            CapstoneClanBingoConfig.TEAM_CODE_KEY,
                            joinedTeamCode
                    );

                    closeBoard();

                    openBoardButton.setEnabled(true);
                    forgetTeamButton.setEnabled(true);

                    if (isDevTeamCode(joinedTeamCode))
                    {
                        playerStatusLabel.setText(
                                "<html><center>"
                                        + "Player:"
                                        + "<br><b>"
                                        + DEV_PLAYER_NAME
                                        + "</b>"
                                        + "<br><span style='color:#d6a64c;'>DEV LOGIN BYPASS</span>"
                                        + "</center></html>"
                        );
                    }
                    else
                    {
                        playerStatusLabel.setText(
                                "<html><center>"
                                        + "Player: <b>Not detected yet</b>"
                                        + "</center></html>"
                        );
                    }

                    teamStatusLabel.setText(
                            "<html><center>"
                                    + "Joined team:"
                                    + "<br><b>"
                                    + joinedTeamCode
                                    + "</b>"
                                    + (
                                    isDevTeamCode(joinedTeamCode)
                                            ? "<br><span style='color:#d6a64c;'>DEV TEAM</span>"
                                            : ""
                            )
                                    + "</center></html>"
                    );
                }
        );
    }

    private void restoreCurrentTeamCode()
    {
        if (joinedTeamCode != null)
        {
            teamCodeField.setText(
                    joinedTeamCode
            );

            openBoardButton.setEnabled(true);
            forgetTeamButton.setEnabled(true);
        }
        else
        {
            teamCodeField.setText("");
            openBoardButton.setEnabled(false);
            forgetTeamButton.setEnabled(false);
        }
    }

    private void forgetTeam()
    {
        closeBoard();

        joinedTeamCode = null;
        currentPlayerName = null;

        configManager.unsetConfiguration(
                CapstoneClanBingoConfig.GROUP,
                CapstoneClanBingoConfig.TEAM_CODE_KEY
        );

        teamCodeField.setText("");
        openBoardButton.setEnabled(false);
        forgetTeamButton.setEnabled(false);

        teamStatusLabel.setText(
                "<html><center>"
                        + "Team forgotten."
                        + "<br>Enter a new team code to reconnect."
                        + "</center></html>"
        );

        playerStatusLabel.setText(
                "<html><center>"
                        + "Player: <b>Not detected yet</b>"
                        + "</center></html>"
        );
    }

    private void detectPlayerAndLoadEverything()
    {
        if (joinedTeamCode == null)
        {
            return;
        }

        /*
         * Dedicated local-development bypass.
         *
         * Team code 205 does not require an authenticated/logged-in
         * RuneScape character. It always acts as DevTester.
         *
         * All normal team codes continue through the real RuneLite
         * player detection flow below.
         */
        if (isDevTeamCode(joinedTeamCode))
        {
            currentPlayerName =
                    DEV_PLAYER_NAME;

            playerStatusLabel.setText(
                    "<html><center>"
                            + "Player:"
                            + "<br><b>"
                            + currentPlayerName
                            + "</b>"
                            + "<br><span style='color:#d6a64c;'>DEV LOGIN BYPASS</span>"
                            + "</center></html>"
            );

            setBusy(true);
            loadManifest();
            return;
        }

        setBusy(true);

        playerStatusLabel.setText(
                "<html><center>"
                        + "Detecting RuneScape character..."
                        + "</center></html>"
        );

        playerProvider.getCurrentPlayerName(
                (success, playerName, message) ->
                {
                    if (!success)
                    {
                        setBusy(false);
                        currentPlayerName = null;

                        playerStatusLabel.setText(
                                "<html><center>"
                                        + "<b>No logged-in player detected.</b>"
                                        + "<br>"
                                        + message
                                        + "</center></html>"
                        );
                        return;
                    }

                    currentPlayerName =
                            playerName;

                    playerStatusLabel.setText(
                            "<html><center>"
                                    + "Player:"
                                    + "<br><b>"
                                    + currentPlayerName
                                    + "</b>"
                                    + "</center></html>"
                    );

                    loadManifest();
                }
        );
    }

    private void loadManifest()
    {
        teamStatusLabel.setText(
                "<html><center>"
                        + "Loading current bingo board..."
                        + "</center></html>"
        );

        apiClient.loadBoardManifest(
                (success, manifest, message) ->
                {
                    if (!success)
                    {
                        failBoardLoad(message);
                        return;
                    }

                    loadImage(manifest);
                }
        );
    }

    private void loadImage(
            CapstoneClanBingoBoardManifest manifest
    )
    {
        apiClient.loadBoardImage(
                manifest,
                (success, image, message) ->
                {
                    if (!success)
                    {
                        failBoardLoad(message);
                        return;
                    }

                    loadTeamState(
                            manifest,
                            image
                    );
                }
        );
    }

    private void loadTeamState(
            CapstoneClanBingoBoardManifest manifest,
            BufferedImage image
    )
    {
        String teamCode =
                joinedTeamCode;

        String playerName =
                currentPlayerName;

        apiClient.loadBoard(
                teamCode,
                (success, board, message) ->
                {
                    setBusy(false);

                    if (!success)
                    {
                        failBoardLoad(message);
                        return;
                    }

                    teamStatusLabel.setText(
                            "<html><center>"
                                    + "Connected to:"
                                    + "<br><b>"
                                    + teamCode
                                    + "</b>"
                                    + (
                                    isDevTeamCode(teamCode)
                                            ? "<br><span style='color:#d6a64c;'>DEV TEAM</span>"
                                            : ""
                            )
                                    + "</center></html>"
                    );

                    playerStatusLabel.setText(
                            "<html><center>"
                                    + "Player:"
                                    + "<br><b>"
                                    + playerName
                                    + "</b>"
                                    + (
                                    isDevTeamCode(teamCode)
                                            ? "<br><span style='color:#d6a64c;'>DEV LOGIN BYPASS</span>"
                                            : ""
                            )
                                    + "</center></html>"
                    );

                    Window runeLiteWindow =
                            SwingUtilities.getWindowAncestor(
                                    this
                            );

                    closeBoard();

                    boardWindow =
                            new CapstoneClanBingoBoardWindow(
                                    runeLiteWindow,
                                    teamCode,
                                    playerName,
                                    apiClient,
                                    manifest,
                                    image,
                                    board
                            );

                    boardWindow.setVisible(true);
                }
        );
    }

    private void failBoardLoad(
            String message
    )
    {
        setBusy(false);
        closeBoard();

        teamStatusLabel.setText(
                "<html><center>"
                        + "<b>Could not load board.</b>"
                        + "<br>"
                        + message
                        + "</center></html>"
        );
    }

    private void setBusy(
            boolean busy
    )
    {
        joinTeamButton.setEnabled(!busy);
        teamCodeField.setEnabled(!busy);

        openBoardButton.setEnabled(
                !busy
                        && joinedTeamCode != null
        );

        forgetTeamButton.setEnabled(
                !busy
                        && joinedTeamCode != null
        );
    }

    public void closeBoard()
    {
        if (
                boardWindow != null
                        && boardWindow.isDisplayable()
        )
        {
            boardWindow.dispose();
        }

        boardWindow = null;
    }
}
