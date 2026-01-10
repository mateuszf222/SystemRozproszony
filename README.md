# Programowanie usług sieciowych 2025

## Zadania zaliczeniowe
1. Web UI dla `join`
1. Web UI do przeglądania tabel `communication_log` i `execution_log`
1. Web UI do usuwania węzła z klastra, obsługa prób ponownego dołączenia 
1. Usuwanie węzła z klastra dla skonfigurowanego czasu braku `heartbeat` od niego
1. Wysyłanie zadania do najmniej obciążonego węzła


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