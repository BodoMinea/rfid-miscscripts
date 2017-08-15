#include <SPI.h>
#include <MFRC522.h>

#define RST_PIN   9     // pin reset
#define SS_PIN    10    // pin date
// Pinout si getting started: http://playground.arduino.cc/Learning/MFRC522


void setup() {
  Serial.begin(9600);  // initializarea comunicarii serial via usb
  while (!Serial);     // nu actiona daca nu e stabilita conexiunea cu calculatorul
  SPI.begin();         // SPI
  mfrc522.PCD_Init();  // bilbioteca MFRC522 pentru RFID
}

void loop() {
  
  if ( ! mfrc522.PICC_IsNewCardPresent() || ! mfrc522.PICC_ReadCardSerial() ) { // doar citiri noi, nu citirea repetata a cardului apropiat
    return;
  }

  for (byte i = 0; i < mfrc522.uid.size; i++) {
    Serial.print(mfrc522.uid.uidByte[i], HEX); // ID hex pe un rand nou prin portul serial
  } 
  Serial.println();
  
  mfrc522.PICC_HaltA(); // oprire citire
  if ( ! mfrc522.PICC_IsNewCardPresent() || ! mfrc522.PICC_ReadCardSerial() ) { // doar citiri noi, nu citirea repetata a cardului apropiat
    return;
  }

}
