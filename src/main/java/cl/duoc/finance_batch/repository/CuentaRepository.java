package cl.duoc.finance_batch.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.duoc.finance_batch.business.Cuenta;

/**
 * Repositorio JPA para la entidad Cuenta.
 *
 * Proporciona operaciones CRUD automáticas para la tabla 'cuentas' en la base de datos.
 * Este repositorio es utilizado por el Writer del job de cálculo de intereses trimestrales
 * para persistir las cuentas procesadas.
 *
 * Spring Data JPA genera automáticamente la implementación de este repositorio,
 * proporcionando métodos como save(), findAll(), findById(), delete(), etc.
 *
 * @author Finance Batch System
 * @version 1.0
 * @see Cuenta
 */
@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Long> {
}