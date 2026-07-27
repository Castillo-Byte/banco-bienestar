package com.banco.bancobienestar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class EstadoCuentaController {

    @GetMapping("/estadoCuenta")
    public String estadoCuenta() {
        return "redirect:/movimientos";
    }
}
