package de.gueler.foerderkredit.antrag;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Request-Body für POST /api/antraege.
 *
 * <p>Die Bean-Validation-Annotationen prüfen nur, ob die Eingabe <em>formal</em>
 * korrekt ist (Pflichtfeld, Zahlenformat, Länge). Ob ein Antrag <em>fachlich</em>
 * förderfähig ist, entscheiden später die Förderregeln. Ein Antrag über 5.000 €
 * ist also gültig und wird gespeichert, ist aber nicht förderfähig.
 *
 * <p>Zahlen sind hier Wrapper-Typen ({@code Integer} statt {@code int}), damit ein
 * fehlendes Feld als {@code null} ankommt und von {@code @NotNull} erkannt wird.
 */
public record AntragAnlegenRequest(

        @NotBlank(message = "Bitte den Namen des Antragstellers angeben.")
        @Size(max = 200, message = "Der Name darf höchstens 200 Zeichen lang sein.")
        String antragstellerName,

        @NotNull(message = "Bitte die Rechtsform angeben.")
        Rechtsform rechtsform,

        @NotNull(message = "Bitte die Branche angeben.")
        Branche branche,

        @NotNull(message = "Bitte das Bundesland angeben.")
        Bundesland bundesland,

        @NotNull(message = "Bitte die Anzahl der Mitarbeitenden angeben.")
        @PositiveOrZero(message = "Die Anzahl der Mitarbeitenden darf nicht negativ sein.")
        Integer mitarbeiterAnzahl,

        @NotNull(message = "Bitte den Jahresumsatz angeben.")
        @PositiveOrZero(message = "Der Jahresumsatz darf nicht negativ sein.")
        @Digits(integer = 13, fraction = 2,
                message = "Der Jahresumsatz darf höchstens 13 Vorkomma- und 2 Nachkommastellen haben.")
        BigDecimal jahresumsatz,

        @NotNull(message = "Bitte den Kreditbetrag angeben.")
        @Positive(message = "Der Kreditbetrag muss größer als 0 sein.")
        @Digits(integer = 13, fraction = 2,
                message = "Der Kreditbetrag darf höchstens 13 Vorkomma- und 2 Nachkommastellen haben.")
        BigDecimal kreditbetrag,

        @NotNull(message = "Bitte die Laufzeit in Jahren angeben.")
        @Min(value = 1, message = "Die Laufzeit muss mindestens 1 Jahr betragen.")
        @Max(value = 50, message = "Die Laufzeit darf höchstens 50 Jahre betragen.")
        Integer laufzeitJahre,

        @NotNull(message = "Bitte die Anzahl der tilgungsfreien Jahre angeben (0, wenn keine).")
        @Min(value = 0, message = "Die tilgungsfreien Jahre dürfen nicht negativ sein.")
        @Max(value = 50, message = "Die tilgungsfreien Jahre dürfen höchstens 50 betragen.")
        Integer tilgungsfreieJahre,

        @NotNull(message = "Bitte den Sollzins in Prozent angeben.")
        @DecimalMin(value = "0.000", message = "Der Sollzins darf nicht negativ sein.")
        @DecimalMax(value = "20.000", message = "Der Sollzins darf höchstens 20 % betragen.")
        @Digits(integer = 2, fraction = 3,
                message = "Der Sollzins darf höchstens 3 Nachkommastellen haben.")
        BigDecimal sollzinsProzent,

        @NotBlank(message = "Bitte den Verwendungszweck angeben.")
        @Size(max = 500, message = "Der Verwendungszweck darf höchstens 500 Zeichen lang sein.")
        String verwendungszweck
) {
}
