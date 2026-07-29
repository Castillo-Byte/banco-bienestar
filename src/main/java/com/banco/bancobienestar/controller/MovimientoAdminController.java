package com.banco.bancobienestar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.banco.bancobienestar.service.BancaService;


@Controller
@RequestMapping("/admin/movimientos")
public class MovimientoAdminController {
    private BancaService bancaService;
    public MovimientoAdminController(BancaService bancaService) {
        this.bancaService = bancaService;
    }

    //mostramos la vista de la lista
    @GetMapping
    public String listaMovimientos(Model modelo) {
    modelo.addAttribute("movimientos",bancaService.todosMovimientos());
    modelo.addAttribute("clabeNombre", bancaService.mapaClabeNombre());
    return"adminmovimientos";
    }
    //actualizar movimiento
    @PostMapping("/actualizar-estado")
    public String actualizarEstado(@RequestParam Long id, @RequestParam String estado,RedirectAttributes redirectAttributes){
        try{
            bancaService.actualizarMovimiento(id,estado);
            redirectAttributes.addAttribute("exito","El estado Del movimiento cambio a " + estado);
        }catch(Exception e){
            redirectAttributes.addAttribute("error", "No se puede actualizar");

        }
        return "redirect:/admin/movimientos";
    }

    @PostMapping("/cancelar")
public String cancelarMovimiento(@RequestParam Long id, RedirectAttributes redirectAttributes) {
    try {
        bancaService.cancelarMovimiento(id);
        redirectAttributes.addFlashAttribute("exito", "Movimiento cancelado correctamente");
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", "No se pudo cancelar: " + e.getMessage());
    }
    return "redirect:/admin/movimientos";
}
   /*  //eliminar movimiento
    @PostMapping("/eliminar")
    public String elimianrMovimiento(@RequestParam Long id,RedirectAttributes redirectAttributes) {
        try{
            bancaService.eliminarMovimiento(id);
            redirectAttributes.addAttribute("exito", "Movimiento eliminado");
        }catch(Exception e){
            redirectAttributes.addAttribute("error", "El error es :" + e);

        }
        return "redirect:/admin/movimientos";
    }*/
  
}
    
