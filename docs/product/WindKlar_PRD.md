# WindKlar PRD

Produktanforderungsdokument für die WindKlar-App

Status: Arbeitsfassung, aktualisiert gegen den aktuellen Stand des `windklar`-Repositories
Kontext: Modul "Digital Product Development and Lifecycle Management", Universität Leipzig, Sommersemester 2026
Produktname: WindKlar
Kurzpositionierung: Transparenz- und Beteiligungsplattform für Windenergie vor Ort

## 1. Problemstellung

Der Ausbau der Windenergie ist für Energiewende und Klimaschutz zentral, wird lokal aber häufig durch unklare Informationen, fehlende Beteiligung und schwer einordenbare Auswirkungen begleitet. Viele Bürgerinnen und Bürger sehen Windenergieanlagen im Alltag, können aber nicht einfach nachvollziehen, welche Anlage sie sehen, wie viel Strom sie erzeugt, welche lokalen Vorteile entstehen, welche Belastungen auftreten und welche Schutzmaßnahmen gelten.

Bestehende Lösungen zeigen oft entweder technische Daten oder Karteninformationen. Was fehlt, ist eine verständliche mobile Anwendung, die technische Windparkdaten, lokale Auswirkungen, kommunalen Nutzen und Beteiligungsmöglichkeiten in einem Produkt zusammenführt.

WindKlar soll diese Transparenzlücke schließen. Die App soll nicht einseitig für Windenergie werben, sondern nachvollziehbar erklären, was ein konkreter Windpark leistet, welche Zielkonflikte bestehen und wie lokale Akteure informiert oder beteiligt werden können.

## 2. Produktvision

WindKlar macht sichtbar, wo der Wind wirkt: Nutzerinnen und Nutzer erkennen Windenergieanlagen in ihrer Umgebung, verstehen deren Beitrag zur Energieversorgung und sehen, welcher Nutzen und welche Auswirkungen lokal entstehen.

Die App beantwortet vier Kernfragen:

1. Welcher Windpark befindet sich in meiner Nähe, und welche Anlagen gehören dazu?
2. Was leistet dieser Park technisch, energetisch und klimabezogen?
3. Welcher konkrete Nutzen entsteht für Kommune und Bürgerschaft?
4. Welche Auswirkungen, Unsicherheiten und Beteiligungsmöglichkeiten gibt es?

## 3. Ziele

- Windenergieanlagen und Windparks an Land (onshore) in der Umgebung auffindbar und verständlich machen.
- Öffentliche Windenergiedaten in eine bürgernahe Darstellung übersetzen.
- Vertrauen durch klare Quellen, Datenqualität und Zeitstempel fördern.
- Akzeptanzrelevante Informationen sichtbar machen, insbesondere CO2-Einsparung, Haushaltsäquivalente und kommunale Beteiligung.
- Lokale Beteiligung durch Favoriten, Verlauf, Rückmeldungen und perspektivisch Bürgerbudget-Funktionen vorbereiten.
- Einen MVP liefern, der im Rahmen eines studentischen Projekts realistisch umsetzbar, vorführbar und evaluierbar ist.

## 4. Nicht-Ziele

- WindKlar ersetzt kein formelles Genehmigungs- oder Beteiligungsverfahren.
- WindKlar trifft keine rechtsverbindlichen Aussagen zu Schall, Schattenwurf, Artenschutz oder kommunalen Einnahmen.
- WindKlar ist im MVP keine Social-Media-Plattform.
- WindKlar ist im MVP keine vollständig automatisierte Echtzeit-Leitwarte für Windparkbetrieb.
- WindKlar soll keine politischen Bewertungen erzwingen; die App soll transparent informieren und Zielkonflikte sichtbar machen.
- Offshore-Windparks im Meer sind nicht Teil des MVP-Umfangs; die App konzentriert sich rein auf Windenergie an Land (onshore).

## 5. Zielgruppen und Stakeholder

### Primäre Nutzerinnen und Nutzer

- Interessierte Bürgerinnen und Bürger, die Windparks in ihrer Umgebung verstehen wollen.
- Anwohnerinnen und Anwohner, die konkrete Informationen zu sichtbaren Anlagen suchen.
- Gemeindemitglieder, die den lokalen Nutzen und mögliche Belastungen einordnen möchten.

### Sekundäre Nutzerinnen und Nutzer

- Kommunen und öffentliche Verwaltungen, die transparente Projektkommunikation unterstützen wollen.
- Windparkbetreiber, die verständliche Informationen bereitstellen möchten.
- Energie- und Klimaschutzagenturen, die Windenergie erklären und Akzeptanzarbeit leisten.
- Lokale Unternehmen und Vereine, die von kommunalen Windparkerträgen oder Projekten betroffen sein können.
- Hochschulteam und Lehrende, die Konzept, Prototyp, Umsetzung und Evaluation bewerten.

## 6. Kern-Personas

### Persona 1: Die interessierte Bürgerin

Sie sieht Windräder in ihrer Umgebung und möchte wissen, welche Anlage das ist, wie weit sie entfernt ist und ob sie tatsächlich einen relevanten Beitrag leistet. Sie braucht eine klare Karte, einfache Kennzahlen und kurze Erklärtexte ohne Fachjargon.

### Persona 2: Der skeptische Anwohner

Er sorgt sich um Belastungen und möchte verstehen, welche Auswirkungen realistisch sind. Er braucht transparente Datenqualität, Hinweise auf Unsicherheiten, sachliche Informationen zu Schutzmaßnahmen und eine Möglichkeit, Fragen oder fehlende Daten zu melden.

### Persona 3: Die kommunale Entscheiderin

Sie möchte zeigen, welcher Nutzen aus Windenergie für die Kommune entsteht. Sie braucht eine Darstellung von kommunaler Beteiligung, Förderprojekten, Ausgleichsmaßnahmen und häufigen Fragen.

### Persona 4: Der Projektentwickler oder Betreiber

Er möchte Stammdaten und Projektinformationen bürgernah darstellen. Er braucht robuste Datenstrukturen, klare Quellenangaben und eine Darstellung, die Vorteile nicht überzeichnet.

## 7. Wettbewerbsumfeld

Die Konzeptpräsentation nennt bestehende Angebote wie WindPower, WindTurbineMap, ENMAP, EnBW E-Cockpit und Juvent. Diese Lösungen decken Teile des Problems ab, etwa Karten, technische Daten oder Betreiberinformationen. Die Differenzierung von WindKlar liegt in der Verbindung von:

- Karte und Geolokalisierung,
- technischen Turbinendaten,
- jährlicher Produktion und CO2-Wirkung,
- lokaler kommunaler Beteiligung,
- bürgernaher Sprache,
- Datenqualitätskennzeichnung,
- Beteiligungs- und Rückmeldeelementen.

## 8. MVP-Umfang

Der MVP konzentriert sich auf Transparenz mit hohem Nutzen für Bürgerinnen und Bürger bei mittlerer technischer Komplexität.

### Muss enthalten

1. Karte mit Windparks als primärer UX-Ebene und Windanlagen als atomarer Datenbasis
2. Detailansicht für einen Windpark
3. Wirkungsdashboard für Bürgerinnen und Bürger
4. Kennzahlen zu Jahresproduktion, CO2-Einsparung und versorgten Haushalten
5. Darstellung kommunaler Beteiligung oder lokaler Nutzenindikatoren
6. Favoriten und zuletzt angesehene Windparks
7. Datenquellen, Datenqualität und Zeitstempel je zentraler Kennzahl
8. Basis-FAQ zu Windenergie, Datenherkunft und App-Grenzen

### Sollte enthalten

1. Filter und Suche nach Standort, Anlage, Kommune oder Windpark
2. Meldung fehlender oder fehlerhafter Anlagen
3. Vergleich von zwei Anlagen oder Windparks
4. Offline-nahe lokale Zwischenspeicherung zentraler Daten
5. Einfache Projekt- oder Windparkübersicht für Kommunen

### Könnte enthalten

1. Prognose für den nächsten Tag auf Basis von Wetterdaten und Leistungskurven
2. Schattenwurf-, Abstands- oder Naturschutz-Ebene als Demo-Daten
3. Bürgerbudget-Demo für lokale Projektpriorisierung
4. Fragen-Tracker für häufige Einwände und Antworten
5. Szenario-Simulator für Variantenvergleich
6. Quellengebundener FAQ-Assistent als optionaler Prototyp für erklärende Fragen

### Nicht im MVP enthalten

1. Kamera- oder AR-basierte Anlagenidentifikation
2. Rechtsverbindliche Abstimmungen
3. Moderierte Kommentarbereiche oder sozialer Feed
4. Vollautomatisierter Import aller Datenquellen in Produktionsqualität
5. Produktionsreifer oder frei halluzinierender KI-Chat
6. KI-Antworten, die aktuelle Betriebsursachen ohne Live-Daten behaupten

