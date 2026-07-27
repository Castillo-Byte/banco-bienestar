package com.banco.bancobienestar.controller;

import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.banco.bancobienestar.entity.CuentaEntity;
import com.banco.bancobienestar.entity.MovimientosEntity;
import com.banco.bancobienestar.entity.UsuarioEntity;
import com.banco.bancobienestar.repository.MovimientoCuentaRepository;
import com.banco.bancobienestar.repository.UsuarioRepository;


@Controller
public class DashboardController {
    private final UsuarioRepository usuarioRepository;
    private final MovimientoCuentaRepository movimientoCuentaRepository;

public DashboardController(UsuarioRepository usuario, MovimientoCuentaRepository movi) {
        this.usuarioRepository = usuario;
        this.movimientoCuentaRepository = movi;
    }
    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String mostrarDashboard(Model modelo, Authentication auth) {
        //datos del cliente simulados, aqui mostrara los datos de la db
    /*String cliente=" Cruz enrique Garcia";
    Double saldo = 45863.00;
    String clabe = "123456789012345678";

    List<Map<String, Object>> ultimosMovi = List.of(
        Map.of("fecha", "15/12/2026", "descripcion", "Deposito Nomina", "monto", "22000.00"),
        Map.of("fecha", "20/12/2026", "descripcion", "Pago de Cinepolis", "monto", "-275.00"),
        Map.of("fecha", "25/12/2026", "descripcion", "Pago Telcel", "monto", "-500.00")
    );*/
    

    if(auth == null){
        return "redirect:/login";
    }

    String username = auth.getName();
    UsuarioEntity usuario = usuarioRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

    if("EJECUTIVO".equalsIgnoreCase(usuario.getRol())) {
        return "redirect:/admin/dashboard";
    }

    String clabe ="No asignada";
    double saldo = 0.0;
    List<MovimientosEntity> ultimosMovimientos = new ArrayList<>();

    if(usuario.getCuentas() != null && !usuario.getCuentas().isEmpty()) {
        CuentaEntity cuentaPrincipal = usuario.getCuentas().get(0); 
        clabe = cuentaPrincipal.getClabe();
        saldo = cuentaPrincipal.getSaldo();

        //cargar los ultimos movimientos
        ultimosMovimientos = movimientoCuentaRepository.findByCuentaOrigenOrCuentaDestinoOrderByFechaDesc(clabe, clabe);
        
        String clabeFinal = clabe;
        ultimosMovimientos.forEach(m -> {double montoAbs = Math.abs(m.getMonto());
        m.setMonto(clabeFinal.equals(m.getCuentaOrigen()) ? -montoAbs : montoAbs);
    });
    }

    //inyectar los datos al modelo de thymeleaf
    modelo.addAttribute("nombreCliente", usuario.getNombre());
    modelo.addAttribute("saldoTotal", saldo);
    modelo.addAttribute("cuentaClabe", clabe);
    modelo.addAttribute("movimientos", ultimosMovimientos);

return "dashboard";
    }
}
