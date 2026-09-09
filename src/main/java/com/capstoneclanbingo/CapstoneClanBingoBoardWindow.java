package com.capstoneclanbingo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.client.util.ImageUtil;

public class CapstoneClanBingoBoardWindow extends JDialog
{
    /*
     * IMPORTANT:
     * The bundled board.png is intentionally much smaller than these values
     * to satisfy Plugin Hub image-size limits.
     *
     * These are the coordinate dimensions used when the clickable grid was
     * calibrated against the original artwork. All click/overlay math stays
     * in this logical coordinate system, regardless of the actual PNG size.
     */
    private static final int LOGICAL_BOARD_WIDTH = 1024;
    private static final int LOGICAL_BOARD_HEIGHT = 1310;

    private static final int PREFERRED_WIDTH = 650;
    private static final int REFRESH_INTERVAL_MS = 60_000;

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm:ss a");

    private static final int[] GRID_X =
            {
                    47, 198, 354, 511, 662, 818, 968
            };

    private static final int[] GRID_Y =
            {
                    568, 692, 808, 921, 1033, 1146, 1265
            };

    private final String teamCode;
    private final String playerName;
    private final CapstoneClanBingoApiClient apiClient;

    private final CapstoneClanBingoApiClient.TileData[] tiles =
            new CapstoneClanBingoApiClient.TileData[36];

    private final boolean[] tileBusy =
            new boolean[36];

    private BingoBoardPanel boardPanel;
    private final Timer syncTimer;

    private boolean refreshInProgress;

    private SyncState syncState =
            SyncState.SYNCED;

    private String lastSyncTime =
            formatCurrentTime();

    private enum SyncState
    {
        SYNCED,
        SYNCING,
        OFFLINE
    }

