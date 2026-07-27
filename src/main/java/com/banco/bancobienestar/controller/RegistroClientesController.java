package com.banco.bancobienestar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RegistroClientesController {
    @GetMapping("/registroClientes")
    public String mostrarRegistroClientes() {
        return "registroClientes";
    }

}
