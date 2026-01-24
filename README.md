# Finance Batch - Sistema de Procesamiento Batch Financiero

Sistema de procesamiento batch para operaciones financieras utilizando Spring Batch Framework. Implementa tres jobs principales para el procesamiento automatizado de movimientos financieros, cálculo de intereses y generación de estados financieros.

## Características Principales

- **Procesamiento por Chunks**: Optimización de memoria mediante procesamiento en lotes
- **Validación y Transformación de Datos**: Reglas de negocio aplicadas automáticamente
- **Tolerancia a Fallos**: Skip de registros inválidos sin detener el proceso
- **Auditoría Automatizada**: Generación de reportes CSV con resultados del procesamiento
- **Procesamiento Paralelo**: Particionamiento para mejorar el rendimiento
- **Soporte Multi-formato**: Parseo de fechas en múltiples formatos
- **Persistencia JPA**: Almacenamiento en PostgreSQL

## Tecnologías Utilizadas

- **Java 17+**
- **Spring Boot 3.x**
- **Spring Batch 5.x**
- **Spring Data JPA**
- **PostgreSQL**
- **Lombok**
- **Maven**

## Arquitectura del Sistema

```
finance-batch/
├── src/main/java/cl/duoc/finance_batch/
│   ├── business/              # Entidades JPA y DTOs
│   │   ├── Cuenta.java
│   │   ├── CuentaDTO.java
│   │   ├── Movimiento.java
│   │   ├── MovimientoDTO.java
│   │   ├── EstadoFinanciero.java
│   │   └── EstadoFinancieroDTO.java
│   ├── items/                 # Procesadores y Readers
│   │   ├── CuentaItemProcessor.java
│   │   ├── MovimientoItemProcessor.java
│   │   ├── EstadoFinancieroProcessor.java
│   │   └── PartitionedEstadoFinancieroReader.java
│   ├── advanced/              # Listeners de auditoría
│   │   ├── CuentaAuditListener.java
│   │   ├── MovimientoAuditListener.java
│   │   └── EstadoAuditListener.java
│   ├── repository/            # Repositorios JPA
│   │   ├── CuentaRepository.java
│   │   ├── MovimientoRepository.java
│   │   └── EstadoFinancieroRepository.java
│   ├── jobs/                  # Configuraciones de Jobs
│   │   ├── ReporteDiarioConfig.java
│   │   ├── InteresesConfig.java
│   │   └── EstadosAnualesConfig.java
│   └── FinanceBatchApplication.java
└── src/main/resources/
    ├── data/                  # Archivos CSV de entrada
    │   ├── movimientos_financieros_diarios.csv
    │   ├── intereses_trimestrales.csv
    │   └── estados_financieros_anuales.csv
    └── application.properties
```

## Jobs Disponibles

### 1. Reporte Diario de Movimientos (reporteDiarioJob)

Procesa movimientos financieros diarios desde archivo CSV.

**Características:**
- **Archivo entrada**: `movimientos_financieros_diarios.csv`
- **Estructura CSV**: `id,fecha,monto,tipo`
- **Chunk size**: 10 registros
- **Validaciones**:
  - Descarta montos vacíos, nulos, no numéricos o <= 0
  - Descarta fechas inválidas o mal formateadas
  - Soporta múltiples formatos de fecha (yyyy/MM/dd, dd-MM-yyyy, yyyy-MM-dd, dd/MM/yyyy)
- **Salida**: Tabla `movimientos` en PostgreSQL
- **Auditoría**: `resumen_carga_{jobId}.csv`

**Reglas de negocio:**
- Los montos negativos son descartados automáticamente
- Las fechas se parsean intentando múltiples formatos
- Se agrega timestamp de procesamiento para auditoría

### 2. Cálculo de Intereses Trimestrales (calculoInteresesJob)

Calcula y aplica intereses sobre cuentas bancarias según su tipo.

**Características:**
- **Archivo entrada**: `intereses_trimestrales.csv`
- **Estructura CSV**: `cuenta_id,nombre,saldo,edad,tipo`
- **Chunk size**: 10 registros
- **Validaciones**:
  - Descarta registros con saldo vacío o nulo
  - Descarta tipos inválidos (-1, unknown, vacío)
  - Descarta edades irreales (< 0 o > 110 años)
- **Salida**: Tabla `cuentas` en PostgreSQL
- **Auditoría**: `reporte_intereses_{jobId}.csv`

**Reglas de negocio - Tasas de interés:**
| Tipo de Cuenta | Tasa | Aplicación |
|----------------|------|------------|
| Ahorro | 1% | Ganancia sobre saldo |
| Préstamo | 3% | Interés sobre saldo |
| Hipoteca | 3% | Interés sobre saldo |
| Otros | 0% | Sin interés |

