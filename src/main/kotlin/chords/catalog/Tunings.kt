package chords.catalog

import chords.schema.*

internal val tunings = listOf(
    Tuning(TuningId("guitar-6-e-standard"), InstrumentId("guitar-6"), "E standard", listOf(
        Pitch("E", 4, 64), Pitch("B", 3, 59), Pitch("G", 3, 55),
        Pitch("D", 3, 50), Pitch("A", 2, 45), Pitch("E", 2, 40),
    )),
    Tuning(TuningId("bass-4-e-standard"), InstrumentId("bass-4"), "E standard", listOf(
        Pitch("G", 2, 43), Pitch("D", 2, 38), Pitch("A", 1, 33), Pitch("E", 1, 28),
    )),
    Tuning(TuningId("bass-5-b-standard"), InstrumentId("bass-5"), "B standard", listOf(
        Pitch("G", 2, 43), Pitch("D", 2, 38), Pitch("A", 1, 33),
        Pitch("E", 1, 28), Pitch("B", 0, 23),
    )),
)
