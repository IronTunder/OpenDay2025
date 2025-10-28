#include <Wire.h>
#include <LiquidCrystal_I2C.h>

LiquidCrystal_I2C lcd(0x27, 20, 4);

int stato = 0;

int seq[100];
int livello = 0;
int indice_ripeti = 0;

int note[4] = {261, 294, 329, 349};
int bottoni[4] = {2, 3, 4, 5};
int led[4] = {10, 11, 12, 13};

int buzzer = 6;

unsigned long ultimo_tempo = 0;
bool premuto[4] = {false, false, false, false};
bool inviato_livello = false;

void setup() { 
  Serial.begin(9600);
  lcd.init();
  lcd.backlight();
  
  // Inizializza LED
  for (int i = 0; i < 4; i++) {
    pinMode(led[i], OUTPUT);
  }
  
  // Inizializza bottoni
  for (int i = 0; i < 4; i++) {
    pinMode(bottoni[i], INPUT_PULLUP);
  }
  
  pinMode(buzzer, OUTPUT);
  
  randomSeed(analogRead(A0));
  
  lcd.setCursor(0, 0);
  lcd.print("  GIOCO SIMON");
  lcd.setCursor(0, 1);
  lcd.print("Premi un bottone");
  lcd.setCursor(0, 2);
  lcd.print("per iniziare!");
}

void loop() {
  switch (stato) {
    case 0: // Attesa inizio
      attesaInizio();
      break;
    
    case 1: // Sorteggia nuova sequenza
      sorteggia();
      break;
    
    case 2: // Ripeti sequenza
      ripeti();
      break;

    case 9: // Errore
      errore();
      break;
    
    case 10: // Reset gioco
      reset();
      break;
  }
}

void attesaInizio() {
  for (int i = 0; i < 4; i++) {
    if (digitalRead(bottoni[i]) == LOW) {
      // Reset completo prima di iniziare una nuova partita
      resetCompleto();
      stato = 1;
      inviato_livello = false;
      lcd.clear();
      break;
    }
  }
}

void resetCompleto() {
  // Azzera completamente livello e sequenza
  livello = 0;
  indice_ripeti = 0;
  
  // Opzionale: azzera l'array seq per sicurezza
  for (int i = 0; i < 100; i++) {
    seq[i] = 0;
  }
  
  // Reset array premuto
  for (int i = 0; i < 4; i++) {
    premuto[i] = false;
  }
}

void sorteggia() {
  delay(1000);
  
  // Sorteggia il nuovo LED
  int nuovo_led = random(0, 4);
  seq[livello] = nuovo_led;
  livello++;
  
  // Invia il livello corrente al computer
  Serial.println(livello);
  
  // Mostra livello corrente
  lcd.setCursor(0, 0);
  lcd.print("Livello: ");
  lcd.print(livello);
  lcd.print("   ");
  
  // Riproduci sequenza completa
  for (int i = 0; i < livello; i++) {
    int colore = seq[i];
    accendiLed(colore);
    delay(300);
    spegniTuttiLed();
    delay(200);
  }
  
  indice_ripeti = 0;
  stato = 2;
  
  lcd.setCursor(0, 1);
  lcd.print("Ripeti:         ");
}

void ripeti() {
  for (int i = 0; i < 4; i++) {
    if (digitalRead(bottoni[i]) == LOW && !premuto[i]) {
      premuto[i] = true;
      delay(50); // Debounce
      
      if (digitalRead(bottoni[i]) == LOW) {
        accendiLed(i);
        
        // Controlla se è il colore corretto
        if (seq[indice_ripeti] == i) {
          // Corretto!
          indice_ripeti++;
          delay(50);
          
          lcd.setCursor(9, 1);
          lcd.print(indice_ripeti);
          lcd.print("/");
          lcd.print(livello);
          spegniTuttiLed();
          if (indice_ripeti == livello) {
            vittoria();
            stato = 1;
          }
        } else {
          stato = 9;
        }
      }
    } else if (digitalRead(bottoni[i]) == HIGH) {
      premuto[i] = false;
    }
  }
}

void accendiLed(int colore) {
  digitalWrite(led[colore], HIGH);
  tone(buzzer, note[colore], 300);
}

void spegniTuttiLed() {
  for (int i = 0; i < 4; i++) {
    digitalWrite(led[i], LOW);
  }
}

void vittoria() {
  spegniTuttiLed();
  // Animazione vittoria
  for (int j = 0; j < 3; j++) {
    for (int i = 0; i < 4; i++) {
      digitalWrite(led[i], HIGH);
      tone(buzzer, note[i], 100);
    }
    delay(200);
    spegniTuttiLed();
    delay(200);
  }
  
  lcd.setCursor(0, 2);
  lcd.print("Corretto!       ");
  delay(1000);
  lcd.setCursor(0, 2);
  lcd.print("                ");
}

void errore() {
  spegniTuttiLed();
  // Invia game over al computer
  Serial.println(-1);
  
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Hai Sbagliato!");
  lcd.setCursor(0, 1);
  lcd.print("Livello: ");
  lcd.print(livello);
  lcd.setCursor(0, 2);
  lcd.print("Game Over!");
  
  // Suono e luce di errore
  for (int i = 0; i < 5; i++) {
    for (int j = 0; j < 4; j++) {
      digitalWrite(led[j], HIGH);
    }
    tone(buzzer, 150, 300);
    delay(300);
    spegniTuttiLed();
    delay(300);
  }
  
  delay(2000);
  stato = 10;
}

void reset() {
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Premi un bottone");
  lcd.setCursor(0, 1);
  lcd.print("per ricominciare");
  
  // NON azzerare livello qui, verrà fatto in resetCompleto()
  // quando si preme un bottone per iniziare
  
  stato = 0;
}