## 9. Nutzergeschichten

1. Als interessierte Bürgerin oder interessierter Bürger möchte ich Windparks in der Nähe meines aktuellen Standorts sehen, damit ich verstehe, was ich in meiner Umgebung sehe.
2. Als interessierte Bürgerin oder interessierter Bürger möchte ich einen Windpark von der Karte aus öffnen, damit ich seine wichtigsten Informationen schnell prüfen kann.
3. Als Anwohnerin oder Anwohner möchte ich die Entfernung zwischen meinem Standort und einem Windpark sehen, damit ich die lokale Relevanz besser einschätzen kann.
4. Als Anwohnerin oder Anwohner möchte ich Windpark- und Anlagendaten in verständlicher Sprache sehen, damit ich keine technische Expertise brauche, um die Informationen zu verstehen.
5. Als Anwohnerin oder Anwohner möchte ich sehen, ob Daten offiziell, gemessen, abgeleitet, geschätzt, simuliert oder fehlend sind, damit ich ihre Verlässlichkeit einschätzen kann.
6. Als Bürgerin oder Bürger möchte ich die jährliche Energieproduktion sehen, damit ich den Beitrag einer Anlage oder eines Windparks verstehen kann.
7. Als Bürgerin oder Bürger möchte ich CO2-Einsparungen sehen, damit die Klimawirkung greifbarer wird.
8. Als Bürgerin oder Bürger möchte ich eine Schätzung der versorgten Haushalte sehen, damit abstrakte Energiewerte leichter verständlich werden.
9. Als Bürgerin oder Bürger möchte ich kommunale Beteiligung oder lokalen Nutzen sehen, damit der lokale Wert von Windenergie sichtbar wird.
10. Als Bürgerin oder Bürger möchte ich sehen, welche Quellen verwendet wurden, damit ich den Informationen vertrauen und sie prüfen kann.
11. Als Bürgerin oder Bürger möchte ich Windparks als Favoriten speichern, damit ich relevante Orte schnell wiederfinden kann.
12. Als Bürgerin oder Bürger möchte ich zuletzt angesehene Windparks sehen, damit ich eine frühere Erkundung fortsetzen kann.
13. Als Bürgerin oder Bürger möchte ich nach Kommune oder Standort suchen, damit ich Orte prüfen kann, die für mich relevant sind.
14. Als Bürgerin oder Bürger möchte ich Kartenergebnisse filtern, damit dicht belegte Gebiete verständlich bleiben.
15. Als Bürgerin oder Bürger möchte ich einen strukturierten Datenhinweis zu fehlenden oder fehlerhaften Windanlagendaten einreichen, damit Datenqualitätsprobleme sichtbar und prüfbar werden können.
16. Als Bürgerin oder Bürger möchte ich eine FAQ zu Windenergie haben, damit häufige Fragen beantwortet werden, ohne anderswo suchen zu müssen.
17. Als skeptische Anwohnerin oder skeptischer Anwohner möchte ich eine transparente Erklärung der Grenzen sehen, damit die App nicht wie Werbung wirkt.
18. Als skeptische Anwohnerin oder skeptischer Anwohner möchte ich, dass mögliche Auswirkungen benannt werden, damit Nutzen und Belastungen fair dargestellt werden.
19. Als Vertreterin oder Vertreter einer Kommune möchte ich lokale Nutzendaten darstellen, damit Bürgerinnen und Bürger verstehen, was in der Gemeinde ankommt.
20. Als Vertreterin oder Vertreter einer Kommune möchte ich geförderte lokale Projekte zeigen, damit Einnahmen aus Windenergie konkret werden.
21. Als Windparkbetreiber möchte ich Anlagen-Stammdaten konsistent darstellen, damit Nutzerinnen und Nutzer klare Informationen erhalten.
22. Als Energieagentur möchte ich WindKlar als Bildungswerkzeug nutzen, damit Bürgerinnen und Bürger Windenergie im Kontext verstehen.
23. Als Produktverantwortliche oder Produktverantwortlicher möchte ich Funktionen nach Bürgernutzen und Umsetzungskomplexität priorisieren, damit der MVP machbar bleibt.
24. Als Entwicklerin oder Entwickler möchte ich ein strukturiertes Datenmodell für Anlagen, Windparks und Metriken haben, damit Funktionen inkrementell umgesetzt werden können.
25. Als Testerin oder Tester möchte ich klare Akzeptanzkriterien für jede Funktion haben, damit Qualität anhand des externen Verhaltens bewertet werden kann.
26. Als Bürgerin oder Bürger möchte ich in der FAQ natürlichsprachliche Anschlussfragen stellen, damit ich häufige Situationen verstehen kann, ohne dass jede Formulierung vorab geschrieben sein muss.
27. Als Bürgerin oder Bürger möchte ich, dass die App sagt, wenn sie eine Antwort aus öffentlichen Daten nicht wissen kann, damit Erklärungen vertrauenswürdig bleiben.
28. Als Lehrende oder Lehrender möchte ich, dass das Produkt Beratung, Analyse, Konzept, Design, Umsetzung und QA-Phasen widerspiegelt, damit es zu den Modulzielen passt.

## 10. Funktionale Anforderungen

### Karte und Geolokalisierung

- Die App muss Windparks auf einer interaktiven Karte anzeigen; einzelne Windanlagen können ergänzt werden, wenn das Datenmodell sie unterstützt.
- Die Karte muss progressive Offenlegung nutzen: Die Standard-Kartenebene zeigt Windparks oder Cluster, während einzelne Windanlagen erst bei höheren Zoomstufen oder im Kontext einer Windpark-Detailansicht sichtbar werden.
- Die Karte muss einen deutschlandweiten Datensatz unterstützen, indem sie bei niedrigen Zoomstufen Marker clustert, filtert oder die Markerdichte auf andere Weise reduziert.
- Die MVP-Kartenimplementierung sollte geteilt oder datengetrieben bleiben und keine getrennten nativen Kartenimplementierungen für Android und iOS erfordern.
- Die App darf den Nutzerstandort nur bei Bedarf anfragen und muss erklären, warum der Standort nützlich ist.
- Der Standortzugriff darf erst nach einer Nutzeraktion wie "auf meinen Standort zentrieren" oder "in meiner Nähe" angefragt werden.
- Die App darf den Nutzerstandort im MVP nicht speichern.
- Die App muss erlauben, einen Kartenmarker auszuwählen, um eine Windpark-Detailansicht zu öffnen.
- Die App sollte erlauben, bei detaillierten Zoomstufen eine einzelne Windanlage auszuwählen, während Windpark- und Gemeindekontext sichtbar bleiben.
- Die App muss als Alternative zu GPS eine manuelle Standortsuche unterstützen.
- Datenhinweise müssen erlauben, statt GPS manuell einen Pin zu setzen.
- Suche muss Teil des Kartenflusses als Overlay oder Sheet sein, nicht ein Ziel der unteren Navigation.
- Suche muss Windparkname, Kommune/Ort und optional MaStR-ID oder Anlagenname unterstützen.
- Die Auswahl eines Suchergebnisses muss das Overlay schließen, die Karte fokussieren und die ausgewählte Windpark-Vorschau oder Detailansicht öffnen.
- Die App muss nicht verfügbaren oder verweigerten Standortzugriff verständlich behandeln.

### Windpark-Detailansicht

- Die App muss Windparkname, Kommune, Standort, Anlagenanzahl, installierte Leistung und Status anzeigen, wenn diese verfügbar sind.
- Die App sollte einzelne Anlagendetails wie Kennung, Hersteller/Modell und Nennleistung anzeigen, wenn der Datensatz sie unterstützt.
- Die App muss den Windpark-Kontext sichtbar halten, wenn einzelne Anlagendetails angezeigt werden, damit Nutzerinnen und Nutzer verstehen, zu welchem lokalen Projekt und zu welcher Gemeinde die Anlage gehört.
- Die App muss produktionsbezogene Metriken anzeigen, wenn sie verfügbar oder geschätzt sind.
- Die App muss fehlende Werte klar markieren, statt sie zu verstecken.
- Die App muss Quellen- und Zeitstempelinformationen für relevante Felder bereitstellen.

### Wirkungsübersicht für Bürgerinnen und Bürger

