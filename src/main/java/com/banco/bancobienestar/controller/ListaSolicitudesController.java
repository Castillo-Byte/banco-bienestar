package com.banco.bancobienestar.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.banco.bancobienestar.entity.SolicitudCreditoEntity;
import com.banco.bancobienestar.entity.UsuarioEntity;
import com.banco.bancobienestar.repository.SolicitudCreditoRepository;
import com.banco.bancobienestar.repository.UsuarioRepository;

@Controller
public class ListaSolicitudesController {
    private final UsuarioRepository usuarioRepository;
    private final SolicitudCreditoRepository solicitudCreditoRepository;

    public ListaSolicitudesController(UsuarioRepository usuario, SolicitudCreditoRepository solicitud) {
        this.usuarioRepository = usuario;
        this.solicitudCreditoRepository = solicitud;
    }

    @GetMapping("/listaSolicitudes")
    public String listarSolicitudes(Model modelo, Authentication auth) {
        if(auth == null){
            return "redirect:/login";
        }

        String username = auth.getName();
        UsuarioEntity usuario = usuarioRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        List<SolicitudCreditoEntity> solicitudes = solicitudCreditoRepository.findByUsuarioOrderByFechaDesc(usuario);

        long totalPendientes = solicitudes.stream().filter(solicitud -> "PENDIENTE".equalsIgnoreCase(solicitud.getEstado())).count();
        long totalAprobadas = solicitudes.stream().filter(solicitud -> "APROBADO".equalsIgnoreCase(solicitud.getEstado())).count();
        long totalRechazadas = solicitudes.stream().filter(solicitud -> "RECHAZADA".equalsIgnoreCase(solicitud.getEstado())).count();

        modelo.addAttribute("usuario", usuario);
        modelo.addAttribute("solicitudes", solicitudes);
        modelo.addAttribute("totalPendientes", totalPendientes);
        modelo.addAttribute("totalAprobados", totalAprobadas);
        modelo.addAttribute("totalRechazadas", totalRechazadas);
        return "listaSolicitudes";
    }
}
