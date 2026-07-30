package com.banco.bancobienestar.config;
//la unica funcion es crear un usuario ejecutivo y cliente

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.banco.bancobienestar.entity.UsuarioEntity;
import com.banco.bancobienestar.repository.UsuarioRepository;
import com.banco.bancobienestar.service.BancaService;

@Component
public class DataInitializer implements CommandLineRunner{
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final BancaService bancaService;

    public DataInitializer(UsuarioRepository usuario, PasswordEncoder pass, BancaService bancaService) {
        this.usuarioRepository = usuario;
        this.passwordEncoder = pass;
        this.bancaService = bancaService;
    }
    @Override
    public void run(String... args) throws Exception {
        if(usuarioRepository.count() ==0){
            System.out.println("Agregando datos de prueba...");
            //creando un ejecutivo
            UsuarioEntity ejecutivo = new UsuarioEntity();
            ejecutivo.setUsername("cegr");
            ejecutivo.setNombre("Cruz Enrique Garcia");
            ejecutivo.setPassword(passwordEncoder.encode("cruzito1234"));
            ejecutivo.setRol("EJECUTIVO");
            usuarioRepository.save(ejecutivo);
            System.out.println("Datos ejecutivo : cegr - cruzito1234");

            //creando un cliente con cuenta
            bancaService.crearClienteConCuenta("Brandon bedolla", "acapulco", "acapulco1234", 1000.0);
            System.out.println("Datos cliente : acapulco - acapulco1234 con cuenta creada.");
        }
    }

}
