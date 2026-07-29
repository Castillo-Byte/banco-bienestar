package com.banco.bancobienestar.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banco.bancobienestar.entity.CuentaEntity;
import com.banco.bancobienestar.entity.MovimientosEntity;
import com.banco.bancobienestar.entity.SolicitudCreditoEntity;
import com.banco.bancobienestar.entity.UsuarioEntity;
import com.banco.bancobienestar.repository.CuentaRepository;
import com.banco.bancobienestar.repository.MovimientoCuentaRepository;
import com.banco.bancobienestar.repository.SolicitudCreditoRepository;
import com.banco.bancobienestar.repository.UsuarioRepository;

@Service
public class BancaService {

    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final MovimientoCuentaRepository movimientoRepository;
    private final SolicitudCreditoRepository solicitudCreditoRepository;
    private final PasswordEncoder passwordEncoder;

    public BancaService(UsuarioRepository usuarioRepository,
                        CuentaRepository cuentaRepository,
                        MovimientoCuentaRepository movimientoRepository,
                        SolicitudCreditoRepository solicitudCreditoRepository,
                        @Lazy PasswordEncoder passwordEncoder) { // Se usa @Lazy para evitar dependencias circulares con SecurityConfig
        this.usuarioRepository = usuarioRepository;
        this.cuentaRepository = cuentaRepository;
        this.movimientoRepository = movimientoRepository;
        this.solicitudCreditoRepository = solicitudCreditoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Transferencia entre CLABEs con garantía transaccional (ACID)
    @Transactional(rollbackFor = Exception.class)
    public void transferirMonto(String clabeOrigen, String clabeDestino, Double monto, String descripcion) {
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero.");
        }
        if (clabeOrigen.equals(clabeDestino)) {
            throw new IllegalArgumentException("La cuenta de destino no puede ser la misma que la de origen.");
        }

        CuentaEntity origen = cuentaRepository.findByClabe(clabeOrigen)
                .orElseThrow(() -> new RuntimeException("La cuenta de origen no existe."));

        CuentaEntity destino = cuentaRepository.findByClabe(clabeDestino)
                .orElseThrow(() -> new RuntimeException("La cuenta de destino no existe."));

        // Validar fondos del remitente
        if (origen.getSaldo() < monto) {
            throw new FondosInsuficientesException("No cuentas con saldo suficiente para esta operación.");
        }

        // 1. CARGO a la cuenta origen
        origen.setSaldo(origen.getSaldo() - monto);
        cuentaRepository.save(origen);

        // --- PUNTO DE FALLO POTENCIAL ---
        // Si ocurriera un error en este punto (por ejemplo, desconexión de DB), 
        // @Transactional deshará el cargo de la cuenta de origen.

        // 2. ABONO a la cuenta destino
        destino.setSaldo(destino.getSaldo() + monto);
        cuentaRepository.save(destino);

        // 3. Registrar el Movimiento
        MovimientosEntity movimiento = new MovimientosEntity();
        movimiento.setCuentaOrigen(clabeOrigen);
        movimiento.setCuentaDestino(clabeDestino);
        movimiento.setMonto(monto);
        movimiento.setDescripcion(descripcion);
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setTipo("Transferencia");
        movimiento.setEstadoMovimiento("AUTORIZADO");
        movimientoRepository.save(movimiento);
    }

    // Inicia una transferencia usando el usuario autenticado (Auditoría Activa)
    @Transactional(rollbackFor = Exception.class)
    public void transferirDesdeUsuario(String username, String clabeDestino, Double monto, String descripcion) {
        UsuarioEntity usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        if (usuario.getCuentas() == null || usuario.getCuentas().isEmpty()) {
            throw new RuntimeException("El usuario no tiene una cuenta bancaria asignada.");
        }

        // Usamos la cuenta principal (la primera asignada) del usuario autenticado
        String clabeOrigen = usuario.getCuentas().get(0).getClabe();
        
        transferirMonto(clabeOrigen, clabeDestino, monto, descripcion);
    }

