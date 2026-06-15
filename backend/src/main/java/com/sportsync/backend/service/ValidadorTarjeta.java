package com.sportsync.backend.service;

import com.sportsync.backend.dto.DatosTarjeta;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.YearMonth;
import java.util.Optional;

/**
 * Validaciones de los datos de tarjeta. Devuelve el mensaje de error (si lo hay)
 * en vez de lanzar excepciones, porque "datos inválidos" es un resultado normal.
 * La fecha actual se toma de un Clock inyectable (testeable).
 */
@Component
public class ValidadorTarjeta {

    private final Clock clock;

    public ValidadorTarjeta(Clock clock) {
        this.clock = clock;
    }

    /** Saca espacios y guiones del número de tarjeta. */
    public String normalizarNumero(String numero) {
        return numero == null ? "" : numero.replaceAll("[\\s-]", "");
    }

    /** @return Optional con el mensaje de error, o vacío si los datos son válidos. */
    public Optional<String> validar(DatosTarjeta t) {
        if (t == null) return Optional.of("Faltan los datos de la tarjeta.");

        Optional<String> e;
        if ((e = validarTitular(t.getTitular())).isPresent())      return e;
        if ((e = validarNumero(t.getNumero())).isPresent())        return e;
        if ((e = validarCvv(t.getCvv())).isPresent())              return e;
        if ((e = validarVencimiento(t.getVencimiento())).isPresent()) return e;
        return Optional.empty();
    }

    Optional<String> validarTitular(String titular) {
        if (titular == null || titular.trim().isEmpty())
            return Optional.of("El nombre del titular es obligatorio.");
        String t = titular.trim();
        // Debe tener al menos una letra y solo letras/espacios/.'- (rechaza solo-números o basura)
        if (!t.matches("[\\p{L} .'-]+") || !t.matches(".*\\p{L}.*"))
            return Optional.of("El nombre del titular no es válido.");
        return Optional.empty();
    }

    Optional<String> validarNumero(String numero) {
        String n = normalizarNumero(numero);
        if (!n.matches("\\d+"))
            return Optional.of("El número de tarjeta solo puede contener dígitos.");
        if (n.length() < 13 || n.length() > 19)
            return Optional.of("El número de tarjeta debe tener entre 13 y 19 dígitos.");
        if (!pasaLuhn(n))
            return Optional.of("El número de tarjeta no es válido.");
        return Optional.empty();
    }

    Optional<String> validarCvv(String cvv) {
        if (cvv == null || !cvv.matches("\\d{3,4}"))
            return Optional.of("El código de seguridad (CVV) no es válido.");
        return Optional.empty();
    }

    Optional<String> validarVencimiento(String vencimiento) {
        if (vencimiento == null || !vencimiento.matches("\\d{1,2}/\\d{2}(\\d{2})?"))
            return Optional.of("La fecha de expiración debe tener formato MM/AA o MM/AAAA.");
        String[] partes = vencimiento.split("/");
        int mes = Integer.parseInt(partes[0]);
        int anio = Integer.parseInt(partes[1]);
        if (partes[1].length() == 2) anio += 2000;
        if (mes < 1 || mes > 12)
            return Optional.of("El mes de expiración debe estar entre 1 y 12.");
        YearMonth expira = YearMonth.of(anio, mes);
        // Válida hasta el último día del mes indicado → vencida solo si el mes ya pasó
        if (expira.isBefore(YearMonth.now(clock)))
            return Optional.of("La tarjeta está vencida.");
        return Optional.empty();
    }

    /** Algoritmo de Luhn. */
    boolean pasaLuhn(String numero) {
        int suma = 0;
        boolean duplicar = false;
        for (int i = numero.length() - 1; i >= 0; i--) {
            int d = numero.charAt(i) - '0';
            if (duplicar) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            suma += d;
            duplicar = !duplicar;
        }
        return suma % 10 == 0;
    }
}