**Estructura de datos:**
- `cuenta_id`: Puede repetirse (no es único en BD)
- `id`: Autoincremental y único (clave primaria)

### 3. Estados Financieros Anuales (estadosAnualesJob)

Procesa estados financieros anuales utilizando **particionamiento paralelo** para mejorar el rendimiento.

**Características:**
- **Archivo entrada**: `estados_financieros_anuales.csv`
- **Estructura CSV**: `cuenta_id,fecha,transaccion,monto,descripcion`
- **Chunk size**: 10 registros por partición
- **Particiones**: 4 (procesamiento paralelo)
- **Hilos concurrentes**: 4
- **Validaciones**:
  - Descarta registros con monto vacío, nulo o no numérico
  - Descarta fechas inválidas
  - Soporta múltiples formatos de fecha
- **Salida**: Tabla `estados_financieros` en PostgreSQL
- **Auditoría**: `reporte_anual_{jobId}.csv`

**Arquitectura de particionamiento:**
```
Master Step
    ├── Partición 0 (Worker Thread 1) → Registros 0-249
    ├── Partición 1 (Worker Thread 2) → Registros 250-499
    ├── Partición 2 (Worker Thread 3) → Registros 500-749
    └── Partición 3 (Worker Thread 4) → Registros 750-999
```

## Configuración

### Base de Datos (application.properties)

```properties
# Conexión PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/finance_db
spring.datasource.username=postgres
spring.datasource.password=tu_password

# Configuración JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Spring Batch
spring.batch.jdbc.initialize-schema=always

# Job a ejecutar (descomentar el deseado)
spring.batch.job.name=calculoInteresesJob
# spring.batch.job.name=reporteDiarioJob
# spring.batch.job.name=estadosAnualesJob
```

### Esquema de Base de Datos

```sql
-- Tabla de movimientos
CREATE TABLE movimientos (
    id BIGINT PRIMARY KEY,
    fecha DATE NOT NULL,
    monto DOUBLE PRECISION NOT NULL,
    tipo VARCHAR(50),
    fecha_proceso VARCHAR(50)
);

-- Tabla de cuentas
CREATE TABLE cuentas (
    id BIGSERIAL PRIMARY KEY,
    cuenta_id BIGINT,
    nombre VARCHAR(100),
    saldo DOUBLE PRECISION,
    edad INTEGER,
    tipo VARCHAR(50),
    interes_aplicado DOUBLE PRECISION
);

-- Tabla de estados financieros
CREATE TABLE estados_financieros (
    id BIGSERIAL PRIMARY KEY,
    cuenta_id BIGINT,
    fecha DATE,
    transaccion VARCHAR(50),
    monto DOUBLE PRECISION,
    descripcion VARCHAR(255)
);
```

## Instalación y Ejecución

### Requisitos Previos

- Java 17 o superior
- Maven 3.6+
- PostgreSQL 12+

### Pasos de Instalación

1. **Clonar el repositorio**
```bash
git clone <url-del-repositorio>
cd finance-batch
```

2. **Configurar la base de datos**
```sql
CREATE DATABASE finance_db;
```

3. **Actualizar credenciales en `application.properties`**

4. **Compilar el proyecto**
```bash
mvn clean install
```

5. **Preparar archivos CSV**

   Colocar los archivos CSV en `src/main/resources/data/`:
   - `movimientos_financieros_diarios.csv`
   - `intereses_trimestrales.csv`
   - `estados_financieros_anuales.csv`

6. **Ejecutar el job deseado**

   Editar `application.properties` para seleccionar el job:
   ```properties
   spring.batch.job.name=calculoInteresesJob
   ```

7. **Iniciar la aplicación**
```bash
mvn spring-boot:run
```

## Formato de Archivos CSV

### movimientos_financieros_diarios.csv
```csv
id,fecha,monto,tipo
1,2024/01/15,1500.50,ingreso
2,15-01-2024,2300.00,egreso
```

### intereses_trimestrales.csv
```csv
cuenta_id,nombre,saldo,edad,tipo
101,Juan Perez,5000.00,35,ahorro
102,Maria Lopez,10000.00,42,prestamo
```

### estados_financieros_anuales.csv
```csv
cuenta_id,fecha,transaccion,monto,descripcion
201,2024-01-15,compra,450.00,Supermercado
202,2024-01-16,deposito,1000.00,Salario
```

## Archivos de Auditoría Generados

Cada job genera un archivo CSV de auditoría en el directorio raíz del proyecto:

