package co.edu.udistrital.backend.model;

import java.math.BigDecimal;

public class SaleLine {
    private String productoId;
    private String nombre;
    private Unit unidad;
    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;

    public SaleLine() {
    }

    public SaleLine(String productoId, String nombre, Unit unidad, BigDecimal cantidad, BigDecimal precioUnitario) {
        this.productoId = productoId;
        this.nombre = nombre;
        this.unidad = unidad;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotal = cantidad.multiply(precioUnitario);
    }

    public String getProductoId() {
        return productoId;
    }

    public void setProductoId(String v) {
        productoId = v;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String v) {
        nombre = v;
    }

    public Unit getUnidad() {
        return unidad;
    }

    public void setUnidad(Unit v) {
        unidad = v;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal v) {
        cantidad = v;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal v) {
        precioUnitario = v;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal v) {
        subtotal = v;
    }
}
