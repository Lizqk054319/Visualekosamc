# KosaVisuals (Fabric 1.21.4, client-side)

Prawy Shift otwiera menu z kolumnami modułów (przeciągaj panele za nagłówek, PPM / strzałka = ustawienia, pole wyszukiwania).
**L** w menu otwiera edytor HUD (przeciągasz Watermark, Spotify i TargetHUD). Pole **Config** jest pod wyszukiwarką.

## Moduły
- **PVP:** Hitbox, **TargetHUD** (kolor + gradient), **Target ESP** (świecący pierścień / okrąg wokół celu, kolor),
  **Kill Effect** (tryb, czas ducha, wysokość wyrzutu, kolor ducha, kolor pioruna), **Damage Numbers** (rozmiar, kolor).
- **Dźwięki:** Hit Sound, Kill Sound, Click Sound. Własne .wav: przeciągnij na okno gry przy otwartym menu
  albo wrzuć do `config/holovis/sounds`.
- **Świat:** Custom Sky – gotowe nieba (Sakura, Zachód słońca, Neon, Galaxy, Fiolet, Lodowe, Toxic, Krwawe, Mięta,
  Złota godzina) albo własne kolory nieba i horyzontu.
- **HUD:** Watermark (nazwa, FPS, koordynaty – każde do wyłączenia, kolor + gradient), Spotify (kolor + gradient),
  **Cooldowns** (lista przedmiotów z cooldownem i czasem, który im został).
- **Klient:** Akcent GUI (kolor, gradient, tło, kolor napisu, efekt hologramu).

Wybór koloru: kliknij wiersz z kolorem – obok panelu otwiera się okienko z kółkiem (odcień + nasycenie), paletą i suwakami
nasycenia i jasności. Tęcza jest tylko w Hitboxie, Watermarku i kolorze napisu GUI.

## Komendy
- `/kosa` albo `/holovis` – otwiera menu
- `/kosa ghost` – duch gracza „TestBot” widoczny tylko dla Ciebie (można go bić; nic nie idzie na serwer)
- `/kosa ghost remove` – usuwa go
- `/kosa killtest` – podgląd Kill Effectu

## Spotify
Windows 10/11: odczyt i sterowanie przez systemową sesję multimediów (bez logowania).
Otwórz czat i najedź na element Spotify: tekst piosenki (lrclib.net, długie linie się zawijają), pasek przewijania i przyciski.
Skróty (Sterowanie → KosaVisuals): `[` poprzedni, `]` następny, `\` pauza/play.

## Configi
Pole Config pod wyszukiwarką: **Kopiuj kod** (kod `HV1:...` trafia do schowka) i **Load config** (wczytuje kod ze schowka).

## Uwaga przy aktualizacji
Plik moda nazywa się teraz `KosaVisuals-1.1.0.jar`. Usuń ze `mods` stary `holovis-*.jar`.
