package cl.duoc.finance_batch.business;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Entidad JPA que representa un estado financiero anual de una cuenta.
 *
 * Esta clase se utiliza en el proceso batch de estados financieros anuales,
 * donde se leen transacciones desde múltiples archivos CSV (procesamiento paralelo
 * con particionamiento), se procesan y se almacenan en la base de datos.
 *
 * Este job utiliza técnicas avanzadas de Spring Batch como particionamiento
 * para procesar múltiples archivos en paralelo y mejorar el rendimiento.
 *
 * @author Finance Batch System
 * @version 1.0
 */
@Data
@Entity
@Table(name = "estados_financieros")
public class EstadoFinanciero {

    /**
     * Identificador único autoincremental generado por la base de datos.
     * Este ID es la clave primaria de la tabla y se genera automáticamente
     * para cada nuevo registro insertado.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identificador de la cuenta asociada a este estado financiero.
     * Este valor puede repetirse ya que una cuenta puede tener múltiples
     * transacciones registradas en diferentes fechas.
     */
    private Long cuentaId;

    /**
     * Fecha de la transacción financiera.
     * Se parsea desde formato String (yyyy-MM-dd) a LocalDate en el procesador.
     */
    private LocalDate fecha;

    /**
     * Tipo de transacción realizada.
     * Valores comunes: "compra", "retiro", "deposito", "transferencia".
     */
    private String transaccion;

    /**
     * Monto de la transacción en la moneda del sistema.
     */
    private Double monto;

    /**
     * Descripción detallada de la transacción.
     * Proporciona contexto adicional sobre la naturaleza del movimiento.
     */
    private String descripcion;
}