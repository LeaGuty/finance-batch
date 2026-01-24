package cl.duoc.finance_batch.advanced;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

import cl.duoc.finance_batch.business.EstadoFinanciero;
import cl.duoc.finance_batch.business.EstadoFinancieroDTO;

/**
 * Listener de auditoría para el proceso batch de estados financieros anuales.
 *
 * Este listener implementa dos interfaces de Spring Batch:
 * - StepExecutionListener: Para crear y cerrar el archivo CSV de auditoría
 * - ItemProcessListener: Para registrar cada item procesado o con error
 *
 * Este listener se utiliza en un job con particionamiento, donde múltiples
 * archivos CSV se procesan en paralelo. Cada partición puede generar su
 * propio archivo de auditoría.
 *
 * Funcionalidad:
 * 1. Antes del step: Crea un archivo CSV de reporte anual con el ID del job
 * 2. Por cada item procesado: Registra si fue cargado exitosamente o tuvo error
 * 3. Después del step: Cierra el archivo CSV correctamente
 *
 * El archivo generado incluye:
 * - ID de la cuenta
 * - Fecha de la transacción
 * - Estado (CARGADO/ERROR_DATOS)
 * - Descripción de la transacción
 *
 * Nombre del archivo: reporte_anual_{jobId}.csv
 *
 * @author Finance Batch System
 * @version 1.0
 * @see EstadoFinancieroDTO
 * @see EstadoFinanciero
 */
public class EstadoAuditListener implements StepExecutionListener, ItemProcessListener<EstadoFinancieroDTO, EstadoFinanciero> {

    private static final Logger log = LoggerFactory.getLogger(EstadoAuditListener.class);

    /**
     * Writer para escribir el archivo CSV de auditoría.
     */
    private BufferedWriter writer;

    /**
     * Se ejecuta antes del inicio del step.
     * Crea el archivo CSV de reporte anual con encabezados.
     *
     * @param stepExecution la ejecución del step actual
     */
    @Override
    public void beforeStep(StepExecution stepExecution) {
        long jobId = stepExecution.getJobExecutionId();
        String fileName = "reporte_anual_" + jobId + ".csv";
        try {
            File file = new File(fileName);
            writer = new BufferedWriter(new FileWriter(file));
            writer.write("CUENTA_ID,FECHA,ESTADO,DESCRIPCION");
            writer.newLine();
            log.info("============== REPORTE ANUAL CREADO EN: " + file.getAbsolutePath() + " ==============");
        } catch (IOException e) {
            log.error("Error creando reporte anual", e);
        }
    }

    /**
     * Se ejecuta después de procesar cada item.
     * Registra el resultado del procesamiento en el archivo CSV.
     *
     * @param item el DTO original leído del CSV
     * @param result la entidad procesada (null si hubo error en los datos)
     */
    @Override
    public void afterProcess(EstadoFinancieroDTO item, EstadoFinanciero result) {
        try {
            if (writer == null) return;

            // Si result es null, el procesador filtró el registro por datos inválidos
            String estado = (result != null) ? "CARGADO" : "ERROR_DATOS";

            writer.write(String.format("%s,%s,%s,%s",
                    item.getCuentaId(), item.getFecha(), estado, item.getDescripcion()));
            writer.newLine();
        } catch (IOException e) {
            log.error("Error escribiendo línea", e);
        }
    }

    /**
     * Se ejecuta después de completar el step.
     * Cierra el archivo CSV para guardar todos los cambios.
     *
     * @param stepExecution la ejecución del step actual
     * @return null (no modifica el estado de salida)
     */
    @Override
    public org.springframework.batch.core.ExitStatus afterStep(StepExecution stepExecution) {
        try {
            if (writer != null) writer.close();
        } catch (IOException e) {
            log.error("Error cerrando reporte anual", e);
        }
        return null;
    }
}