package com.banco.bancobienestar.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MovimientosDTO {
    private String categoria;
    private Long cantidad;
    private String colorHex;
}