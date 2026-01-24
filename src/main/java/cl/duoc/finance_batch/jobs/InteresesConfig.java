package cl.duoc.finance_batch.jobs;

import cl.duoc.finance_batch.business.Cuenta;
import cl.duoc.finance_batch.business.CuentaDTO;
import cl.duoc.finance_batch.items.CuentaItemProcessor;
import cl.duoc.finance_batch.advanced.CuentaAuditListener;
import cl.duoc.finance_batch.repository.CuentaRepository;
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
 * Configuración del job de Cálculo de Intereses Trimestrales.
 *
 * Este job implementa un proceso batch para calcular y aplicar intereses sobre
 * cuentas bancarias desde un archivo CSV a una base de datos PostgreSQL.
 *
 * Arquitectura del Job:
 * - Reader: Lee cuentas desde archivo CSV ubicado en resources/data/
 * - Processor: Valida datos, calcula intereses según tipo de cuenta, filtra inconsistencias
 * - Writer: Persiste cuentas con intereses aplicados en base de datos usando JPA
 * - Listener: Genera archivo CSV de auditoría con resumen de procesamiento
 *
 * Configuración de procesamiento:
 * - Chunk size: 10 registros por transacción
 * - Fault tolerance: Activada con skip de hasta 100 excepciones
 * - Skip policy: Salta cualquier Exception para continuar el procesamiento
 *
 * Reglas de negocio para intereses:
 * - Cuentas de ahorro: 1% de ganancia
 * - Cuentas de préstamo/hipoteca: 3% de interés
 * - Otros tipos: 0% (no se aplica interés)
 *
 * Validaciones aplicadas:
 * - Saldo vacío o nulo: registro descartado
 * - Tipo inválido (-1, unknown, vacío): registro descartado
 * - Edad fuera de rango (< 0 o > 110): registro descartado
 *
 * Estructura de datos:
 * - El campo cuenta_id puede repetirse (no es unique en BD)
 * - El id de tabla es autoincremental y único
 *
 * Nombre del job: calculoInteresesJob
 * Archivo CSV entrada: intereses_trimestrales.csv
 * Archivo CSV auditoría: reporte_intereses_{jobId}.csv
 *
 * @author Finance Batch System
 * @version 1.0
 */
@Configuration
public class InteresesConfig {

    /**
     * Configura el reader para leer cuentas desde CSV.
     *
     * Lee el archivo ubicado en src/main/resources/data/intereses_trimestrales.csv
     * y mapea cada línea a un objeto CuentaDTO.
     *
     * IMPORTANTE: El mapeo usa "cuentaId" (camelCase) para mapear correctamente
     * la columna "cuenta_id" del CSV al campo cuentaId del DTO.
     *
     * Estructura del CSV: cuenta_id,nombre,saldo,edad,tipo
     *
     * @return FlatFileItemReader configurado para CuentaDTO
     */
    @Bean
    public FlatFileItemReader<CuentaDTO> readerIntereses() {
        return new FlatFileItemReaderBuilder<CuentaDTO>()
                .name("interesItemReader")
                .resource(new ClassPathResource("data/intereses_trimestrales.csv"))
                .delimited()
                .names("cuentaId", "nombre", "saldo", "edad", "tipo") // Mapeo corregido a camelCase
                .linesToSkip(1) // Salta la línea de encabezados
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(CuentaDTO.class);
                }})
                .build();
    }

    /**
     * Configura el processor que valida y calcula intereses sobre CuentaDTO.
     *
     * El processor aplica reglas de negocio para calcular intereses según
     * el tipo de cuenta y realiza validaciones de integridad de datos.
     *
     * @return CuentaItemProcessor con lógica de cálculo de intereses
     */
    @Bean
    public CuentaItemProcessor processorIntereses() {
        return new CuentaItemProcessor();
    }

    /**
     * Configura el writer para persistir cuentas con intereses en la base de datos.
     *
     * Utiliza el repositorio JPA para guardar entidades Cuenta procesadas
     * con los intereses ya calculados y aplicados.
     *
     * @param repository el repositorio JPA de cuentas
     * @return RepositoryItemWriter configurado
     */
    @Bean
    public RepositoryItemWriter<Cuenta> writerIntereses(CuentaRepository repository) {
        return new RepositoryItemWriterBuilder<Cuenta>()
                .repository(repository)
                .methodName("save")
                .build();
    }

    /**
     * Configura el listener de auditoría para el job.
     *
     * Este listener genera un archivo CSV de auditoría con el resultado
     * del procesamiento de cada cuenta (PROCESADO/DESCARTADO) y el detalle
     * del saldo actualizado o la razón del descarte.
     *
     * @return CuentaAuditListener para auditoría del job
     */
    @Bean
    public CuentaAuditListener listenerIntereses() {
        return new CuentaAuditListener();
    }

    /**
     * Configura el step del job de cálculo de intereses.
     *
     * El step procesa cuentas en chunks de 10 registros, aplicando
     * la lógica de lectura, procesamiento (cálculo de intereses) y escritura.
     *
     * Configuración de tolerancia a fallos:
     * - Skip limit: 100 excepciones
     * - Skip policy: Cualquier Exception
     *
     * Listeners registrados:
     * - StepExecutionListener: Para crear/cerrar archivo de auditoría
     * - ItemProcessListener: Para registrar cada item procesado/descartado
     *
     * @param jobRepository repositorio de metadatos del job
     * @param transactionManager administrador de transacciones
     * @param repository repositorio de cuentas
     * @return Step configurado para procesar cuentas con intereses
     */
    @Bean
    public Step calculoInteresesStep(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager,
                                     CuentaRepository repository) {

        CuentaAuditListener listener = listenerIntereses();

        return new StepBuilder("calculoInteresesStep", jobRepository)
                .<CuentaDTO, Cuenta>chunk(10, transactionManager)
                .reader(readerIntereses())
                .processor(processorIntereses())
                .writer(writerIntereses(repository))
                .listener((StepExecutionListener) listener)
                .listener((ItemProcessListener<CuentaDTO, Cuenta>) listener)
                .faultTolerant()
                .skipLimit(100)
                .skip(Exception.class)
                .build();
    }

    /**
     * Configura el job principal de cálculo de intereses trimestrales.
     *
     * Este job puede ser ejecutado múltiples veces gracias al RunIdIncrementer,
     * que genera un nuevo ID de ejecución cada vez, permitiendo procesar
     * múltiples periodos trimestrales.
     *
     * @param jobRepository repositorio de metadatos del job
     * @param calculoInteresesStep el step configurado
     * @return Job configurado y listo para ejecutar
     */
    @Bean
    public Job calculoInteresesJob(JobRepository jobRepository, Step calculoInteresesStep) {
        return new JobBuilder("calculoInteresesJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(calculoInteresesStep)
                .build();
    }
}
