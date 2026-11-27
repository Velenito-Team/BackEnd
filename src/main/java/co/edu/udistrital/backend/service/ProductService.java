package co.edu.udistrital.backend.service;

import co.edu.udistrital.backend.model.Product;
import co.edu.udistrital.backend.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {
    private final ProductRepository repository;
    private final OperatorService operator;
    private final co.edu.udistrital.backend.repository.AuditRepository audits;

    public ProductService(ProductRepository repository, OperatorService operator,
            co.edu.udistrital.backend.repository.AuditRepository audits) {
        this.repository = repository;
        this.operator = operator;
        this.audits = audits;
    }

    public List<Product> findAll() {
        return repository.findAll();
    }

    public List<Product> search(String name) {
        return name == null || name.isBlank()
                ? findAll()
                : repository.findByActivoTrueAndNombreNormalizadoContaining(name.trim().toLowerCase());
    }

    public Product findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    @org.springframework.transaction.annotation.Transactional
    public Product create(Product product) {
        validate(product);
        product.setId(null);
        var now = java.time.Instant.now();
        product.setCreadoEn(now);
        product.setActualizadoEn(now);
        Product saved = repository.save(product);
        audits.saveProduct(operator.id(), "crear_producto", saved, null);
        return saved;
    }

    @org.springframework.transaction.annotation.Transactional
    public Product update(String id, Product data) {
        Product current = findById(id);
        validate(data);
        if (current.getStock().compareTo(data.getStock()) != 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El stock cambió. Actualiza el producto. Las entradas se registran en Inventario.");
        if (current.getUnidad() != data.getUnidad())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La unidad de un producto registrado se conserva para mantener su historial.");
        data.setId(current.getId());
        data.setCreadoEn(current.getCreadoEn());
        data.setActualizadoEn(java.time.Instant.now());
        Product saved = repository.save(data);
        audits.saveProduct(operator.id(), "editar_producto", saved, current);
        return saved;
    }

    @org.springframework.transaction.annotation.Transactional
    public void delete(String id) {
        Product product = findById(id);
        if (!product.isActivo())
            return;
        product.setActivo(false);
        product.setActualizadoEn(java.time.Instant.now());
        repository.save(product);
        audits.saveProduct(operator.id(), "desactivar_producto", product, null);
    }

    private void validate(Product product) {
        if (product == null || product.getNombre() == null || product.getNombre().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del producto es obligatorio");
        }
        if (product.getPrecioVenta() == null || product.getPrecioVenta().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El precio de venta debe ser positivo");
        }
        if (product.getPrecioCompra() == null || product.getPrecioCompra().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El precio de compra debe ser positivo");
        }
        if (product.getStock() == null)
            product.setStock(BigDecimal.ZERO);
        if (product.getStock().signum() < 0
                || (product.getUmbralMinimo() != null && product.getUmbralMinimo().signum() < 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El stock y el umbral deben ser no negativos");
        }
        product.setNombre(product.getNombre().strip());
        if (product.getCategoria() == null || product.getCategoria().isBlank() || product.getUnidad() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoría y unidad son obligatorias");
        }
        product.setCategoria(product.getCategoria().strip());
        if (product.getStock().stripTrailingZeros().scale() > 3
                || (!product.isFraccionable() && product.getStock().stripTrailingZeros().scale() > 0)
                || (product.getUmbralMinimo() != null && product.getUmbralMinimo().stripTrailingZeros().scale() > 3))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Revisa el stock, fraccionamiento y umbral. Se admiten hasta tres decimales.");
        new org.bson.types.Decimal128(product.getPrecioCompra());
        new org.bson.types.Decimal128(product.getPrecioVenta());
        new org.bson.types.Decimal128(product.getStock());
    }

}
