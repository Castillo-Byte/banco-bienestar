package com.banco.bancobienestar.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.banco.bancobienestar.entity.MovimientosEntity;

@Repository
public interface MovimientoCuentaRepository extends JpaRepository<MovimientosEntity,Long> {

    List<MovimientosEntity> findByCuentaOrigenOrCuentaDestinoOrderByFechaDesc(String cuentaOrigen, String cuentaDestino);

    List<MovimientosEntity> findByCuentaOrigen(String cuentaOrigen);
    
} 
