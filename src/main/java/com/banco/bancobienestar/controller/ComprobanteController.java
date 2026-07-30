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
import com.banco.bancobienestar.service.ComprobantePdfService;

@RestController
public class ComprobanteController {

    private final UsuarioRepository usuarioRepository;
    private final MovimientoCuentaRepository movimientoCuentaRepository;
    private final SolicitudCreditoRepository solicitudCreditoRepository;
    private final ComprobantePdfService comprobantePdfService;

    public ComprobanteController(UsuarioRepository usuarioRepository,
                                 MovimientoCuentaRepository movimientoCuentaRepository,
                                 SolicitudCreditoRepository solicitudCreditoRepository,
                                 ComprobantePdfService comprobantePdfService) {
        this.usuarioRepository = usuarioRepository;
        this.movimientoCuentaRepository = movimientoCuentaRepository;
        this.solicitudCreditoRepository = solicitudCreditoRepository;
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

    private ResponseEntity<byte[]> respuestaPdf(byte[] pdf, String nombreArchivo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(nombreArchivo).build());
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }
}