- Die App muss zentrale Wirkungsmetriken zusammenfassen: Jahresproduktion, geschätzte CO2-Einsparung, Haushaltsäquivalent und kommunaler Nutzenindikator.
- Die App muss verständliche Einheiten und kontextuelle Labels verwenden.
- Die App darf unsichere Berechnungen nicht überzeichnen.
- Die App muss auf hoher Ebene erklären, wie geschätzte Metriken berechnet werden.
- Die Jahresproduktion im MVP muss aus installierter Leistung und dokumentierten angenommenen Volllaststunden geschätzt werden.
- Die CO2-Einsparung im MVP muss aus geschätzter Produktion und einem dokumentierten Emissionsfaktor geschätzt werden.
- Haushaltsäquivalente im MVP müssen aus geschätzter Produktion und einem dokumentierten durchschnittlichen Haushaltsstromverbrauch geschätzt werden.
- Berechnungsannahmen müssen in Konfiguration oder Snapshot-Metadaten liegen, nicht als hartcodierte UI-Konstanten.
- Konkrete Werte für Volllaststunden, Emissionsfaktor und Haushaltsstromverbrauch bleiben offen, bis ein quellenbasierter Snapshot vorbereitet ist.
- Snapshot-Metadaten müssen für jede Wirkungsannahme Annahmewert, Einheit, Quelle, Quelldatum oder Abrufdatum und Berechnungshinweis enthalten.

### Kommunaler Nutzen

- Die App muss anzeigen, ob für eine Gemeinde dokumentierte Beteiligung, erwarteter Nutzen oder Demo-Nutzendaten vorliegen.
- Die App muss echte öffentliche Daten von Demo- oder simulierten Daten unterscheiden.
- Die App sollte eine einfache Aufschlüsselung lokaler Verwendungszwecke unterstützen, etwa Schule, öffentlicher Nahverkehr, Sport, Naturschutzausgleich oder Gemeinschaftsinfrastruktur.
- Für den MVP sollte kommunale Beteiligung als geschätzter erwarteter §6-EEG-Wert angezeigt werden, nicht als bestätigte Auszahlung, sofern keine Zahlungsquelle existiert.
- Empfohlener Kurztext: "Geschätzte kommunale Beteiligung: ca. X EUR/Jahr. Grundlage: §6 EEG, 0,2 ct/kWh, geschätzte Jahresproduktion. Keine bestätigte Auszahlung."

### Favoriten und Verlauf

- Die App muss Nutzerinnen und Nutzern erlauben, Windparks lokal als Favoriten zu speichern.
- Der MVP darf keine separaten Favoriten für einzelne Windanlagen unterstützen.
- Die App muss zuletzt angesehene Windparks anzeigen, unabhängig davon, ob sie über Karte, Suche oder Favoriten geöffnet wurden.
- Die App darf für MVP-Favoriten kein Nutzerkonto erfordern.
- Favoriten und zuletzt angesehene Windparks müssen im MVP über SQLDelight persistiert werden.

### Datenqualität und Quellen

- Jede zentrale Metrik muss ein Datenqualitätslabel offenlegen: `official`, `measured`, `derived`, `estimated`, `simulated` oder `missing`.
- Jede zentrale Metrik muss, soweit möglich, ein Quellenlabel und einen Zeitstempel offenlegen.
- Die UI muss Datenqualität sichtbar machen, ohne den Hauptscreen zu überladen.
- Der MVP muss echte MaStR/Open-MaStR-basierte Stammdaten für Windanlagen nutzen, sofern verfügbar.
- Der MVP muss MaStR/Open-MaStR-Stammdaten aus einem vorverarbeiteten lokalen JSON-Snapshot laden, statt zur App-Laufzeit eine externe Live-API aufzurufen.
- Der JSON-Snapshot muss sowohl einzelne Windanlagen als auch vorberechnete Windpark-Aggregate enthalten.
- MaStR/Open-MaStR-basierte Anlagen-Stammdaten müssen als `official` gekennzeichnet werden.
- Windpark-Aggregate, die während der Vorverarbeitung berechnet wurden, müssen als `derived` gekennzeichnet werden.
- Produktions- und Akzeptanzwirkungswerte müssen als `estimated` oder `simulated` gekennzeichnet werden, sofern keine gemessenen öffentlichen Werte verfügbar sind.
- Der MVP darf geschätzte oder simulierte Wirkungswerte für Jahresproduktion, CO2-Einsparung, Haushaltsäquivalente und kommunale Beteiligung nutzen, wenn gemessene öffentliche Werte nicht verfügbar sind.
- Geschätzte oder simulierte Wirkungswerte müssen sichtbar gekennzeichnet sein und dürfen nicht als offizielle Messungen dargestellt werden.

### FAQ und Bildung

- Die App muss kurze Erklärungen zu häufigen Themen bereitstellen: Grundlagen der Windenergie, Energieproduktion, CO2-Einsparung, kommunale Beteiligung, Datenquellen und Grenzen.
- FAQ-Inhalte müssen neutrale, zugängliche Sprache verwenden.
- Ein späterer FAQ-Assistent darf natürlichsprachliche Fragen nur aus einer kuratierten WindKlar-Wissensbasis, lokalen Snapshot-Metadaten, ausgewähltem Windpark-Kontext und sichtbaren Metrik-/Quellenmetadaten beantworten.
- Der FAQ-Assistent muss bekannte Fakten, Schätzungen, plausible allgemeine Erklärungen und Unbekanntes unterscheiden.
- Der FAQ-Assistent darf keinen aktuellen Live-Betriebsgrund für Anlagenstillstand behaupten, sofern WindKlar keine quellenbasierte Live-Status- oder Ereignisquelle für diese Aussage hat.
- Bei Fragen wie "Warum steht das gerade still?" sollte der Assistent erklären, dass WindKlar die aktuelle Ursache aus dem MVP-Snapshot nicht bestimmen kann, dann häufige mögliche Gründe nennen und zeigen, welche lokalen Daten verfügbar sind.
- Antworten des FAQ-Assistenten sollten die verwendeten Quellenabschnitte oder Datenfelder offenlegen, einschließlich Datenqualitätslabels und Zeitstempeln, soweit verfügbar.
- Der FAQ-Assistent muss eine deterministische Ausweichlösung haben, wenn kein Plattformmodell verfügbar ist, etwa kuratierte FAQ-Treffer oder vorlagenbasierte Antworten.

### Datenhinweise und fehlende Daten

- Die App sollte Nutzerinnen und Nutzern erlauben, strukturierte Datenhinweise für fehlende oder fehlerhafte Windanlagen aus dem Kartenfluss heraus einzureichen.
- Datenhinweise müssen die MVP-Kategorien `missing_installation`, `wrong_location`, `wrong_status`, `wrong_wind_park_assignment`, `wrong_technical_data`, `installation_removed` und `other` unterstützen.
- Datenhinweise sollten nach Möglichkeit mit einer MaStR-ID, Windanlagen-ID, Windpark-ID, Gemeinde-ID oder Kartenkoordinate verknüpft werden.
- Ein Datenhinweis muss Kategorie, Standort oder bestehende Objektreferenz, Kurzbeschreibung und Hinweissicherheit (`unsure`, `likely`, `certain`) erfordern.
- Ein Datenhinweis darf optional ein Bild oder einen vorgeschlagenen korrigierten Wert enthalten.
- Kontaktinformationen sind für MVP-Datenhinweise nicht erforderlich und sollten vermieden werden, sofern sie nicht später ausdrücklich eingeführt werden.
- Datenhinweise müssen im MVP lokal in SQLDelight gespeichert werden.
- Datenhinweise sollten einen lokalen Status wie `draft`, `ready_for_review` oder `exported` tragen.
- Datenhinweise sollten in einem späteren Umsetzungsschritt in einem einfachen prüfbaren Format exportierbar sein, aber der MVP darf kein Backend erfordern.
- Der MVP darf nicht behaupten, offizielle Korrekturen an MaStR oder einem anderen öffentlichen Register einzureichen.
- Der MVP muss das Sammeln unnötiger personenbezogener Daten vermeiden.

## 11. Nicht-funktionale Anforderungen

### Benutzbarkeit

- Die App muss einer Mobile-First-Navigation mit vorhersehbaren Karten-, Detail- und Übersichtsabläufen folgen.
- Wichtige Status- und Fehlerzustände müssen sichtbar sein.
- Fachbegriffe müssen erklärt oder durch Alltagssprache ersetzt werden.
- Interface-Elemente müssen konsistent und wiedererkennbar sein.

### Barrierefreiheit

- Die App muss ausreichenden Kontrast, skalierbaren Text und zugängliche Labels verwenden.
- Kartenbasierte Informationen müssen dort, wo es relevant ist, Alternativen außerhalb der Karte haben.
- Kritische Informationen dürfen nicht allein über Farbe kommuniziert werden.

### Leistung

- Die Karte muss für einen deutschlandweiten MVP-Datensatz reaktionsfähig bleiben.
- Detailansichten sollten nach Auswahl eines Markers schnell öffnen.
- Zwischengespeicherte Daten sollten dort verwendet werden, wo sie wiederholtes Laden reduzieren.

### Datenschutz