    // Registra un cliente y le asigna una CLABE aleatoria única de 18 dígitos
    @Transactional(rollbackFor = Exception.class)
    public UsuarioEntity crearClienteConCuenta(String nombre,String username, String password, Double saldoInicial) {
        if (usuarioRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("El nombre de usuario ya está registrado.");
        }
        if (usuarioRepository.findByNombre(nombre).isPresent()) {
            throw new RuntimeException("El nombre del cliente ya está registrado.");
        }

        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setNombre(nombre);
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setRol("CLIENTE");
        UsuarioEntity usuarioGuardado = usuarioRepository.save(usuario);

        // Generar CLABE única de 18 dígitos
        String clabe = generarClabeUnica();

        CuentaEntity cuenta = new CuentaEntity();
        cuenta.setClabe(clabe);
        cuenta.setSaldo(saldoInicial);
        cuenta.setUsuario(usuarioGuardado);
        cuentaRepository.save(cuenta);

        List<CuentaEntity> list = new java.util.ArrayList<>();
        list.add(cuenta);
        usuarioGuardado.setCuentas(list);

        return usuarioGuardado;
    }

    // Guarda una solicitud de crédito autorizada por la firma
    @Transactional(rollbackFor = Exception.class)
    public SolicitudCreditoEntity guardarSolicitudCredito(String username, Double monto, String firmaBase64) {
        UsuarioEntity usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        SolicitudCreditoEntity solicitud = new SolicitudCreditoEntity();
        solicitud.setUsuario(usuario);
        solicitud.setMontoSolicitado(monto);
        solicitud.setSaldoPendiente(monto);
        solicitud.setFirmaBase64(firmaBase64);
        solicitud.setEstado("PENDIENTE"); // Por simplificación del flujo, se aprueba con la firma del cliente
        solicitud.setFecha(LocalDateTime.now());
        

        // Se abona el crédito directamente al saldo de la cuenta del cliente
       /*  if (usuario.getCuentas() != null && !usuario.getCuentas().isEmpty()) {
            CuentaEntity cuenta = usuario.getCuentas().get(0);
            cuenta.setSaldo(cuenta.getSaldo() + monto);
            cuentaRepository.save(cuenta);

            // Registrar movimiento de abono de crédito
            MovimientosEntity movimiento = new MovimientosEntity();
            movimiento.setCuentaOrigen("CRÉDITO-BANCO");
            movimiento.setCuentaDestino(cuenta.getClabe());
            movimiento.setMonto(monto);
            movimiento.setDescripcion("Abono de Crédito Autorizado");
            movimiento.setFecha(LocalDateTime.now());
            movimiento.setTipo("Credito");
            movimiento.setEstadoMovimiento("AUTORIZADO");
            movimientoRepository.save(movimiento);
        }*/

        return solicitudCreditoRepository.save(solicitud);
    }
    // Autoriza una solicitud de crédito PENDIENTE: aquí es donde se abona el crédito al saldo del cliente
    @Transactional(rollbackFor = Exception.class)
    public void autorizarSolicitudCredito(Long solicitudId) {
        SolicitudCreditoEntity solicitud = solicitudCreditoRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada."));

        if (!"PENDIENTE".equals(solicitud.getEstado())) {
            throw new RuntimeException("Esta solicitud ya fue procesada (" + solicitud.getEstado() + ").");
        }

        UsuarioEntity usuario = solicitud.getUsuario();
        CuentaEntity cuenta = usuario.getCuentas().get(0);
        cuenta.setSaldo(cuenta.getSaldo() + solicitud.getMontoSolicitado());
        cuentaRepository.save(cuenta);
        solicitud.setSaldoPendiente(solicitud.getMontoSolicitado());

        MovimientosEntity movimiento = new MovimientosEntity();
        movimiento.setCuentaOrigen("CRÉDITO-BANCO");
        movimiento.setCuentaDestino(cuenta.getClabe());
        movimiento.setMonto(solicitud.getMontoSolicitado());
        movimiento.setDescripcion("Abono de Crédito Autorizado");
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setTipo("Credito");
        movimiento.setEstadoMovimiento("AUTORIZADO");
        movimientoRepository.save(movimiento);

        solicitud.setEstado("APROBADO");
        solicitudCreditoRepository.save(solicitud);
    }

    // Rechaza una solicitud de crédito PENDIENTE. No mueve saldo.
    @Transactional(rollbackFor = Exception.class)
    public void rechazarSolicitudCredito(Long solicitudId) {
        SolicitudCreditoEntity solicitud = solicitudCreditoRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada."));

        if (!"PENDIENTE".equals(solicitud.getEstado())) {
            throw new RuntimeException("Esta solicitud ya fue procesada (" + solicitud.getEstado() + ").");
        }

        solicitud.setEstado("RECHAZADO");
        solicitudCreditoRepository.save(solicitud);
    }

