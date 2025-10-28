import com.fazecast.jSerialComm.SerialPort;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

public class ScoreboardGUI extends JFrame {
    private ArrayList<Giocatore> classifica;
    private DefaultTableModel tableModel;
    private JTable table;
    private SerialPort port;
    private Scanner data;
    private String currentPlayer = "Giocatore"; // IMPORTANTE: inizializza con un nome default
    private int currentScore = 0;

    private final Color BACKGROUND_COLOR = new Color(245, 245, 255);
    private final Color HEADER_COLOR = new Color(147, 112, 219);
    private final Color ROW_COLOR_1 = new Color(240, 248, 255);
    private final Color ROW_COLOR_2 = new Color(230, 230, 250);
    private final Color ACCENT_COLOR = new Color(100, 149, 237);
    private final Color TEXT_COLOR = new Color(72, 61, 139);
    private final Color GOLD_COLOR = new Color(255, 215, 0);
    private final Color SILVER_COLOR = new Color(192, 192, 192);
    private final Color BRONZE_COLOR = new Color(205, 127, 50);

    private Color buttonTextColor;
    private volatile boolean serialConnected = false;

    public ScoreboardGUI() {
        classifica = Database.caricaClassifica();
        determineButtonTextColor();
        initializeGUI();
        setupSerialConnection();
        setupWindowListener();
    }

    private void determineButtonTextColor() {
        Color background = UIManager.getColor("Panel.background");
        if (background == null) {
            background = BACKGROUND_COLOR;
        }
        double luminance = (0.299 * background.getRed() + 0.587 * background.getGreen() + 0.114 * background.getBlue()) / 255;
        buttonTextColor = luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    private void initializeGUI() {
        setTitle("Classifica Gioco Simon");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 500);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND_COLOR);