- Standortzugriff muss optional sein.
- Standortzugriff muss nutzerinitiiert sein und darf nicht während des Onboardings angefragt werden.
- Der Nutzerstandort darf im MVP nicht gespeichert werden.
- MVP-Favoriten und Verlauf sollten standardmäßig lokal gespeichert werden.
- Für die MVP-Nutzung darf kein persönliches Konto erforderlich sein.
- Wenn Datenhinweise exportiert oder übertragen werden, müssen personenbezogene Daten minimiert werden.

### Zuverlässigkeit

- Die App muss Ausfälle externer APIs behandeln, indem sie zwischengespeicherte Daten oder Ausweichdaten zeigt.
- Die App muss klare Leer-, Lade- und Fehlerzustände zeigen.
- Die App muss nicht verfügbare Daten von Nullwerten unterscheiden.

### Transparenz

- Die App muss Unsicherheit explizit machen.
- Simulierte oder Demo-Werte müssen als solche gekennzeichnet werden.
- Der Produkton sollte sachlich, nicht werbend sein.

## 12. Datenanforderungen

### Kernentitäten

- Windanlage: ID, Windpark-ID, Name oder Kennung, Koordinaten, Hersteller, Modell, Typ, Nabenhöhe oder Gesamthöhe, sofern verfügbar, installierte Leistung, Status, Betreiber, Quellenmetadaten. Dies ist die atomare Quellen- und Koordinateneinheit.
- Windpark: ID, Name, Gemeinde, repräsentative Koordinaten oder Geometrie, Anlagenanzahl, aggregierte installierte Leistung, aggregierte Metriken, Quellenmetadaten. Dies ist die primäre bürgernahe UX-Einheit für Kartenübersicht, Favoriten und Gemeindekontext.
- Metrik: Wert, Einheit, Zeitraum, Datenqualität, Quelle, Zeitstempel, Berechnungshinweis.
- Kommunaler Nutzen: Gemeinde, Nutzentyp, Betrag oder qualitativer Indikator, Zeitraum, Quelle, Datenqualität.
- Lokaler Nutzerzustand: Favoriten, zuletzt angesehene Windparks, optional ausgeblendete Onboarding-Hinweise.
- Datenhinweis: Hinweiskategorie, Betrefftyp, Betreff-ID, sofern verfügbar, Gemeinde-ID, sofern verfügbar, Koordinaten, Beschreibung, Hinweissicherheit, optionale lokale Bildreferenz, optional vorgeschlagener Wert, Erstellungszeitstempel, lokaler Prüf-/Exportstatus.
- Snapshot-Metadaten: Snapshot-Quelle, Quellversion oder Zeitstempel, Importzeitstempel, Berechnungsannahmen und Vorverarbeitungshinweise.

### Aktuelles SQLDelight-Schema im Repository

Das Repository enthält bereits konkrete SQLDelight-Schemadateien unter `composeApp/src/commonMain/sqldelight/app/data/local/db`:

- `WindPark.sq`: Tabelle `wind_park` mit Aggregatfeldern, Quellenmetadaten, Gruppierungsmethode und Datenqualität; Queries für Auswahl aller Einträge, Auswahl per ID, Textsuche und Upsert.
- `WindTurbine.sq`: Tabelle `wind_turbine` für atomare MaStR/Open-MaStR-basierte Anlagen-Stammdaten.
- `Metric.sq`: generische Tabelle `metric` für Produktions- und Akzeptanzwirkungswerte mit Einheit, Zeitraum, Quelle, Datenqualität und Berechnungshinweis.
- `Favorite.sq`: Tabelle `favorite_wind_park`, geschlüsselt über `wind_park_id`; Queries für Favoriten-IDs, Favoritenexistenz, Hinzufügen und Entfernen.
- `RecentWindPark.sq`: Tabelle `recent_wind_park`, geschlüsselt über `wind_park_id`; Queries für zuletzt geöffnete Windparks, Erfassen und Leeren.
- `DataHint.sq`: lokale Tabelle `data_hint` für strukturierte Datenhinweise.
- `SnapshotMetadata.sq`: importierte Snapshot-Identität, Quelle, Prüfsumme und Vorverarbeitungsmetadaten.

Aktuelle Lücke: Das Kotlin-Domainmodell und die DAO-Interfaces sind noch schmaler als das SQL-Schema. `WindPark` stellt aktuell nur `id`, `name`, `municipality` und `isFavorite` bereit; `WindParkEntity` stellt aktuell nur `id`, `name` und `municipality` bereit. Repository- und DAO-Verträge müssen erweitert oder explizit gemappt werden, bevor das SQL-Schema die UI antreiben kann.

Implementiertes lokales Zielschema:

- `wind_turbine`: atomare MaStR/Open-MaStR-Anlagen-Stammdaten mit Quellen- und Qualitätsmetadaten.
- `wind_park`: vorberechnete Windpark-Aggregate für Karte, Favoriten, Suche und Detailübersicht.
- `metric`: Produktions- und Akzeptanzwirkungswerte wie Jahresproduktion, CO2-Einsparung, Haushaltsäquivalente und kommunale Beteiligung.
- `favorite_wind_park`: gespeicherte Windparks.
- `recent_wind_park`: zuletzt geöffnete Windparks.
- `data_hint`: lokale Datenhinweise mit Kategorie, Betreffreferenz, Standort, Beschreibung, Hinweissicherheit und lokalem Status.
- `snapshot_metadata`: optionale Metadaten für Quelle, Importzeitstempel, Version und Berechnungsannahmen.

Die Quellen-Datenpipeline lebt jetzt außerhalb der App unter `data/`, und die App importiert nur den appfertigen JSON-Snapshot, der unter Compose-Ressourcen gebündelt ist. Rohe und intermediäre MaStR-Dateien werden bewusst ignoriert.

Aktualisierte Modellierungsentscheidung: Das SQL-Schema enthält `wind_turbine` als atomare MaStR/Open-MaStR-basierte Einheit. Windpark-Zeilen sind Aggregate oder kuratierte Gruppierungen über diese Anlagenzeilen.

Aktualisierte lokale Zustandsentscheidung: Das Produktkonzept ist "Zuletzt angesehen", nicht ein reiner Suchverlauf. `RecentWindPark.sq` erfasst geöffnete Windparks unabhängig vom Einstiegspfad.

Aktualisierte lokale Persistenzentscheidung: Favoriten und zuletzt angesehene Windparks sind SQLDelight-gestütztes MVP-Verhalten, nicht nur Mock-Zustand.

Aktualisierte Datenquellenentscheidung: Der MVP nutzt echte MaStR/Open-MaStR-basierte Stammdaten für Windanlagen, sofern verfügbar. Anlagen-Stammdaten werden als `official` gekennzeichnet; Vorverarbeitungsaggregate werden als `derived` gekennzeichnet; Produktion, CO2, Haushaltsäquivalente und kommunale Beteiligung können `estimated` oder `simulated` sein, wenn gemessene öffentliche Daten nicht verfügbar sind.

Aktualisierte Entscheidung zur kommunalen Beteiligung: Kommunale Beteiligung im MVP wird als kurzer geschätzter erwarteter §6-EEG-Wert angezeigt, basierend auf 0,2 ct/kWh und geschätzter Jahresproduktion, mit einem klaren Hinweis "keine bestätigte Auszahlung", sofern keine Zahlungsquelle existiert.

Aktualisierte Entscheidung zur Wirkungsberechnung: Jahresproduktion, CO2-Einsparung und Haushaltsäquivalente werden aus einfachen dokumentierten Annahmen geschätzt. Konkrete Werte für Volllaststunden, Emissionsfaktor und Haushaltsstromverbrauch bleiben offen, bis der Snapshot vorbereitet wird; jede Annahme muss jedoch in den Snapshot-Metadaten mit Wert, Einheit, Quelle, Quelldatum oder Abrufdatum und Berechnungshinweis gespeichert werden.

Aktualisierte lokale Schemaentscheidung: SQLDelight nutzt `wind_turbine`, `wind_park`, `metric`, `favorite_wind_park`, `recent_wind_park`, `data_hint` und `snapshot_metadata`.

Aktualisierte Umfangsentscheidung: Der MVP-Datensatz soll Deutschland abdecken und nicht nur eine Demo-Region wie Leipzig/Sachsen. Dadurch steigen Anforderungen an Kartendichte und Import, weshalb Clustering/Filterung und lokales Cache-Design Teil der MVP-Datenstrategie sind.

Aktualisierte Integrationsentscheidung: Der MVP nutzt einen vorverarbeiteten lokalen MaStR/Open-MaStR-JSON-Snapshot statt Live-API-Zugriff zur Laufzeit. Die App sollte diesen Snapshot in SQLDelight-gestützte lokale Speicherung importieren oder bündeln.

