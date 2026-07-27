package com.banco.bancobienestar.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.banco.bancobienestar.entity.UsuarioEntity;
import com.banco.bancobienestar.repository.SolicitudCreditoRepository;
import com.banco.bancobienestar.repository.UsuarioRepository;
import com.banco.bancobienestar.service.BancaService;

@Controller
public class AdminCreditoController {
    private final BancaService bancaService;
    private final UsuarioRepository usuarioRepository;

    public AdminCreditoController(BancaService bancaService,
                             UsuarioRepository usuarioRepository,
                             SolicitudCreditoRepository solicitudCreditoRepository) {
        this.bancaService = bancaService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/adminCreditos")
    public String panelEjecutivo(Model modelo, Authentication auth) {
        if (auth == null) {
            return "redirect:/login";
        }

        UsuarioEntity usuario = usuarioRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        if (!"EJECUTIVO".equals(usuario.getRol())) {
            return "redirect:/acceso-denegado";
        }

        modelo.addAttribute("solicitudes", bancaService.todasLasSolicitudesCredito());
        return "adminCreditos";
    }

    @PostMapping("/ejecutivo/aprobar-credito")
    public String aprobarCredito(@RequestParam Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        if (auth == null) {
            return "redirect:/login";
        }
        UsuarioEntity usuario = usuarioRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        if (!"EJECUTIVO".equals(usuario.getRol())) {
            return "redirect:/acceso-denegado";
        }

        try {
            bancaService.autorizarSolicitudCredito(id);
            redirectAttributes.addAttribute("exito", "Credito aprobado y saldo acreditado.");
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", e.getMessage());
        }
        return "redirect:/adminCreditos";
    }

    @PostMapping("/ejecutivo/rechazar-credito")
    public String rechazarCredito(@RequestParam Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        if (auth == null) {
            return "redirect:/login";
        }
        UsuarioEntity usuario = usuarioRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        if (!"EJECUTIVO".equals(usuario.getRol())) {
            return "redirect:/acceso-denegado";
        }

        try {
            bancaService.rechazarSolicitudCredito(id);
            redirectAttributes.addAttribute("exito", "Solicitud rechazada.");
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", e.getMessage());
        }
        return "redirect:/adminCreditos";
    }
}