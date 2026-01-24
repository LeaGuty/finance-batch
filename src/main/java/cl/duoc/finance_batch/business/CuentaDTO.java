package cl.duoc.finance_batch.business;

import lombok.Data;

/**
 * Data Transfer Object (DTO) para leer cuentas desde el archivo CSV.
 *
 * Este DTO se utiliza en la fase de lectura (Reader) del proceso batch.
 * Todos los campos son String para facilitar la lectura inicial del CSV
 * y permitir validaciones antes de la conversión a tipos numéricos.
 *
 * La conversión a la entidad {@link Cuenta} se realiza en el
 * {@link cl.duoc.finance_batch.items.CuentaItemProcessor}.
 *
 * @author Finance Batch System
 * @version 1.0
 */
@Data
public class CuentaDTO {

    /**
     * ID de la cuenta leído como String desde el CSV.
     * Se convertirá a Long en el procesador.
     */
    private String cuentaId;

    /**
     * Nombre del titular de la cuenta.
     */
    private String nombre;

    /**
     * Saldo inicial de la cuenta leído como String desde el CSV.
     * Se convertirá a Double en el procesador.
     */
    private String saldo;

    /**
     * Edad del titular leída como String desde el CSV.
     * Se convertirá a Integer en el procesador.
     * Puede ser null o vacío en el CSV.
     */
    private String edad;

    /**
     * Tipo de cuenta (ahorro, prestamo, hipoteca).
     * Se normaliza a minúsculas en el procesador.
     */
    private String tipo;
}