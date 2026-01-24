package cl.duoc.finance_batch.items;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.annotation.Configuration;

import cl.duoc.finance_batch.business.Movimiento;
import cl.duoc.finance_batch.business.MovimientoDTO;

/**
 * Procesador de movimientos financieros diarios para el job de reporte diario.
 *
 * Este procesador es responsable de:
 * 1. Validar la integridad de los datos leídos del CSV
 * 2. Parsear fechas en múltiples formatos posibles
 * 3. Convertir tipos de datos de String a tipos nativos de Java
 * 4. Aplicar reglas de negocio para filtrar datos anómalos
 * 5. Agregar marcas de auditoría (fecha de procesamiento)
 *
 * Reglas de validación:
 * - Descarta registros con monto vacío, null, no numérico o <= 0
 * - Descarta registros con fechas inválidas o en formato incorrecto
 * - Normaliza fechas desde múltiples formatos posibles
 *
 * Formatos de fecha soportados:
 * - yyyy/MM/dd
 * - dd-MM-yyyy
 * - yyyy-MM-dd
 * - dd/MM/yyyy
 *
 * @author Finance Batch System
 * @version 1.0
 * @see MovimientoDTO
 * @see Movimiento
 */
@Configuration
public class MovimientoItemProcessor implements ItemProcessor<MovimientoDTO, Movimiento> {

    private static final Logger log = LoggerFactory.getLogger(MovimientoItemProcessor.class);

    /**
     * Lista de formatos de fecha soportados para parsear fechas del CSV.
     * Se intentan en orden hasta encontrar uno que funcione.
     */
    private final List<DateTimeFormatter> dateFormats = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    );

    /**
     * Procesa un registro de movimiento desde el DTO hacia la entidad.
     *
     * @param item el DTO con los datos leídos del CSV
     * @return la entidad Movimiento procesada, o null si el registro debe ser descartado
     * @throws Exception si ocurre un error durante el procesamiento
     */
    @Override
    public Movimiento process(MovimientoDTO item) throws Exception {
        // 1. Validar y parsear Monto
        Double monto = null;
        try {
            if (item.getMonto() == null || item.getMonto().isEmpty()) {
                log.warn("Registro descartado (Monto vacío): ID {}", item.getId());
                return null; // Descarta el registro
            }
            monto = Double.parseDouble(item.getMonto());

            // Regla de negocio: Descarta montos negativos o cero (anomalías)
            if (monto <= 0) {
                log.warn("Registro descartado (Monto negativo o cero): ID {} Monto: {}", item.getId(), monto);
                return null;
            }
        } catch (NumberFormatException e) {
            log.warn("Registro descartado (Monto no numérico): ID {}", item.getId());
            return null;
        }

        // 2. Validar y parsear Fecha (el CSV puede contener formatos mezclados)
        LocalDate fecha = parseDate(item.getFecha());
        if (fecha == null) {
            log.warn("Registro descartado (Fecha inválida): ID {} Fecha: {}", item.getId(), item.getFecha());
            return null; // Descarta fechas inválidas como "2024-13-01"
        }

        // 3. Transformación: Crear el objeto final limpio
        Movimiento movimiento = new Movimiento();
        movimiento.setId(Long.parseLong(item.getId()));
        movimiento.setFecha(fecha);
        movimiento.setMonto(monto);
        movimiento.setTipo(item.getTipo());
        movimiento.setFechaProceso(LocalDate.now().toString()); // Marca de auditoría

        log.info("Convirtiendo (" + item + ") a (" + movimiento + ")");
        return movimiento;
    }

    /**
     * Intenta parsear una fecha usando múltiples formatos.
     * Prueba cada formato en orden hasta encontrar uno que funcione.
     *
     * @param dateStr la fecha en formato String desde el CSV
     * @return LocalDate parseada exitosamente, o null si ningún formato funciona
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;

        for (DateTimeFormatter formatter : dateFormats) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // Si falla este formato, prueba el siguiente
            }
        }
        return null; // Ningún formato funcionó
    }
}