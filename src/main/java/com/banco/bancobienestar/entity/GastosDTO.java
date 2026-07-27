package com.banco.bancobienestar.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GastosDTO {
private String categoria;
private Double monto;
private String colorHex;

}
