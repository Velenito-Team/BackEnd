package co.edu.udistrital.backend.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("ventas")
@CompoundIndex(name = "venta_dia_estado", def = "{'dia': 1, 'estado': 1}")
public class Sale {
    @Id
    private String id;
    private String cajaDiaId;
    private String dia;
    private String por;
    private Instant fecha;
    private List<SaleLine> lineas = new ArrayList<>();
    private BigDecimal total = BigDecimal.ZERO;
    private PaymentMethod metodo;
    private BigDecimal recibido;
    private BigDecimal cambio;
    private String clienteId;
    private SaleStatus estado = SaleStatus.confirmada;

    public Sale() {
    }

    public void recalculateTotal() {
        total = lineas.stream().map(SaleLine::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String getId() {
        return id;
    }

    public void setId(String v) {
        id = v;
    }

    public String getCajaDiaId() {
        return cajaDiaId;
    }

    public void setCajaDiaId(String v) {
        cajaDiaId = v;
    }

    public String getDia() {
        return dia;
    }

    public void setDia(String v) {
        dia = v;
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

    public List<SaleLine> getLineas() {
        return lineas;
    }

    public void setLineas(List<SaleLine> v) {
        lineas = v;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal v) {
        total = v;
    }

    public PaymentMethod getMetodo() {
        return metodo;
    }

    public void setMetodo(PaymentMethod v) {
        metodo = v;
    }

    public BigDecimal getRecibido() {
        return recibido;
    }

    public void setRecibido(BigDecimal v) {
        recibido = v;
    }

    public BigDecimal getCambio() {
        return cambio;
    }

    public void setCambio(BigDecimal v) {
        cambio = v;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String v) {
        clienteId = v;
    }

    public SaleStatus getEstado() {
        return estado;
    }

    public void setEstado(SaleStatus v) {
        estado = v;
    }
}
