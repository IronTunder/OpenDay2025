// InterfacciaGrafica.java
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

public class InterfacciaGrafica extends JFrame {
    private DefaultTableModel tableModel;
    private JTable table;
    private ArrayList<Giocatore> classifica;
    private SerialReaderThread serialThread;
    private String nomeGiocatoreCorrente;
    private boolean nomeRichiesto = false;
    private JLabel statusLabel;
    private int punteggioCorrente = 0;

    static GraphicsDevice device = GraphicsEnvironment
            .getLocalGraphicsEnvironment().getScreenDevices()[0];

    // Colori pastello
    private final Color COLOR_PRIMARIO = new Color(173, 216, 230); // Azzurro pastello
    private final Color COLOR_SECONDARIO = new Color(240, 248, 255); // Alice blue
    private final Color COLOR_ACCENTO = new Color(221, 160, 221); // Prugna pastello
    private final Color COLOR_TESTO = new Color(70, 70, 70); // Grigio scuro
    private final Color COLOR_HEADER = new Color(176, 224, 230); // Polvere blu

    // Icone - Puoi caricare le tue immagini qui
    private Icon iconaPosizione1;
    private Icon iconaPosizione2;
    private Icon iconaPosizione3;
    private Icon iconaPosizioneDefault;
    private Icon iconaStella;
    private Icon iconaGiocatore;
    private Icon iconaCalendario;
    private Icon iconaTrofeo;

