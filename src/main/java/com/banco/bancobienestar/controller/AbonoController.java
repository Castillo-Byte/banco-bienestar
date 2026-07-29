package com.banco.bancobienestar.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.banco.bancobienestar.entity.CuentaEntity;
import com.banco.bancobienestar.entity.SolicitudCreditoEntity;
import com.banco.bancobienestar.entity.UsuarioEntity;
import com.banco.bancobienestar.repository.UsuarioRepository;
import com.banco.bancobienestar.service.BancaService;

@Controller
public class AbonoController {
    private final BancaService bancaService;
    private final UsuarioRepository usuarioRepository;

    public AbonoController(BancaService bancaService, UsuarioRepository usuarioRepository) {
        this.bancaService = bancaService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/abono")
    public String mostrarAbono(Model modelo, Authentication auth) {
        if (auth == null) {
            return "redirect:/login";
        }

        UsuarioEntity usuario = usuarioRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        if (!"CLIENTE".equals(usuario.getRol())) {
            return "redirect:/acceso-denegado";
        }

        CuentaEntity cuenta = null;
        if (usuario.getCuentas() != null && !usuario.getCuentas().isEmpty()) {
            cuenta = usuario.getCuentas().get(0);
        }

        SolicitudCreditoEntity credito = bancaService.creditoActivoPorUsuario(auth.getName());

        modelo.addAttribute("cuenta", cuenta);
        modelo.addAttribute("credito", credito);
        return "abono";
    }

    @PostMapping("/procesar-abono")
    public String procesarAbono(@RequestParam Long creditoId,
                                @RequestParam Double monto,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        try {
            if (auth == null) {
                return "redirect:/login";
            }

            bancaService.abonarCredito(auth.getName(), creditoId, monto);
            redirectAttributes.addAttribute("exito", "Abono registrado con exito.");
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", e.getMessage());
        }

        return "redirect:/abono";
    }
    
}
