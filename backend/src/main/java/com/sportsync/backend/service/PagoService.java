package com.sportsync.backend.service;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.sportsync.backend.dto.CrearReservaRequest;
import com.sportsync.backend.model.admin.EstadoPago;
import com.sportsync.backend.model.admin.MetodoPago;
import com.sportsync.backend.model.reserva.Pago;
import com.sportsync.backend.model.reserva.Reserva;
import com.sportsync.backend.repository.PagoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pagos vía Mercado Pago (Checkout Pro). La confirmación es por webhook:
 * el backend consulta el estado real del pago al proveedor, no confía en el frontend.
 * No se almacenan datos de tarjeta: solo el id de transacción del proveedor.
 */
@Service
public class PagoService {

    private final PagoRepository pagoRepo;
    private final ReservaService reservaService;

    @Value("${sportsync.mp.access-token:}")
    private String accessToken;

    @Value("${sportsync.mp.webhook-url:}")
    private String webhookUrl;

    @Value("${sportsync.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    /** Modo simulación (dev): no llama a MP, usa un checkout local con resultado manual. */
    @Value("${sportsync.pagos.simulacion:false}")
    private boolean simulacionForzada;

    private final PasarelaPago pasarelaPago;

    public PagoService(PagoRepository pagoRepo, ReservaService reservaService, PasarelaPago pasarelaPago) {
        this.pagoRepo = pagoRepo;
        this.reservaService = reservaService;
        this.pasarelaPago = pasarelaPago;
    }

    public record IniciarResult(Long reservaId, Long pagoId, String initPoint, String preferenciaId) {}

    private boolean mpConfigurado() {
        return accessToken != null && !accessToken.isBlank();
    }

    /** Se simula cuando está forzado por config o cuando no hay credenciales de MP. */
    private boolean usarSimulacion() {
        return simulacionForzada || !mpConfigurado();
    }

    /** Crea la reserva PENDIENTE_PAGO + el Pago + la preferencia de checkout en MP (o simulado). */
    @Transactional
    public IniciarResult iniciarPago(CrearReservaRequest req, MetodoPago metodo) {
        if (metodo == null) metodo = MetodoPago.MERCADO_PAGO;

        Reserva reserva = reservaService.crearReservaPendiente(req);

        Pago pago = new Pago();
        pago.setReserva(reserva);
        pago.setMonto(reserva.getPrecioTotal());
        pago.setMetodo(metodo);
        pago.setEstado(EstadoPago.PENDIENTE);
        pago = pagoRepo.save(pago);

        // Pago con tarjeta (crédito/débito): formulario propio + pasarela mock.
        if (metodo == MetodoPago.TARJETA_CREDITO || metodo == MetodoPago.TARJETA_DEBITO) {
            pago.setPreferenciaId("TARJETA");
            pagoRepo.save(pago);
            String url = frontendUrl + "/pago/tarjeta?reserva=" + reserva.getId();
            return new IniciarResult(reserva.getId(), pago.getId(), url, "TARJETA");
        }

        // Modo simulación (dev): checkout local en vez de Mercado Pago
        if (usarSimulacion()) {
            pago.setPreferenciaId("SIMULADO");
            pagoRepo.save(pago);
            String simUrl = frontendUrl + "/pago/simular?reserva=" + reserva.getId();
            return new IniciarResult(reserva.getId(), pago.getId(), simUrl, "SIMULADO");
        }

        try {
            MercadoPagoConfig.setAccessToken(accessToken);

            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .title("Reserva de cancha — SportSync")
                    .quantity(1)
                    .unitPrice(BigDecimal.valueOf(reserva.getPrecioTotal()))
                    .currencyId("ARS")
                    .build();

            String retorno = frontendUrl + "/pago/resultado?reserva=" + reserva.getId();
            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(retorno).failure(retorno).pending(retorno)
                    .build();

            PreferenceRequest.PreferenceRequestBuilder builder = PreferenceRequest.builder()
                    .items(List.of(item))
                    .backUrls(backUrls)
                    .externalReference(String.valueOf(pago.getId()));
            if (webhookUrl != null && !webhookUrl.isBlank()) {
                builder.notificationUrl(webhookUrl);
            }

            Preference preference = new PreferenceClient().create(builder.build());
            pago.setPreferenciaId(preference.getId());
            pagoRepo.save(pago);

            return new IniciarResult(reserva.getId(), pago.getId(),
                    preference.getInitPoint(), preference.getId());
        } catch (com.mercadopago.exceptions.MPApiException e) {
            // MP devuelve el detalle real en el cuerpo de la respuesta (no en getMessage)
            String detalle = e.getApiResponse() != null ? e.getApiResponse().getContent() : e.getMessage();
            throw new IllegalStateException("No se pudo iniciar el pago con Mercado Pago: " + detalle, e);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo iniciar el pago con Mercado Pago: " + e.getMessage(), e);
        }
    }

    /**
     * Procesa la notificación (webhook) de Mercado Pago. MP envía type=payment con el
     * id del pago; consultamos su estado real y actualizamos Pago + Reserva.
     */
    @Transactional
    public void procesarNotificacion(String tipo, String dataId) {
        if (!mpConfigurado() || dataId == null || dataId.isBlank()) return;
        if (tipo != null && !tipo.equals("payment")) return; // solo notificaciones de pago

        try {
            MercadoPagoConfig.setAccessToken(accessToken);
            Payment payment = new PaymentClient().get(Long.parseLong(dataId));
            String externalRef = payment.getExternalReference(); // = id del Pago
            if (externalRef == null) return;

            Pago pago = pagoRepo.findById(Long.parseLong(externalRef)).orElse(null);
            if (pago == null) return;

            pago.setProveedorId(String.valueOf(payment.getId()));
            EstadoPago nuevo = mapearEstado(payment.getStatus());
            pago.setEstado(nuevo);
            pagoRepo.save(pago);

            // Transición de la reserva según el resultado del pago
            if (nuevo == EstadoPago.APROBADO) {
                reservaService.confirmarReserva(pago.getReserva());
            } else if (nuevo == EstadoPago.RECHAZADO || nuevo == EstadoPago.CANCELADO || nuevo == EstadoPago.VENCIDO) {
                reservaService.cancelarPorPago(pago.getReserva());
            }
        } catch (Exception e) {
            System.err.println("Error procesando webhook de Mercado Pago: " + e.getMessage());
        }
    }

    private EstadoPago mapearEstado(String mpStatus) {
        if (mpStatus == null) return EstadoPago.PENDIENTE;
        return switch (mpStatus) {
            case "approved"   -> EstadoPago.APROBADO;
            case "rejected"   -> EstadoPago.RECHAZADO;
            case "cancelled"  -> EstadoPago.CANCELADO;
            case "refunded", "charged_back" -> EstadoPago.REEMBOLSADO;
            default           -> EstadoPago.PENDIENTE; // in_process, pending, authorized...
        };
    }

    /**
     * Simula el resultado de un pago (solo dev). Aplica la MISMA máquina de estados
     * que el webhook real: actualiza el Pago y la Reserva según el resultado.
     */
    @Transactional
    public Pago simularResultado(Long reservaId, EstadoPago resultado) {
        if (!usarSimulacion())
            throw new IllegalStateException("La simulación de pagos no está habilitada.");

        Pago pago = pagoRepo.findByReservaId(reservaId)
                .orElseThrow(() -> new IllegalStateException("No hay pago para esa reserva."));

        pago.setProveedorId("SIM-" + System.currentTimeMillis());
        pago.setEstado(resultado);
        pagoRepo.save(pago);

        if (resultado == EstadoPago.APROBADO) {
            reservaService.confirmarReserva(pago.getReserva());
        } else if (resultado == EstadoPago.RECHAZADO || resultado == EstadoPago.CANCELADO
                || resultado == EstadoPago.VENCIDO) {
            reservaService.cancelarPorPago(pago.getReserva());
        }
        return pago;
    }

    /**
     * Procesa el pago con tarjeta de una reserva pendiente, usando la pasarela mock.
     * Solo confirma la reserva si el pago es APROBADO. Rechazo/datos inválidos dejan
     * la reserva PENDIENTE_PAGO (reintentable) y devuelven el resultado para el front.
     */
    @Transactional
    public com.sportsync.backend.dto.ResultadoPago procesarPagoTarjeta(
            Long reservaId, com.sportsync.backend.dto.DatosTarjeta tarjeta) {
        Pago pago = pagoRepo.findByReservaId(reservaId)
                .orElseThrow(() -> new IllegalStateException("No hay pago para esa reserva."));

        com.sportsync.backend.dto.ResultadoPago resultado = pasarelaPago.cobrar(
                new com.sportsync.backend.dto.SolicitudPago(
                        BigDecimal.valueOf(pago.getMonto()),
                        "Reserva de cancha SportSync",
                        tarjeta));

        if (resultado.fueAprobado()) {
            pago.setProveedorId(resultado.getTransaccionId());
            pago.setEstado(EstadoPago.APROBADO);
            pagoRepo.save(pago);
            reservaService.confirmarReserva(pago.getReserva());
        }
        return resultado;
    }

    public Pago obtenerPorReserva(Long reservaId) {
        return pagoRepo.findByReservaId(reservaId)
                .orElseThrow(() -> new IllegalStateException("No hay pago para esa reserva."));
    }
}
