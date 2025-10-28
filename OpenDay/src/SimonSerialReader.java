import com.fazecast.jSerialComm.SerialPort;
import org.json.JSONObject;
import java.util.Scanner;

public class SimonSerialReader {
    public static void main(String[] args) {
        SerialPort port = SerialPort.getCommPort("COM3"); //TODO CAMBIA PORTA
        port.setBaudRate(9600);
        port.openPort();

        Scanner data = new Scanner(port.getInputStream());
        int bestLevel = 0;

        while (data.hasNextLine()) {
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
                        int current = obj.getInt("value");
                        if (current > bestLevel) bestLevel = current;
                        System.out.println("Sequenza corretta! Livello " + current);
                        break;
                    case "error":
                        System.out.println("Errore! Hai perso al livello " + obj.getInt("value"));
                        break;
                    case "gameover":
                        System.out.println("Game over! Miglior livello: " + bestLevel);
                        break;
                }
            }
        }
        port.closePort();
    }
}


