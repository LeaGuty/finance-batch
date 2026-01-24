package cl.duoc.finance_batch.items;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import cl.duoc.finance_batch.business.EstadoFinanciero;
import cl.duoc.finance_batch.business.EstadoFinancieroDTO;

/**
 * Procesador de estados financieros anuales para el job de procesamiento paralelo.
 *
 * Este procesador es responsable de:
 * 1. Validar la integridad de los datos leídos de múltiples archivos CSV
 * 2. Parsear fechas en múltiples formatos posibles
 * 3. Convertir tipos de datos de String a tipos nativos de Java
 * 4. Transformar el DTO en una entidad JPA lista para persistir
 *
 * Reglas de validación:
 * - Descarta registros con monto vacío, null o no numérico
 * - Descarta registros con fechas inválidas
 * - Soporta múltiples formatos de fecha para mayor flexibilidad
 *
 * Formatos de fecha soportados:
 * - yyyy/MM/dd
 * - dd-MM-yyyy
 * - yyyy-MM-dd
 * - dd/MM/yyyy
 * - MM/dd/yyyy
 *
 * Este procesador se utiliza en un job con particionamiento, donde múltiples
 * archivos CSV se procesan en paralelo para mejorar el rendimiento.
 *
 * @author Finance Batch System
 * @version 1.0
 * @see EstadoFinancieroDTO
 * @see EstadoFinanciero
 */
public class EstadoFinancieroProcessor implements ItemProcessor<EstadoFinancieroDTO, EstadoFinanciero> {

    private static final Logger log = LoggerFactory.getLogger(EstadoFinancieroProcessor.class);

    /**
     * Lista de formatos de fecha soportados para parsear fechas del CSV.
     * Se intentan en orden hasta encontrar uno que funcione.
     */
    private final List<DateTimeFormatter> dateFormats = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy")
    );

    /**
     * Procesa un registro de estado financiero desde el DTO hacia la entidad.
     *
     * @param item el DTO con los datos leídos del CSV
     * @return la entidad EstadoFinanciero procesada, o null si el registro debe ser descartado
     * @throws Exception si ocurre un error durante el procesamiento
     */
    @Override
    public EstadoFinanciero process(EstadoFinancieroDTO item) throws Exception {

        // 1. Validar y parsear Monto
        Double monto = null;
        try {
            if (item.getMonto() == null || item.getMonto().isEmpty()) {
                return null; // Descarta registros sin monto
            }
            monto = Double.parseDouble(item.getMonto());
        } catch (NumberFormatException e) {
            log.warn("Monto inválido para cuenta {}", item.getCuentaId());
            return null;
        }

        // 2. Validar y parsear Fecha (prueba todos los formatos posibles)
        LocalDate fecha = parseDate(item.getFecha());
        if (fecha == null) {
            log.warn("Fecha inválida para cuenta {}: {}", item.getCuentaId(), item.getFecha());
            return null;
        }

        // 3. Mapear a la Entidad JPA final
        EstadoFinanciero estado = new EstadoFinanciero();
        estado.setCuentaId(Long.parseLong(item.getCuentaId()));
        estado.setFecha(fecha);
        estado.setMonto(monto);
        estado.setTransaccion(item.getTransaccion());
        estado.setDescripcion(item.getDescripcion());

        return estado;
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
        for (DateTimeFormatter fmt : dateFormats) {
            try {
                return LocalDate.parse(dateStr, fmt);
            } catch (DateTimeParseException e) {
                // Sigue intentando con el siguiente formato
            }
        }
        return null; // Ningún formato funcionó
    }
}