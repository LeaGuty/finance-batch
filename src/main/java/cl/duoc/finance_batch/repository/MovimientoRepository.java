package cl.duoc.finance_batch.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.duoc.finance_batch.business.Movimiento;

/**
 * Repositorio JPA para la entidad Movimiento.
 *
 * Proporciona operaciones CRUD automáticas para la tabla 'movimientos' en la base de datos.
 * Este repositorio es utilizado por el Writer del job de reporte diario de movimientos
 * para persistir los movimientos financieros procesados.
 *
 * Spring Data JPA genera automáticamente la implementación de este repositorio,
 * proporcionando métodos como save(), findAll(), findById(), delete(), etc.
 *
 * @author Finance Batch System
 * @version 1.0
 * @see Movimiento
 */
@Repository
public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

}