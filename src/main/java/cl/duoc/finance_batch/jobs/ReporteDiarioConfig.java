package cl.duoc.finance_batch.jobs;

import cl.duoc.finance_batch.business.Movimiento;
import cl.duoc.finance_batch.business.MovimientoDTO;
import cl.duoc.finance_batch.items.MovimientoItemProcessor;
import cl.duoc.finance_batch.advanced.MovimientoAuditListener;
import cl.duoc.finance_batch.repository.MovimientoRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Configuración del job de Reporte Diario de Movimientos Financieros.
 *
 * Este job implementa un proceso batch completo para leer, procesar y almacenar
 * movimientos financieros diarios desde un archivo CSV a una base de datos PostgreSQL.
 *
 * Arquitectura del Job:
 * - Reader: Lee movimientos desde archivo CSV ubicado en resources/data/
 * - Processor: Valida datos, parsea fechas múltiples formatos, filtra anomalías
 * - Writer: Persiste movimientos válidos en base de datos usando JPA
 * - Listener: Genera archivo CSV de auditoría con resumen de carga
 *
 * Configuración de procesamiento:
 * - Chunk size: 10 registros por transacción
 * - Fault tolerance: Activada con skip de hasta 100 excepciones
 * - Skip policy: Salta cualquier Exception para continuar el procesamiento
 *
 * Validaciones aplicadas:
 * - Montos vacíos, nulos, no numéricos o <= 0 son descartados
 * - Fechas inválidas o en formato incorrecto son descartadas
 * - Soporta múltiples formatos de fecha (yyyy/MM/dd, dd-MM-yyyy, etc.)
 *
 * Nombre del job: reporteDiarioJob
 * Archivo CSV entrada: movimientos_financieros_diarios.csv
 * Archivo CSV auditoría: resumen_carga_{jobId}.csv
 *
 * @author Finance Batch System
 * @version 1.0
 */
@Configuration
public class ReporteDiarioConfig {

    /**
     * Configura el reader para leer movimientos financieros desde CSV.
     *
     * Lee el archivo ubicado en src/main/resources/data/movimientos_financieros_diarios.csv
     * y mapea cada línea a un objeto MovimientoDTO.
     *
     * Estructura del CSV: id,fecha,monto,tipo
     *
     * @return FlatFileItemReader configurado para MovimientoDTO
     */
    @Bean
    public FlatFileItemReader<MovimientoDTO> readerMovimientos() {
        return new FlatFileItemReaderBuilder<MovimientoDTO>()
                .name("movimientoItemReader")
                .resource(new ClassPathResource("data/movimientos_financieros_diarios.csv"))
                .delimited()
                .names("id", "fecha", "monto", "tipo")
                .linesToSkip(1) // Salta la línea de encabezados
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(MovimientoDTO.class);
                }})
                .build();
    }

    /**
     * Configura el processor que valida y transforma MovimientoDTO a Movimiento.
     *
     * @return MovimientoItemProcessor con lógica de validación y transformación
     */
    @Bean
    public MovimientoItemProcessor processorMovimientos() {
        return new MovimientoItemProcessor();
    }

    /**
     * Configura el writer para persistir movimientos en la base de datos.
     *
     * Utiliza el repositorio JPA para guardar entidades Movimiento validadas
     * y transformadas por el processor.
     *
     * @param repository el repositorio JPA de movimientos
     * @return RepositoryItemWriter configurado
     */
    @Bean
    public RepositoryItemWriter<Movimiento> writerMovimientos(MovimientoRepository repository) {
        return new RepositoryItemWriterBuilder<Movimiento>()
                .repository(repository)
                .methodName("save")
                .build();
    }

    /**
     * Configura el listener de auditoría para el job.
     *
     * Este listener genera un archivo CSV de auditoría con el resultado
     * del procesamiento de cada movimiento (CARGADO/RECHAZADO).
     *
     * @return MovimientoAuditListener para auditoría del job
     */
    @Bean
    public MovimientoAuditListener listenerMovimientos() {
        return new MovimientoAuditListener();
    }

    /**
     * Configura el step del job de reporte diario.
     *
     * El step procesa movimientos en chunks de 10 registros, aplicando
     * la lógica de lectura, procesamiento y escritura.
     *
     * Configuración de tolerancia a fallos:
     * - Skip limit: 100 excepciones
     * - Skip policy: Cualquier Exception
     *
     * Listeners registrados:
     * - StepExecutionListener: Para crear/cerrar archivo de auditoría
     * - ItemProcessListener: Para registrar cada item procesado
     *
     * @param jobRepository repositorio de metadatos del job
     * @param transactionManager administrador de transacciones
     * @param repository repositorio de movimientos
     * @return Step configurado para procesar movimientos
     */
    @Bean
    public Step reporteDiarioStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  MovimientoRepository repository) {

        MovimientoAuditListener listener = listenerMovimientos();

        return new StepBuilder("reporteDiarioStep", jobRepository)
                .<MovimientoDTO, Movimiento>chunk(10, transactionManager)
                .reader(readerMovimientos())
                .processor(processorMovimientos())
                .writer(writerMovimientos(repository))
                .listener((StepExecutionListener) listener)
                .listener((ItemProcessListener<MovimientoDTO, Movimiento>) listener)
                .faultTolerant()
                .skipLimit(100)
                .skip(Exception.class)
                .build();
    }

    /**
     * Configura el job principal de reporte diario.
     *
     * Este job puede ser ejecutado múltiples veces gracias al RunIdIncrementer,
     * que genera un nuevo ID de ejecución cada vez.
     *
     * @param jobRepository repositorio de metadatos del job
     * @param reporteDiarioStep el step configurado
     * @return Job configurado y listo para ejecutar
     */
    @Bean
    public Job reporteDiarioJob(JobRepository jobRepository, Step reporteDiarioStep) {
        return new JobBuilder("reporteDiarioJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(reporteDiarioStep)
                .build();
    }
}
