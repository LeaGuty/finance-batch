package cl.duoc.finance_batch.advanced;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

import cl.duoc.finance_batch.business.Movimiento;
import cl.duoc.finance_batch.business.MovimientoDTO;

/**
 * Listener de auditoría para el proceso batch de reporte diario de movimientos.
 *
 * Este listener implementa dos interfaces de Spring Batch:
 * - StepExecutionListener: Para crear y cerrar el archivo CSV de auditoría
 * - ItemProcessListener: Para registrar cada item procesado o rechazado
 *
 * Funcionalidad:
 * 1. Antes del step: Crea un archivo CSV de resumen de carga con el ID del job
 * 2. Por cada item procesado: Registra si fue cargado exitosamente o rechazado
 * 3. Después del step: Cierra el archivo CSV correctamente
 *
 * El archivo generado incluye:
 * - ID original del movimiento
 * - Fecha del movimiento (del CSV)
 * - Estado (CARGADO/RECHAZADO)
 * - Detalle (razón del rechazo o confirmación de procesamiento)
 *
 * Nombre del archivo: resumen_carga_{jobId}.csv
 *
 * @author Finance Batch System
 * @version 1.0
 * @see MovimientoDTO
 * @see Movimiento
 */
public class MovimientoAuditListener implements StepExecutionListener, ItemProcessListener<MovimientoDTO, Movimiento> {

    private static final Logger log = LoggerFactory.getLogger(MovimientoAuditListener.class);

    /**
     * Writer para escribir el archivo CSV de auditoría.
     */
    private BufferedWriter writer;

    /**
     * Se ejecuta antes del inicio del step.
     * Crea el archivo CSV de resumen de carga con encabezados y
     * registra la ruta absoluta en los logs.
     *
     * @param stepExecution la ejecución del step actual
     */
    @Override
    public void beforeStep(StepExecution stepExecution) {
        long jobId = stepExecution.getJobExecutionId();
        String fileName = "resumen_carga_" + jobId + ".csv";

        try {
            java.io.File file = new java.io.File(fileName);
            writer = new BufferedWriter(new FileWriter(file));
            writer.write("ID_ORIGINAL,FECHA_CSV,ESTADO,DETALLE");
            writer.newLine();

            log.info("============== ARCHIVO DE AUDITORÍA CREADO EN: " + file.getAbsolutePath() + " ==============");

        } catch (IOException e) {
            log.error("No se pudo crear el archivo de auditoría", e);
        }
    }

    /**
     * Se ejecuta después de procesar cada item.
     * Registra el resultado del procesamiento en el archivo CSV.
     *
     * Si result es null, significa que el procesador filtró/rechazó el registro
     * debido a datos inválidos o inconsistentes.
     *
     * @param item el DTO original leído del CSV
     * @param result la entidad procesada (null si fue rechazada)
     */
    @Override
    public void afterProcess(MovimientoDTO item, Movimiento result) {
        try {
            if (writer == null) return;

            String estado;
            String detalle;

            if (result == null) {
                // El procesador filtró el registro por datos inválidos
                estado = "RECHAZADO";
                detalle = "Datos inválidos o inconsistentes (Monto negativo/Fecha errónea)";
            } else {
                // El registro pasó todas las validaciones
                estado = "CARGADO";
                detalle = "Procesado correctamente";
            }

            String linea = String.format("%s,%s,%s,%s",
                    item.getId(),
                    item.getFecha(),
                    estado,
                    detalle);

            writer.write(linea);
            writer.newLine();

        } catch (IOException e) {
            log.error("Error escribiendo en auditoría", e);
        }
    }

    /**
     * Se ejecuta después de completar el step.
     * Cierra el archivo CSV para guardar todos los cambios.
     *
     * @param stepExecution la ejecución del step actual
     * @return null (no modifica el estado de salida del step)
     */
    @Override
    public org.springframework.batch.core.ExitStatus afterStep(StepExecution stepExecution) {
        try {
            if (writer != null) {
                writer.close();
                log.info("Archivo de auditoría cerrado correctamente.");
            }
        } catch (IOException e) {
            log.error("Error cerrando archivo de auditoría", e);
        }
        return null;
    }

    /**
     * Método del ciclo de vida que se ejecuta antes de procesar cada item.
     * No se utiliza en esta implementación.
     *
     * @param item el item que se va a procesar
     */
    @Override
    public void beforeProcess(MovimientoDTO item) {}

    /**
     * Método del ciclo de vida que se ejecuta cuando ocurre un error al procesar un item.
     * No se utiliza en esta implementación.
     *
     * @param item el item que causó el error
     * @param e la excepción lanzada
     */
    @Override
    public void onProcessError(MovimientoDTO item, Exception e) {}
}