Aktualisierte Aggregationsentscheidung: Windpark-Gruppierung und Aggregatfelder sollten während der Snapshot-Vorverarbeitung berechnet werden. Die App importiert sowohl Windpark-Aggregate als auch einzelne Windanlagen, statt deutschlandweite Gruppierungen zur Laufzeit zu berechnen.

Aktualisierte Metadatenentscheidung: Quellen- und Datenqualitätsmetadaten sollten direkt auf quellenbasierten Stammdatentabellen für Windanlagen und Windpark-Aggregate gespeichert werden. Nutzerseitige Wirkungswerte sollten in einem separaten Metrikmodell mit eigener Einheit, eigenem Zeitraum, eigener Quelle, eigenem Qualitätslabel und eigenem Berechnungshinweis gespeichert werden.

Aktualisierte Beteiligungsentscheidung: Der Meldefluss ist ein `Datenhinweis`-Workflow. Er sammelt strukturierte Hinweise zu fehlenden oder fehlerhaften Windanlagen oder Stammdaten, verspricht aber keine offizielle MaStR-Korrektur.

Aktualisierte Entscheidung zur Datenhinweis-Persistenz: Datenhinweise werden im MVP lokal in SQLDelight gespeichert, mit lokalem Prüf-/Exportstatus. Es ist kein Backend-Submit oder kontobasierter Workflow erforderlich.

Aktualisierte Datenhinweis-Kategorieentscheidung: MVP-Datenhinweise nutzen exakt diese Kategorien: `missing_installation`, `wrong_location`, `wrong_status`, `wrong_wind_park_assignment`, `wrong_technical_data`, `installation_removed` und `other`.

Aktualisierte Datenhinweis-Eingabeentscheidung: MVP-Datenhinweise erfordern Kategorie, Standort oder bestehende Objektreferenz, Kurzbeschreibung und Hinweissicherheit. Bild und vorgeschlagener korrigierter Wert sind optional; Kontaktinformationen sind nicht erforderlich.

Aktualisierte Suchentscheidung: Suche gehört in den Kartenfluss als Overlay oder Sheet. Sie ist kein Ziel der unteren Navigation.

Aktualisierte Kartenimplementierungsentscheidung: Der MVP sollte einen geteilten oder datengetriebenen Kartenansatz verwenden, bevorzugt OSM-kompatibel, sofern machbar, und keine getrennten nativen Kartenimplementierungen für Android und iOS erfordern.

Aktualisierte Standortentscheidung: Standortzugriff ist optional, wird erst nach Nutzeraktion angefragt und im MVP nicht gespeichert. Manuelle Suche und manuelles Pin-Setzen bleiben Alternativen.

Aktualisierte Navigationsentscheidung: Ansichten der obersten Ebene nutzen nur die untere Navigation und sollten keinen Zurück-Button zu `Map` zeigen. Zurückverhalten ist Unterabläufen vorbehalten.

Aktualisierte Profilentscheidung: `Profile` ist im MVP ein Bereich `Info & Einstellungen`. Er sollte keinen Logout, keine Kontosprache und keine Bedienelemente für nicht implementiertes Verhalten wie Benachrichtigungen oder Dunkelmodus enthalten.

Aktualisierte Designentscheidung: Die grüne Nature/Trust-Designrichtung ist für den MVP akzeptiert, aber Farben, Typografie, Abstände, Radien und Elevation sollten als Compose-Theme-/Designtokens zentralisiert werden.

Aktualisierte Figma-Entscheidung: Figma ist eine funktionale und visuelle Referenz für Screenset, Informationsarchitektur, Komponentenabsicht, grobes Layout und Copy. Es ist kein pixelgenauer Implementierungsvertrag für den MVP.

Aktualisierte Build-Tooling-Entscheidung: Die AGP-9.x-/Kotlin-Multiplatform-Kompatibilitätswarnung ist als dokumentiertes Seminar-MVP-Risiko akzeptiert. Vor der Demo soll nicht auf ein separates Android-App-Modul migriert werden, sofern der Build nicht bricht.

Aktualisierte QA-Entscheidung: Android-Manuelle-QA ist vor der Demo erforderlich. Ein kurzer iOS-Test im Simulator oder auf einem iOS-Gerät ist wünschenswert, soweit verfügbar, aber kein Demo-Hindernis, sofern der gemeinsame KMP-Einstiegspunkt intakt bleibt.

### Mögliche Quellen

- Marktstammdatenregister für Anlagen-Stammdaten.
- Open-MaStR für zugängliche Anlagen-Stammdatenexporte oder Integration.
- OpenStreetMap für Basiskarte und geografischen Kontext.
- DWD-Wetterdaten für optionale Prognosen oder Produktionsschätzung.
- SMARD oder öffentliche Energiestatistiken für kontextuelle Energiedaten.
- Umweltbundesamt, BWE oder Fachagentur Wind und Solar für Erklärungskontext und akzeptanzbezogene Informationen.
- Demo-Datensätze für kommunalen Nutzen, Schatten, Schall, Naturschutz oder Szenariodaten, bis verifizierte öffentliche Daten verfügbar sind.

### Datenqualitätslabels

- `official`: veröffentlicht von einer offiziellen oder autoritativen öffentlichen Quelle.
- `measured`: gemessene Betriebs- oder Sensordaten.
- `derived`: mechanisch aus offiziellen oder quellenbasierten Feldern berechnet, ohne zusätzliche Modellannahmen.
- `estimated`: aus Modell, Wetter, Leistungskurve oder öffentlichen Annahmen berechnet.
- `simulated`: Demo- oder Szenariowert.
- `missing`: bewusst als nicht verfügbar angezeigt.

## 13. Informationsarchitektur

Aktuelle Repository-Navigation:

1. `Start`: vollflächiger Einstieg mit CTA, ohne untere Navigation.
2. `Map`: primäre Entdeckungsfläche mit integriertem Suchfeld, Filterchips, Kartenaktionen und Vorschau-Sheet.
3. `Stats`: Produktions- und Energiekontext-Ansicht mit Compose-/Canvas-Diagrammen.
4. `Favorites`: Liste gespeicherter Windparks.
5. `Faq`: akkordeonbasierte Bildungs- und Skepsisbeantwortungs-Ansicht.
6. `Profile`: lokale Ansicht `Info & Einstellungen` ohne Konto- oder Logout-Verhalten.
7. `Detail(parkId)`: Windpark-Detailroute, erreichbar aus Karte und Favoriten.

Aktuelle untere Navigation:

1. `Map`
2. `Stats`
3. `Favorites`
4. `Faq`
5. `Profile` / `Info & Einstellungen`

Aktuelle Ablaufnotizen:

- `Start` ist die Einstiegsroute und navigiert zu `Map`.
- `Search` existiert als Funktionspaket und Platzhalteransicht, aber das bestätigte Produktverhalten ist ein Karten-Overlay/-Sheet statt einer Route der obersten Ebene oder eines Elements der unteren Navigation.
- `ReportWindTurbine` ist jetzt als Datenhinweis-Ablauf definiert und durch ein Kartenaktions-Icon repräsentiert, aber es gibt noch keine Route, kein Paket und keine Formularimplementierung.
- Das bestätigte Kartenverhalten ist progressive Offenlegung: zuerst Windparks oder Cluster, einzelne Windanlagen nur bei höheren Zoomstufen oder im Detailkontext.
- Das bestätigte Favoritenverhalten ist im MVP nur Windpark-bezogen; einzelne Anlagen werden nicht separat favorisiert.
- Ansichten der obersten Ebene sollten kein eigenes Zurück-Bedienelement zu `Map` enthalten; die untere Navigation besitzt die Bewegung auf oberster Ebene.
- Zurück-Bedienelemente gehören nur zu Unterabläufen wie Detail, Such-Overlay/-Sheet, Datenhinweisformular und einzelner Anlagenunterdetailansicht.
- `Profile` sollte als `Info & Einstellungen` behandelt werden, nicht als authentifizierter Kontobereich.

Mögliche spätere Navigation:

1. Windpark-Detail mit Produktionskontext
2. Anlagendetail, sofern einzelne Anlagendaten verfügbar sind
3. Kommunaler Nutzen
4. Beteiligung
5. Szenarien
6. Dokumente
7. Datenhinweis eingereicht / Exportbestätigung

## 14. UX- und Designanforderungen

Das Design sollte dem Fokus der Lehrveranstaltung auf UX, UI, Usability-Heuristiken, Gestaltprinzipien, Prototyping und Nutzerakzeptanztests folgen.

Die akzeptierte visuelle MVP-Richtung ist grün, naturorientiert und vertrauensfokussiert. Farben, Typografie, Abstände, Radien und Elevation sollten als Compose-Theme-/Designtokens zentralisiert werden, statt sie als ansichtslokale Konstanten zu wiederholen.

