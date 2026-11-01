package de.gueler.foerderkredit.workflow;

import de.gueler.foerderkredit.antrag.AntragStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request-Body für POST /api/antraege/{id}/statuswechsel. */
public record StatuswechselRequest(

        @NotNull(message = "Bitte den neuen Status angeben.")
        AntragStatus neuerStatus,

        @Size(max = 500, message = "Der Kommentar darf höchstens 500 Zeichen lang sein.")
        String kommentar,

        @NotBlank(message = "Bitte den Bearbeiter angeben.")
        @Size(max = 100, message = "Der Bearbeiter darf höchstens 100 Zeichen lang sein.")
        String bearbeiter
) {
}
