package de.gueler.foerderkredit.common;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import de.gueler.foerderkredit.antrag.AntragNichtGefundenException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.beans.TypeMismatchException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Zentrale Fehlerbehandlung für alle Controller.
 *
 * <p>Jeder Fehler wird als {@link ProblemDetail} nach RFC 9457 beantwortet
 * (Content-Type {@code application/problem+json}) mit einer deutschen Meldung.
 *
 * <p>Die Basisklasse {@link ResponseEntityExceptionHandler} kümmert sich bereits um
 * alle Standardfehler von Spring MVC (z. B. 405 Method Not Allowed) und liefert sie
 * als ProblemDetail. Hier überschreiben wir nur die Fälle, die wir verständlicher
 * formulieren wollen, und ergänzen unsere eigenen fachlichen Exceptions.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** Bean-Validation-Fehler im Request-Body: 400 mit einer Meldung pro Feld. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        // TreeMap sortiert nach Feldnamen, damit die Antwort immer gleich aussieht.
        // Hat ein Feld mehrere Fehler, werden die Meldungen hintereinander gehängt.
        Map<String, String> fehlerProFeld = new TreeMap<>();
        for (FieldError fehler : ex.getBindingResult().getFieldErrors()) {
            fehlerProFeld.merge(fehler.getField(), fehler.getDefaultMessage(),
                    (bisher, neu) -> bisher + " " + neu);
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Die Anfrage enthält ungültige Felder.");
        problem.setTitle("Ungültige Eingabe");
        problem.setProperty("fehler", fehlerProFeld);
        return ResponseEntity.badRequest().body(problem);
    }

    /** Body ist kein gültiges JSON oder enthält z. B. einen unbekannten Enum-Wert. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        String detail = "Der Request-Body ist kein gültiges JSON oder hat ein falsches Format.";

        if (ex.getCause() instanceof InvalidFormatException formatFehler
                && !formatFehler.getPath().isEmpty()) {
            String feld = formatFehler.getPath().get(formatFehler.getPath().size() - 1).getFieldName();
            detail = "Ungültiger Wert '" + formatFehler.getValue() + "' für das Feld '" + feld + "'.";
            Class<?> zieltyp = formatFehler.getTargetType();
            if (zieltyp != null && zieltyp.isEnum()) {
                detail += " Erlaubte Werte: " + erlaubteWerte(zieltyp) + ".";
            }
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Ungültige Eingabe");
        return ResponseEntity.badRequest().body(problem);
    }

    /** Falscher Typ in URL-Pfad oder Query-Parameter, z. B. /api/antraege/keine-uuid. */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
                                                        HttpHeaders headers,
                                                        HttpStatusCode status,
                                                        WebRequest request) {
        String detail = "Ungültiger Wert '" + ex.getValue() + "' für '" + ex.getPropertyName() + "'.";
        Class<?> zieltyp = ex.getRequiredType();
        if (zieltyp != null && zieltyp.isEnum()) {
            detail += " Erlaubte Werte: " + erlaubteWerte(zieltyp) + ".";
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Ungültige Eingabe");
        return ResponseEntity.badRequest().body(problem);
    }

    /** Unbekanntes Sortierfeld, z. B. ?sort=gibtEsNicht. */
    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail behandleUnbekanntesSortierfeld(PropertyReferenceException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Nach dem Feld '" + ex.getPropertyName() + "' kann nicht sortiert werden.");
        problem.setTitle("Ungültige Eingabe");
        return problem;
    }

    @ExceptionHandler(AntragNichtGefundenException.class)
    public ProblemDetail behandleAntragNichtGefunden(AntragNichtGefundenException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Antrag nicht gefunden");
        return problem;
    }

    private static String erlaubteWerte(Class<?> enumTyp) {
        return Arrays.stream(enumTyp.getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(", "));
    }
}
