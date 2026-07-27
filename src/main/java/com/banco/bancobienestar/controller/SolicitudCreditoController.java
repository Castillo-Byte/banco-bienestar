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
public class SolicitudCreditoController {
    
    private final BancaService bancaService;
    private final UsuarioRepository usuarioRepository;
    private final SolicitudCreditoRepository solicitudCreditoRepository;

    public SolicitudCreditoController(BancaService bancaService,
                             UsuarioRepository usuarioRepository,
                             SolicitudCreditoRepository solicitudCreditoRepository) {
        this.bancaService = bancaService;
        this.usuarioRepository = usuarioRepository;
        this.solicitudCreditoRepository = solicitudCreditoRepository;
    }

    @GetMapping("/solicitudCredito")
    public String mostrarCredito(Model modelo, Authentication auth) {
        if (auth == null) {
            return "redirect:/login";
        }

        UsuarioEntity usuario = usuarioRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        modelo.addAttribute("solicitudes", solicitudCreditoRepository.findByUsuarioOrderByFechaDesc(usuario));
        return "solicitudCredito";
    }

    @PostMapping("/procesar-credito")
    public String procesarCredito(@RequestParam Double monto,
                                  @RequestParam String firmaBase64,
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        try {
            if (auth == null) {
                return "redirect:/login";
            }

            bancaService.guardarSolicitudCredito(auth.getName(), monto, firmaBase64);
            redirectAttributes.addAttribute("exito", "Operacion completada con exito.");
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", e.getMessage());
        }

        return "redirect:/solicitudCredito";
    }

}