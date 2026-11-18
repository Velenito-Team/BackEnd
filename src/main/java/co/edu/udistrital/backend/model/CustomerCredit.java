package co.edu.udistrital.backend.model;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("clientesFiado")
public class CustomerCredit {
    @Id
    private String id;
    private String nombre;
    private String nombreNormalizado;
    private String telefono;
    private BigDecimal saldo = BigDecimal.ZERO;
    private String por;
    private Instant fecha;

    public CustomerCredit() {
    }

    public CustomerCredit(String nombre, String telefono, String por) {
        this.nombre = nombre;
        this.nombreNormalizado = nombre.trim().toLowerCase();
        this.telefono = telefono;
        this.por = por;
        this.fecha = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String v) {
        id = v;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String v) {
        nombre = v;
        nombreNormalizado = v == null ? null : v.trim().toLowerCase();
    }

    public String getNombreNormalizado() {
        return nombreNormalizado;
    }

    public void setNombreNormalizado(String v) {
        nombreNormalizado = v;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String v) {
        telefono = v;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal v) {
        saldo = v;
    }

    public String getPor() {
        return por;
    }

    public void setPor(String v) {
        por = v;
    }

    public Instant getFecha() {
        return fecha;
    }

    public void setFecha(Instant v) {
        fecha = v;
    }
}
