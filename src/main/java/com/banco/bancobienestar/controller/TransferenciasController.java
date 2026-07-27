package com.banco.bancobienestar.controller;


import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.banco.bancobienestar.entity.CuentaEntity;
import com.banco.bancobienestar.entity.UsuarioEntity;
import com.banco.bancobienestar.repository.UsuarioRepository;
import com.banco.bancobienestar.service.BancaService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class TransferenciasController {

    private final BancaService bancaService;
    private final UsuarioRepository usuarioRepository;

    public TransferenciasController(BancaService bancaService, UsuarioRepository usuarioRepository) {
        this.bancaService = bancaService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/transferencia")
    public String mostrarFormTranferencia(Model model, Authentication auth) {
        String username = auth.getName();
        UsuarioEntity usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

                String clabe = "No asignada";
                Double saldo = 0.0;

                if(usuario.getCuentas() != null && !usuario.getCuentas().isEmpty()) {
                    CuentaEntity cuentaPrincipal = usuario.getCuentas().get(0); 
                    clabe = cuentaPrincipal.getClabe();
                    saldo = cuentaPrincipal.getSaldo();
                }

        model.addAttribute("cuentaClabe", clabe);
        model.addAttribute("saldoTotal", saldo);

        return "transferencia";
    }


    @PostMapping("/procesar-transferencia")
    public String procesar(
        @RequestParam String clabeDestino,
        @RequestParam Double monto,
        @RequestParam String descripcion,
        Authentication auth) {
        
            String usernameAutorizado = auth.getName();
        try{
            bancaService.transferirDesdeUsuario(usernameAutorizado, clabeDestino, monto, descripcion);
            return "redirect:/dashboard?exito=Transferencia realizada con éxito.";
        }catch (Exception e) {
            return "redirect:/transferencia?error=" + e.getMessage();
        }
    }
    


}