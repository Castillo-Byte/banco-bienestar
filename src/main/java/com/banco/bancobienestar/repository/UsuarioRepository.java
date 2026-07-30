package com.banco.bancobienestar.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.banco.bancobienestar.entity.UsuarioEntity;


@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioEntity,Long> {
   Optional<UsuarioEntity> findByUsername(String username);
   Optional<UsuarioEntity> findByNombre(String nombre);

   Page<UsuarioEntity> findByRol(String rol, Pageable pageable);

    // Lista paginada de clientes filtrando por nombre o username
    @Query("SELECT u FROM UsuarioEntity u WHERE u.rol = :rol " +
           "AND (LOWER(u.nombre) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<UsuarioEntity> buscarClientes(@Param("rol") String rol,
                                        @Param("q") String q,
                                        Pageable pageable);

   Optional<UsuarioEntity> findByCuentas_Clabe(String clabe);

}
