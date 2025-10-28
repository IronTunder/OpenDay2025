import com.fazecast.jSerialComm.SerialPort;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SerialReaderThread extends Thread {
    private InterfacciaGrafica gui;
    private volatile boolean running = true;
    private SerialPort serialPort;

    public SerialReaderThread(InterfacciaGrafica gui) {
        this.gui = gui;
    }

    @Override
    public void run() {
        // Lista porte disponibili
        SerialPort[] ports = SerialPort.getCommPorts();
        System.out.println("Porte seriali disponibili:");
        for (SerialPort p : ports) {
            System.out.println("Port: " + p.getSystemPortName() + " - " + p.getDescriptivePortName());
        }

        // Configura porta seriale
        serialPort = SerialPort.getCommPort("COM3");
        serialPort.setBaudRate(9600);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);

        if (!serialPort.openPort()) {
            System.err.println("Non posso aprire la porta COM3.");
            JOptionPane.showMessageDialog(gui,
                    "Errore: Impossibile aprire la porta COM3.\n" +
                            "Assicurati che Arduino sia collegato correttamente.",
                    "Errore Connessione",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        System.out.println("Porta aperta: " + serialPort.getSystemPortName());
        System.out.println("In ascolto dei dati dal gioco Simon...");

        while (running) {
            try (InputStream in = serialPort.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = reader.readLine()) != null && running) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    System.out.println("Dato ricevuto: " + line); // Debug

                    try {
                        int val = Integer.parseInt(line);
                        if (val == -1) {
                            gui.resetGiocatoreCorrente();
                            gui.aggiornaPunteggio(gui.getPunteggioCorrente());
                        } else {
                            gui.setPunteggioCorrente(val);
                        }
                    } catch (NumberFormatException nfe) {
                        System.err.println("Linea non valida: \"" + line + "\"");
                    }
                }
            } catch (IOException ignored) {

            }
        }
    }

    public void stopThread() {
        running = false;
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
        }
    }
}