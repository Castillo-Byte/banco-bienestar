package com.banco.bancobienestar.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import com.banco.bancobienestar.entity.UsuarioEntity;
import com.banco.bancobienestar.repository.UsuarioRepository;
import com.banco.bancobienestar.service.BancaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
public class PerfilController {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final BancaService bancaService;

    public PerfilController(UsuarioRepository usuario, PasswordEncoder passwordEncoder, BancaService bancaService) {
        this.usuarioRepository = usuario;
        this.passwordEncoder = passwordEncoder;
        this.bancaService = bancaService;
    }

    @GetMapping("/perfil")
    public String mostrarPerfil(Model modelo, Authentication auth) {
        if (auth == null) {
            return "redirect:/login";
        }
        String username = auth.getName();
        UsuarioEntity usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        modelo.addAttribute("usuario", usuario);
        modelo.addAttribute("perfilForm", new PerfilForm(usuario.getNombre(), usuario.getUsername()));
        modelo.addAttribute("passwordForm", new PasswordForm());
        return "perfil";
    }

    @PostMapping("/perfil/actualizar")
    public String actualizarPerfil(@ModelAttribute PerfilForm perfilForm, Authentication auth, RedirectAttributes redirectAttributes) {
        if (auth == null) {
            return "redirect:/login";
        }

        UsuarioEntity usuario = usuarioRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (perfilForm.getNombre() == null || perfilForm.getNombre().isBlank()) {
            redirectAttributes.addFlashAttribute("mensajeError", "El nombre no puede estar vacio.");
            return "redirect:/perfil";
        }

        if (perfilForm.getUsername() == null || perfilForm.getUsername().isBlank()) {
            redirectAttributes.addFlashAttribute("mensajeError", "El usuario no puede estar vacio.");
            return "redirect:/perfil";
        }

        try {
            bancaService.actualizarUsuario(
                    usuario.getId(),
                    perfilForm.getNombre(),
                    perfilForm.getUsername(),
                    null // no se toca la contrasena en este form
            );
            redirectAttributes.addFlashAttribute("mensajeExito", "Perfil actualizado correctamente.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/perfil";
    }

    @PostMapping("/perfil/cambiar-password")
    public String cambiarPassword(@ModelAttribute PasswordForm passwordForm, Authentication auth, RedirectAttributes redirectAttributes) {
        if (auth == null) {
            return "redirect:/login";
        }

        UsuarioEntity usuario = usuarioRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (passwordForm.getPasswordActual() == null || !passwordEncoder.matches(passwordForm.getPasswordActual(), usuario.getPassword())) {
            redirectAttributes.addFlashAttribute("mensajeError", "La contrasena actual no es correcta.");
            return "redirect:/perfil";
        }

        if (passwordForm.getPasswordNueva() == null || passwordForm.getPasswordNueva().length() < 8) {
            redirectAttributes.addFlashAttribute("mensajeError", "La nueva contrasena debe tener minimo 8 caracteres.");
            return "redirect:/perfil";
        }

        if (!passwordForm.getPasswordNueva().equals(passwordForm.getPasswordConfirm())) {
            redirectAttributes.addFlashAttribute("mensajeError", "Las contrasenas no coinciden.");
            return "redirect:/perfil";
        }

        try {
            bancaService.actualizarUsuario(
                    usuario.getId(),
                    usuario.getNombre(),   // no cambian en este form
                    usuario.getUsername(), // no cambian en este form
                    passwordForm.getPasswordNueva()
            );
            redirectAttributes.addFlashAttribute("mensajeExito", "Contrasena actualizada correctamente.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/perfil";
    }

    public static class PerfilForm {
        private String nombre;
        private String username;

        public PerfilForm() {
        }

        public PerfilForm(String nombre, String username) {
            this.nombre = nombre;
            this.username = username;
        }

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }

    public static class PasswordForm {
        private String passwordActual;
        private String passwordNueva;
        private String passwordConfirm;

        public String getPasswordActual() {
            return passwordActual;
        }

        public void setPasswordActual(String passwordActual) {
            this.passwordActual = passwordActual;
        }

        public String getPasswordNueva() {
            return passwordNueva;
        }

        public void setPasswordNueva(String passwordNueva) {
            this.passwordNueva = passwordNueva;
        }

        public String getPasswordConfirm() {
            return passwordConfirm;
        }

        public void setPasswordConfirm(String passwordConfirm) {
            this.passwordConfirm = passwordConfirm;
        }
    }

}