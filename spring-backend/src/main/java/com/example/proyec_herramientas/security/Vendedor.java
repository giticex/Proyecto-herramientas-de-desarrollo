package com.example.proyec_herramientas.security;

/**
 * Cuenta del panel que atendio una venta. Se guarda como snapshot en el
 * comprobante para que el arqueo conserve al vendedor aunque la cuenta se
 * renombre o deje de estar activa.
 */
public record Vendedor(String usuario, String nombre) {
}
