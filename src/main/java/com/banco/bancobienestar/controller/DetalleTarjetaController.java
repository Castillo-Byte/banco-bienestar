package com.banco.bancobienestar.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.banco.bancobienestar.entity.CuentaEntity;
import com.banco.bancobienestar.entity.UsuarioEntity;
import com.banco.bancobienestar.repository.UsuarioRepository;


@Controller
public class DetalleTarjetaController {
    private final UsuarioRepository usuarioRepository;

    public DetalleTarjetaController(UsuarioRepository usuario) {
        this.usuarioRepository = usuario;
    }

    @GetMapping("/detalleTarjeta")
    public String detalleTarjeta(Model modelo, Authentication auth) {
        if(auth == null){
            return "redirect:/login";
        }
        String username = auth.getName();
        UsuarioEntity usuario = usuarioRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        CuentaEntity cuenta = usuario.getCuentas().get(0);
        modelo.addAttribute("usuario", usuario);
        modelo.addAttribute("cuenta", cuenta);
        return "detalleTarjeta";
    }
    
}
