package cl.duoc.finance_batch.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.finance_batch.business.Cuenta;
import cl.duoc.finance_batch.business.EstadoFinanciero;
import cl.duoc.finance_batch.repository.CuentaRepository;
import cl.duoc.finance_batch.repository.EstadoFinancieroRepository;

@RestController
@RequestMapping("/api/v1")
public class FinanceRestController {

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private EstadoFinancieroRepository estadoFinancieroRepository;

    // 1. Obtener información base de la Cuenta (Saldo, Tipo, Cliente)
    @GetMapping("/cuentas/{id}")
    public ResponseEntity<Cuenta> obtenerCuenta(@PathVariable Long id) {
        // CAMBIO REALIZADO:
        // Ahora usamos 'findByCuentaId' en lugar de 'findById'.
        // El parámetro 'id' de la URL se pasa como el 'cuentaId' que buscamos.
        Optional<Cuenta> cuenta = cuentaRepository.findByCuentaId(id);
        
        return cuenta.map(ResponseEntity::ok)
                     .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 2. Obtener los movimientos registrados en Estados Financieros
    @GetMapping("/cuentas/{id}/transacciones")
    public ResponseEntity<List<EstadoFinanciero>> obtenerTransaccionesCuenta(@PathVariable Long id) {
        // Usamos el método que creamos en el Paso 2
        List<EstadoFinanciero> transacciones = estadoFinancieroRepository.findByCuentaId(id);
        
        if (transacciones.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(transacciones);
    }
    
    // 3. Health Check
    @GetMapping("/ping")
    public String ping() {
        return "Finance-Batch Backend is UP";
    }
}
