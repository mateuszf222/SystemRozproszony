# Programowanie usług sieciowych 2025

## Zadania zaliczeniowe
1. Web UI dla `join`
2. Web UI do przeglądania tabel `communication_log` i `execution_log`
3. Rozszerzenie UI do przeglądania tabel o możliwość przeglądania ich na zdalnym węźle  
4. Web UI do usuwania węzła z klastra, obsługa prób ponownego dołączenia 
5. Usuwanie węzła z klastra dla skonfigurowanego czasu braku `heartbeat` od niego
6. Wysyłanie zadania do najmniej obciążonego (z najmniejszą liczbą tasków) węzła
7. Przesyłanie plików między węzłami

* Na ocenę 3 wystarczy realizacja któregokolwiek z punktów 1, 2 lub 6
* Na ocenę 4 wymagam 3 lub 4+5 lub 6
* Na ocenę 5 trzeba zaimplementować 3+7

## Budowa projektu
IntelliJ IDEA lub
```bash
mvn clean install -DskipTests
```

## Uruchomienie
Konfiguracja w `config.json`

Windows
```bash
run
```

Mac/Linux
```
bash run.sh
```

## Wiele instancji
* do pustego katalogu przekopiuj `config.json`, zmień w nim nazwę węzła i porty
* stwórz w nim skrypt który kopiuje do niego plik jar np. `run.cmd` (Windows)
```bash
@echo off
copy ..\pus2025\target\pus2025-1.0-SNAPSHOT.jar .
java --enable-native-access=ALL-UNNAMED -jar pus2025-1.0-SNAPSHOT.jar
```
dopasuj ścieżkę źródłową dla copy!

## Czyszczenie bazy
* skasuj wszystkie pliki z katalogu `data`