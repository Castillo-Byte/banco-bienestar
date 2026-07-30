package com.banco.bancobienestar.controller;

import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.banco.bancobienestar.entity.CuentaEntity;
import com.banco.bancobienestar.entity.MovimientosEntity;
import com.banco.bancobienestar.entity.SolicitudCreditoEntity;
import com.banco.bancobienestar.entity.UsuarioEntity;
import com.banco.bancobienestar.repository.MovimientoCuentaRepository;
import com.banco.bancobienestar.repository.SolicitudCreditoRepository;
import com.banco.bancobienestar.repository.UsuarioRepository;
import com.banco.bancobienestar.service.BancaService;
import com.banco.bancobienestar.service.ComprobantePdfService;

@RestController
public class ComprobanteController {

    private final UsuarioRepository usuarioRepository;
    private final MovimientoCuentaRepository movimientoCuentaRepository;
    private final SolicitudCreditoRepository solicitudCreditoRepository;
    private final BancaService bancaService;
    private final ComprobantePdfService comprobantePdfService;

    public ComprobanteController(UsuarioRepository usuarioRepository,
                                 MovimientoCuentaRepository movimientoCuentaRepository,
                                 SolicitudCreditoRepository solicitudCreditoRepository,
                                 BancaService bancaService,
                                 ComprobantePdfService comprobantePdfService) {
        this.usuarioRepository = usuarioRepository;
        this.movimientoCuentaRepository = movimientoCuentaRepository;
        this.solicitudCreditoRepository = solicitudCreditoRepository;
        this.bancaService = bancaService;
        this.comprobantePdfService = comprobantePdfService;
    }

    @GetMapping("/movimientos/{id}/comprobante")
    public ResponseEntity<byte[]> descargarComprobanteMovimiento(@PathVariable Long id, Authentication auth) {
        UsuarioEntity usuario = usuarioAutenticado(auth);
        CuentaEntity cuenta = cuentaPrincipal(usuario);
        MovimientosEntity movimiento = movimientoCuentaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movimiento no encontrado."));

        validarMovimientoDelCliente(cuenta, movimiento);

        byte[] pdf = comprobantePdfService.comprobanteMovimiento(usuario, cuenta, movimiento);
        return respuestaPdf(pdf, "comprobante-movimiento-" + id + ".pdf");
    }

    @GetMapping("/movimientos/historial/comprobante")
    public ResponseEntity<byte[]> descargarHistorialMovimientos(Authentication auth) {
        UsuarioEntity usuario = usuarioAutenticado(auth);
        CuentaEntity cuenta = cuentaPrincipal(usuario);
        List<MovimientosEntity> movimientos = movimientoCuentaRepository
                .findByCuentaOrigenOrCuentaDestinoOrderByFechaDesc(cuenta.getClabe(), cuenta.getClabe());

        byte[] pdf = comprobantePdfService.historialMovimientos(usuario, cuenta, movimientos);
        return respuestaPdf(pdf, "historial-movimientos.pdf");
    }

    @GetMapping("/creditos/{id}/comprobante")
    public ResponseEntity<byte[]> descargarComprobanteCredito(@PathVariable Long id, Authentication auth) {
        UsuarioEntity usuario = usuarioAutenticado(auth);
        SolicitudCreditoEntity solicitud = solicitudCreditoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada."));

        if (solicitud.getUsuario() == null || !solicitud.getUsuario().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes descargar esta solicitud.");
        }

        CuentaEntity cuenta = usuario.getCuentas() != null && !usuario.getCuentas().isEmpty()
                ? usuario.getCuentas().get(0)
                : null;
        byte[] pdf = comprobantePdfService.comprobanteCredito(usuario, cuenta, solicitud);
        return respuestaPdf(pdf, "comprobante-credito-" + id + ".pdf");
    }

    @GetMapping("/admin/movimientos/{id}/comprobante")
    public ResponseEntity<byte[]> descargarComprobanteMovimientoAdmin(@PathVariable Long id) {
        MovimientosEntity movimiento = movimientoCuentaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movimiento no encontrado."));

        byte[] pdf = comprobantePdfService.comprobanteMovimientoAdmin(
                movimiento,
                nombrePorClabe(movimiento.getCuentaOrigen()),
                nombrePorClabe(movimiento.getCuentaDestino()));
        return respuestaPdf(pdf, "admin-comprobante-movimiento-" + id + ".pdf");
    }