    public InterfacciaGrafica() {
        setTitle("Classifica Simon Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        device.setFullScreenWindow(this);
        setLocationRelativeTo(null);

        // Carica le icone (sostituisci con i tuoi file)
        caricaIcone();

        // Imposta look and feel moderno
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Carica classifica esistente
        classifica = Database.caricaClassifica();

        initializeComponents();
        setupSerialReader();
        aggiornaPunteggio(punteggioCorrente);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                Database.salvaClassifica(classifica);
                Database.salvaClassificaTesto(classifica);
                if (serialThread != null) {
                    serialThread.stopThread();
                }
            }
        });
    }

    private void caricaIcone() {
        try {
            // Dimensioni consigliate per le icone
            int iconSizeSmall = 16;  // Per icone nelle celle
            int iconSizeMedium = 20; // Per icone posizione
            int iconSizeLarge = 24;  // Per icona titolo

            iconaPosizione1 = loadAndResizeIcon("icons/trofeo_oro.png", iconSizeMedium, iconSizeMedium);
            iconaPosizione2 = loadAndResizeIcon("icons/trofeo_argento.png", iconSizeMedium, iconSizeMedium);
            iconaPosizione3 = loadAndResizeIcon("icons/trofeo_bronzo.png", iconSizeMedium, iconSizeMedium);
            iconaPosizioneDefault = loadAndResizeIcon("icons/punto.png", iconSizeSmall, iconSizeSmall);
            iconaStella = loadAndResizeIcon("icons/stella.png", iconSizeSmall, iconSizeSmall);
            iconaGiocatore = loadAndResizeIcon("icons/giocatore.png", iconSizeSmall, iconSizeSmall);
            iconaCalendario = loadAndResizeIcon("icons/calendario.png", iconSizeSmall, iconSizeSmall);
            iconaTrofeo = loadAndResizeIcon("icons/trofeo.png", iconSizeLarge, iconSizeLarge);

            // Verifica che tutte le icone siano state caricate
            verificaIconeCaricate();

        } catch (Exception e) {
            System.err.println("Errore nel caricamento delle icone: " + e.getMessage());
        }
    }

    private ImageIcon loadAndResizeIcon(String path, int width, int height) {
        try {
            java.io.File file = new java.io.File(path);
            if (!file.exists()) {
                // Prova percorso alternativo
                file = new java.io.File("./" + path);
                if (!file.exists()) {
                    System.err.println("File non trovato: " + path);
                    return null;
                }
            }

            ImageIcon originalIcon = new ImageIcon(path);
            System.out.println("Icona caricata: " + path + " - Dimensioni originali: " +
                    originalIcon.getIconWidth() + "x" + originalIcon.getIconHeight());

            // Ridimensiona solo se necessario
            if (originalIcon.getIconWidth() > width || originalIcon.getIconHeight() > height) {
                Image resizedImage = originalIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(resizedImage);
            } else {
                return originalIcon;
            }

        } catch (Exception e) {
            System.err.println("Errore nel caricamento di " + path + ": " + e.getMessage());
            return null;
        }
    }

    private void verificaIconeCaricate() {
        System.out.println("=== VERIFICA ICONE ===");
        System.out.println("Trofeo oro: " + (iconaPosizione1 != null ?
                iconaPosizione1.getIconWidth() + "x" + iconaPosizione1.getIconHeight() : "NULL"));
        System.out.println("Trofeo argento: " + (iconaPosizione2 != null ?
                iconaPosizione2.getIconWidth() + "x" + iconaPosizione2.getIconHeight() : "NULL"));
        System.out.println("Trofeo bronzo: " + (iconaPosizione3 != null ?
                iconaPosizione3.getIconWidth() + "x" + iconaPosizione3.getIconHeight() : "NULL"));
        System.out.println("Stella: " + (iconaStella != null ?
                iconaStella.getIconWidth() + "x" + iconaStella.getIconHeight() : "NULL"));
        System.out.println("Giocatore: " + (iconaGiocatore != null ?
                iconaGiocatore.getIconWidth() + "x" + iconaGiocatore.getIconHeight() : "NULL"));
        System.out.println("================");
    }


    private void initializeComponents() {
        // Layout principale con sfondo gradiente
        setLayout(new BorderLayout());
        getContentPane().setBackground(COLOR_SECONDARIO);

        // Header con titolo
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Tabella classifica
        JPanel tablePanel = createTablePanel();
        add(tablePanel, BorderLayout.CENTER);

        // Pannello status
        JPanel statusPanel = createStatusPanel();
        add(statusPanel, BorderLayout.SOUTH);

        // Aggiorna tabella iniziale
        aggiornaTabella();
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gradient = new GradientPaint(
                        0, 0, COLOR_PRIMARIO,
                        getWidth(), 0, COLOR_ACCENTO
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        headerPanel.setPreferredSize(new Dimension(getWidth(), 150));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        titlePanel.setOpaque(false);

        JLabel iconLabel = new JLabel(iconaTrofeo);
        JLabel titleLabel = new JLabel("CLASSIFICA SIMON");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setForeground(Color.WHITE);

        titlePanel.add(iconLabel);
        titlePanel.add(titleLabel);

        JLabel subtitleLabel = new JLabel("Leaderboard in tempo reale", JLabel.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        subtitleLabel.setForeground(new Color(240, 240, 240));

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);
        textPanel.add(titlePanel);
        textPanel.add(subtitleLabel);

        headerPanel.add(textPanel, BorderLayout.CENTER);
        return headerPanel;
    }

    private JPanel createTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(COLOR_SECONDARIO);
        tablePanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // Tabella classifica con icone nelle colonne
        String[] columnNames = {"Posizione", "Giocatore", "Punteggio", "Data"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return String.class;
            }
        };

        table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    if (row % 2 == 0) {
                        c.setBackground(COLOR_SECONDARIO);
                    } else {
                        c.setBackground(new Color(245, 245, 255));
                    }
                }
                return c;
            }
        };

        // Styling della tabella
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setRowHeight(35);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBorder(BorderFactory.createEmptyBorder());

        // Header della tabella con icone
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        table.getTableHeader().setBackground(COLOR_HEADER);
        table.getTableHeader().setForeground(COLOR_TESTO);
        table.getTableHeader().setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, COLOR_ACCENTO),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        // Dimensioni colonne
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(250);

        // Renderer personalizzati per le colonne con icone
        table.getColumnModel().getColumn(0).setCellRenderer(new IconCellRenderer(true));
        table.getColumnModel().getColumn(1).setCellRenderer(new IconCellRenderer(false));
        table.getColumnModel().getColumn(2).setCellRenderer(new IconCellRenderer(true));
        table.getColumnModel().getColumn(3).setCellRenderer(new IconCellRenderer(true));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_PRIMARIO, 1, true),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        scrollPane.getViewport().setBackground(COLOR_SECONDARIO);
        scrollPane.setBackground(COLOR_SECONDARIO);

        tablePanel.add(scrollPane, BorderLayout.CENTER);
        return tablePanel;
    }

    // Renderer personalizzato per celle con icone
    private class IconCellRenderer extends DefaultTableCellRenderer {
        private final boolean centerAlign;

        public IconCellRenderer(boolean centerAlign) {
            this.centerAlign = centerAlign;
            setFont(new Font("Segoe UI", Font.PLAIN, 13));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (centerAlign) {
                setHorizontalAlignment(CENTER);
            } else {
                setHorizontalAlignment(LEFT);
            }

            // Aggiungi icone basate sul contenuto della cella
            if (value != null) {
                String text = value.toString();
                if (text.startsWith("1°") && column == 0) {
                    setIcon(iconaPosizione1);
                    setText(" 1°");
                } else if (text.startsWith("2°") && column == 0) {
                    setIcon(iconaPosizione2);
                    setText(" 2°");
                } else if (text.startsWith("3°") && column == 0) {
                    setIcon(iconaPosizione3);
                    setText(" 3°");
                } else if (column == 0 && text.startsWith("-")) {
                    setIcon(iconaPosizioneDefault);
                    setText(" " + text.substring(1).trim());
                } else if (column == 1) {
                    setIcon(iconaGiocatore);
                } else if (column == 2 && text.contains("⭐")) {
                    setIcon(iconaStella);
                    setText(" " + text.replace("⭐", "").trim());
                } else if (column == 3) {
                    setIcon(iconaCalendario);
                } else {
                    setIcon(null);
                }
            }

            return c;
        }
    }

    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(COLOR_SECONDARIO);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(10, 30, 20, 30));

        // Label di status
        statusLabel = new JLabel("Pronto per la partita - Inserisci il nome quando richiesto", JLabel.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        statusLabel.setForeground(COLOR_TESTO);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        // Pannello informazioni
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        infoPanel.setBackground(COLOR_SECONDARIO);

        JLabel playersLabel = new JLabel("Giocatori in classifica: " + classifica.size());
        playersLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        playersLabel.setForeground(COLOR_TESTO);

        JLabel maxScoreLabel = new JLabel("Punteggio massimo: " + getMaxPunteggio());
        maxScoreLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        maxScoreLabel.setForeground(COLOR_TESTO);

        infoPanel.add(playersLabel);
        infoPanel.add(maxScoreLabel);

        statusPanel.add(statusLabel, BorderLayout.NORTH);
        statusPanel.add(infoPanel, BorderLayout.SOUTH);

        return statusPanel;
    }

    private int getMaxPunteggio() {
        if (classifica.isEmpty()) return 0;
        return classifica.stream()
                .mapToInt(Giocatore::getPunteggio)
                .max()
                .orElse(0);
    }

    private void setupSerialReader() {
        serialThread = new SerialReaderThread(this);
        serialThread.start();
    }

    public void aggiornaPunteggio(int punteggio) {
        SwingUtilities.invokeLater(() -> {
            if (punteggio == -1 && !nomeRichiesto) {
                chiediNomeGiocatore();
                nomeRichiesto = true;
                statusLabel.setText("Benvenuto " + nomeGiocatoreCorrente + "! Inizia a giocare");
            }
            else if (punteggio == 0 && !nomeRichiesto) {
                chiediNomeGiocatore();
                nomeRichiesto = true;
                statusLabel.setText("Benvenuto " + nomeGiocatoreCorrente + "! Inizia a giocare");
            }
            else if (punteggio > 0 && nomeGiocatoreCorrente != null) {
                statusLabel.setText(nomeGiocatoreCorrente + " - Punteggio corrente: " + punteggio);
                aggiungiGiocatore(nomeGiocatoreCorrente, punteggio, true);
            }
        });
    }

    private void chiediNomeGiocatore() {
        // Custom dialog con styling
        JDialog dialog = new JDialog(this, "Nuovo Giocatore", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 200);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(COLOR_SECONDARIO);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(COLOR_SECONDARIO);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel messageLabel = new JLabel("Inserisci il tuo nome:");
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        messageLabel.setForeground(COLOR_TESTO);

        JTextField nameField = new JTextField();
        nameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_PRIMARIO, 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(COLOR_SECONDARIO);

        JButton okButton = new JButton("Inizia");
        okButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        okButton.setBackground(COLOR_PRIMARIO);
        okButton.setForeground(Color.WHITE);
        okButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        okButton.setFocusPainted(false);

        okButton.addActionListener(e -> {
            String nome = nameField.getText().trim();
            if (!nome.isEmpty()) {
                nomeGiocatoreCorrente = nome;
                nomeRichiesto = true;
                dialog.dispose();
                statusLabel.setText("Benvenuto " + nome + "! Inizia a giocare");
            }
        });

        nameField.addActionListener(e -> okButton.doClick());

        contentPanel.add(messageLabel, BorderLayout.NORTH);
        contentPanel.add(nameField, BorderLayout.CENTER);
        buttonPanel.add(okButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(contentPanel);
        dialog.setVisible(true);
    }

    public void setPunteggioCorrente(int punteggio) {
        this.punteggioCorrente = punteggio;
        if (punteggio > 0 && nomeGiocatoreCorrente != null) {
            statusLabel.setText(nomeGiocatoreCorrente + " - Punteggio corrente: " + punteggio);
            aggiungiGiocatore(nomeGiocatoreCorrente, punteggio, true);
        }
    }

    private void aggiungiGiocatore(String nome, int punteggio, boolean aggiornaRealtime) {
        Giocatore giocatore = new Giocatore(nome, punteggio);

        // Cerca se il giocatore esiste già
        int index = -1;
        for (int i = 0; i < classifica.size(); i++) {
            if (classifica.get(i).getNome().equals(nome)) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            // Giocatore esistente: aggiorna solo se il punteggio è migliore
            Giocatore esistente = classifica.get(index);
            if (punteggio > esistente.getPunteggio()) {
                esistente.setPunteggio(punteggio);
                esistente.setTimestamp(System.currentTimeMillis());
            }
        } else {
            // Nuovo giocatore
            classifica.add(giocatore);
        }

        // Ordina la classifica per punteggio decrescente
        classifica.sort((g1, g2) -> Integer.compare(g2.getPunteggio(), g1.getPunteggio()));

        // Mantieni solo i top 10
        if (classifica.size() > 10) {
            classifica = new ArrayList<>(classifica.subList(0, 10));
        }

        // Aggiorna la tabella in tempo reale
        aggiornaTabella();

        // Salva solo se è un aggiornamento definitivo (game over)
        if (!aggiornaRealtime) {
            Database.salvaClassifica(classifica);
        }

        // Aggiorna status
        if (punteggio > 0) {
            statusLabel.setText(nome + " - Nuovo punteggio: " + punteggio + " - Posizione in classifica: " + (getPosizioneGiocatore(nome) + 1));
        }
    }

    private int getPosizioneGiocatore(String nome) {
        for (int i = 0; i < classifica.size(); i++) {
            if (classifica.get(i).getNome().equals(nome)) {
                return i;
            }
        }
        return -1;
    }

    private void aggiornaTabella() {
        tableModel.setRowCount(0);

        for (int i = 0; i < classifica.size(); i++) {
            Giocatore g = classifica.get(i);
            String posizione = getPosizioneTesto(i + 1);
            Object[] rowData = {
                    posizione,
                    g.getNome(),
                    String.valueOf(g.getPunteggio()),
                    formatDate(g.getTimestamp())
            };
            tableModel.addRow(rowData);
        }

        // Forza l'aggiornamento visivo della tabella
        tableModel.fireTableDataChanged();
        table.repaint();
    }

    private String getPosizioneTesto(int posizione) {
        switch (posizione) {
            case 1: return "1°";
            case 2: return "2°";
            case 3: return "3°";
            default: return "- " + posizione;
        }
    }

    private String formatDate(long timestamp) {
        java.util.Date date = new java.util.Date(timestamp);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
        return sdf.format(date);
    }

    public int getPunteggioCorrente() {
        return punteggioCorrente;
    }

    public void resetGiocatoreCorrente() {
        nomeRichiesto = false;
        nomeGiocatoreCorrente = null;
        punteggioCorrente = 0;
        statusLabel.setText("Pronto per una nuova partita - Inserisci il nome quando richiesto");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                // Migliora il rendering
                System.setProperty("awt.useSystemAAFontSettings", "on");
                System.setProperty("swing.aatext", "true");
            } catch (Exception e) {
                e.printStackTrace();
            }

            new InterfacciaGrafica().setVisible(true);
        });
    }
}