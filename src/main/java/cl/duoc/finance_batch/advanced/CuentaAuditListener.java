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

import cl.duoc.finance_batch.business.Cuenta;
import cl.duoc.finance_batch.business.CuentaDTO;

/**
 * Listener de auditoría para el proceso batch de cálculo de intereses trimestrales.
 *
 * Este listener implementa dos interfaces de Spring Batch:
 * - StepExecutionListener: Para crear y cerrar el archivo CSV de auditoría
 * - ItemProcessListener: Para registrar cada item procesado o descartado
 *
 * Funcionalidad:
 * 1. Antes del step: Crea un archivo CSV de reporte con el ID del job
 * 2. Por cada item procesado: Registra si fue procesado exitosamente o descartado
 * 3. Después del step: Cierra el archivo CSV correctamente
 *
 * El archivo generado incluye:
 * - ID de la cuenta
 * - Tipo de cuenta
 * - Estado (PROCESADO/DESCARTADO)
 * - Detalle (saldo actualizado o razón del descarte)
 *
 * Nombre del archivo: reporte_intereses_{jobId}.csv
 *
 * @author Finance Batch System
 * @version 1.0
 * @see CuentaDTO
 * @see Cuenta
 */
public class CuentaAuditListener implements StepExecutionListener, ItemProcessListener<CuentaDTO, Cuenta> {

    private static final Logger log = LoggerFactory.getLogger(CuentaAuditListener.class);

    /**
     * Writer para escribir el archivo CSV de auditoría.
     */
    private BufferedWriter writer;

    /**
     * Se ejecuta antes del inicio del step.
     * Crea el archivo CSV de reporte con encabezados.
     *
     * @param stepExecution la ejecución del step actual
     */
    @Override
    public void beforeStep(StepExecution stepExecution) {
        long jobId = stepExecution.getJobExecutionId();
        String fileName = "reporte_intereses_" + jobId + ".csv";

        try {
            File file = new File(fileName);
            writer = new BufferedWriter(new FileWriter(file));
            writer.write("ID_CUENTA,TIPO,ESTADO,DETALLE");
            writer.newLine();
            log.info("============== REPORTE INTERESES CREADO EN: " + file.getAbsolutePath() + " ==============");
        } catch (IOException e) {
            log.error("Error creando reporte", e);
        }
    }

    /**
     * Se ejecuta después de procesar cada item.
     * Registra el resultado del procesamiento en el archivo CSV.
     *
     * @param item el DTO original leído del CSV
     * @param result la entidad procesada (null si fue descartada)
     */
    @Override
    public void afterProcess(CuentaDTO item, Cuenta result) {
        try {
            if (writer == null) return;

            String estado = (result != null) ? "PROCESADO" : "DESCARTADO";
            String detalle = (result != null)
                    ? "Saldo actualizado: " + result.getSaldo()
                    : "Datos inconsistentes (Edad/Tipo)";

            writer.write(String.format("%s,%s,%s,%s",
                    item.getCuentaId(), item.getTipo(), estado, detalle));
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
            log.error("Error cerrando reporte", e);
        }
        return null;
    }
}