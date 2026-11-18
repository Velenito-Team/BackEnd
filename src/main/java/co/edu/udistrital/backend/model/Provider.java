package co.edu.udistrital.backend.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("proveedores")
public class Provider {
    @Id
    private String id;
    @Indexed(unique = true)
    private String nombreNormalizado;
    private String nombre;
    private String telefono;
    private String por;
    private Instant fecha;

    public Provider() {
    }

    public Provider(String nombre, String telefono, String por) {
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