        String[] colonne = {"Pos", "Nome", "Punteggio", "Data"};
        tableModel = new DefaultTableModel(colonne, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }
        };

        table = new JTable(tableModel);
        customizeTable();

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        scrollPane.getViewport().setBackground(BACKGROUND_COLOR);

        JPanel controlPanel = createControlPanel();

        setLayout(new BorderLayout());
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        aggiornaTabellaClassifica();
        setVisible(true);
    }

    private void customizeTable() {

        Font headerFont = new Font("Segoe UI", Font.BOLD, 16);
        Font tableFont = new Font("Segoe UI", Font.PLAIN, 14);

        JTableHeader header = table.getTableHeader();
        header.setBackground(HEADER_COLOR);
        header.setForeground(Color.BLACK);
        header.setFont(headerFont);
        header.setBorder(BorderFactory.createRaisedBevelBorder());

        table.setFont(tableFont);
        table.setRowHeight(35);
        table.setSelectionBackground(new Color(173, 216, 230)); // Blu pastello per selezione
        table.setSelectionForeground(TEXT_COLOR);
        table.setGridColor(new Color(200, 200, 220));
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {

                Component c = super.getTableCellRendererComponent(table, value,
                        isSelected, hasFocus, row, column);

                if (!isSelected) {

                    if (row % 2 == 0) {
                        c.setBackground(ROW_COLOR_1);
                    } else {
                        c.setBackground(ROW_COLOR_2);
                    }

                    if (row == 0) {
                        c.setBackground(new Color(255, 250, 205)); // Giallo molto chiaro per l'oro
                        c.setForeground(GOLD_COLOR);
                        setFont(getFont().deriveFont(Font.BOLD, 16));
                    } else if (row == 1) {
                        c.setBackground(new Color(240, 240, 240)); // Grigio chiaro per l'argento
                        c.setForeground(SILVER_COLOR);
                        setFont(getFont().deriveFont(Font.BOLD, 15));
                    } else if (row == 2) {
                        c.setBackground(new Color(245, 222, 179)); // Bronzo chiaro
                        c.setForeground(BRONZE_COLOR);
                        setFont(getFont().deriveFont(Font.BOLD, 14));
                    } else {
                        c.setForeground(TEXT_COLOR);
                        setFont(tableFont);
                    }
                }

                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                setHorizontalAlignment(column == 0 ? SwingConstants.CENTER :
                        column == 2 ? SwingConstants.CENTER : SwingConstants.LEFT);

                return c;
            }
        });

        table.getColumnModel().getColumn(0).setPreferredWidth(50); // Posizione
        table.getColumnModel().getColumn(1).setPreferredWidth(150); // Nome
        table.getColumnModel().getColumn(2).setPreferredWidth(100); // Punteggio
        table.getColumnModel().getColumn(3).setPreferredWidth(200); // Data
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(BACKGROUND_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = new JLabel("CLASSIFICA SIMON GAME", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(HEADER_COLOR);

        JLabel subtitleLabel = new JLabel("Punteggi in tempo reale dal gioco Simon", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(TEXT_COLOR);

        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);

        return headerPanel;
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        controlPanel.setBackground(BACKGROUND_COLOR);
        controlPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2, 0, 0, 0, HEADER_COLOR),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JTextField nameField = new JTextField(15);
        nameField.setText("Giocatore");
        nameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        nameField.setBackground(Color.WHITE);
        nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_COLOR, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        JButton changeNameButton = createStyledButton("Cambia Nome", ACCENT_COLOR);
        JButton resetButton = createStyledButton("Reset Classifica", new Color(220, 20, 60));

        changeNameButton.addActionListener(e -> {
            String newName = nameField.getText().trim();
            if (!newName.isEmpty()) {
                currentPlayer = newName;
                JOptionPane.showMessageDialog(this,
                        "Nome cambiato in: " + currentPlayer,
                        "Nome Aggiornato",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

        resetButton.addActionListener(e -> resetClassifica());

        controlPanel.add(createStyledLabel("Nome Giocatore:"));
        controlPanel.add(nameField);
        controlPanel.add(changeNameButton);
        controlPanel.add(resetButton);

        return controlPanel;
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(color);
        button.setForeground(buttonTextColor); // Usa il colore determinato automaticamente
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color.darker(), 2),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(color.brighter());

                button.setForeground(buttonTextColor);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(color);
                button.setForeground(buttonTextColor);
            }
        });

        return button;
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(TEXT_COLOR);
        return label;
    }

    private void setupWindowListener() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Database.salvaClassifica(classifica);
                close();
            }
        });
    }

    private void setupSerialConnection() {
        new Thread(() -> {
            try {
                // Prova diverse porte COM automaticamente
                SerialPort[] ports = SerialPort.getCommPorts();
                port = null;

                for (SerialPort p : ports) {
                    System.out.println("Trovata porta: " + p.getSystemPortName());
                    if (p.getSystemPortName().startsWith("COM") ||
                            p.getSystemPortName().startsWith("/dev/")) {
                        port = p;
                        break;
                    }
                }

                if (port == null) {
                    System.err.println("Nessuna porta seriale trovata!");
                    JOptionPane.showMessageDialog(this,
                            "Nessuna porta seriale trovata!\nAssicurati che Arduino sia collegato.",
                            "Errore Connessione",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                port.setBaudRate(9600);
                port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);

                if (port.openPort()) {
                    System.out.println("Porta seriale aperta con successo su " + port.getSystemPortName());
                    serialConnected = true;
                    data = new Scanner(port.getInputStream());
                    readSerialData();
                } else {
                    System.err.println("Errore nell'apertura della porta seriale " + port.getSystemPortName());
                }
            } catch (Exception e) {
                System.err.println("Errore di connessione: " + e.getMessage());
                JOptionPane.showMessageDialog(this,
                        "Errore di connessione seriale: " + e.getMessage(),
                        "Errore Connessione",
                        JOptionPane.ERROR_MESSAGE);
            }
        }).start();
    }

    private void readSerialData() {
        System.out.println("Thread di lettura seriale avviato...");

        while (serialConnected && port != null && port.isOpen()) {
            try {
                if (data.hasNextLine()) {
                    String line = data.nextLine().trim();
                    System.out.println("Dati ricevuti: " + line); // DEBUG

                    if (line.startsWith("{")) {
                        processJSONData(line);
                    }
                } else {
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                System.err.println("Errore nella lettura seriale: " + e.getMessage());
                try { Thread.sleep(1000); } catch (InterruptedException ie) {}
            }
        }
        System.out.println("Thread di lettura seriale terminato");
    }

    private void processJSONData(String jsonLine) {
        try {
            JSONObject obj = new JSONObject(jsonLine);
            String event = obj.getString("event");
            int value = obj.getInt("value");

            System.out.println("Evento ricevuto: " + event + " - Valore: " + value); // DEBUG

            switch (event) {
                case "level":
                    System.out.println("Livello: " + value);
                    break;

                case "correct":
                    currentScore = value;
                    System.out.println("Sequenza corretta! Livello " + currentScore);
                    break;

                case "error":
                    System.out.println("Errore! Hai perso al livello " + value);
                    // Usa il livello al momento dell'errore come punteggio
                    aggiornaClassifica(currentPlayer, value);
                    currentScore = 0;
                    break;

                case "gameover":
                    System.out.println("Game over! Miglior livello: " + value);
                    // Usa il miglior livello come punteggio
                    aggiornaClassifica(currentPlayer, value);
                    currentScore = 0;
                    break;

                default:
                    System.out.println("Evento sconosciuto: " + event);
            }
        } catch (Exception e) {
            System.err.println("Errore nel processing JSON: " + e.getMessage() + " - JSON: " + jsonLine);
        }
    }

    private void aggiornaClassifica(String nome, int punteggio) {
        if (nome == null || nome.trim().isEmpty()) {
            System.err.println("Nome giocatore vuoto! Usando 'Giocatore'");
            nome = "Giocatore";
        }

        System.out.println("Aggiornamento classifica per: " + nome + " - Punteggio: " + punteggio);

        boolean giocatoreTrovato = false;

        for (Giocatore g : classifica) {
            if (g.getNome().equals(nome)) {
                if (punteggio > g.getPunteggio()) {
                    g.setPunteggio(punteggio);
                    System.out.println("Punteggio aggiornato per " + nome + ": " + punteggio);
                } else {
                    System.out.println("Punteggio non migliorato per " + nome + " (" + punteggio + " <= " + g.getPunteggio() + ")");
                }
                giocatoreTrovato = true;
                break;
            }
        }

        if (!giocatoreTrovato) {
            classifica.add(new Giocatore(nome, punteggio));
            System.out.println("Nuovo giocatore aggiunto: " + nome + " con punteggio: " + punteggio);
        }

        // Ordina la classifica
        Collections.sort(classifica, (g1, g2) -> {
            int scoreCompare = Integer.compare(g2.getPunteggio(), g1.getPunteggio());
            if (scoreCompare == 0) {
                return Long.compare(g2.getTimestamp(), g1.getTimestamp());
            }
            return scoreCompare;
        });

        // Salva e aggiorna GUI
        Database.salvaClassifica(classifica);
        aggiornaTabellaClassifica();

        // Mostra notifica
        if (punteggio > 0) {
            showScoreNotification(nome, punteggio);
        }
    }

    private void showScoreNotification(String nome, int punteggio) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    nome + " ha raggiunto il livello " + punteggio + "!",
                    "Nuovo Punteggio",
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private void aggiornaTabellaClassifica() {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            for (int i = 0; i < classifica.size(); i++) {
                Giocatore g = classifica.get(i);
                tableModel.addRow(new Object[]{
                        getPositionText(i),
                        g.getNome(),
                        String.valueOf(g.getPunteggio()),
                        new java.util.Date(g.getTimestamp()).toString()
                });
            }
            System.out.println("Tabella aggiornata con " + classifica.size() + " giocatori"); // DEBUG
        });
    }

    private String getPositionText(int position) {
        switch (position) {
            case 0: return "1°";
            case 1: return "2°";
            case 2: return "3°";
            default: return (position + 1) + "°";
        }
    }

    private void resetClassifica() {
        int response = JOptionPane.showConfirmDialog(this,
                "Sei sicuro di voler resettare tutta la classifica?\nQuesta operazione non può essere annullata.",
                "Reset Classifica",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (response == JOptionPane.YES_OPTION) {
            classifica.clear();
            aggiornaTabellaClassifica();
            Database.salvaClassifica(classifica);
            JOptionPane.showMessageDialog(this,
                    "Classifica resettata con successo",
                    "Reset Completato",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public void close() {
        serialConnected = false;
        if (data != null) {
            data.close();
        }
        if (port != null && port.isOpen()) {
            port.closePort();
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new ScoreboardGUI();
        });
    }
}