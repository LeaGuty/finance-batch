package cl.duoc.finance_batch.business;
import lombok.AllArgsConstructor;
import lombok.Data;
@Data
@AllArgsConstructor
public class ResumenAnualDTO {
    private Long cuentaId;
    private Double totalMonto;
    private Long cantidadTransacciones;
    private String detalleAuditoria; // Ej: "Periodo 2025 - Revisado"
}