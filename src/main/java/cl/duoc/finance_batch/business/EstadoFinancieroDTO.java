package cl.duoc.finance_batch.business;

import lombok.Data;

/**
 * Data Transfer Object (DTO) para leer estados financieros desde archivos CSV.
 *
 * Este DTO se utiliza en la fase de lectura (Reader) del proceso batch de estados
 * financieros anuales. Todos los campos son String para facilitar la lectura inicial
 * y permitir validaciones antes de la conversión a tipos específicos.
 *
 * Este DTO es utilizado en un proceso batch con particionamiento, donde múltiples
 * archivos CSV se procesan en paralelo para mejorar el rendimiento.
 *
 * La conversión a la entidad {@link EstadoFinanciero} se realiza en el
 * {@link cl.duoc.finance_batch.items.EstadoFinancieroProcessor}.
 *
 * Estructura del CSV: cuenta_id,fecha,transaccion,monto,descripcion
 *
 * @author Finance Batch System
 * @version 1.0
 */
@Data
public class EstadoFinancieroDTO {

    /**
     * ID de la cuenta leído como String desde el CSV.
     * Se convertirá a Long en el procesador.
     */
    private String cuentaId;

    /**
     * Fecha de la transacción leída como String desde el CSV (formato yyyy-MM-dd).
     * Se parseará a LocalDate en el procesador.
     */
    private String fecha;

    /**
     * Tipo de transacción (compra, retiro, deposito, etc.).
     */
    private String transaccion;

    /**
     * Monto de la transacción leído como String desde el CSV.
     * Se convertirá a Double en el procesador.
     */
    private String monto;

    /**
     * Descripción detallada de la transacción.
     */
    private String descripcion;

    /**
     * Constructor por defecto requerido por el framework de Spring Batch
     * para la creación de instancias durante el proceso de lectura.
     */
    public EstadoFinancieroDTO() {}
}