Figma ist die Quelle der Wahrheit für Screenset, Informationsarchitektur, Komponentenabsicht, grobes Layout und verfügbare Copy. Die Compose-Implementierung sollte die nutzerseitige Struktur und Absicht erhalten, aber pixelgenaue Parität ist für den Seminar-MVP nicht erforderlich.

- Sichtbarkeit des Systemstatus: Lade-, GPS-, veraltete Daten- und Fehlende-Daten-Zustände müssen klar sein.
- Übereinstimmung mit der realen Welt: Formulierungen sollten vertraute Begriffe wie "Strom für Haushalte" statt nur technischer Einheiten verwenden.
- Nutzerkontrolle und Freiheit: Nutzerinnen und Nutzer können ohne Standortberechtigung browsen und den lokalen Verlauf löschen, sofern implementiert.
- Konsistenz: Kartenmarker, Datenkarten, Quellenlabels und Statuschips sollten sich konsistent verhalten.
- Fehlervermeidung: Berechtigungs- und Datenhinweis-Flows sollten versehentliche Einreichungen verhindern.
- Wiedererkennen statt Erinnern: zentrale Metriken, gespeicherte Einträge und zuletzt angesehene Einträge sollten leicht auffindbar sein.
- Minimalistisches Design: Die Übersicht sollte wenige starke Metriken priorisieren, statt Nutzerinnen und Nutzer zu überfordern.
- Hilfe und Dokumentation: FAQ und Quellenerklärungen sollten aus datenreichen Ansichten erreichbar sein.

## 15. Implementierungsentscheidungen

- Den MVP als Kotlin-Multiplatform-Mobile-App für Android und iOS bauen.
- Geteilte UI und State in Compose Multiplatform unter `composeApp/src/commonMain` umsetzen.
- Android- und iOS-Einstiegspunkte dünn halten: Android startet `App()` aus `MainActivity`; iOS startet `App()` über `ComposeUIViewController`.
- SQLDelight als local-first Persistenzschicht verwenden.
- Ein klares Domainmodell rund um Anlagen, Windparks, Gemeinden, Metriken, Quellen und lokalen Nutzerzustand verwenden.
- Quellenmetadaten und Datenqualität als Daten erster Klasse behandeln, nicht als UI-Nachgedanken.
- Ein hybrides Metadatenschema verwenden: Quellenfelder auf Stammdatentabellen und ein separates Metrikmodell für Produktions- und Akzeptanzwirkungswerte.
- Echte MaStR/Open-MaStR-basierte Stammdaten für die MVP-Basis nutzen, sofern verfügbar.
- Die öffentlichen Quellen-Stammdaten für MVP-Zuverlässigkeit über einen vorverarbeiteten lokalen JSON-Snapshot laden.
- Windpark-Aggregation in der Vorverarbeitung halten, nicht in der App-Laufzeitlogik.
- Kartenverhalten für den MVP geteilt oder datengetrieben halten; getrennte native Android-/iOS-Kartenstacks vermeiden, sofern sie später nicht erforderlich werden.
- Nutzerstandort als optionalen, nutzerinitiierten Kontext behandeln; im MVP nicht speichern.
- Lokale Demo- oder abgeleitete Daten nur für Wirkungswerte verwenden, die im Seminarumfang nicht als gemessene öffentliche Daten beschafft werden können, und sie als geschätzt oder simuliert kennzeichnen.
- Datenhinweise im MVP lokal in SQLDelight speichern; kein Backend erfordern.
- Kommunalen Nutzen von technischen Anlagendaten getrennt halten, weil Verfügbarkeit, Quelle und Vertrauensniveau unterschiedlich sind.
- Favoriten und Verlauf im MVP lokal halten, um Authentifizierung und Datenschutzaufwand zu vermeiden.
- Kamera-basierte Identifikation und AR-Overlay zurückstellen, weil der Wert interessant ist, die Komplexität aber XL ist.
- Soziale Funktionen zurückstellen, weil das Moderationsrisiko im Verhältnis zum MVP-Wert hoch ist.
- Den Produktarbeitsvorrat in kleinen vertikalen Umsetzungsschritten vorbereiten: Kartenbrowsing, Detailansicht, Wirkungsmetriken, gespeicherte Einträge, Datenqualität, FAQ, Rückmeldung.

### Aktuelle Repository-Basis

- Projekttyp: Kotlin Multiplatform mit Compose Multiplatform und Android-App-Packaging.
- Zielplattformen: Android, `iosArm64` und `iosSimulatorArm64`.
- Hauptmodul: `composeApp`; nativer iOS-Launcher: `iosApp`.
- Paket-Namespace/Application-ID: `product.lifecycle.windenergy`.
- Aktuelle Bibliotheken: Kotlin 2.3.21, Compose Multiplatform 1.10.3, Material 3 1.10.0-alpha05, SQLDelight 2.3.2, Android compile/target SDK 36 und min SDK 24.
- Geteilte App-Wurzel: `app.App`, umschließt `AppNavHost` in `WindklarTheme`.
- Aktuelles Routenmodell: `Start`, `Map`, `Stats`, `Favorites`, `Faq`, `Profile`, `Detail(parkId)`, `RegionDetail(type, id)`.
- Aktuelle Routen der obersten Ebene in der unteren Navigation: `Map`, `Stats`, `Favorites`, `Faq`, `Profile` / `Info & Einstellungen`.
- Implementierte Screens: `StartScreen`, `MapScreen`, `StatsScreen`, `FavoritesScreen`, `FaqScreen`, `ProfileScreen`, `ParkDetailScreen`, `RegionDetailScreen`.
- Suche ist im `Map`-Flow umgesetzt; der separate `SearchScreen`-/`SearchViewModel`-/`SearchUiState`-Platzhalter wurde entfernt.
- `ReportWindTurbine` ist als Dialog-Composable (`ReportWindTurbineDialog`) umgesetzt und wird über den Pin-Placement-FAB in `MapScreen` ausgelöst.
- Alle Repositories/DAO-Verträge sind über generierte SQLDelight-Datenbank-APIs verdrahtet.
- Snapshot-Seed-Importer (`SnapshotSeedDataImporter`) läuft beim App-Start mit Checksum-aware Fast-Path.
- Daten-Fluss: `UI -> ViewModel -> Repository -> SQLDelight DAOs -> SQLite`.
- `Favorites` unterstützt Parks und Regionen; `Recents` speichert jeden geöffneten Park.
- Aktueller Datenstand: Die UI ist an reale Repository- und SQLDelight-Daten angebunden; Mock-`UiState`-Defaults sind durch echte Daten ersetzt.
- Aktuelle Assets: Start-Hintergrund/Icon und Favoriten-Windpark-Thumbnails sind unter `composeResources/drawable` gebündelt.
- Aktuelles Build-Risiko: Der Gradle-Problems-Report markiert eine Kotlin-Multiplatform-/Android-Gradle-Plugin-Kompatibilitätswarnung für die Nutzung von `org.jetbrains.kotlin.multiplatform` mit `com.android.application` unter AGP 9.x. Dies ist für den Seminar-MVP akzeptiert; eine spätere Migration könnte ein separates Android-App-Subprojekt erfordern, wenn die App über das Seminar hinaus weitergeführt wird oder der Build bricht.

## 16. Testentscheidungen

Für das aktuelle Seminarprojekt sind neue automatisierte Tests kein Teil des angegebenen Lieferziels. Qualitätsarbeit sollte sich daher auf Akzeptanzkriterien, manuelle QA, Build-Verifikation und präsentationsfähige Flows konzentrieren. Wenn das Projekt später über den Seminar-MVP hinausgeht, sollten automatisierte Tests externes Verhalten statt Implementierungsdetails prüfen.

### Manuelle QA-Prüfpunkte

- Datentransformations-Prüfpunkt: rohe Quellen-/Demo-Windparkdaten werden zu appfertigen Windpark- und Metrikobjekten.
- Karteninteraktions-Prüfpunkt: Die Auswahl einer Kartenvorschau öffnet die korrekte Route `Detail(parkId)`.
- Detailansichts-Prüfpunkt: fehlende, abgeleitete, geschätzte und offizielle Werte werden mit korrekten Labels gerendert.
- Wirkungsberechnungs-Prüfpunkt: Haushalts- und CO2-Äquivalent-Berechnungen sind deterministisch und als Schätzungen gekennzeichnet.
- Lokaler-Zustands-Prüfpunkt: Favoriten und zuletzt angesehene Windparks bleiben lokal erhalten und können von Nutzerinnen und Nutzern geändert werden.
- Berechtigungs-Prüfpunkt: verweigerter, nicht verfügbarer oder gewährter Standortzugriff führt zu verständlichen UI-Zuständen.
- Datenhinweis-Prüfpunkt: Über den Pin-Placement-FAB erstellte Datenhinweise werden lokal gespeichert, erhalten den ausgewählten Park-Kontext und können exportiert werden.
- Fehler-Prüfpunkt: fehlgeschlagenes Quellen-/API-Laden zeigt Ausweich- oder Fehlerzustände ohne Absturz, falls externe Daten eingeführt werden.

