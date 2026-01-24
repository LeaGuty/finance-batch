package cl.duoc.finance_batch.items;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.core.io.ClassPathResource;

import cl.duoc.finance_batch.business.EstadoFinancieroDTO;

public class PartitionedEstadoFinancieroReader implements ItemReader<EstadoFinancieroDTO>, ItemStream {

    private final FlatFileItemReader<EstadoFinancieroDTO> delegate;
    private int start;
    private int end;
    private int currentLine = 0;

    public PartitionedEstadoFinancieroReader() {
        // Configuramos el lector base igual que siempre
        this.delegate = new FlatFileItemReaderBuilder<EstadoFinancieroDTO>()
                .name("anualItemReaderDelegate")
                .resource(new ClassPathResource("data/estados_financieros_anuales.csv"))
                .delimited()
                .names("cuenta_id", "fecha", "transaccion", "monto", "descripcion")
                .linesToSkip(1) // Saltamos cabecera
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(EstadoFinancieroDTO.class);
                }})
                .build();
    }

    @Override
    public EstadoFinancieroDTO read() throws Exception {
        // 1. Saltar líneas hasta llegar a mi zona (Start)
        // IMPORTANTE: currentLine es mi contador local relativo a los DATOS (sin contar header)
        if (currentLine < start) {
            while (currentLine < start) {
                // Leemos y descartamos para avanzar el cursor del archivo
                if (delegate.read() == null) {
                    return null; // Se acabó el archivo antes de llegar a mi parte
                }
                currentLine++;
            }
        }

        // 2. Leer solo si estoy dentro de mi rango (End)
        if (currentLine <= end) {
            EstadoFinancieroDTO item = delegate.read();
            if (item == null) return null; // Fin de archivo real
            
            currentLine++;
            return item;
        } else {
            return null; // Ya terminé mi cuota de líneas
        }
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        // Aquí recibimos los parámetros que nos manda el Particionador
        this.start = executionContext.getInt("start", 0);
        this.end = executionContext.getInt("end", Integer.MAX_VALUE);
        this.currentLine = 0;
        delegate.open(executionContext);
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        delegate.update(executionContext);
    }

    @Override
    public void close() throws ItemStreamException {
        delegate.close();
    }
}