    public CapstoneClanBingoBoardWindow(
            Window owner,
            String teamCode,
            String playerName,
            CapstoneClanBingoApiClient apiClient,
            CapstoneClanBingoApiClient.BoardResponse initialBoard
    )
    {
        super(owner);

        this.teamCode = teamCode;
        this.playerName = playerName;
        this.apiClient = apiClient;

        setUndecorated(true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(false);

        initializeEmptyTiles();
        applyBoard(initialBoard);

        /*
         * The optimized bundled image is 440x563. We do NOT use its pixel
         * dimensions for click mapping; the logical 1024x1310 coordinate
         * system above is used instead.
         */
        BufferedImage boardImage =
                ImageUtil.loadImageResource(
                        CapstoneClanBingoBoardWindow.class,
                        "board.png"
                );

        int displayWidth =
                calculateDisplayWidth(owner);

        boardPanel =
                new BingoBoardPanel(
                        boardImage,
                        displayWidth
                );

        setContentPane(boardPanel);
        pack();
        setLocationRelativeTo(owner);

        syncTimer =
                new Timer(
                        REFRESH_INTERVAL_MS,
                        e -> refreshBoard(false)
                );

        syncTimer.setRepeats(true);
        syncTimer.start();
    }

    @Override
    public void dispose()
    {
        if (syncTimer != null)
        {
            syncTimer.stop();
        }

        super.dispose();
    }

    private void initializeEmptyTiles()
    {
        for (int i = 0; i < tiles.length; i++)
        {
            tiles[i] =
                    createOpenTile(i);
        }
    }

    public void applyBoard(
            CapstoneClanBingoApiClient.BoardResponse board
    )
    {
        if (board == null || board.tiles == null)
        {
            return;
        }

        initializeEmptyTiles();

        for (
                CapstoneClanBingoApiClient.TileData incoming
                : board.tiles
        )
        {
            if (incoming == null)
            {
                continue;
            }

            int index =
                    incoming.tileNumber - 1;

            if (index < 0 || index >= tiles.length)
            {
                continue;
            }

            tiles[index] =
                    incoming.copy();
        }

        if (boardPanel != null)
        {
            boardPanel.repaint();
        }
    }

    private void refreshBoard(
            boolean showError
    )
    {
        if (
                refreshInProgress
                        || anyTileBusy()
                        || !isDisplayable()
        )
        {
            return;
        }

        refreshInProgress = true;
        syncState = SyncState.SYNCING;

        if (boardPanel != null)
        {
            boardPanel.repaint();
        }

        apiClient.loadBoard(
                teamCode,
                (success, board, message) ->
                {
                    refreshInProgress = false;

                    if (!isDisplayable())
                    {
                        return;
                    }

                    if (!success)
                    {
                        syncState = SyncState.OFFLINE;
                        boardPanel.repaint();

                        if (showError)
                        {
                            JOptionPane.showMessageDialog(
                                    this,
                                    "Could not refresh the shared board.\n\n"
                                            + message,
                                    "Bingo Server Unavailable",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        }

                        return;
                    }

                    applyBoard(board);

                    syncState = SyncState.SYNCED;
                    lastSyncTime = formatCurrentTime();

                    boardPanel.repaint();
                }
        );
    }

    private boolean anyTileBusy()
    {
        for (boolean busy : tileBusy)
        {
            if (busy)
            {
                return true;
            }
        }

        return false;
    }

    private int calculateDisplayWidth(
            Window owner
    )
    {
        int width =
                PREFERRED_WIDTH;

        if (owner == null)
        {
            return width;
        }

        int availableWidth =
                Math.max(
                        350,
                        owner.getWidth() - 80
                );

        int availableHeight =
                Math.max(
                        450,
                        owner.getHeight() - 80
                );

        int widthAllowedByHeight =
                (int) Math.floor(
                        availableHeight
                                * (
                                LOGICAL_BOARD_WIDTH
                                        / (double) LOGICAL_BOARD_HEIGHT
                        )
                );

        width =
                Math.min(
                        width,
                        availableWidth
                );

        width =
                Math.min(
                        width,
                        widthAllowedByHeight
                );

        return Math.max(
                350,
                width
        );
    }

    private class BingoBoardPanel extends JPanel
    {
        private final Image scaledImage;

        private final int displayWidth;
        private final int displayHeight;

        private final double scaleX;
        private final double scaleY;

        private final Rectangle closeButtonBounds =
                new Rectangle();

        private final Rectangle refreshButtonBounds =
                new Rectangle();

        private Point dragStartScreen;
        private Point dragStartWindow;

        BingoBoardPanel(
                BufferedImage boardImage,
                int displayWidth
        )
        {
            this.displayWidth =
                    displayWidth;

            /*
             * Use the logical board ratio rather than the optimized PNG's
             * physical dimensions.
             */
            displayHeight =
                    (int) Math.round(
                            displayWidth
                                    * (
                                    LOGICAL_BOARD_HEIGHT
                                            / (double) LOGICAL_BOARD_WIDTH
                            )
                    );

            /*
             * Mouse coordinates and overlays are mapped back to the original
             * 1024x1310 calibration coordinates.
             */
            scaleX =
                    displayWidth
                            / (double) LOGICAL_BOARD_WIDTH;

            scaleY =
                    displayHeight
                            / (double) LOGICAL_BOARD_HEIGHT;

            scaledImage =
                    boardImage.getScaledInstance(
                            displayWidth,
                            displayHeight,
                            Image.SCALE_SMOOTH
                    );

            setPreferredSize(
                    new Dimension(
                            displayWidth,
                            displayHeight
                    )
            );

            setOpaque(true);

            addMouseListener(
                    new MouseAdapter()
                    {
                        @Override
                        public void mousePressed(
                                MouseEvent event
                        )
                        {
                            handlePopup(event);

                            if (
                                    !SwingUtilities.isLeftMouseButton(
                                            event
                                    )
                            )
                            {
                                return;
                            }

                            if (
                                    closeButtonBounds.contains(
                                            event.getPoint()
                                    )
                            )
                            {
                                dispose();
                                return;
                            }

                            if (
                                    refreshButtonBounds.contains(
                                            event.getPoint()
                                    )
                            )
                            {
                                return;
                            }

                            int tileIndex =
                                    getTileIndex(
                                            event.getX(),
                                            event.getY()
                                    );

                            if (tileIndex == -1)
                            {
                                dragStartScreen =
                                        event.getLocationOnScreen();

                                dragStartWindow =
                                        CapstoneClanBingoBoardWindow
                                                .this
                                                .getLocation();
                            }
                        }

                        @Override
                        public void mouseReleased(
                                MouseEvent event
                        )
                        {
                            handlePopup(event);

                            dragStartScreen = null;
                            dragStartWindow = null;
                        }

                        @Override
                        public void mouseClicked(
                                MouseEvent event
                        )
                        {
                            if (
                                    !SwingUtilities.isLeftMouseButton(
                                            event
                                    )
                            )
                            {
                                return;
                            }

                            if (
                                    closeButtonBounds.contains(
                                            event.getPoint()
                                    )
                            )
                            {
                                return;
                            }

                            if (
                                    refreshButtonBounds.contains(
                                            event.getPoint()
                                    )
                            )
                            {
                                refreshBoard(true);
                                return;
                            }

                            int tileIndex =
                                    getTileIndex(
                                            event.getX(),
                                            event.getY()
                                    );

                            if (tileIndex == -1)
                            {
                                return;
                            }

                            handleLeftClick(tileIndex);
                        }
                    }
            );

            addMouseMotionListener(
                    new MouseMotionAdapter()
                    {
                        @Override
                        public void mouseDragged(
                                MouseEvent event
                        )
                        {
                            if (
                                    dragStartScreen == null
                                            || dragStartWindow == null
                            )
                            {
                                return;
                            }

                            Point currentScreen =
                                    event.getLocationOnScreen();

                            int deltaX =
                                    currentScreen.x
                                            - dragStartScreen.x;

                            int deltaY =
                                    currentScreen.y
                                            - dragStartScreen.y;

                            CapstoneClanBingoBoardWindow
                                    .this
                                    .setLocation(
                                            dragStartWindow.x
                                                    + deltaX,
                                            dragStartWindow.y
                                                    + deltaY
                                    );
                        }
                    }
            );
        }

        private void handleLeftClick(
                int tileIndex
        )
        {
            if (tileBusy[tileIndex])
            {
                return;
            }

            CapstoneClanBingoApiClient.TileData current =
                    tiles[tileIndex];

            CapstoneClanBingoApiClient.TileData proposed =
                    current.copy();

            if ("OPEN".equals(current.status))
            {
                proposed.status = "OWNED";
                proposed.owner = playerName;
                proposed.progress = null;
                proposed.aidRequested = false;
                proposed.helpers = new ArrayList<>();
            }
            else if ("OWNED".equals(current.status))
            {
                proposed.status = "COMPLETE";
                proposed.progress = null;
                proposed.aidRequested = false;
            }
            else
            {
                proposed =
                        createOpenTile(tileIndex);
            }

            saveProposedTile(
                    tileIndex,
                    proposed
            );
        }

        private void handlePopup(
                MouseEvent event
        )
        {
            if (!event.isPopupTrigger())
            {
                return;
            }

            int tileIndex =
                    getTileIndex(
                            event.getX(),
                            event.getY()
                    );

            if (tileIndex == -1)
            {
                return;
            }

            showTileMenu(
                    event,
                    tileIndex
            );
        }

        private void showTileMenu(
                MouseEvent event,
                int tileIndex
        )
        {
            if (tileBusy[tileIndex])
            {
                return;
            }

            CapstoneClanBingoApiClient.TileData current =
                    tiles[tileIndex];

            JPopupMenu menu =
                    new JPopupMenu();

            JMenuItem markOpen =
                    new JMenuItem(
                            "Mark Open"
                    );

            JMenuItem claimForMe =
                    new JMenuItem(
                            "Claim for "
                                    + playerName
                    );

            JMenuItem markComplete =
                    new JMenuItem(
                            "Mark Complete"
                    );

            JMenuItem setProgress =
                    new JMenuItem(
                            "Set Progress..."
                    );

            JMenuItem aidAction =
                    new JMenuItem(
                            current.aidRequested
                                    ? "Cancel Aid Request"
                                    : "Request Aid"
                    );

            JMenuItem joinAid =
                    new JMenuItem(
                            isHelper(
                                    current,
                                    playerName
                            )
                                    ? "Leave Aid"
                                    : "Join Aid"
                    );

            markOpen.addActionListener(e ->
                    saveProposedTile(
                            tileIndex,
                            createOpenTile(
                                    tileIndex
                            )
                    )
            );

            claimForMe.addActionListener(e ->
            {
                CapstoneClanBingoApiClient.TileData proposed =
                        current.copy();

                proposed.status = "OWNED";
                proposed.owner = playerName;
                proposed.progress = null;
                proposed.aidRequested = false;
                proposed.helpers = new ArrayList<>();

                saveProposedTile(
                        tileIndex,
                        proposed
                );
            });

            markComplete.addActionListener(e ->
            {
                CapstoneClanBingoApiClient.TileData proposed =
                        current.copy();

                if (proposed.owner == null)
                {
                    proposed.owner =
                            playerName;
                }

                proposed.status = "COMPLETE";
                proposed.progress = null;
                proposed.aidRequested = false;

                saveProposedTile(
                        tileIndex,
                        proposed
                );
            });

            setProgress.addActionListener(e ->
            {
                String input =
                        JOptionPane.showInputDialog(
                                this,
                                "Enter progress as a percentage or fraction.\n"
                                        + "Examples: 70% or 7/10",
                                current.progress == null
                                        ? ""
                                        : current.progress
                        );

                if (input == null)
                {
                    return;
                }

                String normalized =
                        normalizeProgress(input);

                if (normalized == null)
                {
                    JOptionPane.showMessageDialog(
                            this,
                            "Please enter progress like 70% or 7/10.",
                            "Invalid Progress",
                            JOptionPane.ERROR_MESSAGE
                    );

                    return;
                }

                CapstoneClanBingoApiClient.TileData proposed =
                        current.copy();

                proposed.status = "OWNED";

                if (proposed.owner == null)
                {
                    proposed.owner =
                            playerName;
                }

                proposed.progress =
                        normalized;

                saveProposedTile(
                        tileIndex,
                        proposed
                );
            });

            aidAction.addActionListener(e ->
            {
                CapstoneClanBingoApiClient.TileData proposed =
                        current.copy();

                if ("COMPLETE".equals(proposed.status))
                {
                    return;
                }

                if ("OPEN".equals(proposed.status))
                {
                    proposed.status = "OWNED";
                    proposed.owner = playerName;
                }

                if (
                        !samePlayer(
                                proposed.owner,
                                playerName
                        )
                )
                {
                    JOptionPane.showMessageDialog(
                            this,
                            "Only the owner can request or cancel aid.",
                            "Aid Request",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    return;
                }

                proposed.aidRequested =
                        !proposed.aidRequested;

                saveProposedTile(
                        tileIndex,
                        proposed
                );
            });

            joinAid.addActionListener(e ->
            {
                if (
                        samePlayer(
                                current.owner,
                                playerName
                        )
                )
                {
                    JOptionPane.showMessageDialog(
                            this,
                            "You are already the owner of this tile.",
                            "Join Aid",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    return;
                }

                CapstoneClanBingoApiClient.TileData proposed =
                        current.copy();

                if (
                        isHelper(
                                proposed,
                                playerName
                        )
                )
                {
                    removeHelper(
                            proposed,
                            playerName
                    );
                }
                else
                {
                    if (!proposed.aidRequested)
                    {
                        JOptionPane.showMessageDialog(
                                this,
                                "The owner is not currently requesting aid.",
                                "Join Aid",
                                JOptionPane.INFORMATION_MESSAGE
                        );

                        return;
                    }

                    proposed.helpers.add(
                            playerName
                    );
                }

                saveProposedTile(
                        tileIndex,
                        proposed
                );
            });

            menu.add(markOpen);
            menu.add(claimForMe);
            menu.add(markComplete);

            menu.addSeparator();

            menu.add(setProgress);

            menu.addSeparator();

            if (!"COMPLETE".equals(current.status))
            {
                menu.add(aidAction);
            }

            if (
                    current.aidRequested
                            || isHelper(
                            current,
                            playerName
                    )
            )
            {
                menu.add(joinAid);
            }

            menu.show(
                    this,
                    event.getX(),
                    event.getY()
            );
        }

        private void saveProposedTile(
                int tileIndex,
                CapstoneClanBingoApiClient.TileData proposed
        )
        {
            if (tileBusy[tileIndex])
            {
                return;
            }

            tileBusy[tileIndex] = true;
            syncState = SyncState.SYNCING;

            repaint();

            apiClient.saveTile(
                    teamCode,
                    proposed,
                    (success, savedTile, message) ->
                    {
                        tileBusy[tileIndex] = false;

                        if (!success)
                        {
                            syncState = SyncState.OFFLINE;

                            repaint();

                            JOptionPane.showMessageDialog(
                                    this,
                                    "Your change was NOT saved.\n\n"
                                            + message,
                                    "Bingo Server Unavailable",
                                    JOptionPane.ERROR_MESSAGE
                            );

                            return;
                        }

                        tiles[tileIndex] =
                                savedTile.copy();

                        syncState = SyncState.SYNCED;
                        lastSyncTime = formatCurrentTime();

                        repaint();
                    }
            );
        }

        private int getTileIndex(
                int mouseX,
                int mouseY
        )
        {
            int originalX =
                    (int) (
                            mouseX
                                    / scaleX
                    );

            int originalY =
                    (int) (
                            mouseY
                                    / scaleY
                    );

            int column =
                    findGridPosition(
                            originalX,
                            GRID_X
                    );

            int row =
                    findGridPosition(
                            originalY,
                            GRID_Y
                    );

            if (row == -1 || column == -1)
            {
                return -1;
            }

            return (
                    row * 6
            ) + column;
        }

        @Override
        protected void paintComponent(
                Graphics graphics
        )
        {
            super.paintComponent(graphics);

            Graphics2D g2 =
                    (Graphics2D) graphics.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            );

            g2.drawImage(
                    scaledImage,
                    0,
                    0,
                    null
            );

            for (int row = 0; row < 6; row++)
            {
                for (int column = 0; column < 6; column++)
                {
                    int tileIndex =
                            (
                                    row * 6
                            ) + column;

                    CapstoneClanBingoApiClient.TileData tile =
                            tiles[tileIndex];

                    if (
                            tile == null
                                    || "OPEN".equals(
                                    tile.status
                            )
                    )
                    {
                        continue;
                    }

                    drawTileStatus(
                            g2,
                            tile,
                            row,
                            column
                    );
                }
            }

            drawTopControls(g2);

            g2.setColor(
                    new Color(
                            20,
                            20,
                            20,
                            220
                    )
            );

            g2.setStroke(
                    new BasicStroke(2f)
            );

            g2.drawRect(
                    1,
                    1,
                    displayWidth - 3,
                    displayHeight - 3
            );

            g2.dispose();
        }

        private void drawTopControls(
                Graphics2D g2
        )
        {
            int buttonSize = 26;
            int top = 8;

            int refreshX = 8;

            refreshButtonBounds.setBounds(
                    refreshX,
                    top,
                    buttonSize,
                    buttonSize
            );

            g2.setColor(
                    new Color(
                            0,
                            0,
                            0,
                            180
                    )
            );

            g2.fillRoundRect(
                    refreshX,
                    top,
                    buttonSize,
                    buttonSize,
                    8,
                    8
            );

            g2.setColor(Color.WHITE);

            g2.setFont(
                    new Font(
                            Font.SANS_SERIF,
                            Font.BOLD,
                            17
                    )
            );

            drawCenteredText(
                    g2,
                    "↻",
                    refreshX,
                    buttonSize,
                    top + 19
            );

            String syncText;
            Color syncColor;

            switch (syncState)
            {
                case SYNCING:
                    syncText = "SYNCING";
                    syncColor =
                            new Color(
                                    210,
                                    145,
                                    30
                            );
                    break;

                case OFFLINE:
                    syncText = "OFFLINE";
                    syncColor =
                            new Color(
                                    180,
                                    55,
                                    55
                            );
                    break;

                default:
                    syncText = "SYNCED";
                    syncColor =
                            new Color(
                                    45,
                                    145,
                                    75
                            );
                    break;
            }

            g2.setFont(
                    new Font(
                            Font.SANS_SERIF,
                            Font.BOLD,
                            10
                    )
            );

            int textWidth =
                    g2.getFontMetrics()
                            .stringWidth(syncText);

            int badgeWidth =
                    textWidth + 18;

            int badgeX =
                    refreshX
                            + buttonSize
                            + 6;

            g2.setColor(
                    new Color(
                            syncColor.getRed(),
                            syncColor.getGreen(),
                            syncColor.getBlue(),
                            225
                    )
            );

            g2.fillRoundRect(
                    badgeX,
                    top,
                    badgeWidth,
                    buttonSize,
                    8,
                    8
            );

            g2.setColor(Color.WHITE);

            drawCenteredText(
                    g2,
                    syncText,
                    badgeX,
                    badgeWidth,
                    top + 17
            );

            if (syncState == SyncState.SYNCED)
            {
                g2.setFont(
                        new Font(
                                Font.SANS_SERIF,
                                Font.PLAIN,
                                8
                        )
                );

                g2.setColor(
                        new Color(
                                255,
                                255,
                                255,
                                220
                        )
                );

                g2.drawString(
                        lastSyncTime,
                        badgeX
                                + badgeWidth
                                + 6,
                        top + 17
                );
            }

            int closeX =
                    displayWidth
                            - buttonSize
                            - 8;

            closeButtonBounds.setBounds(
                    closeX,
                    top,
                    buttonSize,
                    buttonSize
            );

            g2.setColor(
                    new Color(
                            0,
                            0,
                            0,
                            180
                    )
            );

            g2.fillRoundRect(
                    closeX,
                    top,
                    buttonSize,
                    buttonSize,
                    8,
                    8
            );

            g2.setColor(Color.WHITE);

            g2.setFont(
                    new Font(
                            Font.SANS_SERIF,
                            Font.BOLD,
                            18
                    )
            );

            drawCenteredText(
                    g2,
                    "×",
                    closeX,
                    buttonSize,
                    top + 19
            );
        }

        private void drawTileStatus(
                Graphics2D g2,
                CapstoneClanBingoApiClient.TileData tile,
                int row,
                int column
        )
        {
            int x1 =
                    (int) Math.round(
                            GRID_X[column]
                                    * scaleX
                    );

            int x2 =
                    (int) Math.round(
                            GRID_X[column + 1]
                                    * scaleX
                    );

            int y1 =
                    (int) Math.round(
                            GRID_Y[row]
                                    * scaleY
                    );

            int y2 =
                    (int) Math.round(
                            GRID_Y[row + 1]
                                    * scaleY
                    );

            int width =
                    x2 - x1;

            int height =
                    y2 - y1;

            boolean complete =
                    "COMPLETE".equals(
                            tile.status
                    );

            boolean hasProgress =
                    tile.progress != null;

            boolean aidRequested =
                    tile.aidRequested;

            Color accent;

            if (complete)
            {
                accent =
                        new Color(
                                42,
                                150,
                                75
                        );
            }
            else if (aidRequested)
            {
                accent =
                        new Color(
                                145,
                                75,
                                190
                        );
            }
            else if (hasProgress)
            {
                accent =
                        new Color(
                                45,
                                115,
                                190
                        );
            }
            else
            {
                accent =
                        new Color(
                                215,
                                145,
                                25
                        );
            }

            g2.setColor(
                    new Color(
                            accent.getRed(),
                            accent.getGreen(),
                            accent.getBlue(),
                            72
                    )
            );

            g2.fillRect(
                    x1 + 1,
                    y1 + 1,
                    width - 2,
                    height - 2
            );

            g2.setColor(
                    new Color(
                            accent.getRed(),
                            accent.getGreen(),
                            accent.getBlue(),
                            230
                    )
            );

            g2.setStroke(
                    new BasicStroke(2f)
            );

            g2.drawRect(
                    x1 + 1,
                    y1 + 1,
                    width - 3,
                    height - 3
            );

            drawParticipants(
                    g2,
                    tile,
                    x1,
                    y1,
                    width,
                    height
            );

            String statusText;

            if (complete)
            {
                statusText = "COMPLETE";
            }
            else if (
                    aidRequested
                            && hasProgress
            )
            {
                statusText =
                        "AID • "
                                + tile.progress;
            }
            else if (aidRequested)
            {
                statusText =
                        "AID REQUESTED";
            }
            else if (hasProgress)
            {
                statusText =
                        tile.progress;
            }
            else
            {
                statusText =
                        "CLAIMED";
            }

            int statusHeight =
                    Math.max(
                            18,
                            height / 4
                    );

            int statusX =
                    x1 + 4;

            int statusY =
                    y2
                            - statusHeight
                            - 4;

            int statusWidth =
                    width - 8;

            g2.setColor(
                    new Color(
                            accent.getRed(),
                            accent.getGreen(),
                            accent.getBlue(),
                            235
                    )
            );

            g2.fillRoundRect(
                    statusX,
                    statusY,
                    statusWidth,
                    statusHeight,
                    8,
                    8
            );

            int statusFontSize =
                    clamp(
                            height / 6,
                            9,
                            13
                    );

            g2.setFont(
                    new Font(
                            Font.SANS_SERIF,
                            Font.BOLD,
                            statusFontSize
                    )
            );

            String fittedStatus =
                    fitText(
                            g2,
                            statusText,
                            statusWidth - 6
                    );

            drawCenteredTextWithShadow(
                    g2,
                    fittedStatus,
                    statusX,
                    statusWidth,
                    statusY
                            + statusHeight
                            - 5
            );
        }

        private void drawParticipants(
                Graphics2D g2,
                CapstoneClanBingoApiClient.TileData tile,
                int x1,
                int y1,
                int width,
                int height
        )
        {
            List<String> names =
                    new ArrayList<>();

            if (tile.owner != null)
            {
                names.add(
                        tile.owner
                );
            }

            if (tile.helpers != null)
            {
                for (
                        String helper
                        : tile.helpers
                )
                {
                    names.add(
                            "+ "
                                    + helper
                    );
                }
            }

            if (names.isEmpty())
            {
                return;
            }

            int statusHeight =
                    Math.max(
                            18,
                            height / 4
                    );

            int availableHeight =
                    height
                            - statusHeight
                            - 14;

            int lineHeight = 11;

            int maxLines =
                    Math.max(
                            1,
                            Math.min(
                                    3,
                                    (
                                            availableHeight
                                                    - 6
                                    ) / lineHeight
                            )
                    );

            List<String> visibleLines =
                    new ArrayList<>();

            if (names.size() <= maxLines)
            {
                visibleLines.addAll(names);
            }
            else
            {
                for (
                        int i = 0;
                        i < maxLines - 1;
                        i++
                )
                {
                    visibleLines.add(
                            names.get(i)
                    );
                }

                int remaining =
                        names.size()
                                - (
                                maxLines
                                        - 1
                        );

                visibleLines.add(
                        "+"
                                + remaining
                                + " more"
                );
            }

            int panelHeight =
                    8
                            + (
                            visibleLines.size()
                                    * lineHeight
                    );

            panelHeight =
                    Math.min(
                            panelHeight,
                            availableHeight
                    );

            int panelX =
                    x1 + 4;

            int panelY =
                    y1 + 4;

            int panelWidth =
                    width - 8;

            g2.setColor(
                    new Color(
                            15,
                            15,
                            15,
                            210
                    )
            );

            g2.fillRoundRect(
                    panelX,
                    panelY,
                    panelWidth,
                    panelHeight,
                    8,
                    8
            );

            int fontSize =
                    clamp(
                            height / 8,
                            8,
                            11
                    );

            g2.setFont(
                    new Font(
                            Font.SANS_SERIF,
                            Font.BOLD,
                            fontSize
                    )
            );

            int baseline =
                    panelY + 12;

            for (String line : visibleLines)
            {
                String fitted =
                        fitText(
                                g2,
                                line,
                                panelWidth - 8
                        );

                drawCenteredTextWithShadow(
                        g2,
                        fitted,
                        panelX,
                        panelWidth,
                        baseline
                );

                baseline += lineHeight;
            }
        }
    }

    private CapstoneClanBingoApiClient.TileData createOpenTile(
            int tileIndex
    )
    {
        CapstoneClanBingoApiClient.TileData tile =
                new CapstoneClanBingoApiClient.TileData();

        tile.tileNumber =
                tileIndex + 1;

        tile.status = "OPEN";
        tile.owner = null;
        tile.progress = null;
        tile.aidRequested = false;
        tile.helpers = new ArrayList<>();

        return tile;
    }

    private boolean isHelper(
            CapstoneClanBingoApiClient.TileData tile,
            String player
    )
    {
        if (
                tile == null
                        || player == null
                        || tile.helpers == null
        )
        {
            return false;
        }

        for (String helper : tile.helpers)
        {
            if (
                    samePlayer(
                            helper,
                            player
                    )
            )
            {
                return true;
            }
        }

        return false;
    }

    private void removeHelper(
            CapstoneClanBingoApiClient.TileData tile,
            String player
    )
    {
        if (
                tile.helpers == null
                        || player == null
        )
        {
            return;
        }

        tile.helpers.removeIf(
                helper ->
                        samePlayer(
                                helper,
                                player
                        )
        );
    }

    private boolean samePlayer(
            String first,
            String second
    )
    {
        if (
                first == null
                        || second == null
        )
        {
            return false;
        }

        return first
                .trim()
                .equalsIgnoreCase(
                        second.trim()
                );
    }

    private String normalizeProgress(
            String input
    )
    {
        String value =
                input
                        .trim()
                        .replace(
                                " ",
                                ""
                        );

        if (value.endsWith("%"))
        {
            try
            {
                int percent =
                        Integer.parseInt(
                                value.substring(
                                        0,
                                        value.length() - 1
                                )
                        );

                if (
                        percent < 0
                                || percent > 100
                )
                {
                    return null;
                }

                return percent
                        + "%";
            }
            catch (NumberFormatException exception)
            {
                return null;
            }
        }

        if (value.contains("/"))
        {
            String[] parts =
                    value.split("/");

            if (parts.length != 2)
            {
                return null;
            }

            try
            {
                int current =
                        Integer.parseInt(
                                parts[0]
                        );

                int total =
                        Integer.parseInt(
                                parts[1]
                        );

                if (
                        current < 0
                                || total <= 0
                                || current > total
                )
                {
                    return null;
                }

                return current
                        + "/"
                        + total;
            }
            catch (NumberFormatException exception)
            {
                return null;
            }
        }

        return null;
    }

    private int findGridPosition(
            int value,
            int[] boundaries
    )
    {
        for (
                int i = 0;
                i < boundaries.length - 1;
                i++
        )
        {
            if (
                    value >= boundaries[i]
                            && value < boundaries[i + 1]
            )
            {
                return i;
            }
        }

        return -1;
    }

    private void drawCenteredText(
            Graphics2D g2,
            String text,
            int x,
            int width,
            int baselineY
    )
    {
        int textWidth =
                g2.getFontMetrics()
                        .stringWidth(text);

        int textX =
                x
                        + (
                        width - textWidth
                ) / 2;

        g2.drawString(
                text,
                textX,
                baselineY
        );
    }

    private void drawCenteredTextWithShadow(
            Graphics2D g2,
            String text,
            int x,
            int width,
            int baselineY
    )
    {
        int textWidth =
                g2.getFontMetrics()
                        .stringWidth(text);

        int textX =
                x
                        + (
                        width - textWidth
                ) / 2;

        g2.setColor(
                new Color(
                        0,
                        0,
                        0,
                        220
                )
        );

        g2.drawString(
                text,
                textX + 1,
                baselineY + 1
        );

        g2.setColor(Color.WHITE);

        g2.drawString(
                text,
                textX,
                baselineY
        );
    }

    private String fitText(
            Graphics2D g2,
            String text,
            int maxWidth
    )
    {
        if (
                g2.getFontMetrics()
                        .stringWidth(text)
                        <= maxWidth
        )
        {
            return text;
        }

        String shortened =
                text;

        while (
                shortened.length() > 1
                        && g2.getFontMetrics()
                        .stringWidth(
                                shortened
                                        + "..."
                        )
                        > maxWidth
        )
        {
            shortened =
                    shortened.substring(
                            0,
                            shortened.length() - 1
                    );
        }

        return shortened
                + "...";
    }

    private int clamp(
            int value,
            int minimum,
            int maximum
    )
    {
        return Math.max(
                minimum,
                Math.min(
                        maximum,
                        value
                )
        );
    }

    private static String formatCurrentTime()
    {
        return LocalTime
                .now()
                .format(
                        TIME_FORMAT
                );
    }
}