    // Registra un abono al crédito aprobado del cliente y descuenta el monto de su cuenta
    @Transactional(rollbackFor = Exception.class)
    public void abonarCredito(String username, Long creditoId, Double monto) {
        if (monto == null || monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero.");
        }

        UsuarioEntity usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        if (usuario.getCuentas() == null || usuario.getCuentas().isEmpty()) {
            throw new RuntimeException("El usuario no tiene una cuenta bancaria asignada.");
        }

        SolicitudCreditoEntity credito = solicitudCreditoRepository.findById(creditoId)
                .orElseThrow(() -> new RuntimeException("Crédito no encontrado."));

        if (credito.getUsuario() == null || !credito.getUsuario().getId().equals(usuario.getId())) {
            throw new RuntimeException("El crédito seleccionado no pertenece al usuario autenticado.");
        }

        if (!"APROBADO".equals(credito.getEstado())) {
            throw new RuntimeException("Solo se puede abonar a un crédito aprobado.");
        }

        double saldoPendiente = obtenerSaldoPendienteCredito(credito);
        if (saldoPendiente <= 0) {
            credito.setEstado("PAGADO");
            credito.setSaldoPendiente(0.0);
            solicitudCreditoRepository.save(credito);
            throw new RuntimeException("Este crédito ya se encuentra pagado.");
        }

        if (monto > saldoPendiente) {
            throw new RuntimeException("El abono no puede ser mayor al saldo pendiente del crédito.");
        }

        CuentaEntity cuenta = usuario.getCuentas().get(0);
        if (cuenta.getSaldo() < monto) {
            throw new FondosInsuficientesException("No cuentas con saldo suficiente para realizar este abono.");
        }

        cuenta.setSaldo(cuenta.getSaldo() - monto);
        cuentaRepository.save(cuenta);

        double nuevoSaldoPendiente = saldoPendiente - monto;
        if (nuevoSaldoPendiente < 0.01) {
            nuevoSaldoPendiente = 0;
        }
        credito.setSaldoPendiente(nuevoSaldoPendiente);
        if (nuevoSaldoPendiente == 0) {
            credito.setEstado("PAGADO");
        }
        solicitudCreditoRepository.save(credito);

        MovimientosEntity movimiento = new MovimientosEntity();
        movimiento.setCuentaOrigen(cuenta.getClabe());
        movimiento.setCuentaDestino("CREDITO-BANCO");
        movimiento.setMonto(monto);
        movimiento.setDescripcion("Abono a Crédito");
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setTipo("Abono Credito");
        movimiento.setEstadoMovimiento("AUTORIZADO");
        movimientoRepository.save(movimiento);
    }

    // Lista todas las solicitudes de crédito
    public List<SolicitudCreditoEntity> todasLasSolicitudesCredito() {
        return solicitudCreditoRepository.findAllByOrderByFechaDesc();
    }

    public List<SolicitudCreditoEntity> creditosAprobadosPorUsuario(String username) {
        UsuarioEntity usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        List<SolicitudCreditoEntity> creditos = solicitudCreditoRepository.findByUsuarioAndEstadoOrderByFechaDesc(usuario, "APROBADO");
        for (SolicitudCreditoEntity credito : creditos) {
            if (credito.getSaldoPendiente() == null) {
                credito.setSaldoPendiente(credito.getMontoSolicitado());
            }
        }
        return creditos;
    }

    public SolicitudCreditoEntity creditoActivoPorUsuario(String username) {
        List<SolicitudCreditoEntity> creditos = creditosAprobadosPorUsuario(username);
        if (creditos.isEmpty()) {
            return null;
        }
        return creditos.get(0);
    }

    private double obtenerSaldoPendienteCredito(SolicitudCreditoEntity credito) {
        if (credito.getSaldoPendiente() == null) {
            return credito.getMontoSolicitado();
        }
        return credito.getSaldoPendiente();
    }

    private String generarClabeUnica() {
        Random random = new Random();
        String clabe;
        do {
            // Estructura: 012 (código banco simulado) + 15 dígitos aleatorios
            StringBuilder sb = new StringBuilder("012");
            for (int i = 0; i < 15; i++) {
                sb.append(random.nextInt(10));
            }
            clabe = sb.toString();
        } while (cuentaRepository.findByClabe(clabe).isPresent());
        return clabe;
    }

