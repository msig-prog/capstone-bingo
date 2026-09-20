package com.capstoneclanbingo;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.AbstractBorder;

public class CapstoneClanBingoBoardWindow extends JDialog
{
    private static final int PREFERRED_WIDTH = 650;
    private static final int REFRESH_INTERVAL_MS = 60_000;

    /*
     * OSRS-inspired popup colors.
     *
     * These are intentionally local to this window instead of changing
     * Swing's global UI defaults, so the rest of RuneLite is unaffected.
     */
    private static final Color OSRS_MENU_BACKGROUND =
            new Color(24, 20, 15);

    private static final Color OSRS_MENU_HOVER_BACKGROUND =
            new Color(52, 43, 31);

    private static final Color OSRS_MENU_BORDER =
            new Color(92, 71, 39);

    private static final Color OSRS_MENU_SEPARATOR =
            new Color(58, 51, 42);

    private static final Color OSRS_GOLD =
            new Color(236, 190, 90);

    private static final Color OSRS_GOLD_STRONG =
            new Color(224, 165, 54);

    private static final Color OSRS_TEXT =
            new Color(244, 238, 226);

    private static final Color OSRS_MUTED_TEXT =
            new Color(181, 169, 148);

    private static final Color OSRS_DISABLED_TEXT =
            new Color(108, 99, 84);

    private static final Color OSRS_CARD =
            new Color(26, 22, 17);

    private static final Color OSRS_CARD_SOFT =
            new Color(32, 27, 21);

    private static final Color OSRS_INPUT =
            new Color(12, 10, 8);

    private static final Color OSRS_SUBTLE_BORDER =
            new Color(65, 56, 45);

    private static final Color OSRS_RED =
            new Color(218, 105, 92);

    /*
     * Match the website typography more closely. The website uses a modern
     * system sans for controls and Georgia for display headings. Segoe UI is
     * the Windows system face and Java will gracefully substitute on other OSes.
     */
    private static final Font WEB_BODY_FONT =
            new Font("Segoe UI", Font.PLAIN, 13);

    private static final Font WEB_BODY_BOLD_FONT =
            new Font("Segoe UI", Font.BOLD, 13);

    private static final Font WEB_SMALL_BOLD_FONT =
            new Font("Segoe UI", Font.BOLD, 10);

    private static final Font WEB_TITLE_FONT =
            new Font("Georgia", Font.BOLD, 22);

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm:ss a");

    private final String teamCode;
    private final String playerName;
    private final CapstoneClanBingoApiClient apiClient;
    private final CapstoneClanBingoBoardManifest manifest;
    private final BufferedImage boardImage;

    private final Map<Integer, CapstoneClanBingoApiClient.TileData> tiles =
            new HashMap<>();

    private final Map<Integer, Boolean> tileBusy =
            new HashMap<>();

    private BingoBoardPanel boardPanel;
    private final Timer syncTimer;

    private boolean refreshInProgress;
    private SyncState syncState = SyncState.SYNCED;
    private String lastSyncTime = formatCurrentTime();

    private enum SyncState
    {
        SYNCED,
        SYNCING,
        OFFLINE
    }

    private enum MenuTone
    {
        NORMAL,
        SUCCESS,
        AID,
        DANGER
    }

    /**
     * Small rounded border used by the web-style card controls.
     */
    private static class RoundedBorder extends AbstractBorder
    {
        private final Color color;
        private final int radius;
        private final int thickness;

        RoundedBorder(
                Color color,
                int radius,
                int thickness
        )
        {
            this.color = color;
            this.radius = radius;
            this.thickness = thickness;
        }

        @Override
        public void paintBorder(
                Component component,
                Graphics graphics,
                int x,
                int y,
                int width,
                int height
        )
        {
            Graphics2D g2 =
                    (Graphics2D) graphics.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(color);
            g2.setStroke(
                    new BasicStroke(thickness)
            );

            int inset =
                    Math.max(1, thickness);

            g2.drawRoundRect(
                    x + inset / 2,
                    y + inset / 2,
                    width - inset,
                    height - inset,
                    radius,
                    radius
            );

            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(
                Component component
        )
        {
            return new Insets(
                    thickness + 1,
                    thickness + 1,
                    thickness + 1,
                    thickness + 1
            );
        }
    }

    private static class RoundedPanel extends JPanel
    {
        private final Color fillColor;
        private final Color lineColor;
        private final int radius;

