package cl.duoc.finance_batch.business;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Entidad JPA que representa un movimiento financiero diario.
 *
 * Esta clase se utiliza en el proceso batch de reporte diario de movimientos,
 * donde se leen transacciones desde un archivo CSV, se procesan aplicando
 * validaciones y transformaciones, y se almacenan en la base de datos.
 *
 * @author Finance Batch System
 * @version 1.0
 */
@Data
@Entity
@Table(name = "movimientos")
public class Movimiento {

    /**
     * Identificador único del movimiento.
     * Este ID proviene del archivo CSV y actúa como clave primaria.
     */
    @Id
    private Long id;

    /**
     * Fecha en que se realizó el movimiento financiero.
     * Se parsea desde formato String (dd/MM/yyyy) a LocalDate en el procesador.
     */
    private LocalDate fecha;

    /**
     * Monto de la transacción en la moneda del sistema.
     * Los valores negativos son convertidos a positivos en el procesador.
     */
    private Double monto;

    /**
     * Tipo de movimiento financiero.
     * Valores esperados: "ingreso", "egreso", "transferencia", etc.
     * Los valores inválidos (como "-1") son filtrados en el procesador.
     */
    private String tipo;

    /**
     * Marca de tiempo que registra cuándo fue procesado este movimiento.
     * Se genera automáticamente en el procesador para propósitos de auditoría.
     */
    private String fechaProceso;
}