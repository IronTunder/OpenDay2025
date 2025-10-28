import java.io.Serializable;

class Giocatore implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nome;
    private int punteggio;
    private long timestamp; // Aggiungiamo un timestamp per tracciare quando è stato fatto il punteggio

    public Giocatore(String nome, int punteggio) {
        this.nome = nome;
        this.punteggio = punteggio;
        this.timestamp = System.currentTimeMillis();
    }

    public String getNome() { return nome; }
    public int getPunteggio() { return punteggio; }
    public long getTimestamp() { return timestamp; }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setPunteggio(int punteggio) {
        this.punteggio = punteggio;
        this.timestamp = System.currentTimeMillis(); // Aggiorna il timestamp
    }

    @Override
    public String toString() {
        return nome + ": " + punteggio + " (il " + new java.util.Date(timestamp) + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Giocatore other = (Giocatore) obj;
        return nome.equals(other.nome);
    }

    @Override
    public int hashCode() {
        return nome.hashCode();
    }
}