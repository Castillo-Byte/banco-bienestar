package com.banco.bancobienestar.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.banco.bancobienestar.entity.CuentaEntity;
import com.banco.bancobienestar.entity.MovimientosEntity;
import com.banco.bancobienestar.entity.UsuarioEntity;
import com.banco.bancobienestar.repository.MovimientoCuentaRepository;
import com.banco.bancobienestar.repository.UsuarioRepository;

@Controller
public class MovimientosController {
    private final UsuarioRepository usuarioRepository;
    private final MovimientoCuentaRepository movimientoCuentaRepository;

    public MovimientosController(UsuarioRepository usuario, MovimientoCuentaRepository movi) {
        this.usuarioRepository = usuario;
        this.movimientoCuentaRepository = movi;
    }

    @GetMapping("/movimientos")
    public String mostrarMovimientos(Model modelo, Authentication auth, @RequestParam(required = false) String q) {
        if(auth == null){
            return "redirect:/login";
        }

        String username = auth.getName();
        UsuarioEntity usuario = usuarioRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if(usuario.getCuentas() == null || usuario.getCuentas().isEmpty()){
            return "redirect:/dashboard";
        }

        CuentaEntity cuenta = usuario.getCuentas().get(0);
        List<MovimientosEntity> movimientos = movimientoCuentaRepository.findByCuentaOrigenOrCuentaDestinoOrderByFechaDesc(cuenta.getClabe(), cuenta.getClabe());

        if(q != null && !q.isBlank()){
            String busqueda = q.toLowerCase();
            movimientos = movimientos.stream()
                .filter(mov -> contiene(mov.getDescripcion(), busqueda)
                    || contiene(mov.getCuentaOrigen(), busqueda)
                    || contiene(mov.getCuentaDestino(), busqueda))
                .toList();
        }

        double totalIngresos = movimientos.stream()
            .filter(mov -> cuenta.getClabe().equals(mov.getCuentaDestino()))
            .mapToDouble(MovimientosEntity::getMonto)
            .sum();

        double totalEgresos = movimientos.stream()
            .filter(mov -> cuenta.getClabe().equals(mov.getCuentaOrigen()))
            .mapToDouble(MovimientosEntity::getMonto)
            .sum();

        modelo.addAttribute("cuenta", cuenta);
        modelo.addAttribute("movimientos", movimientos);
        modelo.addAttribute("totalIngresos", totalIngresos);
        modelo.addAttribute("totalEgresos", totalEgresos);
        modelo.addAttribute("q", q);
        return "movimientos";
    }

    private boolean contiene(String texto, String busqueda) {
        return texto != null && texto.toLowerCase().contains(busqueda);
    }
}
