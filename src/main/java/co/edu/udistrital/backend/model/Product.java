package co.edu.udistrital.backend.model;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("productos")
@CompoundIndex(name = "producto_nombre_categoria", def = "{'nombreNormalizado': 1, 'categoriaNormalizada': 1}", unique = true)
public class Product {
    @Id
    private String id;
    private String nombre;
    private String nombreNormalizado;
    private String categoria;
    private String categoriaNormalizada;
    private BigDecimal precioCompra;
    private BigDecimal precioVenta;
    private Unit unidad;
    private boolean fraccionable;
    private BigDecimal stock;
    private BigDecimal umbralMinimo;
    private boolean activo = true;
    private Instant creadoEn;
    private Instant actualizadoEn;

    public Product() {
    }

    public Product(String nombre, String categoria, BigDecimal precioCompra, BigDecimal precioVenta, Unit unidad) {
        this.nombre = nombre;
        this.nombreNormalizado = normalize(nombre);
        this.categoria = categoria;
        this.categoriaNormalizada = normalize(categoria);
        this.precioCompra = precioCompra;
        this.precioVenta = precioVenta;
        this.unidad = unidad;
        this.stock = BigDecimal.ZERO;
        this.creadoEn = Instant.now();
        this.actualizadoEn = this.creadoEn;
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String v) {
        nombre = v;
        nombreNormalizado = normalize(v);
    }

    public String getNombreNormalizado() {
        return nombreNormalizado;
    }

    public void setNombreNormalizado(String v) {
        nombreNormalizado = v;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String v) {
        categoria = v;
        categoriaNormalizada = normalize(v);
    }

    public String getCategoriaNormalizada() {
        return categoriaNormalizada;
    }

    public void setCategoriaNormalizada(String v) {
        categoriaNormalizada = v;
    }

    public BigDecimal getPrecioCompra() {
        return precioCompra;
    }

    public void setPrecioCompra(BigDecimal v) {
        precioCompra = v;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal v) {
        precioVenta = v;
    }

    public Unit getUnidad() {
        return unidad;
    }

    public void setUnidad(Unit v) {
        unidad = v;
    }

    public boolean isFraccionable() {
        return fraccionable;
    }

    public void setFraccionable(boolean v) {
        fraccionable = v;
    }

    public BigDecimal getStock() {
        return stock;
    }

    public void setStock(BigDecimal v) {
        stock = v;
    }

    public BigDecimal getUmbralMinimo() {
        return umbralMinimo;
    }

    public void setUmbralMinimo(BigDecimal v) {
        umbralMinimo = v;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean v) {
        activo = v;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Instant v) {
        creadoEn = v;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(Instant v) {
        actualizadoEn = v;
    }
}
