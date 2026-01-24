package cl.duoc.finance_batch.business;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Entidad JPA que representa una cuenta bancaria en el sistema.
 *
 * Esta clase se utiliza en el proceso batch de cálculo de intereses trimestrales,
 * donde se leen cuentas desde un archivo CSV, se procesan aplicando tasas de interés
 * según el tipo de cuenta, y se almacenan en la base de datos PostgreSQL.
 *
 * @author Finance Batch System
 * @version 1.0
 */
@Data
@Entity
@Table(name = "cuentas")
public class Cuenta {

    /**
     * Identificador único autoincremental generado por la base de datos.
     * Este ID es la clave primaria de la tabla y se genera automáticamente
     * para cada nuevo registro insertado.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identificador de cuenta proveniente del archivo CSV.
     * Este campo NO es único y puede repetirse, ya que una misma cuenta
     * puede tener múltiples registros de intereses calculados en diferentes períodos.
     */
    private Long cuentaId;

    /**
     * Nombre del titular de la cuenta.
     */
    private String nombre;

    /**
     * Saldo actualizado de la cuenta después de aplicar los intereses.
     * Este valor es el resultado de sumar el saldo original más el interés calculado.
     */
    private Double saldo;

    /**
     * Edad del titular de la cuenta en años.
     * Se utiliza para validaciones de datos (debe estar entre 0 y 110 años).
     */
    private Integer edad;

    /**
     * Tipo de cuenta bancaria.
     * Valores permitidos: "ahorro", "prestamo", "hipoteca".
     * Este campo determina la tasa de interés que se aplicará:
     * - ahorro: 1% de ganancia
     * - prestamo/hipoteca: 3% de interés
     */
    private String tipo;

    /**
     * Monto del interés calculado y aplicado a la cuenta.
     * Este valor se calcula en el procesador según el tipo de cuenta.
     */
    private Double interesAplicado;
}