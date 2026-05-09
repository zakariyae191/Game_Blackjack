package blackjack;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Client Swing soigne pour le Blackjack multijoueur.
 * L'interface garde le protocole texte du serveur, mais presente une table plus lisible.
 */
public class BlackjackGUIClient extends JFrame {
    private static final int PORT = 5555;
    private static final Color FELT_TOP = new Color(20, 103, 75);
    private static final Color FELT_BOTTOM = new Color(11, 54, 42);
    private static final Color PANEL_BG = new Color(15, 25, 34, 190);
    private static final Color GOLD = new Color(232, 190, 104);
    private static final Color TEXT = new Color(242, 246, 247);
    private static final Color MUTED_TEXT = new Color(172, 188, 193);
    private static final Font UI_FONT = new Font("SansSerif", Font.PLAIN, 14);

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private JPanel dealerCardsPanel;
    private JPanel playerCardsPanel;
    private JLabel statusLabel;
    private JLabel scoreLabel;
    private JLabel dealerScoreLabel;
    private JLabel connectionLabel;
    private JButton hitButton;
    private JButton standButton;
    private JTextArea logArea;

    private final List<String> playerCards = new ArrayList<>();

    public BlackjackGUIClient(String host) {
        configureFrame();
        setContentPane(createMainPanel(host));
        setVisible(true);
        connect(host);
    }

    private void configureFrame() {
        setTitle("Blackjack Pro");
        setMinimumSize(new Dimension(960, 720));
        setSize(1040, 760);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private JPanel createMainPanel(String host) {
        JPanel mainPanel = new FeltPanel(new BorderLayout(0, 0));
        mainPanel.setBorder(new EmptyBorder(18, 22, 22, 22));
        mainPanel.add(createHeader(host), BorderLayout.NORTH);
        mainPanel.add(createTable(), BorderLayout.CENTER);
        return mainPanel;
    }

    private JComponent createHeader(String host) {
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 18, 0));

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBlock.setOpaque(false);

        JLabel title = new JLabel("BLACKJACK PRO");
        title.setForeground(TEXT);
        title.setFont(new Font("SansSerif", Font.BOLD, 28));

        JLabel subtitle = new JLabel("Table multijoueur - serveur " + host + ":" + PORT);
        subtitle.setForeground(MUTED_TEXT);
        subtitle.setFont(UI_FONT);

        titleBlock.add(title);
        titleBlock.add(subtitle);

