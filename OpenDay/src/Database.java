import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private static final String FILE_NAME = "classifica.dat";

    public static void salvaClassifica(ArrayList<Giocatore> classifica) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(classifica);
            System.out.println("Classifica salvata correttamente su " + FILE_NAME);
        } catch (IOException e) {
            System.err.println("Errore nel salvataggio della classifica: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<Giocatore> caricaClassifica() {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            System.out.println("Nessun file di classifica trovato. Verrà creata una nuova classifica.");
            return new ArrayList<>();
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_NAME))) {
            ArrayList<Giocatore> classifica = (ArrayList<Giocatore>) ois.readObject();
            System.out.println("Classifica caricata correttamente. " + classifica.size() + " giocatori trovati.");
            return classifica;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Errore nel caricamento della classifica: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public static void salvaClassificaTesto(ArrayList<Giocatore> classifica) {
        String textFileName = "classifica.csv";
        try (PrintWriter writer = new PrintWriter(new FileWriter(textFileName))) {
            writer.println("Posizione,Nome,Punteggio");
            for (int i = 0; i < classifica.size(); i++) {
                Giocatore g = classifica.get(i);
                writer.println((i + 1) + "," + g.getNome() + "," + g.getPunteggio());
            }
            System.out.println("Classifica esportata in formato testo: " + textFileName);
        } catch (IOException e) {
            System.err.println("Errore nell'esportazione della classifica: " + e.getMessage());
        }
    }
}