package cl.duoc.finance_batch.items;

import org.springframework.batch.item.ItemProcessor;

import cl.duoc.finance_batch.business.Cuenta;
import cl.duoc.finance_batch.business.CuentaDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Procesador de cuentas bancarias para el job de cálculo de intereses trimestrales.
 *
 * Este procesador es responsable de:
 * 1. Validar la integridad de los datos leídos del CSV
 * 2. Convertir tipos de datos de String a tipos nativos de Java
 * 3. Aplicar reglas de negocio para el cálculo de intereses
 * 4. Transformar el DTO en una entidad JPA lista para persistir
 *
 * Reglas de validación:
 * - Descarta registros con saldo vacío o null
 * - Descarta registros con tipo inválido (-1, unknown, vacío)
 * - Descarta registros con edades irreales (menores a 0 o mayores a 110 años)
 *
 * Reglas de negocio para intereses:
 * - Cuentas de ahorro: 1% de ganancia
 * - Cuentas de préstamo/hipoteca: 3% de interés
 * - Otros tipos de cuenta: 0% de interés
 *
 * @author Finance Batch System
 * @version 1.0
 * @see CuentaDTO
 * @see Cuenta
 */
public class CuentaItemProcessor implements ItemProcessor<CuentaDTO, Cuenta> {

    private static final Logger log = LoggerFactory.getLogger(CuentaItemProcessor.class);

    /**
     * Procesa un registro de cuenta desde el DTO hacia la entidad.
     *
     * @param item el DTO con los datos leídos del CSV
     * @return la entidad Cuenta procesada, o null si el registro debe ser descartado
     * @throws Exception si ocurre un error durante el procesamiento
     */
    @Override
    public Cuenta process(CuentaDTO item) throws Exception {
        // 1. Validaciones de Integridad
        if (item.getSaldo() == null || item.getSaldo().isEmpty()) {
            return null; // Descarta registros sin saldo
        }

        // Normaliza el tipo a minúsculas para facilitar comparaciones
        String tipoRaw = (item.getTipo() != null) ? item.getTipo().toLowerCase() : "";

        // Filtra tipos inválidos (-1, unknown, vacíos)
        if (tipoRaw.equals("-1") || tipoRaw.contains("unknown") || tipoRaw.isEmpty()) {
            log.warn("Cuenta descartada (Tipo inválido): ID {}", item.getCuentaId());
            return null;
        }

        // 2. Conversión de Tipos
        Long cuentaId = Long.parseLong(item.getCuentaId());
        Double saldoOriginal = Double.parseDouble(item.getSaldo());
        Integer edad = (item.getEdad() != null && !item.getEdad().isEmpty())
                        ? Integer.parseInt(item.getEdad()) : 0;

        // Valida que la edad esté en un rango realista (0-110 años)
        if (edad > 110 || edad < 0) {
             log.warn("Cuenta descartada (Edad irreal): cuenta_id {} Edad: {}", cuentaId, edad);
             return null;
        }

        // 3. Cálculo de Intereses (Lógica de Negocio)
        Double interes = 0.0;

        if (tipoRaw.contains("ahorro")) {
            interes = saldoOriginal * 0.01; // 1% de ganancia para cuentas de ahorro
        } else if (tipoRaw.contains("prestamo") || tipoRaw.contains("hipoteca")) {
            interes = saldoOriginal * 0.03; // 3% de interés para préstamos e hipotecas
        }

        Double nuevoSaldo = saldoOriginal + interes;

        // 4. Mapeo final a la entidad JPA
        Cuenta cuenta = new Cuenta();
        cuenta.setCuentaId(cuentaId); // ID del CSV (puede repetirse)
        cuenta.setNombre(item.getNombre());
        cuenta.setEdad(edad);
        cuenta.setTipo(tipoRaw);
        cuenta.setSaldo(nuevoSaldo);
        cuenta.setInteresAplicado(interes);

        return cuenta;
    }
}