class Giocatore {
    private String nome;
    private int punteggio;

    public Giocatore(String nome, int punteggio) {
        this.nome = nome;
        this.punteggio = punteggio;
    }

    public String getNome() {
        return nome;
    }

    public int getPunteggio() {
        return punteggio;
    }

    public void setPunteggio(int punteggio) {
        this.punteggio = punteggio;
    }

    @Override
    public String toString() {
        return nome + ": " + punteggio;
    }
}