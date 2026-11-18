package co.edu.udistrital.backend.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("usuarios")
public class User {
    @Id
    private String id;
    private String nombre;
    @Indexed(unique = true)
    private String usuarioNormalizado;
    private String usuario;
    private String contrasenaHash;
    private Role rol;
    private boolean activo = true;
    private int intentosFallidos;
    private Instant bloqueadoHasta;
    private Instant creadoEn = Instant.now();

    public User() {
    }

    public User(String nombre, String usuario, String contrasenaHash, Role rol) {
        this.nombre = nombre;
        this.usuario = usuario;
        this.usuarioNormalizado = usuario.trim().toLowerCase();
        this.contrasenaHash = contrasenaHash;
        this.rol = rol;
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
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String v) {
        usuario = v;
        usuarioNormalizado = v == null ? null : v.trim().toLowerCase();
    }

    public String getUsuarioNormalizado() {
        return usuarioNormalizado;
    }

    public void setUsuarioNormalizado(String v) {
        usuarioNormalizado = v;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public void setContrasenaHash(String v) {
        contrasenaHash = v;
    }

    public Role getRol() {
        return rol;
    }

    public void setRol(Role v) {
        rol = v;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean v) {
        activo = v;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public void setIntentosFallidos(int v) {
        intentosFallidos = v;
    }

    public Instant getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public void setBloqueadoHasta(Instant v) {
        bloqueadoHasta = v;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Instant v) {
        creadoEn = v;
    }
}
