import com.fazecast.jSerialComm.SerialPort;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;

public class ScoreboardGUI extends JFrame {
    private ArrayList<Giocatore> classifica;
    private DefaultTableModel tableModel;
    private JTable table;
    private SerialPort port;
    private Scanner data;
    private String currentPlayer = "Giocatore";
    private int currentScore = 0;

    public ScoreboardGUI() {
        classifica = new ArrayList<>();
        initializeGUI();
        setupSerialConnection();
    }

    private void initializeGUI() {
        setTitle("Classifica Gioco Simon");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);

        // Creazione del modello della tabella
        String[] colonne = {"Posizione", "Nome", "Punteggio"};
        tableModel = new DefaultTableModel(colonne, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Tabella non editabile
            }
        };

        table = new JTable(tableModel);
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.setRowHeight(25);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));

        JScrollPane scrollPane = new JScrollPane(table);

        // Pannello per i controlli
        JPanel controlPanel = new JPanel(new FlowLayout());

        JTextField nameField = new JTextField(15);
        nameField.setText("Giocatore");
        JButton changeNameButton = new JButton("Cambia Nome");
        JButton resetButton = new JButton("Reset Classifica");

        changeNameButton.addActionListener(e -> {
            String newName = nameField.getText().trim();
            if (!newName.isEmpty()) {
                currentPlayer = newName;
                JOptionPane.showMessageDialog(this, "Nome cambiato in: " + currentPlayer);
            }
        });

        resetButton.addActionListener(e -> resetClassifica());

        controlPanel.add(new JLabel("Nome Giocatore:"));
        controlPanel.add(nameField);
        controlPanel.add(changeNameButton);
        controlPanel.add(resetButton);

        // Layout principale
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void setupSerialConnection() {
        new Thread(() -> {
            try {
                port = SerialPort.getCommPort("COM3"); // TODO: CAMBIA PORTA
                port.setBaudRate(9600);

                if (port.openPort()) {
                    System.out.println("Porta seriale aperta con successo");
                    data = new Scanner(port.getInputStream());
                    readSerialData();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Errore nell'apertura della porta seriale!",
                            "Errore", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Errore di connessione: " + e.getMessage(),
                        "Errore", JOptionPane.ERROR_MESSAGE);
            }
        }).start();
    }

    private void readSerialData() {
        int bestLevel = 0;

        while (data != null && data.hasNextLine()) {
            try {
                String line = data.nextLine().trim();
                if (line.startsWith("{")) {
                    JSONObject obj = new JSONObject(line);
                    String event = obj.getString("event");

                    switch (event) {
                        case "level":
                            int lvl = obj.getInt("value");
                            System.out.println("Livello: " + lvl);
                            break;

                        case "correct":
                            currentScore = obj.getInt("value");
                            System.out.println("Sequenza corretta! Livello " + currentScore);
                            break;

                        case "error":
                            int errorLevel = obj.getInt("value");
                            System.out.println("Errore! Hai perso al livello " + errorLevel);
                            // Quando c'è un errore, registra il punteggio
                            aggiornaClassifica(currentPlayer, errorLevel);
                            currentScore = 0;
                            break;

                        case "gameover":
                            bestLevel = obj.getInt("value");
                            System.out.println("Game over! Miglior livello: " + bestLevel);
                            // Registra il miglior punteggio
                            aggiornaClassifica(currentPlayer, bestLevel);
                            currentScore = 0;
                            break;
                    }
                }
            } catch (Exception e) {
                System.err.println("Errore nella lettura dei dati seriali: " + e.getMessage());
            }
        }
    }

    private void aggiornaClassifica(String nome, int punteggio) {
        // Cerca se il giocatore esiste già
        boolean giocatoreTrovato = false;
        for (Giocatore g : classifica) {
            if (g.getNome().equals(nome)) {
                // Aggiorna solo se il punteggio è migliore
                if (punteggio > g.getPunteggio()) {
                    g.setPunteggio(punteggio);
                }
                giocatoreTrovato = true;
                break;
            }
        }

        if (!giocatoreTrovato) {
            classifica.add(new Giocatore(nome, punteggio));
        }

        Collections.sort(classifica, new Comparator<Giocatore>() {
            @Override
            public int compare(Giocatore g1, Giocatore g2) {
                return Integer.compare(g2.getPunteggio(), g1.getPunteggio()); // Decrescente
            }
        });

        // Aggiorna la GUI
        aggiornaTabellaClassifica();
    }

    private void aggiornaTabellaClassifica() {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            for (int i = 0; i < classifica.size(); i++) {
                Giocatore g = classifica.get(i);
                tableModel.addRow(new Object[]{
                        i + 1,
                        g.getNome(),
                        g.getPunteggio()
                });
            }
        });
    }

    private void resetClassifica() {
        int response = JOptionPane.showConfirmDialog(this,
                "Sei sicuro di voler resettare tutta la classifica?",
                "Reset Classifica",
                JOptionPane.YES_NO_OPTION);

        if (response == JOptionPane.YES_OPTION) {
            classifica.clear();
            aggiornaTabellaClassifica();
        }
    }

    public void close() {
        if (data != null) {
            data.close();
        }
        if (port != null && port.isOpen()) {
            port.closePort();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ScoreboardGUI::new);
    }
}