### Akzeptanzprüfungen

- Eine Nutzerin oder ein Nutzer kann die App öffnen, von `Start` zu `Map` wechseln, eine Demo-Windpark-Vorschau prüfen und ihre Detailseite öffnen.
- Eine Nutzerin oder ein Nutzer kann mindestens Jahresproduktion, CO2-Einsparung und Haushaltsäquivalent aus der Übersicht verstehen.
- Eine Nutzerin oder ein Nutzer kann erkennen, ob eine angezeigte Metrik offiziell, gemessen, abgeleitet, geschätzt, simuliert oder fehlend ist.
- Eine Nutzerin oder ein Nutzer kann einen Windpark speichern und wieder aufrufen, ohne ein Konto zu erstellen, sobald Persistenz verdrahtet ist.
- Eine Nutzerin oder ein Nutzer kann die App verwenden, ohne Standortberechtigung zu erteilen.
- Eine Nutzerin oder ein Nutzer kann Quellen und Grenzen für zentrale Daten lesen.

### Manuelle QA

- Vor der Demo auf mindestens einem Android-Emulator oder Android-Gerät testen.
- Einen kurzen iOS-Test im Simulator oder auf einem iOS-Gerät ausführen, soweit verfügbar; dieser iOS-Kurztest ist wünschenswert, aber kein Demo-Hindernis, sofern der gemeinsame KMP-Einstiegspunkt intakt bleibt.
- Kartemarkerdichte und Detailnavigation prüfen.
- Leer-, Lade-, Fehler- und Fehlende-Daten-Zustände prüfen.
- Formulierungen mit nicht-technischen Nutzerinnen und Nutzern prüfen, soweit möglich.
- Manueller Demo-Pfad als Checkliste: Start zu Map, Such-Overlay, Park-Vorschau zu Detail, Favorit hinzufügen/entfernen, zuletzt angesehen, lokalen Datenhinweis speichern, FAQ, Stats, Info & Einstellungen und verweigerter/kein-Standort-Pfad.

## 17. Analyse und Evaluation

Die Evaluation sollte darauf fokussieren, ob WindKlar Verständnis und wahrgenommene Transparenz verbessert.

Mögliche Produktmetriken:

- Konversionsrate von Karte zu Detailansicht.
- Anteil der Nutzerinnen und Nutzer, die Quellen- oder Datenqualitätserklärungen öffnen.
- Anzahl erstellter Favoriten.
- Anzahl geöffneter FAQ-Einträge.
- Anzahl eingereichter Datenhinweise.
- Abschlussrate für eine Testaufgabe wie "Finde einen Windpark in der Nähe von Leipzig und erkläre seinen Beitrag."

Mögliche Studienfragen:

- Ich verstehe besser, welchen Beitrag ein lokaler Windpark leistet.
- Ich kann gemessene Daten von Schätzungen unterscheiden.
- Ich kann mindestens einen lokalen Nutzen oder eine Einschränkung nennen.
- Die App wirkt sachlich statt werbend.
- Ich würde diese App vor oder während lokaler Windenergie-Diskussionen nutzen.

## 18. Risikoregister

| Risiko | Wahrscheinlichkeit | Auswirkung | Gegenmaßnahme |
|---|---:|---:|---|
| Unvollständige oder veraltete Windparkdaten | Hoch | Hoch | Datenqualität, Zeitstempel, Quellen und fehlende Zustände klar anzeigen. |
| Datenschutz- oder DSGVO-Problem | Mittel | Sehr hoch | Standort optional machen, Konten im MVP vermeiden, Feedback-Daten minimieren. |
| Geringe Nutzerakzeptanz | Mittel | Hoch | MVP auf unmittelbaren Kartenwert und verständliche Wirkungsmetriken fokussieren. |
| Ausfall externer APIs | Mittel | Mittel | Demo-Daten, Caching und Ausweichzustände verwenden. |
| Reibung bei Plattform-Build oder Distribution | Niedrig/Mittel | Mittel | Android- und iOS-Buildpfade früh validieren und plattformspezifischen Code dünn halten. |
| Missbrauch sozialer Funktionen | Hoch | Niedrig/Mittel | Kommentare/sozialen Feed über den MVP hinaus zurückstellen. |
| Performanceprobleme der Karte | Mittel | Niedrig/Mittel | MVP-Datensatz begrenzen, Marker bei Bedarf clustern. |
| Sicherheitslücken | Niedrig | Sehr hoch | Backend minimal halten, Eingaben validieren, unnötige personenbezogene Daten vermeiden. |
| Ausufernder Umfang | Hoch | Mittel/Hoch | MVP strikt halten und AR, KI und erweiterte Simulationen in spätere Phasen verschieben. |
| Irreführende KI-Antwort | Mittel | Hoch | Assistent auf kuratiertes Wissen und App-Daten begrenzen, Unsicherheitstext, Quellenanzeige und deterministische Ausweichlösung erfordern. |
| GPS-Ungenauigkeit | Niedrig/Mittel | Niedrig | Manuelle Suche anbieten und exakte Aussagen aus der Nutzerposition vermeiden. |

## 19. Projektfahrplan

### Phase 1: Fundament

- Kotlin-Domainmodelle und SQLDelight-Schema mit dem lokalen Zielmodell abgleichen.
- Generierte SQLDelight-Datenbank-APIs über DAO-/Repository-Verträge verdrahten.
- Seed-Import für einen deutschlandweiten vorverarbeiteten MaStR/Open-MaStR-abgeleiteten JSON-Windanlagen-Snapshot implementieren oder anpassen.
- App-Shell und untere Navigation in Besitz von `AppNavHost` halten.
- Platzhalter-Detailansicht durch repository-gestützte Windpark-Details ersetzen.
- Quellen- und Datenqualitätsfelder zum Schema oder einem begleitenden Metrik-/Quellenmodell hinzufügen.
- Metriktabelle für Jahresproduktion, CO2-Einsparung, Haushaltsäquivalente und kommunale Beteiligungswerte hinzufügen.
- Schema-Unterstützung für `wind_turbine`, `recent_wind_park`, `data_hint` und optional `snapshot_metadata` hinzufügen.

### Phase 2: MVP-Transparenz

- Ansichten `Map`, `Favorites`, `Search` und `Stats` mit lokalen Daten verbinden.
- Wirkungsdashboard für ausgewählte Windparks und Gemeinden hinzufügen.
- Jahresproduktion, CO2-Einsparung und Haushaltsäquivalent hinzufügen.
- Geschätzte erwartete §6-EEG-kommunale Beteiligung mit kurzem Hinweis "keine bestätigte Auszahlung" hinzufügen.
- Wirkungsberechnungsannahmen in Konfiguration oder Snapshot-Metadaten speichern, einschließlich Wert, Einheit, Quelle, Quelldatum oder Abrufdatum und Berechnungshinweis.
- Favoriten und zuletzt angesehene Windparks hinzufügen.
- Favoriten und zuletzt angesehene Windparks über SQLDelight persistieren.
- FAQ- und Grenzentexte aus datenreichen Flows erreichbar halten.

### Phase 3: Validierung und Feinschliff

- Manuelle Akzeptanzprüfungen ausführen und bekannte Demo-Grenzen dokumentieren.
- Leer-, Fehler- und Berechtigungszustände verbessern.
- Texte und Barrierefreiheit verfeinern.
- Präsentationsreife Demo-Daten vorbereiten.

### Phase 4: Erweiterung

- Strukturierte Datenhinweise über `ReportWindTurbine` hinzufügen, sofern für den MVP bestätigt.
- Vergleichsansicht hinzufügen.
- Prognose- oder Szenariofunktionalität hinzufügen.
- Umfangreichere Module für kommunalen Nutzen und Beteiligung hinzufügen.

### Phase 5: Optionale fortgeschrittene Funktionen

- Kamera-basierte Identifikation oder AR-Overlay.
- Quellengebundener FAQ-Assistent ausschließlich auf Basis von kuratiertem WindKlar-Wissen, lokalen Snapshot-Metadaten und ausgewähltem Windpark-Kontext.
- Android-Implementierungsoption: plattformlokales Gemini Nano über Google AI Edge oder ML Kit GenAI/AICore, sofern verfügbar, mit Laufzeit-Verfügbarkeitsprüfung und Ausweichlösung.
- iOS-Implementierungsoption: Apple Foundation Models, sofern auf Apple-Intelligence-fähigen Geräten verfügbar, hinter einem dünnen Plattformadapter.
- Lokale Modellimplementierungsoption: kleines offenes Modell auf dem Gerät über eine dedizierte Inferenzlaufzeit nur dann, wenn Größe, Leistung, Lizenzierung und QA-Risiken akzeptabel sind.
- Bürgerbudget und Fragen-Tracker.
- Echtzeit- oder automatisierter öffentlicher Datenimport.

