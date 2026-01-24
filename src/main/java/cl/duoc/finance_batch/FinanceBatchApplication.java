package cl.duoc.finance_batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal de la aplicación Finance Batch.
 *
 * Esta aplicación de Spring Boot ejecuta procesos batch automatizados para
 * procesamiento financiero utilizando Spring Batch Framework.
 *
 * La aplicación incluye tres jobs principales:
 * 1. reporteDiarioJob: Procesa movimientos financieros diarios desde CSV
 * 2. calculoInteresesJob: Calcula intereses trimestrales sobre cuentas bancarias
 * 3. estadosAnualesJob: Procesa estados financieros anuales usando particionamiento paralelo
 *
 * Cada job puede ser ejecutado individualmente configurando la propiedad
 * spring.batch.job.name en application.properties
 *
 * Características técnicas:
 * - Procesamiento por chunks para optimización de memoria
 * - Validaciones y transformaciones de datos
 * - Tolerancia a fallos con skip de registros inválidos
 * - Generación de reportes CSV de auditoría
 * - Procesamiento paralelo con particionamiento
 *
 * Base de datos soportada: PostgreSQL
 *
 * @author Finance Batch System
 * @version 1.0
 */
@SpringBootApplication
public class FinanceBatchApplication {

	/**
	 * Punto de entrada de la aplicación.
	 * Inicia el contexto de Spring Boot y ejecuta el job configurado.
	 *
	 * @param args argumentos de línea de comandos
	 */
	public static void main(String[] args) {
		SpringApplication.run(FinanceBatchApplication.class, args);
	}
}
