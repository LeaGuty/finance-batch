package cl.duoc.finance_batch.jobs;

import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import cl.duoc.finance_batch.advanced.EstadoAuditListener;
import cl.duoc.finance_batch.business.EstadoFinanciero;
import cl.duoc.finance_batch.business.EstadoFinancieroDTO;
import cl.duoc.finance_batch.business.ResumenAnualDTO;
import cl.duoc.finance_batch.items.EstadoFinancieroProcessor;
import cl.duoc.finance_batch.items.PartitionedEstadoFinancieroReader;
import cl.duoc.finance_batch.repository.EstadoFinancieroRepository;

@Configuration
public class EstadosAnualesConfig {

    // 1. READER PARTICIONADO (Debe ser @StepScope para recibir parámetros únicos por hilo)
    @Bean
    @StepScope
    public PartitionedEstadoFinancieroReader readerAnualPartitioned() {
        return new PartitionedEstadoFinancieroReader();
    }

    // 2. PROCESSOR (Reutilizamos el mismo)
    @Bean
    public EstadoFinancieroProcessor processorAnual() {
        return new EstadoFinancieroProcessor();
    }

    // 3. WRITER (Reutilizamos el mismo)
    @Bean
    public RepositoryItemWriter<EstadoFinanciero> writerAnual(EstadoFinancieroRepository repository) {
        return new RepositoryItemWriterBuilder<EstadoFinanciero>()
                .repository(repository)
                .methodName("save")
                .build();
    }

    @Bean
    public EstadoAuditListener listenerAnual() {
        return new EstadoAuditListener();
    }

    // 4. EL PARTICIONADOR (Calcula los rangos start-end)
    @Bean
    public Partitioner partitionerAnual() {
        return gridSize -> {
            Map<String, ExecutionContext> partitions = new HashMap<>();
            int totalData = 0;
            
            // 1. Contar las líneas del archivo REAL
            try {
                ClassPathResource resource = new ClassPathResource("data/estados_financieros_anuales.csv");
                try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(resource.getInputStream()))) {
                    while (reader.readLine() != null) {
                        // Solo avanzamos para contar
                    }
                    // Restamos 1 porque la primera línea es el encabezado
                    totalData = reader.getLineNumber() - 1; 
                }
            } catch (Exception e) {
                throw new RuntimeException("Error al contar líneas del archivo para particionar", e);
            }

            // Si el archivo está vacío o solo tiene cabecera
            if (totalData <= 0) return new HashMap<>();

            // 2. Calcular particiones dinámicamente
            int partitionSize = (int) Math.ceil((double) totalData / gridSize);
            int start = 0;

            for (int i = 0; i < gridSize; i++) {
                ExecutionContext context = new ExecutionContext();
                int end = Math.min(start + partitionSize - 1, totalData - 1); 

                context.putInt("start", start);
                context.putInt("end", end);
                context.putString("partitionName", "partition-" + i);
                
                partitions.put("partition" + i, context);
                
                System.out.println(">>> Configurando Partición " + i + ": Start=" + start + " End=" + end);
                
                start += partitionSize;
                if (start >= totalData) break;
            }
            return partitions;
        };
    }

    // 5. WORKER STEP (El "Esclavo" que hace el trabajo real)
    @Bean
    public Step workerStep(JobRepository jobRepository,
                           PlatformTransactionManager transactionManager,
                           EstadoFinancieroRepository repository) {
        
        EstadoAuditListener listener = listenerAnual();
        
        return new StepBuilder("workerStep", jobRepository)
                .<EstadoFinancieroDTO, EstadoFinanciero>chunk(10, transactionManager)
                .reader(readerAnualPartitioned()) // Usa nuestro lector custom
                .processor(processorAnual())
                .writer(writerAnual(repository))
                .listener((StepExecutionListener) listener)
                .listener((ItemProcessListener<EstadoFinancieroDTO, EstadoFinanciero>) listener)
                .build();
    }

    // 6. PARTITION HANDLER (El "Capataz" que asigna hilos a los workers)
    @Bean
    public TaskExecutorPartitionHandler partitionHandler(Step workerStep) {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        handler.setGridSize(4); // Queremos 4 particiones
        handler.setTaskExecutor(taskExecutorPartitioned());
        handler.setStep(workerStep);
        return handler;
    }

    @Bean
    public TaskExecutor taskExecutorPartitioned() {
        // Usamos ThreadPool como en el ejemplo para mejor control
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("partition-thread-");
        executor.setConcurrencyLimit(4);
        return executor;
    }

    // 7. MASTER STEP (El paso principal que contiene la lógica de partición)
    @Bean
    public Step masterStep(JobRepository jobRepository,
                           TaskExecutorPartitionHandler partitionHandler,
                           Partitioner partitionerAnual) {
        return new StepBuilder("masterStep", jobRepository)
                .partitioner("workerStep", partitionerAnual) // Une el Partitioner con el Worker
                .partitionHandler(partitionHandler)
                .build();
    }

 
    // 8. READER para el Informe: Agrupa datos por cuenta desde la DB
    @Bean
    public JdbcCursorItemReader<ResumenAnualDTO> readerInforme(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<ResumenAnualDTO>()
                .name("readerInforme")
                .dataSource(dataSource)
                .sql("SELECT cuenta_id, SUM(monto) as total, COUNT(*) as cantidad " +
                     "FROM estados_financieros GROUP BY cuenta_id")
                .rowMapper((rs, rowNum) -> new ResumenAnualDTO(
                        rs.getLong("cuenta_id"),
                        rs.getDouble("total"),
                        rs.getLong("cantidad"),
                        "Informe Anual de Auditoría - Generado exitosamente"
                ))
                .build();
    }

    // 9. WRITER para el Informe: Genera el archivo físico
    @Bean
    public FlatFileItemWriter<ResumenAnualDTO> writerInforme() {
        return new FlatFileItemWriterBuilder<ResumenAnualDTO>()
                .name("writerInforme")
                .resource(new FileSystemResource("outputs/reporte_auditoria_anual.csv"))
                .delimited()
                .delimiter(",")
                .names("cuentaId", "totalMonto", "cantidadTransacciones", "detalleAuditoria")
                .headerCallback(writer -> writer.write("ID_CUENTA,TOTAL_ANUAL,TOTAL_TXS,ESTADO_AUDITORIA"))
                .build();
    }

    // 10. STEP de Generación de Informe
    @Bean
    public Step generarReporteStep(JobRepository jobRepository, 
                                   PlatformTransactionManager transactionManager,
                                   JdbcCursorItemReader<ResumenAnualDTO> readerInforme,
                                   FlatFileItemWriter<ResumenAnualDTO> writerInforme) {
        return new StepBuilder("generarReporteStep", jobRepository)
                .<ResumenAnualDTO, ResumenAnualDTO>chunk(100, transactionManager)
                .reader(readerInforme)
                .writer(writerInforme)
                .build();
    }

    // 11. JOB FINAL ACTUALIZADO
    @Bean
    public Job estadosAnualesJob(JobRepository jobRepository, 
                                 Step masterStep, 
                                 Step generarReporteStep) {
        return new JobBuilder("estadosAnualesJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(masterStep)          // Primero carga y procesa los CSV en paralelo
                .next(generarReporteStep)   // Luego compila el informe de auditoría
                .build();
    }
}