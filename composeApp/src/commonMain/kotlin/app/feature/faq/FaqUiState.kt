package app.feature.faq

data class FaqUiState(
    val questions: List<FaqQuestionUiModel> = defaultFaqQuestions,
    val initialExpandedQuestionId: String? = null,
)

data class FaqQuestionUiModel(
    val id: String,
    val question: String,
    val answer: String,
    val icon: FaqQuestionIcon,
    val category: FaqCategory,
)

enum class FaqCategory {
    Basics,
    LocalBenefit,
    Concerns,
    DataTrust,
}

enum class FaqQuestionIcon {
    Wind,
    Co2,
    Operator,
    Participation,
    Money,
    Noise,
    Wildlife,
    Agriculture,
    Data,
    Limits,
    Warning,
}

private val defaultFaqQuestions = listOf(
    FaqQuestionUiModel(
        id = "what-windklar-shows",
        question = "Was zeigt mir WindKlar?",
        answer = "WindKlar zeigt Windparks in Deutschland, die zugehörigen Windanlagen, öffentliche Stammdaten und verständliche Wirkungswerte. Dazu gehören zum Beispiel installierte Leistung, geschätzte Jahresproduktion, CO₂-Einsparung, Haushaltsäquivalente und möglicher kommunaler Nutzen. Die App erklärt außerdem, welche Werte offiziell, abgeleitet oder geschätzt sind.",
        icon = FaqQuestionIcon.Data,
        category = FaqCategory.Basics,
    ),
    FaqQuestionUiModel(
        id = "wind-turbine-function",
        question = "Wie funktioniert eine Windenergieanlage?",
        answer = "Windenergieanlagen wandeln die Bewegungsenergie des Windes in Strom um. Der Wind dreht die Rotorblätter, die über den Antriebsstrang einen Generator bewegen. Wie viel Strom entsteht, hängt vor allem von Standort, Windangebot, Anlagenhöhe, Rotordurchmesser und installierter Leistung ab.",
        icon = FaqQuestionIcon.Wind,
        category = FaqCategory.Basics,
    ),
    FaqQuestionUiModel(
        id = "co2-impact",
        question = "Spart Windenergie wirklich CO₂?",
        answer = "Ja, Windstrom vermeidet Emissionen, wenn er fossile Stromerzeugung ersetzt. Der konkrete Wert hängt aber davon ab, welche Jahresproduktion angenommen wird und mit welchem Emissionsfaktor gerechnet wird. WindKlar zeigt solche Werte deshalb als geschätzte Wirkungsdaten und nicht als gemessene Einsparung.",
        icon = FaqQuestionIcon.Co2,
        category = FaqCategory.Basics,
    ),
    FaqQuestionUiModel(
        id = "operators",
        question = "Wer betreibt Windparks in Deutschland?",
        answer = "Windparks werden von Energieunternehmen, Stadtwerken, Projektgesellschaften, Genossenschaften und teils auch von Bürgerenergie-Initiativen betrieben.",
        icon = FaqQuestionIcon.Operator,
        category = FaqCategory.Basics,
    ),
    FaqQuestionUiModel(
        id = "municipal-benefit",
        question = "Was bringt ein Windpark meiner Gemeinde?",
        answer = "Ein Windpark kann vor Ort auf mehreren Wegen Nutzen stiften: durch mögliche kommunale Beteiligung bei Windenergie an Land, Gewerbesteuer, Pachten, Aufträge für lokale Betriebe oder konkrete Projekte in der Gemeinde. WindKlar zeigt solche Angaben vorsichtig und unterscheidet zwischen belegten Daten, abgeleiteten Werten und Schätzungen.",
        icon = FaqQuestionIcon.Money,
        category = FaqCategory.LocalBenefit,
    ),
    FaqQuestionUiModel(
        id = "municipal-participation-calculation",
        question = "Wie wird der kommunale Nutzen berechnet?",
        answer = "Im MVP wird der kommunale Nutzen als mögliche Beteiligung für Windenergie an Land nach § 6 EEG eingeordnet. Dafür kann die geschätzte Jahresproduktion mit einer möglichen Beteiligung von bis zu 0,2 Cent pro Kilowattstunde verknüpft werden. Das ist keine bestätigte Auszahlung, sondern ein transparenter Orientierungswert.",
        icon = FaqQuestionIcon.Participation,
        category = FaqCategory.LocalBenefit,
    ),
    FaqQuestionUiModel(
        id = "participation",
        question = "Kann ich mich als Bürgerin oder Bürger beteiligen?",
        answer = "Das hängt vom konkreten Projekt ab. Manche Windparks bieten Bürgerenergie-Modelle, Genossenschaften, Nachrangdarlehen oder kommunale Beteiligungsangebote an. WindKlar kann solche Möglichkeiten nur anzeigen, wenn sie als verlässliche lokale Information vorliegen.",
        icon = FaqQuestionIcon.Participation,
        category = FaqCategory.LocalBenefit,
    ),
    FaqQuestionUiModel(
        id = "agriculture",
        question = "Kann die Fläche unter Windanlagen weiter landwirtschaftlich genutzt werden?",
        answer = "In vielen Fällen ja. Dauerhaft benötigt werden vor allem Fundament, Kranstellfläche und Zuwegung. Die umliegenden Flächen können häufig weiter als Acker, Wiese oder Weide genutzt werden. Wie viel Fläche tatsächlich gebunden ist, hängt vom Standort, der Bauweise und den Zufahrten ab.",
        icon = FaqQuestionIcon.Agriculture,
        category = FaqCategory.LocalBenefit,
    ),
    FaqQuestionUiModel(
        id = "noise",
        question = "Wie laut sind Windenergieanlagen?",
        answer = "Windenergieanlagen erzeugen hörbare Geräusche, zum Beispiel durch Rotorblätter und technische Komponenten. Deshalb werden Schallwerte im Genehmigungsverfahren geprüft und durch Grenzwerte begrenzt. WindKlar ersetzt keine lokale Schallprognose, kann aber erklären, warum Lärm eine relevante Prüfgröße ist.",
        icon = FaqQuestionIcon.Noise,
        category = FaqCategory.Concerns,
    ),
    FaqQuestionUiModel(
        id = "infrasound",
        question = "Ist Infraschall gefährlich?",
        answer = "Windenergieanlagen erzeugen auch tieffrequenten Schall. Nach dem Stand der öffentlichen Bewertung liegen die Infraschallpegel moderner Windanlagen in üblichen Wohnabständen meist deutlich unterhalb der Wahrnehmungsschwelle. Individuelle Beschwerden sollten trotzdem ernst genommen und lokal geprüft werden.",
        icon = FaqQuestionIcon.Noise,
        category = FaqCategory.Concerns,
    ),
    FaqQuestionUiModel(
        id = "wildlife",
        question = "Wie gefährlich sind Windanlagen für Vögel und Fledermäuse?",
        answer = "Windenergieanlagen können für bestimmte Vogel- und Fledermausarten ein Risiko sein. Besonders relevant sind Standortwahl, Flugrouten, Brutplätze und Betriebszeiten. Im Planungs- und Genehmigungsverfahren werden deshalb Artenschutz, Schutzabstände und mögliche Abschaltungen geprüft. WindKlar kann diese Risiken einordnen, aber keine rechtsverbindliche Artenschutzprüfung ersetzen.",
        icon = FaqQuestionIcon.Wildlife,
        category = FaqCategory.Concerns,
    ),
    FaqQuestionUiModel(
        id = "standstill",
        question = "Warum stehen Windanlagen manchmal still?",
        answer = "Windanlagen können aus vielen Gründen stillstehen: zu wenig oder zu viel Wind, Wartung, technische Störungen, Netzengpässe, Schall- oder Schattenauflagen, Naturschutzabschaltungen oder Vereisung. WindKlar kennt im MVP keine Live-Betriebsursachen und sollte deshalb nur mögliche Gründe nennen, nicht die konkrete Ursache behaupten.",
        icon = FaqQuestionIcon.Warning,
        category = FaqCategory.Concerns,
    ),
    FaqQuestionUiModel(
        id = "data-source",
        question = "Woher kommen die Daten?",
        answer = "Die Windanlagen-Stammdaten stammen aus öffentlichen Quellen wie MaStR und werden für den MVP als deutschlandweiter Snapshot vorverarbeitet. Die App nutzt diesen lokalen Datensatz zur Laufzeit, statt für die Grundfunktionen live eine externe API abzufragen.",
        icon = FaqQuestionIcon.Data,
        category = FaqCategory.DataTrust,
    ),
    FaqQuestionUiModel(
        id = "estimated-values",
        question = "Warum sind manche Werte nur geschätzt?",
        answer = "Nicht alle wichtigen Informationen liegen öffentlich und einheitlich vor. Jahresproduktion, CO₂-Einsparung, Haushaltsäquivalente und die mögliche kommunale Beteiligung für Windenergie an Land können deshalb aus Stammdaten und dokumentierten Annahmen berechnet werden. WindKlar markiert solche Werte als geschätzt, damit sie nicht wie Messwerte wirken.",
        icon = FaqQuestionIcon.Limits,
        category = FaqCategory.DataTrust,
    ),
    FaqQuestionUiModel(
        id = "data-hint",
        question = "Kann ich fehlende oder falsche Daten melden?",
        answer = "Ja, als lokalen Datenhinweis. Über den Pin-Placement-Knopf in der Kartenansicht können Sie einen Hinweis an eine Position setzen und lokal speichern. Das ist keine offizielle MaStR-Korrektur und kein rechtsverbindlicher Antrag, sondern ein strukturierter Hinweis, dass eine Windanlage fehlen, falsch zugeordnet oder veraltet sein könnte. Gespeicherte Hinweise bleiben lokal auf dem Gerät und können exportiert werden.",
        icon = FaqQuestionIcon.Warning,
        category = FaqCategory.DataTrust,
    ),
    FaqQuestionUiModel(
        id = "app-limits",
        question = "Was kann WindKlar nicht beantworten?",
        answer = "WindKlar kann keine aktuellen Betriebsursachen, keine bestätigten kommunalen Auszahlungen und keine rechtsverbindlichen Aussagen zu Schall, Schattenwurf, Artenschutz oder Genehmigungen liefern. Die App soll öffentliche Daten verständlich machen und Unsicherheiten sichtbar halten.",
        icon = FaqQuestionIcon.Limits,
        category = FaqCategory.DataTrust,
    ),
    FaqQuestionUiModel(
        id = "calculation-assumptions",
        question = "Welche Berechnungsannahmen nutzt WindKlar?",
        answer = "WindKlar nutzt standardisierte, wissenschaftlich fundierte Richtwerte für seine Berechnungen:\n\n1) Volllaststunden: Standortspezifisch berechnet (bundesweiter Richtwert: ca. 2.000 h/a), um die Ertragsleistung zu schätzen.\n2) CO₂-Emissionsfaktor: Ein durchschnittlicher Wert des deutschen Strommixes von 380 g/kWh (0,38 kg/kWh), um die CO₂-Einsparung zu ermitteln.\n3) Haushaltsverbrauch: Ein typischer Jahresstromverbrauch von 3.500 kWh pro 3-Personen-Haushalt.\n4) Kommunale Beteiligung: Bis zu 0,2 ct/kWh der Jahresproduktion gemäß § 6 EEG für Windanlagen an Land.",
        icon = FaqQuestionIcon.Limits,
        category = FaqCategory.DataTrust,
    ),
)