    @GetMapping("/admin/movimientos/comprobante")
    public ResponseEntity<byte[]> descargarHistorialMovimientosAdmin() {
        byte[] pdf = comprobantePdfService.historialMovimientosAdmin(bancaService.todosMovimientos());
        return respuestaPdf(pdf, "admin-historial-movimientos.pdf");
    }

    @GetMapping("/admin/creditos/{id}/comprobante")
    public ResponseEntity<byte[]> descargarComprobanteCreditoAdmin(@PathVariable Long id) {
        SolicitudCreditoEntity solicitud = solicitudCreditoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada."));

        UsuarioEntity usuario = solicitud.getUsuario();
        CuentaEntity cuenta = usuario != null && usuario.getCuentas() != null && !usuario.getCuentas().isEmpty()
                ? usuario.getCuentas().get(0)
                : null;
        byte[] pdf = comprobantePdfService.comprobanteCredito(usuario, cuenta, solicitud);
        return respuestaPdf(pdf, "admin-comprobante-credito-" + id + ".pdf");
    }

    @GetMapping("/admin/creditos/comprobante")
    public ResponseEntity<byte[]> descargarHistorialCreditosAdmin() {
        byte[] pdf = comprobantePdfService.historialCreditosAdmin(bancaService.todasLasSolicitudesCredito());
        return respuestaPdf(pdf, "admin-historial-creditos.pdf");
    }

    @GetMapping("/admin/clientes/comprobante")
    public ResponseEntity<byte[]> descargarListaClientesAdmin() {
        byte[] pdf = comprobantePdfService.listaClientesAdmin(usuarioRepository.findByRol("CLIENTE"));
        return respuestaPdf(pdf, "admin-lista-clientes.pdf");
    }

    @GetMapping("/admin/clientes/{id}/historial")
    public ResponseEntity<byte[]> descargarHistorialClienteAdmin(@PathVariable Long id) {
        UsuarioEntity cliente = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado."));

        if (!"CLIENTE".equals(cliente.getRol())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario seleccionado no es cliente.");
        }

        CuentaEntity cuenta = cliente.getCuentas() != null && !cliente.getCuentas().isEmpty()
                ? cliente.getCuentas().get(0)
                : null;
        List<MovimientosEntity> movimientos = cuenta != null
                ? movimientoCuentaRepository.findByCuentaOrigenOrCuentaDestinoOrderByFechaDesc(cuenta.getClabe(), cuenta.getClabe())
                : List.of();
        List<SolicitudCreditoEntity> solicitudes = solicitudCreditoRepository.findByUsuarioOrderByFechaDesc(cliente);

        byte[] pdf = comprobantePdfService.historialClienteAdmin(cliente, movimientos, solicitudes);
        return respuestaPdf(pdf, "admin-historial-cliente-" + id + ".pdf");
    }

    private UsuarioEntity usuarioAutenticado(Authentication auth) {
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesion para descargar comprobantes.");
        }
        return usuarioRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));
    }

    private CuentaEntity cuentaPrincipal(UsuarioEntity usuario) {
        if (usuario.getCuentas() == null || usuario.getCuentas().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario no tiene cuenta asignada.");
        }
        return usuario.getCuentas().get(0);
    }

    private void validarMovimientoDelCliente(CuentaEntity cuenta, MovimientosEntity movimiento) {
        String clabe = cuenta.getClabe();
        if (!clabe.equals(movimiento.getCuentaOrigen()) && !clabe.equals(movimiento.getCuentaDestino())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes descargar este movimiento.");
        }
    }

    private String nombrePorClabe(String clabe) {
        return usuarioRepository.findByCuentas_Clabe(clabe)
                .map(UsuarioEntity::getNombre)
                .orElse("No registrado");
    }

    private ResponseEntity<byte[]> respuestaPdf(byte[] pdf, String nombreArchivo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(nombreArchivo).build());
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }
}