## 20. Fertigstellungskriterien für den MVP

- Kernfluss aus Start, Karte, Vorschau und Windpark-Detail funktioniert Ende zu Ende.
- Ein deutschlandweiter, öffentlich quellenbasierter JSON-Windanlagen-Snapshot ist lokal verfügbar oder über einen Import-/Cache-Pfad vorhanden.
- Zentrale Metriken zeigen Wert, Einheit, Zeitraum, Quelle und Datenqualität.
- Favoriten und zuletzt angesehene Windparks funktionieren ohne Nutzerkonto.
- Standortberechtigung ist optional und wird verständlich behandelt.
- FAQ- und Grenzentexte sind verfügbar.
- Optionale KI- oder Assistentenprototypen sind für MVP-Abschluss nicht erforderlich und dürfen die Quellen-, Datenqualitäts- und Grenzenkommunikation nicht schwächen.
- Haupt-User-Stories haben manuelle Akzeptanzprüfungen.
- Manuelle QA wurde auf Android durchgeführt; ein iOS-Kurztest wurde durchgeführt, soweit verfügbar.
- Das Produkt kann als kohärenter WindKlar-Prototyp demonstriert werden.

## 21. Offene Fragen für das App-Repository

- Entschieden: Windanlagen sind die atomare Quellen- und Koordinateneinheit; Windparks sind die primäre bürgernahe UX-Einheit für Kartenübersicht, Favoriten und Gemeindekontext.
- Entschieden: Die Karte nutzt progressive Offenlegung. Nutzerinnen und Nutzer sehen zuerst Windparks oder Cluster; einzelne Windanlagen erscheinen nur bei höheren Zoomstufen oder im Windpark-Detailkontext.
- Entschieden: Favoriten sind im MVP nur Windpark-bezogen. Einzelne Windanlagen können geprüft, aber nicht separat gespeichert werden.
- Entschieden: Der MVP nutzt echte MaStR/Open-MaStR-Stammdaten für Windanlagen, sofern verfügbar, während Produktions- und Akzeptanzwirkungswerte mit expliziten Datenqualitätslabels geschätzt oder simuliert sein dürfen.
- Entschieden: MaStR/Open-MaStR-Anlagen-Stammdaten sind `official`; vorverarbeitungsgenerierte Windpark-Aggregate sind `derived`; Produktions- und Akzeptanzwirkungswerte sind `estimated` oder `simulated`, sofern gemessene öffentliche Werte nicht verfügbar sind.
- Entschieden: Der MVP-Datensatz soll Deutschland abdecken, nicht nur Leipzig/Sachsen oder eine andere lokale Demo-Region.
- Entschieden: Der MVP nutzt einen vorverarbeiteten lokalen MaStR/Open-MaStR-JSON-Snapshot statt Live-API-Zugriff innerhalb der App und importiert diesen Snapshot anschließend in SQLDelight.
- Entschieden: `ReportWindTurbine` ist ein Datenhinweis-Ablauf für strukturierte, lokale/exportierbare Datenqualitätshinweise. Er darf keine offizielle MaStR-Korrektur versprechen.
- Entschieden: Datenhinweise werden im MVP lokal in SQLDelight gespeichert und können später exportiert werden; kein Backend- oder Kontoablauf ist erforderlich.
- Entschieden: Datenhinweise nutzen die Kategorien `missing_installation`, `wrong_location`, `wrong_status`, `wrong_wind_park_assignment`, `wrong_technical_data`, `installation_removed` und `other`.
- Entschieden: Datenhinweise erfordern Kategorie, Standort oder bestehende Objektreferenz, Beschreibung und Hinweissicherheit; Bild und vorgeschlagener korrigierter Wert sind optional, Kontaktinformationen sind nicht Teil des MVP.
- Entschieden: Suche bleibt Teil des Kartenflusses als Overlay oder Sheet, nicht als Route der unteren Navigation.
- Entschieden: Die MVP-Karte sollte geteilt/OSM-kompatibel oder datengetrieben sein und darf keine getrennten nativen Android- und iOS-Kartenimplementierungen erfordern.
- Entschieden: Das appseitige Snapshot-Format ist JSON. Quellenvorverarbeitung darf CSV oder andere Rohformate verwenden, aber die App importiert JSON in SQLDelight.
- Entschieden: Der JSON-Snapshot enthält sowohl einzelne Windanlagen als auch vorberechnete Windpark-Aggregate; die App führt zur Laufzeit keine deutschlandweite Windpark-Gruppierung durch.
- Entschieden: Ein hybrides Quellen-/Qualitätsschema verwenden. Stammdatentabellen tragen einfache Quellenfelder; Produktions- und Akzeptanzwirkungswerte liegen in einem dedizierten Metrikmodell.
- Entschieden: SQLDelight-/Domain-Zielmodell ist `wind_turbine`, `wind_park`, `metric`, `favorite_wind_park`, `recent_wind_park`, `data_hint` und optional `snapshot_metadata`.
- Entschieden: Jahresproduktion, CO2-Einsparung und Haushaltsäquivalente werden aus einfachen dokumentierten Annahmen geschätzt; Volllaststunden, Emissionsfaktor und Haushaltsverbrauch liegen in Konfiguration oder Snapshot-Metadaten.
- Entschieden: Konkrete Annahmewerte bleiben bis zur Snapshot-Vorbereitung offen, aber Snapshot-Metadaten müssen Wert, Einheit, Quelle, Quellen-/Abrufdatum und Berechnungshinweis enthalten.
- Entschieden: Kommunale Beteiligung wird als kurzer geschätzter erwarteter §6-EEG-Wert auf Basis von 0,2 ct/kWh und geschätzter Jahresproduktion angezeigt, mit "Keine bestätigte Auszahlung", sofern keine Zahlungsquelle existiert.
- Entschieden: Das lokale Verlaufskonzept ist "Zuletzt angesehen". Jeder geöffnete Windpark wird erfasst, unabhängig davon, ob er über Karte, Suche oder Favoriten erreicht wurde.
- Entschieden: Favoriten und zuletzt angesehene Windparks sind im MVP SQLDelight-gestützt.
- Entschieden: Standort ist optional, wird nur nach Nutzeraktion angefragt, nicht gespeichert und kann durch manuelle Suche oder Pin-Platzierung ersetzt werden.
- Entschieden: Ansichten der obersten Ebene zeigen keine Zurück-Buttons zu `Map`; die untere Navigation behandelt Bewegung auf oberster Ebene, und Zurück-Bedienelemente sind Unterabläufen vorbehalten.
- Entschieden: `Profile` ist im MVP `Info & Einstellungen`, ohne Logout, ohne Kontosprache und ohne Bedienelemente für nicht implementierte Benachrichtigungen oder Dunkelmodus.
- Entschieden: Die grüne Nature/Trust-Designrichtung ist für den MVP akzeptiert, aber Farben, Typografie, Abstände, Radien und Elevation sollten als Compose-Theme-/Designtokens zentralisiert werden.
- Entschieden: Figma ist die funktionale/visuelle Referenz für Screenset, Informationsarchitektur, Komponentenabsicht, grobes Layout und Copy, kein pixelgenauer Vertrag.
- Entschieden: Ein späterer FAQ-Assistent darf nur als quellengebundener Erklär-Assistent erkundet werden, nicht als allgemeiner Chatbot. Er muss kuratiertes Wissen, App-Kontext und Quellenmetadaten verwenden und klar sagen, wenn WindKlar eine Live-Betriebsursache wie Anlagenstillstand nicht kennen kann.
- Entschieden: Die AGP-9.x-/KMP-Kompatibilitätswarnung ist als Seminar-MVP-Risiko akzeptiert; keine Modulmigration vor der Demo, sofern der Build nicht bricht.
- Entschieden: Android-Manuelle-QA ist vor der Demo verpflichtend; ein kurzer Test im iOS-Simulator oder auf einem iOS-Gerät ist optional, soweit verfügbar, und kein Demo-Hindernis.

## 22. Weitere Hinweise

Dieses PRD verbindet bewusst die Struktur der Lehrveranstaltung mit dem WindKlar-Konzept. Es spiegelt die Modulphasen wider: Beratung und Analyse, Stakeholder- und Anforderungsarbeit, Funktionspriorisierung nach Nutzen und Komplexität, Konzept- und Designprinzipien, Implementierungsentscheidungen sowie QA- und Release-Denken.

Der stärkste MVP ist nicht die funktionsreichste Version. Der stärkste MVP ist ein vertrauenswürdiger, verständlicher Transparenzfluss: Karte, Windpark-Detail, Bürgerwirkung, lokaler Nutzen, Datenqualität und gespeicherter Kontext.