    //metodo para buscar los movimientos
    public List<MovimientosEntity>todosMovimientos(){
        return movimientoRepository.findAll();
    }

    // obtener movimiento por id
    public MovimientosEntity obtenerMovimientoId(Long id){
        return movimientoRepository.findById(id).orElse(null);
    }

    //metodo para modificar estado del movimiento
    public void actualizarMovimiento(Long id, String nuevoEstado){
        MovimientosEntity movimiento = obtenerMovimientoId(id);
        if(movimiento != null){
            movimiento.setEstadoMovimiento(nuevoEstado);
            movimientoRepository.save(movimiento);
        }
    }

    //eliminar movimiento
    public void eliminarMovimiento(Long id){
        movimientoRepository.deleteById(id);
    }

    //cancelar un movimiento y revertir el saldo
@Transactional(rollbackFor = Exception.class)
public void cancelarMovimiento(Long id) {
    MovimientosEntity movimiento = obtenerMovimientoId(id);
    if (movimiento == null) {
        throw new RuntimeException("Movimiento no encontrado.");
    }
    if ("CANCELADO".equals(movimiento.getEstadoMovimiento())) {
        throw new RuntimeException("El movimiento ya se encuentra cancelado.");
    }

    revertirSaldo(movimiento);

    movimiento.setEstadoMovimiento("CANCELADO");
    movimientoRepository.save(movimiento);
}

private void revertirSaldo(MovimientosEntity movimiento) {
    if ("CRÉDITO-BANCO".equals(movimiento.getCuentaOrigen())) {
        // Es un abono de crédito: solo se resta al cliente
        CuentaEntity cuenta = cuentaRepository.findByClabe(movimiento.getCuentaDestino())
                .orElseThrow(() -> new RuntimeException("Cuenta no existe."));
        if (cuenta.getSaldo() < movimiento.getMonto()) {
            throw new RuntimeException("No se puede cancelar: el cliente ya no tiene fondos suficientes.");
        }
        cuenta.setSaldo(cuenta.getSaldo() - movimiento.getMonto());
        cuentaRepository.save(cuenta);
    } else {
        // Es una transferencia: se regresa de destino a origen
        CuentaEntity origen = cuentaRepository.findByClabe(movimiento.getCuentaOrigen())
                .orElseThrow(() -> new RuntimeException("Cuenta origen no existe."));
        CuentaEntity destino = cuentaRepository.findByClabe(movimiento.getCuentaDestino())
                .orElseThrow(() -> new RuntimeException("Cuenta destino no existe."));

        if (destino.getSaldo() < movimiento.getMonto()) {
            throw new RuntimeException("No se puede cancelar: la cuenta destino ya no tiene fondos suficientes.");
        }
        destino.setSaldo(destino.getSaldo() - movimiento.getMonto());
        origen.setSaldo(origen.getSaldo() + movimiento.getMonto());
        cuentaRepository.save(destino);
        cuentaRepository.save(origen);
    }
}


    //eliminar usuario
    public void eliminarUsuario(long id){
        usuarioRepository.deleteById(id);
    }


// mapea CLABE -> nombre del cliente, útil para mostrar en vistas de movimientos
public Map<String, String> mapaClabeNombre() {
    List<CuentaEntity> cuentas = cuentaRepository.findAll();
    Map<String, String> mapa = new HashMap<>();
    for (CuentaEntity c : cuentas) {
        if (c.getUsuario() != null) {
            mapa.put(c.getClabe(), c.getUsuario().getNombre());
        }
    }
    return mapa;
}

// actualizar datos de un cliente
@Transactional(rollbackFor = Exception.class)
public void actualizarUsuario(Long id, String nombre, String username, String password) {
    UsuarioEntity usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

    // Validar que el nuevo username no esté en uso por otro usuario
    if (username != null && !username.equals(usuario.getUsername())) {
        usuarioRepository.findByUsername(username).ifPresent(u -> {
            if (!u.getId().equals(id)) {
                throw new RuntimeException("El nombre de usuario ya está registrado por otro cliente.");
            }
        });
        usuario.setUsername(username);
    }

    usuario.setNombre(nombre);

    // Solo actualizar contraseña si se proporcionó una nueva
    if (password != null && !password.isBlank()) {
        usuario.setPassword(passwordEncoder.encode(password));
    }

    usuarioRepository.save(usuario);

        CuentaEntity cuenta = usuario.getCuentas().get(0);
        cuentaRepository.save(cuenta);
}

}