        RoundedPanel(
                Color fillColor,
                Color lineColor,
                int radius
        )
        {
            this.fillColor = fillColor;
            this.lineColor = lineColor;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(
                Graphics graphics
        )
        {
            Graphics2D g2 =
                    (Graphics2D) graphics.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(fillColor);
            g2.fillRoundRect(
                    0,
                    0,
                    getWidth() - 1,
                    getHeight() - 1,
                    radius,
                    radius
            );

            if (lineColor != null)
            {
                g2.setColor(lineColor);
                g2.drawRoundRect(
                        0,
                        0,
                        getWidth() - 1,
                        getHeight() - 1,
                        radius,
                        radius
                );
            }

            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class RoundedPopupMenu extends JPopupMenu
    {
        private static final int POPUP_WIDTH = 286;
        private static final int INNER_WIDTH = 270;

        private final RoundedPanel content;

        RoundedPopupMenu()
        {
            setOpaque(false);
            setBorder(
                    BorderFactory.createEmptyBorder()
            );
            setLayout(new BorderLayout());

            content =
                    new RoundedPanel(
                            OSRS_MENU_BACKGROUND,
                            OSRS_MENU_BORDER,
                            12
                    );

            content.setLayout(
                    new BoxLayout(
                            content,
                            BoxLayout.Y_AXIS
                    )
            );

            content.setBorder(
                    BorderFactory.createEmptyBorder(
                            7,
                            7,
                            7,
                            7
                    )
            );

            add(
                    content,
                    BorderLayout.CENTER
            );
        }

        void addContent(
                Component component
        )
        {
            component.setMaximumSize(
                    new Dimension(
                            INNER_WIDTH,
                            component.getPreferredSize().height
                    )
            );

            if (component instanceof JComponent)
            {
                ((JComponent) component).setAlignmentX(
                        Component.LEFT_ALIGNMENT
                );
            }

            content.add(component);
        }

        void addGap(
                int height
        )
        {
            content.add(
                    Box.createVerticalStrut(height)
            );
        }

        @Override
        public Dimension getPreferredSize()
        {
            Dimension preferred =
                    super.getPreferredSize();

            return new Dimension(
                    POPUP_WIDTH,
                    preferred.height
            );
        }
    }

    private static class WebMenuButton extends JButton
    {
        WebMenuButton(
                String text
        )
        {
            super(text);

            setFont(WEB_BODY_FONT);
            setHorizontalAlignment(
                    SwingConstants.LEFT
            );
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(
                    Cursor.getPredefinedCursor(
                            Cursor.HAND_CURSOR
                    )
            );
            setBorder(
                    BorderFactory.createEmptyBorder(
                            0,
                            10,
                            0,
                            10
                    )
            );
            setPreferredSize(
                    new Dimension(
                            270,
                            36
                    )
            );
            setMinimumSize(getPreferredSize());
            setMaximumSize(getPreferredSize());
        }

        @Override
        protected void paintComponent(
                Graphics graphics
        )
        {
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

            if (
                    isEnabled()
                            && getModel().isRollover()
            )
            {
                g2.setColor(
                        new Color(
                                214,
                                168,
                                79,
                                33
                        )
                );
                g2.fillRoundRect(
                        0,
                        1,
                        getWidth(),
                        getHeight() - 2,
                        8,
                        8
                );
            }

            g2.setFont(getFont());
            g2.setColor(
                    isEnabled()
                            ? OSRS_TEXT
                            : new Color(111, 103, 90)
            );

            int baseline =
                    (
                            getHeight()
                                    - g2.getFontMetrics().getHeight()
                    ) / 2
                            + g2.getFontMetrics().getAscent();

            g2.drawString(
                    getText(),
                    10,
                    baseline
            );

            g2.dispose();
        }
    }

    private static class WebStyleButton extends JButton
    {
        private final boolean primary;

        WebStyleButton(
                String text,
                boolean primary
        )
        {
            super(text);
            this.primary = primary;

            setFont(WEB_BODY_BOLD_FONT);

            setForeground(
                    primary
                            ? new Color(24, 16, 7)
                            : OSRS_TEXT
            );

            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(
                    Cursor.getPredefinedCursor(
                            Cursor.HAND_CURSOR
                    )
            );
            setMargin(
                    new Insets(
                            9,
                            13,
                            9,
                            13
                    )
            );
        }

        @Override
        protected void paintComponent(
                Graphics graphics
        )
        {
            Graphics2D g2 =
                    (Graphics2D) graphics.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            Color fill;
            Color border;

            if (!isEnabled())
            {
                fill = new Color(31, 28, 23);
                border = OSRS_SUBTLE_BORDER;
            }
            else if (primary)
            {
                fill = getModel().isRollover()
                        ? new Color(241, 190, 76)
                        : OSRS_GOLD_STRONG;
                border = new Color(245, 199, 101);
            }
            else
            {
                fill = getModel().isRollover()
                        ? new Color(52, 45, 35)
                        : new Color(37, 32, 26);
                border = new Color(84, 70, 48);
            }

            g2.setColor(fill);
            g2.fillRoundRect(
                    0,
                    0,
                    getWidth() - 1,
                    getHeight() - 1,
                    10,
                    10
            );

            g2.setColor(border);
            g2.drawRoundRect(
                    0,
                    0,
                    getWidth() - 1,
                    getHeight() - 1,
                    10,
                    10
            );

            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class HintTextField extends JTextField
    {
        private final String hint;

        HintTextField(
                String hint
        )
        {
            this.hint = hint;
        }

        @Override
        protected void paintComponent(
                Graphics graphics
        )
        {
            super.paintComponent(graphics);

            if (
                    getText().isEmpty()
                            && !isFocusOwner()
            )
            {
                Graphics2D g2 =
                        (Graphics2D) graphics.create();

                g2.setColor(OSRS_MUTED_TEXT);
                g2.setFont(getFont());

                Insets insets =
                        getInsets();

                g2.drawString(
                        hint,
                        insets.left + 2,
                        getHeight() / 2
                                + getFontMetrics(
                                getFont()
                        ).getAscent() / 2
                                - 2
                );

                g2.dispose();
            }
        }
    }

    public CapstoneClanBingoBoardWindow(
            Window owner,
            String teamCode,
            String playerName,
            CapstoneClanBingoApiClient apiClient,
            CapstoneClanBingoBoardManifest manifest,
            BufferedImage boardImage,
            CapstoneClanBingoApiClient.BoardResponse initialBoard
    )
    {
        super(owner);

        this.teamCode = teamCode;
        this.playerName = playerName;
        this.apiClient = apiClient;
        this.manifest = manifest;
        this.boardImage = boardImage;

        setUndecorated(true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(false);

        initializeOpenTiles();
        applyBoard(initialBoard);

        int displayWidth =
                calculateDisplayWidth(owner);

        boardPanel =
                new BingoBoardPanel(
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

    private void initializeOpenTiles()
    {
        tiles.clear();

        for (
                CapstoneClanBingoBoardManifest.TileDefinition definition
                : manifest.tiles
        )
        {
            tiles.put(
                    definition.tileNumber,
                    createOpenTile(
                            definition.tileNumber
                    )
            );
        }
    }

    public void applyBoard(
            CapstoneClanBingoApiClient.BoardResponse board
    )
    {
        initializeOpenTiles();

        if (
                board != null
                        && board.tiles != null
        )
        {
            for (
                    CapstoneClanBingoApiClient.TileData incoming
                    : board.tiles
            )
            {
                if (
                        incoming != null
                                && tiles.containsKey(
                                incoming.tileNumber
                        )
                )
                {
                    tiles.put(
                            incoming.tileNumber,
                            incoming.copy()
                    );
                }
            }
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

        boardPanel.repaint();

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
        for (Boolean busy : tileBusy.values())
        {
            if (Boolean.TRUE.equals(busy))
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
        int width = PREFERRED_WIDTH;

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
                                manifest.width
                                        / (double) manifest.height
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

    private boolean isOwner(
            CapstoneClanBingoApiClient.TileData tile
    )
    {
        return samePlayer(
                tile.owner,
                playerName
        );
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
            if (samePlayer(helper, player))
            {
                return true;
            }
        }

        return false;
    }

    private String tileName(
            int tileNumber
    )
    {
        for (
                CapstoneClanBingoBoardManifest.TileDefinition definition
                : manifest.tiles
        )
        {
            if (
                    definition.tileNumber
                            == tileNumber
            )
            {
                return definition.name;
            }
        }

        return "Tile " + tileNumber;
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
                int displayWidth
        )
        {
            this.displayWidth =
                    displayWidth;

            displayHeight =
                    (int) Math.round(
                            manifest.height
                                    * (
                                    displayWidth
                                            / (double) manifest.width
                            )
                    );

            scaleX =
                    displayWidth
                            / (double) manifest.width;

            scaleY =
                    displayHeight
                            / (double) manifest.height;

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

            addMouseListener(new MouseAdapter()
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

                    Integer tileNumber =
                            getTileNumber(
                                    event.getX(),
                                    event.getY()
                            );

                    if (tileNumber == null)
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

                    Integer tileNumber =
                            getTileNumber(
                                    event.getX(),
                                    event.getY()
                            );

                    if (tileNumber == null)
                    {
                        return;
                    }

                    handleLeftClick(tileNumber);
                }
            });

            addMouseMotionListener(new MouseMotionAdapter()
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

                    Point current =
                            event.getLocationOnScreen();

                    /*
                     * IMPORTANT:
                     * This must move the OUTER JDialog, not this JPanel.
                     * Calling setLocation(...) unqualified here moves the
                     * board panel inside the dialog and leaves a gray shell
                     * behind.
                     */
                    CapstoneClanBingoBoardWindow.this.setLocation(
                            dragStartWindow.x
                                    + current.x
                                    - dragStartScreen.x,
                            dragStartWindow.y
                                    + current.y
                                    - dragStartScreen.y
                    );
                }
            });
        }

        private Integer getTileNumber(
                int mouseX,
                int mouseY
        )
        {
            int logicalX =
                    (int) (
                            mouseX / scaleX
                    );

            int logicalY =
                    (int) (
                            mouseY / scaleY
                    );

            for (
                    CapstoneClanBingoBoardManifest.TileDefinition definition
                    : manifest.tiles
            )
            {
                if (
                        definition.contains(
                                logicalX,
                                logicalY
                        )
                )
                {
                    return definition.tileNumber;
                }
            }

            return null;
        }

        private void handleLeftClick(
                int tileNumber
        )
        {
            if (isBusy(tileNumber))
            {
                return;
            }

            CapstoneClanBingoApiClient.TileData current =
                    tiles.get(tileNumber);

            if (current == null)
            {
                return;
            }

            if (current.aidRequested)
            {
                showAidRoster(tileNumber);
                return;
            }

            if (
                    !"OPEN".equals(current.status)
                            && !isOwner(current)
            )
            {
                JOptionPane.showMessageDialog(
                        this,
                        current.owner
                                + " owns this tile.\n"
                                + "Right-click and choose Overwrite Tile Owner "
                                + "to take it over.",
                        "Tile Owned",
                        JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }

            CapstoneClanBingoApiClient.TileData proposed =
                    current.copy();

            if ("OPEN".equals(current.status))
            {
                proposed.status = "OWNED";
                proposed.owner = playerName;
                proposed.progress = null;
                proposed.aidRequested = false;
                proposed.aidNeeded = null;
                proposed.helpers = new ArrayList<>();
            }
            else if ("OWNED".equals(current.status))
            {
                proposed.status = "COMPLETE";
                proposed.progress = null;
                proposed.aidRequested = false;
                proposed.aidNeeded = null;
            }
            else
            {
                proposed =
                        createOpenTile(tileNumber);
            }

            saveProposedTile(
                    tileNumber,
                    proposed,
                    false
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

            Integer tileNumber =
                    getTileNumber(
                            event.getX(),
                            event.getY()
                    );

            if (
                    tileNumber == null
                            || isBusy(tileNumber)
            )
            {
                return;
            }

            showTileMenu(
                    event,
                    tileNumber
            );
        }

        private void showTileMenu(
                MouseEvent event,
                int tileNumber
        )
        {
            CapstoneClanBingoApiClient.TileData current =
                    tiles.get(tileNumber);

            if (current == null)
            {
                return;
            }

            RoundedPopupMenu menu =
                    new RoundedPopupMenu();

            boolean open =
                    "OPEN".equals(current.status);

            boolean owner =
                    isOwner(current);

            boolean complete =
                    "COMPLETE".equals(current.status);

            boolean ownedByOther =
                    !open
                            && current.owner != null
                            && !owner;

            addOsrsMenuHeader(
                    menu,
                    tileNumber
            );

            WebMenuButton markOpen =
                    createOsrsMenuItem(
                            menu,
                            "Mark Open",
                            owner,
                            () ->
                                    saveProposedTile(
                                            tileNumber,
                                            createOpenTile(tileNumber),
                                            false
                                    )
                    );

            menu.addContent(markOpen);

            if (open)
            {
                WebMenuButton claim =
                        createOsrsMenuItem(
                                menu,
                                "Claim for " + playerName,
                                true,
                                () ->
                                {
                                    CapstoneClanBingoApiClient.TileData proposed =
                                            current.copy();

                                    proposed.status = "OWNED";
                                    proposed.owner = playerName;
                                    proposed.progress = null;
                                    proposed.aidRequested = false;
                                    proposed.aidNeeded = null;
                                    proposed.helpers = new ArrayList<>();

                                    saveProposedTile(
                                            tileNumber,
                                            proposed,
                                            false
                                    );
                                }
                        );

                menu.addContent(claim);
            }

            if (ownedByOther)
            {
                WebMenuButton overwrite =
                        createOsrsMenuItem(
                                menu,
                                "Overwrite Tile Owner",
                                true,
                                () -> overwriteOwner(tileNumber)
                        );

                menu.addContent(overwrite);
            }

            WebMenuButton markComplete =
                    createOsrsMenuItem(
                            menu,
                            "Mark Complete",
                            owner,
                            () ->
                            {
                                CapstoneClanBingoApiClient.TileData proposed =
                                        current.copy();

                                proposed.status = "COMPLETE";
                                proposed.progress = null;
                                proposed.aidRequested = false;
                                proposed.aidNeeded = null;

                                saveProposedTile(
                                        tileNumber,
                                        proposed,
                                        false
                                );
                            }
                    );

            menu.addContent(markComplete);
            addOsrsSeparator(menu);

            WebMenuButton progress =
                    createOsrsMenuItem(
                            menu,
                            "Set Progress...",
                            owner,
                            () -> setProgress(tileNumber)
                    );

            menu.addContent(progress);
            addOsrsSeparator(menu);

            WebMenuButton aid =
                    createOsrsMenuItem(
                            menu,
                            current.aidRequested
                                    ? "Cancel Aid Request"
                                    : "Request Aid",
                            owner && !complete,
                            () ->
                            {
                                CapstoneClanBingoApiClient.TileData proposed =
                                        current.copy();

                                proposed.aidRequested =
                                        !current.aidRequested;

                                if (!proposed.aidRequested)
                                {
                                    proposed.aidNeeded = null;
                                }

                                saveProposedTile(
                                        tileNumber,
                                        proposed,
                                        false
                                );
                            }
                    );

            menu.addContent(aid);

            if (
                    current.aidRequested
                            && !owner
            )
            {
                boolean joined =
                        isHelper(
                                current,
                                playerName
                        );

                boolean full =
                        current.aidNeeded != null
                                && current.helpers.size()
                                >= current.aidNeeded
                                && !joined;

                WebMenuButton joinAid =
                        createOsrsMenuItem(
                                menu,
                                joined
                                        ? "Leave Aid"
                                        : "Join Aid",
                                !full,
                                () -> joinOrLeaveAid(tileNumber)
                        );

                menu.addContent(joinAid);
            }

            menu.show(
                    this,
                    event.getX(),
                    event.getY()
            );
        }

        private void addOsrsMenuHeader(
                RoundedPopupMenu menu,
                int tileNumber
        )
        {
            JPanel header =
                    new JPanel();

            header.setLayout(
                    new BoxLayout(
                            header,
                            BoxLayout.Y_AXIS
                    )
            );

            header.setOpaque(false);
            header.setBorder(
                    BorderFactory.createEmptyBorder(
                            7,
                            10,
                            9,
                            10
                    )
            );
            header.setPreferredSize(
                    new Dimension(
                            270,
                            53
                    )
            );

            JLabel number =
                    new JLabel(
                            "TILE " + tileNumber
                    );

            number.setAlignmentX(
                    Component.LEFT_ALIGNMENT
            );
            number.setForeground(OSRS_GOLD);
            number.setFont(WEB_SMALL_BOLD_FONT);

            JLabel name =
                    new JLabel(
                            tileName(tileNumber)
                    );

            name.setAlignmentX(
                    Component.LEFT_ALIGNMENT
            );
            name.setForeground(OSRS_TEXT);
            name.setFont(
                    WEB_BODY_BOLD_FONT.deriveFont(15f)
            );

            header.add(number);
            header.add(
                    Box.createVerticalStrut(3)
            );
            header.add(name);

            menu.addContent(header);
            addOsrsSeparator(menu);
        }

        private WebMenuButton createOsrsMenuItem(
                RoundedPopupMenu menu,
                String text,
                boolean enabled,
                Runnable action
        )
        {
            WebMenuButton item =
                    new WebMenuButton(text);

            item.setEnabled(enabled);
            item.addActionListener(e ->
            {
                menu.setVisible(false);

                if (item.isEnabled())
                {
                    action.run();
                }
            });

            return item;
        }

        private void addOsrsSeparator(
                RoundedPopupMenu menu
        )
        {
            JPanel separatorWrap =
                    new JPanel(
                            new BorderLayout()
                    );

            separatorWrap.setOpaque(false);
            separatorWrap.setBorder(
                    BorderFactory.createEmptyBorder(
                            5,
                            4,
                            5,
                            4
                    )
            );
            separatorWrap.setPreferredSize(
                    new Dimension(
                            270,
                            11
                    )
            );

            JPanel line =
                    new JPanel();

            line.setOpaque(true);
            line.setBackground(
                    OSRS_MENU_SEPARATOR
            );
            line.setPreferredSize(
                    new Dimension(
                            262,
                            1
                    )
            );

            separatorWrap.add(
                    line,
                    BorderLayout.CENTER
            );

            menu.addContent(separatorWrap);
        }

        private void overwriteOwner(
                int tileNumber
        )
        {
            CapstoneClanBingoApiClient.TileData current =
                    tiles.get(tileNumber);

            if (current == null)
            {
                return;
            }

            int answer =
                    JOptionPane.showConfirmDialog(
                            this,
                            "Transfer ownership of "
                                    + tileName(tileNumber)
                                    + " from "
                                    + current.owner
                                    + " to "
                                    + playerName
                                    + "?\n\nProgress, aid status, and helpers "
                                    + "will be kept.",
                            "Overwrite Tile Owner",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE
                    );

            if (
                    answer
                            != JOptionPane.YES_OPTION
            )
            {
                return;
            }

            CapstoneClanBingoApiClient.TileData proposed =
                    current.copy();

            proposed.owner =
                    playerName;

            removeHelper(
                    proposed,
                    playerName
            );

            saveProposedTile(
                    tileNumber,
                    proposed,
                    true
            );
        }

        private void setProgress(
                int tileNumber
        )
        {
            CapstoneClanBingoApiClient.TileData current =
                    tiles.get(tileNumber);

            if (
                    current == null
                            || !isOwner(current)
            )
            {
                return;
            }

            JDialog dialog =
                    new JDialog(
                            CapstoneClanBingoBoardWindow.this,
                            true
                    );

            dialog.setUndecorated(true);
            dialog.setResizable(false);
            dialog.setBackground(
                    new Color(0, 0, 0, 0)
            );
            dialog.setDefaultCloseOperation(
                    JDialog.DISPOSE_ON_CLOSE
            );

            RoundedPanel card =
                    new RoundedPanel(
                            OSRS_CARD,
                            OSRS_MENU_BORDER,
                            16
                    );

            card.setLayout(
                    new BoxLayout(
                            card,
                            BoxLayout.Y_AXIS
                    )
            );
            card.setBorder(
                    BorderFactory.createEmptyBorder(
                            22,
                            22,
                            20,
                            22
                    )
            );

            JLabel kicker =
                    new JLabel("UPDATE PROGRESS");

            kicker.setForeground(OSRS_GOLD);
            kicker.setFont(
                    WEB_SMALL_BOLD_FONT.deriveFont(11f)
            );
            kicker.setAlignmentX(
                    Component.LEFT_ALIGNMENT
            );

            JLabel title =
                    new JLabel("Set tile progress");

            title.setForeground(OSRS_TEXT);
            title.setFont(WEB_TITLE_FONT);
            title.setAlignmentX(
                    Component.LEFT_ALIGNMENT
            );

            JLabel help =
                    new JLabel(
                            "<html>Enter a percentage such as <b>70%</b> or a fraction such as <b>7/10</b>.</html>"
                    );

            help.setForeground(OSRS_MUTED_TEXT);
            help.setFont(
                    WEB_BODY_FONT.deriveFont(12f)
            );
            help.setAlignmentX(
                    Component.LEFT_ALIGNMENT
            );

            JLabel fieldLabel =
                    createAidSectionLabel("PROGRESS");

            HintTextField progressField =
                    new HintTextField("7/10");

            progressField.setText(
                    current.progress == null
                            ? ""
                            : current.progress
            );
            progressField.setBackground(OSRS_INPUT);
            progressField.setForeground(OSRS_TEXT);
            progressField.setCaretColor(OSRS_GOLD);
            progressField.setFont(
                    WEB_BODY_FONT.deriveFont(14f)
            );
            progressField.setBorder(
                    BorderFactory.createCompoundBorder(
                            new RoundedBorder(
                                    OSRS_SUBTLE_BORDER,
                                    10,
                                    1
                            ),
                            BorderFactory.createEmptyBorder(
                                    8,
                                    10,
                                    8,
                                    10
                            )
                    )
            );
            progressField.setAlignmentX(
                    Component.LEFT_ALIGNMENT
            );
            progressField.setMaximumSize(
                    new Dimension(
                            Integer.MAX_VALUE,
                            42
                    )
            );

            JLabel validation =
                    new JLabel(" ");

            validation.setForeground(OSRS_RED);
            validation.setFont(
                    WEB_BODY_BOLD_FONT.deriveFont(11f)
            );
            validation.setAlignmentX(
                    Component.LEFT_ALIGNMENT
            );

            card.add(kicker);
            card.add(
                    Box.createVerticalStrut(4)
            );
            card.add(title);
            card.add(
                    Box.createVerticalStrut(10)
            );
            card.add(help);
            card.add(
                    Box.createVerticalStrut(17)
            );
            card.add(fieldLabel);
            card.add(
                    Box.createVerticalStrut(7)
            );
            card.add(progressField);
            card.add(validation);
            card.add(
                    Box.createVerticalStrut(10)
            );

            JPanel actions =
                    new JPanel(
                            new FlowLayout(
                                    FlowLayout.RIGHT,
                                    9,
                                    0
                            )
                    );

            actions.setOpaque(false);
            actions.setAlignmentX(
                    Component.LEFT_ALIGNMENT
            );
            actions.setMaximumSize(
                    new Dimension(
                            Integer.MAX_VALUE,
                            44
                    )
            );

            WebStyleButton cancel =
                    new WebStyleButton(
                            "Cancel",
                            false
                    );

            cancel.addActionListener(e ->
                    dialog.dispose()
            );

            WebStyleButton save =
                    new WebStyleButton(
                            "Save Progress",
                            true
                    );

            save.addActionListener(e ->
            {
                String normalized =
                        normalizeProgress(
                                progressField
                                        .getText()
                        );

                if (normalized == null)
                {
                    validation.setText(
                            "Enter progress like 70% or 7/10."
                    );
                    return;
                }

                CapstoneClanBingoApiClient.TileData proposed =
                        current.copy();

                proposed.status = "OWNED";
                proposed.progress = normalized;

                dialog.dispose();
                saveProposedTile(
                        tileNumber,
                        proposed,
                        false
                );
            });

            actions.add(cancel);
            actions.add(save);
            card.add(actions);

            dialog.setContentPane(card);
            dialog.pack();

            Dimension packed =
                    dialog.getPreferredSize();

            dialog.setSize(
                    new Dimension(
                            430,
                            packed.height
                    )
            );
            dialog.setLocationRelativeTo(
                    CapstoneClanBingoBoardWindow.this
            );
            dialog.setVisible(true);
        }

        private void joinOrLeaveAid(
                int tileNumber
        )
        {
            CapstoneClanBingoApiClient.TileData current =
                    tiles.get(tileNumber);

            if (
                    current == null
                            || !current.aidRequested
                            || isOwner(current)
            )
            {
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
                if (
                        proposed.aidNeeded != null
                                && proposed.helpers.size()
                                >= proposed.aidNeeded
                )
                {
                    JOptionPane.showMessageDialog(
                            this,
                            "The requested helper roster is already full.",
                            "Aid Roster Full",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    return;
                }

                proposed.helpers.add(
                        playerName
                );
            }

            saveProposedTile(
                    tileNumber,
                    proposed,
                    false
            );
        }

        private void showAidRoster(
                int tileNumber
        )
        {
            CapstoneClanBingoApiClient.TileData current =
                    tiles.get(tileNumber);

            if (
                    current == null
                            || !current.aidRequested
            )
            {
                return;
            }

            boolean owner =
                    isOwner(current);

            JDialog dialog =
                    new JDialog(
                            CapstoneClanBingoBoardWindow.this,
                            true
                    );

            dialog.setUndecorated(true);
            dialog.setResizable(false);
            dialog.setBackground(
                    new Color(0, 0, 0, 0)
            );
            dialog.setDefaultCloseOperation(
                    JDialog.DISPOSE_ON_CLOSE
            );

            RoundedPanel card =
                    new RoundedPanel(
                            OSRS_CARD,
                            OSRS_MENU_BORDER,
                            18
                    );

            card.setLayout(
                    new BoxLayout(
                            card,
                            BoxLayout.Y_AXIS
                    )
            );

            card.setBorder(
                    BorderFactory.createEmptyBorder(
                            22,
                            21,
                            22,
                            21
                    )
            );

            JLabel kicker =
                    new JLabel("AID REQUEST");

            kicker.setAlignmentX(
                    Component.LEFT_ALIGNMENT
            );
            kicker.setForeground(OSRS_GOLD);
            kicker.setFont(
                    WEB_SMALL_BOLD_FONT.deriveFont(11f)
            );

            JLabel title =
                    new JLabel(
                            tileName(tileNumber)
                    );

            title.setAlignmentX(
                    Component.LEFT_ALIGNMENT
            );
            title.setForeground(OSRS_TEXT);
            title.setFont(WEB_TITLE_FONT);

            card.add(kicker);
            card.add(
                    Box.createVerticalStrut(4)
            );
            card.add(title);
            card.add(
                    Box.createVerticalStrut(16)
            );

            JPanel summary =
                    new JPanel(
                            new GridLayout(
                                    1,
                                    2,
                                    10,
                                    0
                            )
                    );

            summary.setOpaque(false);
            summary.setAlignmentX(
                    Component.LEFT_ALIGNMENT
            );
            summary.setMaximumSize(
                    new Dimension(
                            Integer.MAX_VALUE,
                            66
                    )
            );

            int helperCount =
                    current.helpers == null
                            ? 0
                            : current.helpers.size();

            String rosterText =
                    current.aidNeeded == null
                            ? helperCount
                            + " helper"
                            + (helperCount == 1 ? "" : "s")
                            : helperCount
                            + " / "
                            + current.aidNeeded
                            + " helpers";

            summary.add(
                    createAidSummaryCard(
                            "OWNER",
                            current.owner == null
                                    ? "No owner"
                                    : current.owner
                    )
            );

            summary.add(
                    createAidSummaryCard(
                            "ROSTER",
                            rosterText
                    )
            );

            card.add(summary);
            card.add(
                    Box.createVerticalStrut(17)
            );

            JLabel joinedLabel =
                    createAidSectionLabel(
                            "CURRENTLY JOINED"
                    );

            card.add(joinedLabel);
            card.add(
                    Box.createVerticalStrut(7)
            );

            JPanel roster =
                    new JPanel();

            roster.setLayout(
                    new BoxLayout(
                            roster,
                            BoxLayout.Y_AXIS
                    )
            );
            roster.setOpaque(false);

            if (current.owner != null)
            {
                roster.add(
                        createAidRosterRow(
                                current.owner + " — Owner",
                                true
                        )
                );
                roster.add(
                        Box.createVerticalStrut(6)
                );
            }

            if (
                    current.helpers == null
                            || current.helpers.isEmpty()
            )
            {
                roster.add(
                        createAidRosterRow(
                                "No helpers joined yet.",
                                false
                        )
                );
            }
            else
            {
                for (String helper : current.helpers)
                {
                    roster.add(
                            createAidRosterRow(
                                    helper,
                                    false
                            )
                    );
                    roster.add(
                            Box.createVerticalStrut(6)
                    );
                }
            }

            int rosterRows =
                    1
                            + (
                            current.helpers == null
                                    || current.helpers.isEmpty()
                                    ? 1
                                    : current.helpers.size()
                    );

            roster.setAlignmentX(
                    Component.LEFT_ALIGNMENT
            );

            if (rosterRows <= 4)
            {
                roster.setMaximumSize(
                        new Dimension(
                                Integer.MAX_VALUE,
                                rosterRows * 46
                        )
                );
                card.add(roster);
            }
            else
            {
                JScrollPane rosterScroll =
                        new JScrollPane(roster);

                rosterScroll.setBorder(null);
                rosterScroll.setOpaque(false);
                rosterScroll.getViewport()
                        .setOpaque(false);
                rosterScroll.setHorizontalScrollBarPolicy(
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                );
                rosterScroll.setVerticalScrollBarPolicy(
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                );
                rosterScroll.setAlignmentX(
                        Component.LEFT_ALIGNMENT
                );
                rosterScroll.setPreferredSize(
                        new Dimension(
                                388,
                                178
                        )
                );
                rosterScroll.setMaximumSize(
                        new Dimension(
                                Integer.MAX_VALUE,
                                178
                        )
                );
                rosterScroll.getVerticalScrollBar()
                        .setUnitIncrement(16);

                card.add(rosterScroll);
            }

            HintTextField targetField = null;
            JLabel validation = null;

            if (owner)
            {
                card.add(
                        Box.createVerticalStrut(17)
                );

                JLabel targetLabel =
                        createAidSectionLabel(
                                "HELPERS NEEDED (OPTIONAL)"
                        );

                card.add(targetLabel);
                card.add(
                        Box.createVerticalStrut(7)
                );

                targetField =
                        new HintTextField(
                                "e.g. 3"
                        );

                targetField.setText(
                        current.aidNeeded == null
                                ? ""
                                : String.valueOf(
                                current.aidNeeded
                        )
                );
                targetField.setBackground(OSRS_INPUT);
                targetField.setForeground(OSRS_TEXT);
                targetField.setCaretColor(OSRS_GOLD);
                targetField.setFont(
                        WEB_BODY_FONT.deriveFont(14f)
                );
                targetField.setBorder(
                        BorderFactory.createCompoundBorder(
                                new RoundedBorder(
                                        OSRS_GOLD_STRONG,
                                        12,
                                        1
                                ),
                                BorderFactory.createEmptyBorder(
                                        8,
                                        10,
                                        8,
                                        10
                                )
                        )
                );
                targetField.setAlignmentX(
                        Component.LEFT_ALIGNMENT
                );
                targetField.setMaximumSize(
                        new Dimension(
                                Integer.MAX_VALUE,
                                43
                        )
                );

                card.add(targetField);
                card.add(
                        Box.createVerticalStrut(5)
                );

                JLabel help =
                        new JLabel(
                                "<html>Set an optional helper target, or leave it blank for an<br>open-ended aid request.</html>"
                        );

                help.setForeground(OSRS_MUTED_TEXT);
                help.setFont(
                        WEB_BODY_FONT.deriveFont(12f)
                );
                help.setAlignmentX(
                        Component.LEFT_ALIGNMENT
                );

                card.add(help);

                validation =
                        new JLabel(" ");

                validation.setForeground(OSRS_RED);
                validation.setFont(
                        WEB_BODY_BOLD_FONT.deriveFont(11f)
                );
                validation.setAlignmentX(
                        Component.LEFT_ALIGNMENT
                );

                card.add(validation);
            }

            card.add(
                    Box.createVerticalStrut(18)
            );

            JPanel actions =
                    new JPanel(
                            new FlowLayout(
                                    FlowLayout.RIGHT,
                                    9,
                                    0
                            )
                    );

            actions.setOpaque(false);
            actions.setAlignmentX(
                    Component.LEFT_ALIGNMENT
            );
            actions.setMaximumSize(
                    new Dimension(
                            Integer.MAX_VALUE,
                            44
                    )
            );

            WebStyleButton close =
                    new WebStyleButton(
                            "Close",
                            false
                    );

            close.addActionListener(e ->
                    dialog.dispose()
            );

            actions.add(close);

            if (owner)
            {
                WebStyleButton end =
                        new WebStyleButton(
                                "End Aid Request",
                                false
                        );

                end.addActionListener(e ->
                {
                    CapstoneClanBingoApiClient.TileData proposed =
                            current.copy();

                    proposed.aidRequested = false;
                    proposed.aidNeeded = null;

                    dialog.dispose();
                    saveProposedTile(
                            tileNumber,
                            proposed,
                            false
                    );
                });

                actions.add(end);

                WebStyleButton save =
                        new WebStyleButton(
                                "Save Aid Settings",
                                true
                        );

                final HintTextField finalTargetField =
                        targetField;

                final JLabel finalValidation =
                        validation;

                save.addActionListener(e ->
                {
                    String raw =
                            finalTargetField
                                    .getText()
                                    .trim();

                    Integer aidNeeded = null;

                    if (!raw.isEmpty())
                    {
                        try
                        {
                            int parsed =
                                    Integer.parseInt(raw);

                            if (
                                    parsed < 1
                                            || parsed > 10
                            )
                            {
                                throw new NumberFormatException();
                            }

                            aidNeeded = parsed;
                        }
                        catch (NumberFormatException exception)
                        {
                            finalValidation.setText(
                                    "Enter a number from 1 to 10, or leave it blank."
                            );
                            return;
                        }
                    }

                    CapstoneClanBingoApiClient.TileData proposed =
                            current.copy();

                    proposed.aidRequested = true;
                    proposed.aidNeeded = aidNeeded;

                    dialog.dispose();
                    saveProposedTile(
                            tileNumber,
                            proposed,
                            false
                    );
                });

                actions.add(save);
            }
            else
            {
                boolean joined =
                        isHelper(
                                current,
                                playerName
                        );

                boolean full =
                        current.aidNeeded != null
                                && helperCount
                                >= current.aidNeeded
                                && !joined;

                WebStyleButton primary =
                        new WebStyleButton(
                                joined
                                        ? "Leave Aid"
                                        : full
                                        ? "Roster Full"
                                        : "Join Aid",
                                true
                        );

                primary.setEnabled(!full);
                primary.addActionListener(e ->
                {
                    dialog.dispose();
                    joinOrLeaveAid(tileNumber);
                });

                actions.add(primary);
            }

            card.add(actions);

            dialog.setContentPane(card);
            dialog.pack();

            Dimension packed =
                    dialog.getPreferredSize();

            dialog.setSize(
                    new Dimension(
                            430,
                            packed.height
                    )
            );

            dialog.setLocationRelativeTo(
                    CapstoneClanBingoBoardWindow.this
            );
            dialog.setVisible(true);
        }

        private JPanel createAidSummaryCard(
                String label,
                String value
        )
        {
            RoundedPanel panel =
                    new RoundedPanel(
                            OSRS_CARD_SOFT,
                            OSRS_SUBTLE_BORDER,
                            11
                    );

            panel.setLayout(
                    new BoxLayout(
                            panel,
                            BoxLayout.Y_AXIS
                    )
            );

            panel.setBorder(
                    BorderFactory.createEmptyBorder(
                            10,
                            11,
                            9,
                            11
                    )
            );

            JLabel key =
                    createAidSectionLabel(label);

            JLabel val =
                    new JLabel(value);

            val.setForeground(OSRS_TEXT);
            val.setFont(
                    WEB_BODY_BOLD_FONT.deriveFont(14f)
            );
            val.setAlignmentX(
                    Component.LEFT_ALIGNMENT
            );

            panel.add(key);
            panel.add(
                    Box.createVerticalStrut(5)
            );
            panel.add(val);

            return panel;
        }

        private JLabel createAidSectionLabel(
                String text
        )
        {
            JLabel label =
                    new JLabel(text);

            label.setForeground(OSRS_GOLD);
            label.setFont(
                    WEB_SMALL_BOLD_FONT.deriveFont(10f)
            );
            label.setAlignmentX(
                    Component.LEFT_ALIGNMENT
            );

            return label;
        }

        private JPanel createAidRosterRow(
                String text,
                boolean ownerRow
        )
        {
            RoundedPanel row =
                    new RoundedPanel(
                            ownerRow
                                    ? new Color(50, 39, 19)
                                    : OSRS_CARD_SOFT,
                            ownerRow
                                    ? new Color(116, 86, 29)
                                    : OSRS_SUBTLE_BORDER,
                            10
                    );

            row.setLayout(
                    new BorderLayout()
            );
            row.setBorder(
                    BorderFactory.createEmptyBorder(
                            9,
                            11,
                            9,
                            11
                    )
            );
            row.setAlignmentX(
                    Component.LEFT_ALIGNMENT
            );
            row.setPreferredSize(
                    new Dimension(
                            388,
                            40
                    )
            );
            row.setMinimumSize(
                    new Dimension(
                            100,
                            40
                    )
            );
            row.setMaximumSize(
                    new Dimension(
                            Integer.MAX_VALUE,
                            40
                    )
            );

            JLabel name =
                    new JLabel(text);

            name.setForeground(
                    ownerRow
                            ? OSRS_TEXT
                            : OSRS_TEXT
            );
            name.setFont(WEB_BODY_FONT);

            row.add(
                    name,
                    BorderLayout.CENTER
            );

            return row;
        }

        private void saveProposedTile(
                int tileNumber,
                CapstoneClanBingoApiClient.TileData proposed,
                boolean overwriteOwner
        )
        {
            if (isBusy(tileNumber))
            {
                return;
            }

            tileBusy.put(
                    tileNumber,
                    true
            );

            syncState =
                    SyncState.SYNCING;

            repaint();

            apiClient.saveTile(
                    teamCode,
                    playerName,
                    proposed,
                    overwriteOwner,
                    (success, savedTile, message) ->
                    {
                        tileBusy.put(
                                tileNumber,
                                false
                        );

                        if (!success)
                        {
                            syncState =
                                    SyncState.OFFLINE;

                            repaint();

                            JOptionPane.showMessageDialog(
                                    this,
                                    "Your change was NOT saved.\n\n"
                                            + message,
                                    "Bingo Server",
                                    JOptionPane.ERROR_MESSAGE
                            );
                            return;
                        }

                        tiles.put(
                                tileNumber,
                                savedTile.copy()
                        );

                        syncState =
                                SyncState.SYNCED;

                        lastSyncTime =
                                formatCurrentTime();

                        repaint();
                    }
            );
        }

        private boolean isBusy(
                int tileNumber
        )
        {
            return Boolean.TRUE.equals(
                    tileBusy.get(
                            tileNumber
                    )
            );
        }

        @Override
        protected void paintComponent(
                Graphics graphics
        )
        {
            super.paintComponent(
                    graphics
            );

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

            for (
                    CapstoneClanBingoBoardManifest.TileDefinition definition
                    : manifest.tiles
            )
            {
                CapstoneClanBingoApiClient.TileData tile =
                        tiles.get(
                                definition.tileNumber
                        );

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
                        definition,
                        tile
                );
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
                            .stringWidth(
                                    syncText
                            );

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

            if (
                    syncState
                            == SyncState.SYNCED
            )
            {
                g2.setFont(
                        new Font(
                                Font.SANS_SERIF,
                                Font.PLAIN,
                                8
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
                CapstoneClanBingoBoardManifest.TileDefinition definition,
                CapstoneClanBingoApiClient.TileData tile
        )
        {
            int x1 =
                    (int) Math.round(
                            definition.x
                                    * scaleX
                    );

            int x2 =
                    (int) Math.round(
                            (
                                    definition.x
                                            + definition.width
                            )
                                    * scaleX
                    );

            int y1 =
                    (int) Math.round(
                            definition.y
                                    * scaleY
                    );

            int y2 =
                    (int) Math.round(
                            (
                                    definition.y
                                            + definition.height
                            )
                                    * scaleY
                    );

            int width = x2 - x1;
            int height = y2 - y1;

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
                statusText =
                        "COMPLETE";
            }
            else if (aidRequested)
            {
                statusText =
                        tile.aidNeeded == null
                                ? "AID"
                                : "AID "
                                + tile.helpers.size()
                                + "/"
                                + tile.aidNeeded;
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

            drawCenteredTextWithShadow(
                    g2,
                    fitText(
                            g2,
                            statusText,
                            statusWidth - 6
                    ),
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
                names.add(tile.owner);
            }

            if (tile.helpers != null)
            {
                for (String helper : tile.helpers)
                {
                    names.add(
                            "+ " + helper
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

                visibleLines.add(
                        "+"
                                + (
                                names.size()
                                        - maxLines
                                        + 1
                        )
                                + " more"
                );
            }

            int panelHeight =
                    Math.min(
                            8
                                    + visibleLines.size()
                                    * lineHeight,
                            availableHeight
                    );

            int panelX = x1 + 4;
            int panelY = y1 + 4;
            int panelWidth = width - 8;

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

            g2.setFont(
                    new Font(
                            Font.SANS_SERIF,
                            Font.BOLD,
                            clamp(
                                    height / 8,
                                    8,
                                    11
                            )
                    )
            );

            int baseline =
                    panelY + 12;

            for (
                    String line
                    : visibleLines
            )
            {
                drawCenteredTextWithShadow(
                        g2,
                        fitText(
                                g2,
                                line,
                                panelWidth - 8
                        ),
                        panelX,
                        panelWidth,
                        baseline
                );

                baseline += lineHeight;
            }
        }
    }

    private CapstoneClanBingoApiClient.TileData createOpenTile(
            int tileNumber
    )
    {
        CapstoneClanBingoApiClient.TileData tile =
                new CapstoneClanBingoApiClient.TileData();

        tile.tileNumber = tileNumber;
        tile.status = "OPEN";
        tile.owner = null;
        tile.progress = null;
        tile.aidRequested = false;
        tile.aidNeeded = null;
        tile.helpers = new ArrayList<>();

        return tile;
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

                return percent + "%";
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
                        Integer.parseInt(parts[0]);

                int total =
                        Integer.parseInt(parts[1]);

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

    private String escapeHtml(
            String value
    )
    {
        if (value == null)
        {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
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

        g2.drawString(
                text,
                x
                        + (
                        width
                                - textWidth
                ) / 2,
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
                        width
                                - textWidth
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

        String shortened = text;

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

        return shortened + "...";
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
