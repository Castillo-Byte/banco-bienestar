package com.banco.bancobienestar.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.banco.bancobienestar.entity.SolicitudCreditoEntity;
import com.banco.bancobienestar.entity.UsuarioEntity;
import com.banco.bancobienestar.repository.SolicitudCreditoRepository;
import com.banco.bancobienestar.repository.UsuarioRepository;
import com.banco.bancobienestar.service.BancaService;

@Controller
@RequestMapping("/admin")
public class adminController {
    private final BancaService bancaService;
    private final UsuarioRepository usuarioRepository;
    private final SolicitudCreditoRepository solicitudCreditoRepository;

    public adminController(BancaService bancaService, UsuarioRepository usuarioRepository,
                           SolicitudCreditoRepository solicitudCreditoRepository) {
        this.bancaService = bancaService;
        this.usuarioRepository = usuarioRepository;
        this.solicitudCreditoRepository = solicitudCreditoRepository;
    }

    @GetMapping("/dashboard")
    public String mostrarDashboard(Model modelo) {
    List<UsuarioEntity> clientes = usuarioRepository.findAll().stream().filter(u->"CLIENTE".equals(u.getRol())).collect(Collectors.toList());
    
    //vargar solicitudes en una lista
    List<SolicitudCreditoEntity> solicitudes = solicitudCreditoRepository.findAllByOrderByFechaDesc();
    modelo.addAttribute("clientes", clientes);
    modelo.addAttribute("solicitudes", solicitudes);

        return "admin";
    
}
 @PostMapping("/Crea-cliente")
    public String crearCliente(
        @RequestParam String username, 
        @RequestParam String password, 
        @RequestParam Double saldoInicial,
        @RequestParam String nombre) {

            if(username== null || username.trim().isEmpty() 
                || password == null || password.trim().isEmpty()
                || nombre == null || nombre.trim().isEmpty()) {
                return "redirect:/admin/dashboard?error=Llenar todos los campos";
            }

            if(saldoInicial == null || saldoInicial < 0){
                return "redirect:/admin/dashboard?error=No puede tener saldo 0";
            }
            try {
                bancaService.crearClienteConCuenta(nombre, username, password, saldoInicial);
                return "redirect:/admin/dashboard?exito=Cliente creado exitosamente";
            } catch (Exception e) {
                return "redirect:/admin/dashboard?error=" + e.getMessage();
            }

    } 
}