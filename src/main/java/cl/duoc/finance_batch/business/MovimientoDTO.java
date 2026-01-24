package cl.duoc.finance_batch.business;

import lombok.Data;

/**
 * Data Transfer Object (DTO) para leer movimientos financieros desde el archivo CSV.
 *
 * Este DTO se utiliza en la fase de lectura (Reader) del proceso batch.
 * Todos los campos son String para evitar errores de parseo durante la lectura
 * inicial y permitir validaciones personalizadas antes de la conversión a tipos específicos.
 *
 * La conversión a la entidad {@link Movimiento} se realiza en el
 * {@link cl.duoc.finance_batch.items.MovimientoItemProcessor}.
 *
 * @author Finance Batch System
 * @version 1.0
 */
@Data
public class MovimientoDTO {

    /**
     * ID del movimiento leído como String desde el CSV.
     * Se convertirá a Long en el procesador.
     */
    private String id;

    /**
     * Fecha del movimiento leída como String desde el CSV (formato dd/MM/yyyy).
     * Se parseará a LocalDate en el procesador.
     */
    private String fecha;

    /**
     * Monto de la transacción leído como String desde el CSV.
     * Se convertirá a Double en el procesador y se aplicarán validaciones.
     */
    private String monto;

    /**
     * Tipo de movimiento leído desde el CSV.
     * Se validará en el procesador para filtrar valores inválidos.
     */
    private String tipo;
}