### resumen_carga_{jobId}.csv (Movimientos Diarios)
```csv
ID_ORIGINAL,FECHA_CSV,ESTADO,DETALLE
1,2024/01/15,CARGADO,Procesado correctamente
2,2024/13/01,RECHAZADO,Datos inválidos o inconsistentes
```

### reporte_intereses_{jobId}.csv (Cálculo de Intereses)
```csv
ID_CUENTA,TIPO,ESTADO,DETALLE
101,ahorro,PROCESADO,Saldo actualizado: 5050.00
102,unknown,DESCARTADO,Datos inconsistentes (Tipo)
```

### reporte_anual_{jobId}.csv (Estados Financieros)
```csv
CUENTA_ID,FECHA,ESTADO,DESCRIPCION
201,2024-01-15,CARGADO,Compra en supermercado
202,2024/13/45,ERROR_DATOS,Fecha inválida
```

## Características Técnicas Avanzadas

### Tolerancia a Fallos

Todos los jobs están configurados con tolerancia a fallos:
- **Skip Limit**: 100 excepciones por job
- **Skip Policy**: Cualquier `Exception`
- Los registros problemáticos se saltan sin detener el procesamiento
- Los registros saltados se registran en el archivo de auditoría

### Procesamiento por Chunks

- **Tamaño del chunk**: 10 registros
- **Transaccionalidad**: Cada chunk es una transacción independiente
- **Ventajas**:
  - Optimización de memoria
  - Rollback parcial en caso de errores
  - Mejor rendimiento en grandes volúmenes

### Particionamiento Paralelo (Solo Estados Anuales)

- **Grid Size**: 4 particiones
- **Concurrency Limit**: 4 hilos simultáneos
- **Particionador**: Dinámico basado en tamaño del archivo
- **Ventajas**:
  - Reducción significativa del tiempo de procesamiento
  - Mejor utilización de recursos del servidor
  - Escalabilidad horizontal

### Validaciones Multi-capa

1. **Nivel Reader**: Validación de estructura CSV
2. **Nivel Processor**: Validación de datos y reglas de negocio
3. **Nivel Writer**: Validación de integridad en base de datos

## Monitoreo y Logging

La aplicación utiliza SLF4J para logging. Los logs incluyen:

- Inicio y fin de cada job
- Conteo de registros procesados/saltados/descartados
- Ubicación de archivos de auditoría generados
- Errores y advertencias durante el procesamiento

Ejemplo de log:
```
2024-01-24 INFO  - ============== ARCHIVO DE AUDITORÍA CREADO EN: C:\...\resumen_carga_1.csv ==============
2024-01-24 WARN  - Registro descartado (Monto negativo): ID 123
2024-01-24 INFO  - Convirtiendo (MovimientoDTO(...)) a (Movimiento(...))
2024-01-24 INFO  - Archivo de auditoría cerrado correctamente.
```

## Solución de Problemas Comunes

### Error: "el valor nulo en la columna «id» viola la restricción de no nulo"

**Causa**: La tabla no está configurada correctamente para IDs autoincrementales.

**Solución**:
1. Cambiar `spring.jpa.hibernate.ddl-auto=create-drop` temporalmente
2. Ejecutar el job una vez
3. Volver a `spring.jpa.hibernate.ddl-auto=update`

### Error: Valores cruzados entre CSV y BD

**Causa**: Desajuste entre nombres de columnas CSV y propiedades del DTO.

**Solución**: Verificar que el mapeo en los readers use camelCase:
```java
.names("cuentaId", "nombre", "saldo", "edad", "tipo") // ✓ Correcto
.names("cuenta_id", "nombre", "saldo", "edad", "tipo") // ✗ Incorrecto
```

### No se procesan registros

**Causa**: Archivos CSV no están en la ubicación correcta.

**Solución**: Los archivos deben estar en `src/main/resources/data/`

## Mejoras Futuras

- [ ] Implementar retry policy para errores transitorios
- [ ] Agregar métricas con Micrometer
- [ ] Implementar notificaciones por email al finalizar jobs
- [ ] Dashboard web para monitoreo de jobs
- [ ] Soporte para múltiples formatos de entrada (Excel, JSON)
- [ ] Integración con sistemas externos via REST
- [ ] Implementar chunking adaptativo según carga del sistema

## Contribuciones

Las contribuciones son bienvenidas. Por favor:

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## Licencia

Este proyecto es parte del sistema Finance Batch desarrollado para procesamiento financiero automatizado.

## Contacto

Finance Batch System - Sistema de Procesamiento Batch Financiero

Project Link: [https://github.com/tu-usuario/finance-batch](https://github.com/tu-usuario/finance-batch)

---

Desarrollado con Spring Batch Framework | 2024
