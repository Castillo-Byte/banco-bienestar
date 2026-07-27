package com.banco.bancobienestar.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.banco.bancobienestar.entity.UsuarioEntity;
import com.banco.bancobienestar.repository.UsuarioRepository;
import com.banco.bancobienestar.service.BancaService;

@Controller
public class ListaUsuariosController {

    private final UsuarioRepository usuarioRepository;
    private BancaService bancaService;


    public ListaUsuariosController(UsuarioRepository usuarioRepository,BancaService bancaService) {
        this.usuarioRepository = usuarioRepository;
        this.bancaService = bancaService;
    }

    @GetMapping("/ejecutivoUsuarios")
    public String mostrarUsuarios(Model modelo, @RequestParam(name = "pagina", defaultValue = "0") int pagina, @RequestParam(name = "q", required = false) String q) {

        int tamanoPagina = 10;
        Pageable pageable = PageRequest.of(pagina, tamanoPagina, Sort.by("nombre"));

        Page<UsuarioEntity> resultado;
        if (q != null && !q.isBlank()) {
            resultado = usuarioRepository.buscarClientes("CLIENTE", q, pageable);
        } else {
            resultado = usuarioRepository.findByRol("CLIENTE", pageable);
        }

        modelo.addAttribute("usuarios", resultado.getContent());
        modelo.addAttribute("totalElementos", resultado.getTotalElements());
        modelo.addAttribute("totalPaginas", resultado.getTotalPages());
        modelo.addAttribute("paginaActual", pagina);
        modelo.addAttribute("q", q);

        return "ejecutivoUsuarios";
    }
    //eliminar movimiento
    @PostMapping("/ejecutivoUsuarios/eliminar")
    public String eliminarUsuario(@RequestParam Long id,RedirectAttributes redirectAttributes) {
        try{
            bancaService.eliminarUsuario(id);
            redirectAttributes.addAttribute("exito", "Usuario eliminado");
        }catch(Exception e){
            redirectAttributes.addAttribute("error", "El error es :" + e);

        }
        return "redirect:/ejecutivoUsuarios";
    }


@PostMapping("/ejecutivoUsuarios/actualizar")
public String actualizarUsuario(@RequestParam Long id, @RequestParam String nombre, @RequestParam String username, @RequestParam(required = false) String password, RedirectAttributes redirectAttributes) {
    try {
        bancaService.actualizarUsuario(id, nombre, username, password);
        redirectAttributes.addFlashAttribute("mensajeExito", "Cliente actualizado correctamente");
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("mensajeError", "Error al actualizar: " + e.getMessage());
    }
    return "redirect:/ejecutivoUsuarios";
}

}