package cl.duoc.finance_batch.repository;

import cl.duoc.finance_batch.business.EstadoFinanciero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repositorio JPA para la entidad EstadoFinanciero.
 *
 * Proporciona operaciones CRUD automáticas para la tabla 'estados_financieros' en la base de datos.
 * Este repositorio es utilizado por el Writer del job de estados financieros anuales
 * para persistir las transacciones procesadas desde múltiples archivos CSV.
 *
 * Spring Data JPA genera automáticamente la implementación de este repositorio,
 * proporcionando métodos como save(), findAll(), findById(), delete(), etc.
 *
 * Este repositorio se utiliza en un contexto de procesamiento paralelo con particionamiento,
 * donde múltiples hilos pueden estar escribiendo datos simultáneamente.
 *
 * @author Finance Batch System
 * @version 1.0
 * @see EstadoFinanciero
 */
@Repository
public interface EstadoFinancieroRepository extends JpaRepository<EstadoFinanciero, Long> {

    List<EstadoFinanciero> findByCuentaId(Long cuentaId);
}