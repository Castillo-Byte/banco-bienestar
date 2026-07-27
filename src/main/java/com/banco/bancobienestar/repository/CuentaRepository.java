package com.banco.bancobienestar.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.banco.bancobienestar.entity.CuentaEntity;
import com.banco.bancobienestar.entity.UsuarioEntity;

@Repository
public interface CuentaRepository extends JpaRepository<CuentaEntity, Long> {
    Optional<CuentaEntity> findByClabe(String clabe);
   List<CuentaEntity> findByUsuario(UsuarioEntity usuario);
    
}
