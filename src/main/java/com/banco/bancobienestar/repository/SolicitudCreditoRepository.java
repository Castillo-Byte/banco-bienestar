package com.banco.bancobienestar.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.banco.bancobienestar.entity.SolicitudCreditoEntity;
import com.banco.bancobienestar.entity.UsuarioEntity;

@Repository
public interface SolicitudCreditoRepository extends JpaRepository<SolicitudCreditoEntity,Long> {

    List<SolicitudCreditoEntity> findByUsuarioOrderByFechaDesc (UsuarioEntity usuario);

    List<SolicitudCreditoEntity> findByUsuarioAndEstadoOrderByFechaDesc(UsuarioEntity usuario, String estado);

    List<SolicitudCreditoEntity> findAllByOrderByFechaDesc();

    List<SolicitudCreditoEntity> findByUsuarioAndEstadoIn(UsuarioEntity usuario, List<String> estados);

}
