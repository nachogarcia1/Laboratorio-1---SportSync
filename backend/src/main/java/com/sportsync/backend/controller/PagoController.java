package com.sportsync.backend.controller;

import com.sportsync.backend.dto.IniciarPagoRequest;
import com.sportsync.backend.model.reserva.Pago;
import com.sportsync.backend.service.PagoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/pagos")
public class PagoController {

    private final PagoService service;

    public PagoController(PagoService service) {
        this.service = service;
    }

    // POST /pagos/iniciar — crea reserva PENDIENTE_PAGO + pago + preferencia de MP
    @PostMapping("/iniciar")
    public ResponseEntity<?> iniciar(@RequestBody IniciarPagoRequest req) {
        try {
            PagoService.IniciarResult r = service.iniciarPago(req.getReserva(), req.getMetodo());
            return ResponseEntity.ok(Map.of(
                    "reservaId",     r.reservaId(),
                    "pagoId",        r.pagoId(),
                    "initPoint",     r.initPoint(),
                    "preferenciaId", r.preferenciaId()
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // POST /pagos/webhook — notificación de Mercado Pago (público, sin auth).
    // MP puede mandar ?type=payment&data.id=... o ?topic=payment&id=...
    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestParam Map<String, String> params,
                                     @RequestBody(required = false) Map<String, Object> body) {
        String tipo  = params.getOrDefault("type", params.get("topic"));
        String dataId = params.get("data.id");
        if (dataId == null) dataId = params.get("id");
        if (dataId == null && body != null && body.get("data") instanceof Map<?, ?> data) {
            Object id = ((Map<?, ?>) data).get("id");
            if (id != null) dataId = String.valueOf(id);
        }
        service.procesarNotificacion(tipo, dataId);
        return ResponseEntity.ok().build(); // MP espera 200 para no reintentar
    }

    // POST /pagos/confirmar — confirmación al volver del checkout (sin depender del webhook).
    // El front pasa el payment_id que MP agregó a la back_url; el backend consulta el estado
    // real a MP (no confía en el front) y devuelve el pago actualizado.
    @PostMapping("/confirmar")
    public ResponseEntity<?> confirmar(@RequestParam(required = false) String paymentId,
                                       @RequestParam Long reservaId) {
        try {
            if (paymentId != null && !paymentId.isBlank()) {
                service.procesarNotificacion("payment", paymentId);
            }
            return ResponseEntity.ok(service.obtenerPorReserva(reservaId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /pagos/simular — (solo dev) fija el resultado del pago y aplica la transición de la reserva.
    // ?reservaId=X&resultado=APROBADO|RECHAZADO|PENDIENTE|...
    @PostMapping("/simular")
    public ResponseEntity<?> simular(@RequestParam Long reservaId,
                                     @RequestParam com.sportsync.backend.model.admin.EstadoPago resultado) {
        try {
            return ResponseEntity.ok(service.simularResultado(reservaId, resultado));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /pagos/reserva/{reservaId} — estado del pago (para que el front muestre el resultado)
    @GetMapping("/reserva/{reservaId}")
    public ResponseEntity<?> estado(@PathVariable Long reservaId) {
        try {
            Pago pago = service.obtenerPorReserva(reservaId);
            return ResponseEntity.ok(pago);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}
