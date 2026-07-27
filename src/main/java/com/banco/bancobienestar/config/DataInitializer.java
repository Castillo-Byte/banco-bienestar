package com.banco.bancobienestar.config;
//la unica funcion es crear un usuario ejecutivo y cliente

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.banco.bancobienestar.entity.UsuarioEntity;
import com.banco.bancobienestar.repository.UsuarioRepository;

@Component
public class DataInitializer implements CommandLineRunner{
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuario, PasswordEncoder pass) {
        this.usuarioRepository = usuario;
        this.passwordEncoder = pass;
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

            //creando un cliente
            UsuarioEntity cliente = new UsuarioEntity();
            cliente.setUsername("acapulco");
            cliente.setNombre("Brandon bedolla");
            cliente.setPassword(passwordEncoder.encode("acapulco1234"));
            cliente.setRol("CLIENTE");
            usuarioRepository.save(cliente);
            System.out.println("Datos cliente : acapulco - acapulco1234");
        }
    }

}
