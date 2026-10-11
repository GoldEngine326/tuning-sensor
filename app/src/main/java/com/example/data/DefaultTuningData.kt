package com.example.data

import com.example.model.CalibrationPoint
import com.example.model.CharType
import com.example.model.CharacteristicInfo
import com.example.model.ComponentCategory
import com.example.model.DeadTimePoint
import com.example.model.DeadTimeTable
import com.example.model.PinInfo
import com.example.model.PinType
import com.example.model.TuningComponent

object DefaultTuningData {

  fun getInitialComponents(): List<TuningComponent> {
    return listOf(
      // 1. Cewki czerwone z VW (Audi R8 / TFSI)
      TuningComponent(
        id = "vw_red_coils_r8",
        name = "Cewki czerwone z VW (TFSI / R8)",
        partNumber = "06E 905 115 E / 06E 905 115 F / NGK U5014",
        category = ComponentCategory.COILS,
        shortDescription = "Aktywne cewki zapłonowe z wbudowanym igniterem stosowane m.in. w silnikach 2.0 TFSI, 4.2 FSI i V10 Audi R8. Bardzo popularny upgrade w 1.8T 20V i projektach standalone.",
        imageUrl = "https://images.unsplash.com/photo-1619642751034-765dfdf7c58e?auto=format&fit=crop&w=800&q=80",
        vectorIllustrationKey = "vw_coils",
        pinout = listOf(
          PinInfo(
            pinNumber = "Pin 1",
            name = "+12V Zasilanie główne",
            description = "Zasilanie z przekaźnika po stacyjce / przekaźnika pompy paliwa. Wymagany bezpiecznik 15A lub 20A na parę cewek.",
            wireColor = "Czerwony / Fioletowy",
            type = PinType.POWER
          ),
          PinInfo(
            pinNumber = "Pin 2",
            name = "Masa iskry / głowicy (Power GND)",
            description = "Obwód wtórny wysokiego napięcia. Musi być przykręcony bezpośrednio do głowicy silnika grubym przewodem!",
            wireColor = "Brązowy",
            type = PinType.GROUND
          ),
          PinInfo(
            pinNumber = "Pin 3",
            name = "Sygnał wyzwalający (Trigger 5V TTL)",
            description = "Impuls wyzwalający z ECU (np. uaefi, EMU Black, Ecumaster, Megasquirt). Logika 5V TTL. Czas trwania impulsu to czas ładowania (Dwell).",
            wireColor = "Żółty / Szary",
            type = PinType.IGNITION
          ),
          PinInfo(
            pinNumber = "Pin 4",
            name = "Masa logiczna / akumulatora (Logic GND)",
            description = "Masa obwodu sterującego tranzystora. Podłączana do masy silnika lub masy logicznej ECU.",
            wireColor = "Brązowy / Czarny",
            type = PinType.GROUND
          )
        ),
        characteristic = CharacteristicInfo(
          type = CharType.DWELL_TIME,
          title = "Zalecane czasy ładowania cewki (Dwell Time)",
          inputLabel = "Napięcie instalacji (V)",
          outputLabel = "Czas ładowania (ms)",
          points = listOf(
            CalibrationPoint("10.0", "V", "3.20", "ms"),
            CalibrationPoint("11.0", "V", "2.80", "ms"),
            CalibrationPoint("12.0", "V", "2.50", "ms"),
            CalibrationPoint("13.0", "V", "2.30", "ms"),
            CalibrationPoint("14.0", "V", "2.10", "ms"),
            CalibrationPoint("15.0", "V", "1.90", "ms")
          ),
          formulaOrNotes = "Optymalny dwell przy 13.8V-14.2V to około 2.1 - 2.3 ms. Nie przekraczać 2.8 ms przy normalnej pracy, aby uniknąć przegrzania cewki i jej uszkodzenia."
        ),
        pullupRequired = false,
        importantNotes = "• Wtyczka: 1J0 973 724 (4-pin VAG).\n• Jest to cewka aktywna (ze zintegrowanym modułem zapłonowym), więc wymaga wyjścia logicznego 5V z ECU, a NIE wyjścia masowego dla cewek pasywnych!\n• W przypadku montażu w 1.8T wymagane są adaptery montażowe do pokrywy zaworów.",
        authorNick = "TunerPRO_PL",
        rating = 4.95f,
        ratingVotesCount = 42
      ),

      // 2. Mapsensor 3 bar stosowany w grupie VW (z charakterystyką napięciową MAP ORAZ opornościową IAT!)
      TuningComponent(
        id = "vw_mapsensor_3bar",
        name = "MAP Sensor 3 Bar VAG (Bosch)",
        partNumber = "0 281 002 401 / 038 906 051 AC / 0 281 002 976",
        category = ComponentCategory.MAP_SENSOR,
        shortDescription = "Zintegrowany czujnik ciśnienia doładowania (MAP) i temperatury powietrza dolotowego (IAT) z grupy VW/Audi/Seat/Skoda (np. 1.9 TDI ARL/ASZ, 2.0 TDI). Mierzy do 300 kPa ciśnienia absolutnego (czyli 2.0 bar doładowania).",
        imageUrl = "https://images.unsplash.com/photo-1580273916550-e323be2ae537?auto=format&fit=crop&w=800&q=80",
        vectorIllustrationKey = "map_3b",
        pinout = listOf(
          PinInfo(
            pinNumber = "Pin 1",
            name = "Masa czujnika (Sensor GND)",
            description = "Masa czujników z ECU (odizolowana od masy zasilającej, nie podłączać do karoserii).",
            wireColor = "Brązowy / Niebieski",
            type = PinType.SENSOR_GND
          ),
          PinInfo(
            pinNumber = "Pin 2",
            name = "Sygnał temperatury (IAT NTC)",
            description = "Termistor NTC do wejścia pomiaru temperatury powietrza w ECU. Zmiana rezystancji w zależności od temperatury (wymaga pull-upa 2.49 kΩ).",
            wireColor = "Czarny / Fioletowy",
            type = PinType.SIGNAL_OUT
          ),
          PinInfo(
            pinNumber = "Pin 3",
            name = "Zasilanie referencyjne +5V (VREF)",
            description = "Zasilanie 5.00V ± 0.05V z dedykowanego wyjścia 5V w sterowniku silnika.",
            wireColor = "Niebieski / Czerwony",
            type = PinType.VREF
          ),
          PinInfo(
            pinNumber = "Pin 4",
            name = "Sygnał ciśnienia doładowania (MAP Signal)",
            description = "Napięciowy sygnał analogowy 0.4V - 4.65V proporcjonalny do ciśnienia absolutnego.",
            wireColor = "Żółty / Zielony",
            type = PinType.SIGNAL_OUT
          )
        ),
        characteristic = CharacteristicInfo(
          type = CharType.VOLTAGE_PRESSURE,
          title = "Charakterystyka napięciowa MAP (Ciśnienie vs Napięcie)",
          inputLabel = "Ciśnienie bezwzględne (kPa / bar)",
          outputLabel = "Napięcie wyjściowe (V)",
          points = listOf(
            CalibrationPoint("20 kPa (0.2 bar abs)", "kPa", "0.40", "V"),
            CalibrationPoint("50 kPa (0.5 bar abs)", "kPa", "0.93", "V"),
            CalibrationPoint("100 kPa (1.0 bar / atmosf.)", "kPa", "1.82", "V"),
            CalibrationPoint("150 kPa (+0.5 bar boost)", "kPa", "2.70", "V"),
            CalibrationPoint("200 kPa (+1.0 bar boost)", "kPa", "3.58", "V"),
            CalibrationPoint("250 kPa (+1.5 bar boost)", "kPa", "4.46", "V"),
            CalibrationPoint("300 kPa (+2.0 bar boost)", "kPa", "4.65", "V")
          ),
          formulaOrNotes = "Wzór kalibracji MAP: V = 0.01518 * p + 0.0964 [V] (gdzie p w kPa).\nWspółczynnik nachylenia (gain): ~65.88 kPa/V. Offset: 13.6 kPa."
        ),
        // CHARAKTERYSTYKA OPORNOŚCIOWA DLA ZINTEGROWANEGO CZUJNIKA TEMPERATURY IAT:
        resistanceCharacteristic = CharacteristicInfo(
          type = CharType.RESISTANCE_TEMP,
          title = "Charakterystyka opornościowa IAT NTC (Pin 2: Temp vs Rezystancja)",
          inputLabel = "Temperatura powietrza (°C)",
          outputLabel = "Opór NTC (Ω)",
          points = listOf(
            CalibrationPoint("-20", "°C", "15000", "Ω"),
            CalibrationPoint("-10", "°C", "9200", "Ω"),
            CalibrationPoint("0", "°C", "5800", "Ω"),
            CalibrationPoint("10", "°C", "3800", "Ω"),
            CalibrationPoint("20", "°C", "2500", "Ω"),
            CalibrationPoint("30", "°C", "1700", "Ω"),
            CalibrationPoint("40", "°C", "1200", "Ω"),
            CalibrationPoint("60", "°C", "600", "Ω"),
            CalibrationPoint("80", "°C", "320", "Ω"),
            CalibrationPoint("100", "°C", "180", "Ω")
          ),
          formulaOrNotes = ""
        ),
        pullupRequired = true,
        pullupRecommendedValue = "2.49 kΩ (do +5V VREF)",
        pullupNote = "⚠️ Pin 2 (Temperatura IAT) to pasywny termistor NTC. Wymaga podciągnięcia rezystorem Pull-Up 2.49 kΩ (lub 2.7 kΩ) do szyny +5V VREF w sterowniku ECU, jeśli wejście analogowe nie posiada wbudowanego sprzętowego pull-upa!",
        importantNotes = "• Wtyczka: 1J0 973 704 (4-pin w jednym rzędzie, ząbek prowadzący).\n• Średnica króćca montażowego: 12 mm lub 18 mm (w zależności od obudowy, popularny adapter spawany w rurę IC).\n• Należy stosować o-ring odporny na olej i temperaturę dolotu.",
        authorNick = "VAG_Tuning_Lab",
        rating = 4.90f,
        ratingVotesCount = 38
      ),

      // 3. Pinout sterownika UAEFI Standalone ECU
      TuningComponent(
        id = "ecu_uaefi_standalone",
        name = "Sterownik UAEFI Standalone ECU",
        partNumber = "UAEFI v1.x / v2.x (STM32 / Speeduino Core)",
        category = ComponentCategory.ECU,
        shortDescription = "Otwarty i wszechstronny sterownik silnika typu Standalone bazujący na architekturze STM32 i ekosystemie Speeduino/TunerStudio. Obsługuje silniki 4, 6 i 8 cylindrowe z sekwencyjnym lub półsekwencyjnym wtryskiem i zapłonem.",
        imageUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=800&q=80",
        vectorIllustrationKey = "uaefi",
        pinout = listOf(
          PinInfo("Pin 1", "+12V Główny (Switched 12V)", "Zasilanie po stacyjce przez przekaźnik główny ECU. Bezpiecznik 10A.", "Czerwony", PinType.POWER),
          PinInfo("Pin 2", "Masa zasilania (Power GND 1)", "Masa wysokoprądowa do głowicy/bloku silnika. Przekrój min. 1.5mm².", "Czarny", PinType.GROUND),
          PinInfo("Pin 3", "Masa zasilania (Power GND 2)", "Druga masa wysokoprądowa dla sterowników wtryskiwaczy.", "Czarny", PinType.GROUND),
          PinInfo("Pin 4", "Masa sensoryczna (Sensor GND)", "Dedykowana masa wyłącznie dla czujników (TPS, MAP, CLT, IAT). Nigdy nie łączyć z masą budy!", "Brązowy", PinType.SENSOR_GND),
          PinInfo("Pin 5", "+5V VREF (Sensor Reference)", "Stabilizowane napięcie referencyjne 5.0V z wewnętrznej przetwornicy dla TPS i MAP.", "Pomarańczowy", PinType.VREF),
          PinInfo("Pin 6", "Wejście wału korbowego (Crank VR/Hall)", "Sygnał obrotów z koła fonicznego (60-2, 36-1 itp.). Zworka VR/Hall na PCB.", "Biały (Ekran)", PinType.SIGNAL_IN),
          PinInfo("Pin 7", "Wejście wałka rozrządu (Cam Sync)", "Sygnał fazy rozrządu (1 ząb / koło rozrządu) dla pełnej sekwencji.", "Niebieski (Ekran)", PinType.SIGNAL_IN),
          PinInfo("Pin 8", "Wejście TPS (Throttle Position)", "Sygnał położenia przepustnicy 0-5V.", "Żółty", PinType.SIGNAL_IN),
          PinInfo("Pin 9", "Wejście CLT (Coolant Temp)", "Temperatura płynu chłodzącego NTC. Wbudowany rezystor pull-up 2.49kΩ na płycie.", "Fioletowy", PinType.SIGNAL_IN),
          PinInfo("Pin 10", "Wejście IAT (Intake Air Temp)", "Temperatura powietrza dolotowego NTC. Wbudowany rezystor pull-up 2.49kΩ na płycie.", "Szary", PinType.SIGNAL_IN),
          PinInfo("Pin 11", "Wejście MAP (External Boost)", "Wejście zewnętrznego czujnika ciśnienia 0-5V lub opcjonalny pokładowy sensor.", "Zielony", PinType.SIGNAL_IN),
          PinInfo("Pin 12", "Wejście szerokopasmowe AFR (O2/Lambda)", "Wejście analogowe 0-5V z zewnętrznego kontrolera (Spartan, AEM, LSU).", "Biały", PinType.SIGNAL_IN),
          PinInfo("Pin 13", "Wyjście Wtrysk 1 (INJ 1)", "Sterowanie wtryskiwaczem 1 (Low Side - zwiera do masy). Max 5A.", "Czerwony/Biały", PinType.INJECTOR),
          PinInfo("Pin 14", "Wyjście Wtrysk 2 (INJ 2)", "Sterowanie wtryskiwaczem 2 (Low Side - zwiera do masy). Max 5A.", "Czerwony/Biały", PinType.INJECTOR),
          PinInfo("Pin 15", "Wyjście Wtrysk 3 (INJ 3)", "Sterowanie wtryskiwaczem 3 (Low Side - zwiera do masy). Max 5A.", "Czerwony/Biały", PinType.INJECTOR),
          PinInfo("Pin 16", "Wyjście Wtrysk 4 (INJ 4)", "Sterowanie wtryskiwaczem 4 (Low Side - zwiera do masy). Max 5A.", "Czerwony/Biały", PinType.INJECTOR),
          PinInfo("Pin 17", "Wyjście Zapłon 1 (IGN 1)", "Trigger logiczny 5V TTL dla cewek aktywnych (np. cewki R8/TFSI).", "Niebieski/Biały", PinType.IGNITION),
          PinInfo("Pin 18", "Wyjście Zapłon 2 (IGN 2)", "Trigger logiczny 5V TTL dla cewki 2.", "Niebieski/Biały", PinType.IGNITION),
          PinInfo("Pin 19", "Wyjście Zapłon 3 (IGN 3)", "Trigger logiczny 5V TTL dla cewki 3.", "Niebieski/Biały", PinType.IGNITION),
          PinInfo("Pin 20", "Wyjście Zapłon 4 (IGN 4)", "Trigger logiczny 5V TTL dla cewki 4.", "Niebieski/Biały", PinType.IGNITION),
          PinInfo("Pin 21", "Przekaźnik pompy paliwa (Fuel Pump)", "Wyjście sterujące masą przekaźnika pompy paliwa (Low Side).", "Niebieski", PinType.SIGNAL_OUT),
          PinInfo("Pin 22", "Zawór Boost / Wolne obroty (PWM Out)", "Wyjście PWM dla elektrozaworu MAC (doładowanie) lub zaworu IACV.", "Brązowy/Żółty", PinType.SIGNAL_OUT),
          PinInfo("Pin 23", "CAN-H (CAN Bus High)", "Magistrala CAN 2.0B (np. 500 kbps lub 1 Mbps) do wyświetlaczy dash i modułów.", "Żółty/Czarny", PinType.COMMUNICATION),
          PinInfo("Pin 24", "CAN-L (CAN Bus Low)", "Magistrala CAN 2.0B linia niska.", "Zielony/Czarny", PinType.COMMUNICATION)
        ),
        characteristic = CharacteristicInfo(
          type = CharType.ECU_SPEC,
          title = "Specyfikacja elektryczna UAEFI Standalone",
          inputLabel = "Kanał / Szyna",
          outputLabel = "Parametr / Zakres roboczy",
          points = listOf(
            CalibrationPoint("Napięcie zasilania", "V", "9.0 - 16.5", "V DC"),
            CalibrationPoint("Wyjścia wtryskiwaczy (x4)", "A", "Low-Side max 5A ciągły", "na kanał"),
            CalibrationPoint("Wyjścia cewek (x4)", "V", "Logiczne 5V TTL (High-Active)", "dla cewek aktywnych"),
            CalibrationPoint("Wejścia analogowe (0-5V)", "bit", "12-bit ADC (4096 kroków)", "zabezpieczenie 18V"),
            CalibrationPoint("Pull-up CLT/IAT na PCB", "kΩ", "2.49 kΩ do 5.0V", "standard Bosch NTC"),
            CalibrationPoint("Magistrala komunikacyjna", "typ", "USB-C + CAN Bus 2.0B", "TunerStudio MS")
          ),
          formulaOrNotes = "UAEFI posiada wbudowany interfejs USB do połączenia z programem TunerStudio. Wbudowane pull-upy 2.49kΩ na liniach CLT i IAT eliminują konieczność lutowania zewnętrznych rezystorów."
        ),
        pullupRequired = false,
        pullupRecommendedValue = "Wbudowany 2.49 kΩ na płycie",
        pullupNote = "Informacja dla użytkownika: Płyta sterownika UAEFI posiada już zintegrowane precyzyjne rezystory pull-up 2.49kΩ 1% dla wejść czujników temperatury IAT i CLT.",
        importantNotes = "• Zawsze rozdzielaj masę zasilania (Power GND) od masy czujników (Sensor GND).\n• Przewody czujników wału korbowego (Crank) i wałka (Cam) muszą biec w dedykowanym ekranie podłączonym do Sensor GND tylko z jednej strony (przy sterowniku)!\n• Przed pierwszym rozruchem sprawdź poprawność mapy dwell i kolejności zapłonu (1-3-4-2 w 4-cyl).",
        authorNick = "ECU_Engineer_PL",
        rating = 4.98f,
        ratingVotesCount = 56
      ),

      // 4. Czujnik temperatury dolotu IAT Bosch NTC M12 (z pełną charakterystyką opornościową NTC!)
      TuningComponent(
        id = "bosch_iat_0280130085",
        name = "Czujnik IAT Bosch NTC M12 (Otwarty element)",
        partNumber = "0 280 130 085 / 0280130085",
        category = ComponentCategory.TEMP_SENSOR,
        shortDescription = "Kultowy czujnik temperatury powietrza w dolocie o otwartej konstrukcji termistora. Charakteryzuje się błyskawiczną reakcją termiczną (czas odpowiedzi poniżej 1 sekundy), idealny przy strojeniu turbodoładowanych silników na standalone.",
        imageUrl = "https://images.unsplash.com/photo-1581092160607-ee22621dd758?auto=format&fit=crop&w=800&q=80",
        vectorIllustrationKey = "bosch_iat",
        pinout = listOf(
          PinInfo("Pin 1", "Sygnał czujnika / Wejście ECU", "Połączenie z wejściem IAT w ECU oraz rezystorem pull-up 2.49kΩ.", "Biały", PinType.SIGNAL_IN),
          PinInfo("Pin 2", "Masa sensoryczna (Sensor GND)", "Dedykowana masa czujników z ECU.", "Brązowy", PinType.SENSOR_GND)
        ),
        characteristic = CharacteristicInfo(
          type = CharType.RESISTANCE_TEMP,
          title = "Charakterystyka opornościowa NTC (Temperatura vs Rezystancja)",
          inputLabel = "Temperatura powietrza dolotu (°C)",
          outputLabel = "Opór termistora NTC (Ω)",
          points = listOf(
            CalibrationPoint("-20", "°C", "15462", "Ω"),
            CalibrationPoint("-10", "°C", "9397", "Ω"),
            CalibrationPoint("0", "°C", "5896", "Ω"),
            CalibrationPoint("10", "°C", "3792", "Ω"),
            CalibrationPoint("20", "°C", "2500", "Ω"),
            CalibrationPoint("30", "°C", "1707", "Ω"),
            CalibrationPoint("40", "°C", "1175", "Ω"),
            CalibrationPoint("50", "°C", "834", "Ω"),
            CalibrationPoint("60", "°C", "596", "Ω"),
            CalibrationPoint("80", "°C", "323", "Ω"),
            CalibrationPoint("100", "°C", "187", "Ω"),
            CalibrationPoint("120", "°C", "113", "Ω")
          ),
          formulaOrNotes = ""
        ),
        pullupRequired = true,
        pullupRecommendedValue = "2.49 kΩ (1% precyzyjny do +5V VREF)",
        pullupNote = "⚠️ KONIECZNE ZASTOSOWANIE REZYSTORA PULL-UP:\nCzujnik temperatury NTC jest elementem pasywnym zmiennooporowym. Wejście analogowe mikrokontrolera mierzy wyłącznie napięcie (0-5V). Aby zmierzyć opór, konieczny jest dzielnik napięcia z rezystorem pull-up (najczęściej 2.49 kΩ lub 2.7 kΩ) podłączonym pomiędzy linię sygnałową a zasilanie +5V VREF.\n(Jeśli Twój sterownik np. EMU Black lub uaefi ma wbudowany pullup na wejściu IAT, dodatkowy rezystor nie jest potrzebny).",
        importantNotes = "• Gwint montażowy: M12 x 1.5.\n• Wtyczka: Bosch EV1 / Junior Power Timer (JPT) 2-pin.\n• Moment dokręcania: max 15 Nm w aluminiowym króćcu dolotu.",
        authorNick = "Bartek_Motorsport",
        rating = 4.92f,
        ratingVotesCount = 29
      ),

      // 5. Czujnik temperatury cieczy chłodzącej Bosch CLT (NTC)
      TuningComponent(
        id = "bosch_clt_0280130026",
        name = "Czujnik CLT Bosch NTC (Temp. Cieczy Chłodzącej)",
        partNumber = "0 280 130 026 / 0280130026 / VAG 059 919 501 A",
        category = ComponentCategory.TEMP_SENSOR,
        shortDescription = "Standardowy czujnik temperatury płynu chłodzącego (Coolant Temp Sensor) NTC stosowany powszechnie w silnikach VAG, BMW, Opel oraz projektach motorsportowych na standalone.",
        imageUrl = "https://images.unsplash.com/photo-1517524008697-84bbe3c3fd98?auto=format&fit=crop&w=800&q=80",
        vectorIllustrationKey = "bosch_iat",
        pinout = listOf(
          PinInfo("Pin 1", "Sygnał CLT (Wejście do ECU)", "Podłącz do wejścia Coolant Temp w sterowniku oraz rezystora Pull-up.", "Biały / Fioletowy", PinType.SIGNAL_IN),
          PinInfo("Pin 2", "Masa czujników (Sensor GND)", "Dedykowana odizolowana masa czujników z ECU.", "Brązowy", PinType.SENSOR_GND)
        ),
        characteristic = CharacteristicInfo(
          type = CharType.RESISTANCE_TEMP,
          title = "Charakterystyka opornościowa NTC (Temperatura Cieczy vs Rezystancja)",
          inputLabel = "Temperatura płynu chłodzącego (°C)",
          outputLabel = "Opór czujnika NTC (Ω)",
          points = listOf(
            CalibrationPoint("-30", "°C", "26114", "Ω"),
            CalibrationPoint("-20", "°C", "15462", "Ω"),
            CalibrationPoint("-10", "°C", "9397", "Ω"),
            CalibrationPoint("0", "°C", "5896", "Ω"),
            CalibrationPoint("20", "°C", "2500", "Ω"),
            CalibrationPoint("40", "°C", "1175", "Ω"),
            CalibrationPoint("60", "°C", "596", "Ω"),
            CalibrationPoint("80", "°C", "323", "Ω"),
            CalibrationPoint("90", "°C", "243", "Ω"),
            CalibrationPoint("100", "°C", "187", "Ω"),
            CalibrationPoint("110", "°C", "144", "Ω"),
            CalibrationPoint("120", "°C", "113", "Ω")
          ),
          formulaOrNotes = "Standardowa krzywa kalibracji NTC Bosch CLT do wpisania w TunerStudio / EMU Client. Rezystancja maleje wykładniczo ze wzrostem temperatury silnika."
        ),
        pullupRequired = true,
        pullupRecommendedValue = "2.49 kΩ (do +5V VREF)",
        pullupNote = "⚠️ Wymaga rezystora pull-up 2.49 kΩ do szyny 5V w ECU, o ile sterownik nie ma sprzętowego pullupa na wejściu CLT.",
        importantNotes = "• Gwint montażowy: M12 x 1.5 lub szybkozłączka z zawleczką C-clip.\n• Zawsze wymieniaj miedzianą podkładkę lub oring przy montażu nowego czujnika.",
        authorNick = "Tuner_Service_PL",
        rating = 4.94f,
        ratingVotesCount = 31
      ),

      // 6. Szerokopasmowa sonda Bosch LSU 4.9
      TuningComponent(
        id = "bosch_lsu_49_lambda",
        name = "Sonda Szerokopasmowa Bosch LSU 4.9",
        partNumber = "0 258 017 025 / 0258017025",
        category = ComponentCategory.LAMBDA,
        shortDescription = "Standard przemysłowy w sondach szerokopasmowych AFR. Zapewnia precyzyjny odczyt składu mieszanki od bogatej (Lambda 0.65) do ubogiej (powietrze wolne). Wymaga dedykowanego kontrolera (CJ125 lub analogowego).",
        imageUrl = "https://images.unsplash.com/photo-1508974239320-0a029497e820?auto=format&fit=crop&w=800&q=80",
        vectorIllustrationKey = "lsu_49",
        pinout = listOf(
          PinInfo("Pin 1", "Ip (Prąd komórki pompującej)", "Pomiar prądu pompy jonowej tlenu (Pump Current).", "Czerwony", PinType.SIGNAL_IN),
          PinInfo("Pin 2", "Vm (Wirtualna masa komórki)", "Punkt odniesienia dla pomiaru Nernsta i komórki pompy.", "Żółty", PinType.SENSOR_GND),
          PinInfo("Pin 3", "H- (Grzałka minus / PWM)", "Sterowanie grzałką poprzez Low-Side FET z kontrolera.", "Biały", PinType.HEATER),
          PinInfo("Pin 4", "H+ (Grzałka plus +12V)", "Zasilanie grzałki sondy z bezpiecznika 10A.", "Szary", PinType.POWER),
          PinInfo("Pin 5", "Ia / Rcal (Rezystor kalibracyjny)", "Laserowo dostrojony rezystor kalibracyjny we wtyczce sondy.", "Zielony", PinType.SIGNAL_IN),
          PinInfo("Pin 6", "Vs / Un (Napięcie komórki Nernsta)", "Napięcie elektrody pomiarowej Nernsta (cel 450mV).", "Czarny", PinType.SIGNAL_IN)
        ),
        characteristic = CharacteristicInfo(
          type = CharType.VOLTAGE_PRESSURE,
          title = "Zależność prądu komórki pompującej Ip vs Lambda",
          inputLabel = "Współczynnik Lambda (λ) / AFR Benzyna",
          outputLabel = "Prąd komórki Ip (mA)",
          points = listOf(
            CalibrationPoint("λ 0.70 (AFR 10.29)", "λ", "-1.48", "mA"),
            CalibrationPoint("λ 0.80 (AFR 11.76)", "λ", "-0.85", "mA"),
            CalibrationPoint("λ 0.90 (AFR 13.23)", "λ", "-0.38", "mA"),
            CalibrationPoint("λ 1.00 (AFR 14.70 stechiom.)", "λ", "0.00", "mA"),
            CalibrationPoint("λ 1.10 (AFR 16.17)", "λ", "+0.32", "mA"),
            CalibrationPoint("λ 1.30 (AFR 19.11)", "λ", "+0.78", "mA"),
            CalibrationPoint("λ 2.00 (AFR 29.40)", "λ", "+1.56", "mA")
          ),
          formulaOrNotes = "Rezystancja wewnętrzna komórki Ri w temperaturze roboczej 780°C wynosi równo 300 Ω. Kontroler utrzymuje tę temperaturę poprzez modulację PWM grzałki."
        ),
        pullupRequired = false,
        importantNotes = "• Wtyczka: Kompaktowa 6-pinowa z wbudowanym rezystorem kalibracyjnym Rcal w obudowie wtyku.\n• ZAWSZE montować sondę pod kątem min. 10° powyżej poziomu, aby skropliny wody w wydechu nie niszczyły rozgrzanej ceramiki.\n• Nigdy nie zostawiaj sondy w wydechu bez podłączonego zasilania grzałki podczas pracy silnika (ulegnie zasnufferowaniu/sadzeniu).",
        authorNick = "AFR_Master",
        rating = 4.88f,
        ratingVotesCount = 33
      ),

      // 7. Wtryskiwacze Bosch EV14 630cc (Z tabelami Dead Time dla ciśnienia 3.0 bar i 4.0 bar)
      TuningComponent(
        id = "bosch_ev14_630cc",
        name = "Wtryskiwacze Bosch EV14 630cc",
        partNumber = "0 280 158 117 / 0280158117",
        category = ComponentCategory.INJECTORS,
        shortDescription = "Wysokooporowe wtryskiwacze benzynowe Bosch EV14 (12 Ω) o wydajności 630 cm³/min przy 3.0 bar. Jeden z najpopularniejszych wyborów w projektach turbo (1.8T, VR6, BMW M50/M52/M54, Honda B/K seria).",
        imageUrl = "https://images.unsplash.com/photo-1486006920555-c77dce18193b?auto=format&fit=crop&w=800&q=80",
        vectorIllustrationKey = "vw_coils",
        pinout = listOf(
          PinInfo(
            pinNumber = "Pin 1",
            name = "+12V Zasilanie po stacyjce",
            description = "Stałe napięcie +12V z przekaźnika wtryskiwaczy / pompy paliwa.",
            wireColor = "Czerwony / Biały",
            type = PinType.POWER
          ),
          PinInfo(
            pinNumber = "Pin 2",
            name = "Sterowanie masą z ECU (Low Side)",
            description = "Kluczowanie masą z wyjścia tranzystorowego INJ w EMU / uaefi / Megasquirt.",
            wireColor = "Niebieski",
            type = PinType.INJECTOR
          )
        ),
        characteristic = CharacteristicInfo(
          type = CharType.OTHER,
          title = "Wtryskiwacz benzynowy Bosch EV14",
          inputLabel = "Napięcie (V)",
          outputLabel = "Czas martwy (ms)",
          points = emptyList(),
          formulaOrNotes = ""
        ),
        pullupRequired = false,
        flowRateCc = 630.0,
        flowPressureBar = 3.0,
        deadTimeTables = listOf(
          DeadTimeTable(
            tableName = "3.0 bar (Diff)",
            pressureUnit = "bar",
            points = listOf(
              DeadTimePoint("8.0", "1.98"),
              DeadTimePoint("10.0", "1.44"),
              DeadTimePoint("11.0", "1.23"),
              DeadTimePoint("12.0", "1.06"),
              DeadTimePoint("13.0", "0.93"),
              DeadTimePoint("14.0", "0.82"),
              DeadTimePoint("15.0", "0.74"),
              DeadTimePoint("16.0", "0.67")
            ),
            notes = "Przepływ nominalny: 630 cc/min (48 lb/hr) przy 3.0 bar. Kąt strugi: Conical 20°."
          ),
          DeadTimeTable(
            tableName = "4.0 bar (Diff)",
            pressureUnit = "bar",
            points = listOf(
              DeadTimePoint("8.0", "2.25"),
              DeadTimePoint("10.0", "1.62"),
              DeadTimePoint("11.0", "1.38"),
              DeadTimePoint("12.0", "1.18"),
              DeadTimePoint("13.0", "1.03"),
              DeadTimePoint("14.0", "0.91"),
              DeadTimePoint("15.0", "0.82"),
              DeadTimePoint("16.0", "0.74")
            ),
            notes = "Przepływ przy 4.0 bar: ~727 cc/min. Wymaga mocniejszej pompy paliwa (np. Walbro 255 / DW200)."
          )
        ),
        importantNotes = "• Wtyczka: USCAR / EV6 (dostępne adaptery do klasycznego EV1 Bosch Jetronic).\n• Rezystancja cewki: 12.0 Ω (High-Z) – nie wymaga rezystorów 'drop resistor', sterowanie nasyceniem standardowe.\n• O-ringi: 14mm viton (góra i dół).",
        authorNick = "TurboSpec_PL",
        rating = 5.0f,
        ratingVotesCount = 28
      )
    )
  }
}