        connectionLabel = new JLabel("Connexion...");
        connectionLabel.setOpaque(true);
        connectionLabel.setBorder(new EmptyBorder(8, 14, 8, 14));
        connectionLabel.setForeground(new Color(12, 39, 31));
        connectionLabel.setBackground(GOLD);
        connectionLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        header.add(titleBlock, BorderLayout.WEST);
        header.add(connectionLabel, BorderLayout.EAST);
        return header;
    }

    private JComponent createTable() {
        JPanel table = new JPanel(new GridBagLayout());
        table.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 14, 0);

        gbc.gridy = 0;
        gbc.weighty = 0.30;
        table.add(createHandSection("Croupier", true), gbc);

        gbc.gridy = 1;
        gbc.weighty = 0.30;
        table.add(createCenterSection(), gbc);

        gbc.gridy = 2;
        gbc.weighty = 0.40;
        gbc.insets = new Insets(0, 0, 0, 0);
        table.add(createPlayerSection(), gbc);

        return table;
    }

    private JComponent createHandSection(String title, boolean dealer) {
        JPanel section = new TranslucentPanel(new BorderLayout(12, 12), PANEL_BG);
        section.setBorder(new EmptyBorder(18, 20, 18, 20));

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);

        JLabel label = new JLabel(title.toUpperCase());
        label.setForeground(MUTED_TEXT);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));

        JLabel score = new JLabel(dealer ? "Carte visible" : "Score 0");
        score.setForeground(GOLD);
        score.setFont(new Font("SansSerif", Font.BOLD, 15));

        heading.add(label, BorderLayout.WEST);
        heading.add(score, BorderLayout.EAST);
        section.add(heading, BorderLayout.NORTH);

        JPanel cards = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 6));
        cards.setOpaque(false);
        cards.add(new EmptySeatComponent(dealer ? "En attente du croupier" : "En attente des cartes"));
        section.add(cards, BorderLayout.CENTER);

        if (dealer) {
            dealerCardsPanel = cards;
            dealerScoreLabel = score;
        } else {
            playerCardsPanel = cards;
            scoreLabel = score;
        }

        return section;
    }

    private JComponent createCenterSection() {
        JPanel center = new JPanel(new BorderLayout(18, 0));
        center.setOpaque(false);

        JPanel statusPanel = new TranslucentPanel(new BorderLayout(0, 12), new Color(10, 21, 29, 210));
        statusPanel.setBorder(new EmptyBorder(18, 18, 18, 18));

        statusLabel = new JLabel("Connexion au serveur...", SwingConstants.CENTER);
        statusLabel.setForeground(TEXT);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        statusPanel.add(statusLabel, BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        controls.setOpaque(false);

        hitButton = createStyledButton("Tirer", new Color(35, 159, 95));
        standButton = createStyledButton("Rester", new Color(191, 68, 70));

        hitButton.addActionListener(e -> sendMessage("tirer"));
        standButton.addActionListener(e -> sendMessage("rester"));

        controls.add(hitButton);
        controls.add(standButton);
        statusPanel.add(controls, BorderLayout.SOUTH);

        logArea = new JTextArea(8, 34);
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setBackground(new Color(8, 14, 20));
        logArea.setForeground(new Color(224, 232, 235));
        logArea.setCaretColor(GOLD);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBorder(new EmptyBorder(12, 12, 12, 12));

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 26)));
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        scroll.getVerticalScrollBar().setUI(new DarkScrollBarUI());

        center.add(statusPanel, BorderLayout.CENTER);
        center.add(scroll, BorderLayout.EAST);
        return center;
    }

    private JComponent createPlayerSection() {
        JPanel playerSection = new JPanel(new BorderLayout(0, 12));
        playerSection.setOpaque(false);
        playerSection.add(createHandSection("Votre main", false), BorderLayout.CENTER);
        return playerSection;
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 15));
        button.setForeground(Color.WHITE);
        button.setBackground(bg);
        button.setBorder(new EmptyBorder(12, 28, 12, 28));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setEnabled(false);
        button.addMouseListener(new ButtonHoverAdapter(button, bg));
        return button;
    }

    private void connect(String host) {
        setControlsEnabled(false);
        new Thread(() -> {
            try {
                socket = new Socket(host, PORT);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                SwingUtilities.invokeLater(() -> {
                    connectionLabel.setText("Connecte");
                    connectionLabel.setBackground(new Color(91, 212, 143));
                    statusLabel.setText("Bienvenue a la table");
                });

                String msg;
                while ((msg = in.readLine()) != null) {
                    processServerMessage(msg);
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    connectionLabel.setText("Hors ligne");
                    connectionLabel.setBackground(new Color(235, 101, 101));
                    statusLabel.setText("Serveur indisponible");
                    appendLog("Impossible de se connecter au serveur sur " + host + ":" + PORT + ".");
                    setControlsEnabled(false);
                });
            }
        }, "blackjack-gui-network").start();
    }

    private void processServerMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            appendLog(msg);

            if (msg.contains("=== NOUVELLE PARTIE ===")) {
                resetTable();
                statusLabel.setText("Nouvelle partie");
                return;
            }

            if (msg.contains("Vos cartes:")) {
                setPlayerCards(parseCardsBetween(msg, ":", "("));
                updateScoreFromMessage(msg, scoreLabel, "Score ");
            } else if (msg.contains("Vous avez tire") || msg.contains("Vous avez tir")) {
                addDrawnCard(msg);
                updateScoreFromMessage(msg, scoreLabel, "Score ");
            } else if (msg.contains("Carte visible du croupier:")) {
                setDealerCards(parseCardsAfter(msg, ":"), true);
            } else if (msg.contains("Croupier:")) {
                setDealerCards(parseCardsBetween(msg, ":", "("), false);
                updateScoreFromMessage(msg, dealerScoreLabel, "Score ");
            }

            updateTurnState(msg);
            updateRoundResult(msg);
        });
    }

    private void updateTurnState(String msg) {
        String normalized = normalize(msg);
        if (msg.contains("C'est votre tour") || msg.contains("Tirer ou rester?")) {
            setControlsEnabled(true);
            statusLabel.setText("A vous de jouer");
            return;
        }

        if (normalized.contains("RESTE") || normalized.contains("PERDU")
                || normalized.contains("GAGNE") || normalized.contains("GAGN")
                || normalized.contains("EGALITE") || normalized.contains("DEPASSE")) {
            setControlsEnabled(false);
        }
    }

    private void updateRoundResult(String msg) {
        String normalized = normalize(msg);
        if (normalized.contains("GAGNE")) {
            statusLabel.setText("Victoire");
        } else if (normalized.contains("PERDU")) {
            statusLabel.setText("Defaite");
        } else if (normalized.contains("EGALITE")) {
            statusLabel.setText("Egalite");
        } else if (normalized.contains("DEPASSE")) {
            statusLabel.setText("Main brulee");
        }
    }

    private void appendLog(String msg) {
        logArea.append(cleanText(msg) + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void resetTable() {
        playerCards.clear();
        clearCards(playerCardsPanel, "En attente des cartes");
        clearCards(dealerCardsPanel, "En attente du croupier");
        scoreLabel.setText("Score 0");
        dealerScoreLabel.setText("Carte visible");
        setControlsEnabled(false);
    }

    private void clearCards(JPanel panel, String emptyText) {
        panel.removeAll();
        panel.add(new EmptySeatComponent(emptyText));
        panel.revalidate();
        panel.repaint();
    }

    private void setPlayerCards(List<String> cards) {
        playerCards.clear();
        playerCards.addAll(cards);
        renderCards(playerCardsPanel, playerCards, false);
    }

    private void addDrawnCard(String msg) {
        String card = parseCardsBetween(msg, ":", "(").stream().findFirst().orElse("");
        if (!card.isEmpty()) {
            playerCards.add(card);
            renderCards(playerCardsPanel, playerCards, false);
        }
    }

    private void setDealerCards(List<String> cards, boolean showHiddenCard) {
        dealerCardsPanel.removeAll();
        for (String card : cards) {
            dealerCardsPanel.add(new CardComponent(card));
        }
        if (showHiddenCard) {
            dealerCardsPanel.add(new CardBackComponent());
        }
        dealerCardsPanel.revalidate();
        dealerCardsPanel.repaint();
    }

    private void renderCards(JPanel panel, List<String> cards, boolean hidden) {
        panel.removeAll();
        if (cards.isEmpty()) {
            panel.add(new EmptySeatComponent("En attente des cartes"));
        } else {
            for (String card : cards) {
                panel.add(hidden ? new CardBackComponent() : new CardComponent(card));
            }
        }
        panel.revalidate();
        panel.repaint();
    }

    private List<String> parseCardsAfter(String msg, String marker) {
        return splitCards(msg.substring(msg.indexOf(marker) + marker.length()).trim());
    }

    private List<String> parseCardsBetween(String msg, String startMarker, String endMarker) {
        int start = msg.indexOf(startMarker);
        if (start < 0) {
            return new ArrayList<>();
        }
        int end = msg.indexOf(endMarker, start + startMarker.length());
        String cardsText = end >= 0
                ? msg.substring(start + startMarker.length(), end)
                : msg.substring(start + startMarker.length());
        return splitCards(cardsText.trim());
    }

    private List<String> splitCards(String cardsText) {
        List<String> cards = new ArrayList<>();
        if (cardsText.isEmpty()) {
            return cards;
        }
        for (String card : cardsText.split(",")) {
            String clean = cleanText(card).trim();
            if (!clean.isEmpty()) {
                cards.add(clean);
            }
        }
        return cards;
    }

    private void updateScoreFromMessage(String msg, JLabel label, String prefix) {
        String normalized = cleanText(msg);
        int scoreIndex = normalized.toLowerCase().indexOf("score: ");
        if (scoreIndex < 0) {
            return;
        }
        int start = scoreIndex + 7;
        int end = normalized.indexOf(")", start);
        if (end < 0) {
            end = normalized.length();
        }
        label.setText(prefix + normalized.substring(start, end).trim());
    }

    private void setControlsEnabled(boolean enabled) {
        hitButton.setEnabled(enabled);
        standButton.setEnabled(enabled);
    }

    private void sendMessage(String msg) {
        if (out != null) {
            out.println(msg);
            setControlsEnabled(false);
        }
    }

    private String cleanText(String text) {
        return text
                .replace("Ã©", "e")
                .replace("Ã¨", "e")
                .replace("Ãª", "e")
                .replace("Ã ", "a")
                .replace("Ã€", "A")
                .replace("Ã‰", "E")
                .replace("Ã§", "c")
                .replace("â†’", "->")
                .replace("â™¥", String.valueOf((char) 9829))
                .replace("â™¦", String.valueOf((char) 9830))
                .replace("â™£", String.valueOf((char) 9827))
                .replace("â™ ", String.valueOf((char) 9824));
    }

    private String normalize(String text) {
        return cleanText(text).toUpperCase();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String host = (args.length > 0) ? args[0] : "localhost";
            new BlackjackGUIClient(host);
        });
    }

    private static class FeltPanel extends JPanel {
        FeltPanel(LayoutManager layout) {
            super(layout);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, FELT_TOP, 0, getHeight(), FELT_BOTTOM));
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(new Color(255, 255, 255, 16));
            g2.setStroke(new BasicStroke(2f));
            int inset = 34;
            g2.drawRoundRect(inset, inset + 58, getWidth() - inset * 2, getHeight() - inset * 2 - 58, 240, 240);
            g2.dispose();
        }
    }

    private static class TranslucentPanel extends JPanel {
        private final Color background;

        TranslucentPanel(LayoutManager layout, Color background) {
            super(layout);
            this.background = background;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(background);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g2.setColor(new Color(255, 255, 255, 28));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class ButtonHoverAdapter extends MouseAdapter {
        private final JButton button;
        private final Color base;

        ButtonHoverAdapter(JButton button, Color base) {
            this.button = button;
            this.base = base;
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            if (button.isEnabled()) {
                button.setBackground(base.brighter());
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            button.setBackground(base);
        }
    }

    private class CardComponent extends JPanel {
        private String value = "?";
        private String suit = "";

        CardComponent(String cardStr) {
            setPreferredSize(new Dimension(92, 132));
            setOpaque(false);
            parseCard(cardStr);
        }

        private void parseCard(String text) {
            String normalized = cleanText(text);
            if (normalized.contains(" de ")) {
                String[] parts = normalized.split(" de ", 2);
                value = parts[0].trim();
                suit = parts[1].trim();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillRoundRect(5, 7, getWidth() - 8, getHeight() - 7, 18, 18);

            g2.setColor(new Color(248, 249, 245));
            g2.fillRoundRect(0, 0, getWidth() - 8, getHeight() - 8, 18, 18);
            g2.setColor(new Color(205, 209, 205));
            g2.drawRoundRect(0, 0, getWidth() - 9, getHeight() - 9, 18, 18);

            Color suitColor = isRedSuit(suit) ? new Color(188, 38, 50) : new Color(25, 31, 36);
            g2.setColor(suitColor);

            g2.setFont(new Font("Serif", Font.BOLD, 21));
            g2.drawString(shortValue(value), 12, 28);

            String symbol = suitSymbol(suit);
            g2.setFont(new Font("Serif", Font.PLAIN, 50));
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - 8 - fm.stringWidth(symbol)) / 2;
            int y = (getHeight() - 8 + fm.getAscent()) / 2 - 4;
            g2.drawString(symbol, x, y);

            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            FontMetrics labelMetrics = g2.getFontMetrics();
            String suitLabel = suit.toUpperCase();
            int labelX = (getWidth() - 8 - labelMetrics.stringWidth(suitLabel)) / 2;
            g2.drawString(suitLabel, labelX, getHeight() - 22);
            g2.dispose();
        }

        private boolean isRedSuit(String suitName) {
            return suitName.equalsIgnoreCase("Coeur") || suitName.equalsIgnoreCase("Carreau");
        }

        private String shortValue(String cardValue) {
            if (cardValue.equalsIgnoreCase("As")) return "A";
            if (cardValue.equalsIgnoreCase("Roi")) return "K";
            if (cardValue.equalsIgnoreCase("Dame")) return "Q";
            if (cardValue.equalsIgnoreCase("Valet")) return "J";
            return cardValue;
        }

        private String suitSymbol(String suitName) {
            if (suitName.equalsIgnoreCase("Coeur")) return String.valueOf((char) 9829);
            if (suitName.equalsIgnoreCase("Carreau")) return String.valueOf((char) 9830);
            if (suitName.equalsIgnoreCase("Trefle") || suitName.equalsIgnoreCase("TrÃ¨fle")) return String.valueOf((char) 9827);
            if (suitName.equalsIgnoreCase("Pique")) return String.valueOf((char) 9824);
            return "";
        }
    }

    private static class CardBackComponent extends JPanel {
        CardBackComponent() {
            setPreferredSize(new Dimension(92, 132));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillRoundRect(5, 7, getWidth() - 8, getHeight() - 7, 18, 18);
            g2.setColor(new Color(29, 61, 94));
            g2.fillRoundRect(0, 0, getWidth() - 8, getHeight() - 8, 18, 18);
            g2.setColor(GOLD);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(11, 11, getWidth() - 30, getHeight() - 30, 14, 14);
            g2.drawLine(22, 22, getWidth() - 30, getHeight() - 30);
            g2.drawLine(getWidth() - 30, 22, 22, getHeight() - 30);
            g2.dispose();
        }
    }

    private static class EmptySeatComponent extends JPanel {
        private final String label;

        EmptySeatComponent(String label) {
            this.label = label;
            setPreferredSize(new Dimension(260, 88));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(255, 255, 255, 22));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g2.setColor(MUTED_TEXT);
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, (getWidth() - fm.stringWidth(label)) / 2, (getHeight() + fm.getAscent()) / 2 - 3);
            g2.dispose();
        }
    }

    private static class DarkScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = new Color(72, 86, 96);
            trackColor = new Color(8, 14, 20);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }
    }
}
