package de.gueler.foerderkredit.common;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import de.gueler.foerderkredit.antrag.AntragNichtGefundenException;
import de.gueler.foerderkredit.tilgung.TilgungsplanNichtBerechenbarException;
import de.gueler.foerderkredit.workflow.NichtFoerderfaehigException;
import de.gueler.foerderkredit.workflow.UngueltigerStatuswechselException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.OptimisticLockingFailureException;
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

    /** 422: Der Antrag ist gespeichert, aber mit seinen Werten ist kein Tilgungsplan möglich. */
    @ExceptionHandler(TilgungsplanNichtBerechenbarException.class)
    public ProblemDetail behandleTilgungsplanNichtBerechenbar(TilgungsplanNichtBerechenbarException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Tilgungsplan nicht berechenbar");
        return problem;
    }

    /** 409: Der Übergang passt nicht zum aktuellen Zustand des Antrags. */
    @ExceptionHandler(UngueltigerStatuswechselException.class)
    public ProblemDetail behandleUngueltigenStatuswechsel(UngueltigerStatuswechselException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Statuswechsel nicht erlaubt");
        problem.setProperty("aktuellerStatus", ex.getVonStatus());
        problem.setProperty("gewuenschterStatus", ex.getNachStatus());
        // Sortiert, damit die Antwort immer gleich aussieht (Set hat keine feste Reihenfolge).
        problem.setProperty("erlaubteFolgestatus", ex.getErlaubteFolgestatus().stream().sorted().toList());
        return problem;
    }

    /**
     * 409: Zwei Bearbeiter haben denselben Antrag gleichzeitig geändert. Der zweite Commit
     * ist an der Versionsprüfung ({@code @Version}) gescheitert und wurde komplett zurückgerollt.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail behandleGleichzeitigeAenderung(OptimisticLockingFailureException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Der Antrag wurde zwischenzeitlich von jemand anderem geändert. "
                        + "Bitte den Antrag neu laden und den Statuswechsel erneut versuchen.");
        problem.setTitle("Gleichzeitige Änderung");
        return problem;
    }

    /** 422: Die Anfrage ist formal richtig, aber fachlich nicht umsetzbar. */
    @ExceptionHandler(NichtFoerderfaehigException.class)
    public ProblemDetail behandleNichtFoerderfaehig(NichtFoerderfaehigException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Antrag nicht förderfähig");
        problem.setProperty("nichtErfuellteRegeln", ex.getNichtErfuellteRegeln());
        return problem;
    }

    private static String erlaubteWerte(Class<?> enumTyp) {
        return Arrays.stream(enumTyp.getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(", "));
    }
}
