package com.banco.bancobienestar.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banco.bancobienestar.entity.CuentaEntity;
import com.banco.bancobienestar.entity.GastosDTO;
import com.banco.bancobienestar.entity.MovimientosDTO;
import com.banco.bancobienestar.entity.MovimientosEntity;
import com.banco.bancobienestar.entity.UsuarioEntity;
import com.banco.bancobienestar.repository.MovimientoCuentaRepository;
import com.banco.bancobienestar.repository.UsuarioRepository;


import java.util.ArrayList; 
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/v1/finanzas")
public class FinanzasRestController {
    private final UsuarioRepository usuarioRepository;
    private final MovimientoCuentaRepository movimientoCuentaRepository;

    public FinanzasRestController(UsuarioRepository usuario, MovimientoCuentaRepository movi) {
    this.usuarioRepository = usuario;
    this.movimientoCuentaRepository = movi;
}

    @GetMapping("/gastos-mes")
    public List<GastosDTO> obtenerGastos(Authentication auth) {
        String username = auth.getName();
        

        UsuarioEntity usuario = usuarioRepository.findByUsername(username)
        .orElseThrow();

        CuentaEntity cuenta = usuario.getCuentas().get(0);
        String clabe = cuenta.getClabe();

        List<MovimientosEntity> movimientos = movimientoCuentaRepository.findByCuentaOrigenOrCuentaDestinoOrderByFechaDesc(clabe, clabe);

        String clabeFinal = clabe;
        movimientos.forEach(m -> {double montoAbs = Math.abs(m.getMonto());
    m.setMonto(clabeFinal.equals(m.getCuentaOrigen()) ? -montoAbs : montoAbs);
});

        List<Map.Entry<String, Double>> gastosAgrupados = movimientos.stream()
                .filter(m -> m.getMonto() < 0)
                .collect(Collectors.groupingBy(MovimientosEntity::getDescripcion,Collectors.summingDouble(m -> Math.abs(m.getMonto())))
                )
                .entrySet().stream()
                .toList();

        List<String> colores = List.of(
                "#FF5733",
                "#33FF57",
                "#3357FF",
                "#F1C40F",
                "#9B59B6",
                "#E67E22"
        );

        List<GastosDTO> resultado = new ArrayList<>();

        for (int i = 0; i < gastosAgrupados.size(); i++) {
            Map.Entry<String, Double> gasto = gastosAgrupados.get(i);
            String color = colores.get(i % colores.size());

            resultado.add(new GastosDTO(gasto.getKey(), gasto.getValue(), color));
        }

        return resultado;
    }

    @GetMapping("/ingresos-mes")
    public List<GastosDTO> obtenerIngresos(Authentication auth) {
        String username = auth.getName();

        UsuarioEntity usuario = usuarioRepository.findByUsername(username)
        .orElseThrow();

        CuentaEntity cuenta = usuario.getCuentas().get(0);
        String clabe = cuenta.getClabe();

        List<MovimientosEntity> movimientos = movimientoCuentaRepository.findByCuentaOrigenOrCuentaDestinoOrderByFechaDesc(clabe, clabe);

        String clabeFinal = clabe;
        movimientos.forEach(m -> {double montoAbs = Math.abs(m.getMonto());
    m.setMonto(clabeFinal.equals(m.getCuentaOrigen()) ? -montoAbs : montoAbs);
});

        List<Map.Entry<String, Double>> ingresosAgrupados = movimientos.stream()
                .filter(m -> m.getMonto() > 0)
                .collect(Collectors.groupingBy(MovimientosEntity::getDescripcion, Collectors.summingDouble(MovimientosEntity::getMonto))
                )
                .entrySet().stream()
                .toList();

        List<String> colores = List.of(
                "#FF5733",
                "#33FF57",
                "#3357FF",
                "#F1C40F",
                "#9B59B6",
                "#E67E22"
        );

        List<GastosDTO> resultado = new ArrayList<>();

        for (int i = 0; i < ingresosAgrupados.size(); i++) {
            Map.Entry<String, Double> ingreso = ingresosAgrupados.get(i);
            String color = colores.get(i % colores.size());

            resultado.add(new GastosDTO(ingreso.getKey(), ingreso.getValue(), color));
        }

        return resultado;
    }

    @GetMapping("/movimientos-por-tipo")
    public List<MovimientosDTO> obtenerMovimientosPorTipo(Authentication auth) {
        String username = auth.getName();

        UsuarioEntity usuario = usuarioRepository.findByUsername(username)
        .orElseThrow();

        CuentaEntity cuenta = usuario.getCuentas().get(0);
        String clabe = cuenta.getClabe();

        List<MovimientosEntity> movimientos = movimientoCuentaRepository.findByCuentaOrigenOrCuentaDestinoOrderByFechaDesc(clabe, clabe);

        String clabeFinal = clabe;
        movimientos.forEach(m -> {double montoAbs = Math.abs(m.getMonto());
    m.setMonto(clabeFinal.equals(m.getCuentaOrigen()) ? -montoAbs : montoAbs);
});

        List<Map.Entry<String, Long>> movimientosAgrupados = movimientos.stream()
                .collect(Collectors.groupingBy(MovimientosEntity::getTipo, Collectors.counting())
                )
                .entrySet().stream()
                .toList();

        List<String> colores = List.of(
                "#FF5733",
                "#33FF57",
                "#3357FF",
                "#F1C40F",
                "#9B59B6",
                "#E67E22"
        );

        List<MovimientosDTO> resultado = new ArrayList<>();

        for (int i = 0; i < movimientosAgrupados.size(); i++) {
            Map.Entry<String, Long> movimiento = movimientosAgrupados.get(i);
            String color = colores.get(i % colores.size());

            resultado.add(new MovimientosDTO(movimiento.getKey(), movimiento.getValue(), color));
        }

        return resultado;
    }
    
}
