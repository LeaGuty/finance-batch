package cl.duoc.finance_batch.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaConsumer {

    // Esta anotación le dice a Spring que escuche permanentemente este tópico
    @KafkaListener(topics = "auditoria-topic", groupId = "finance-group")
    public void consumirMensajeAuditoria(String mensaje) {
        System.out.println("\n=================================================");
        System.out.println("🚨 EVENTO KAFKA RECIBIDO EN FINANCE-BATCH 🚨");
        System.out.println("Detalle del evento: " + mensaje);
        System.out.println("=================================================\n");
    }
}