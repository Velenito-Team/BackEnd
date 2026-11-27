package co.edu.udistrital.backend.service;

import co.edu.udistrital.backend.repository.SaleHistoryRepository;
import co.edu.udistrital.backend.repository.SalesQueryRepository;
import java.time.LocalDate;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SaleHistoryService {
    private final SalesQueryRepository queries;
    private final SaleHistoryRepository repository;
    private final OperatorService operator;
    private final SaleService sales;

    public SaleHistoryService(SalesQueryRepository queries, SaleHistoryRepository repository,
            OperatorService operator, SaleService sales) {
        this.queries = queries;
        this.repository = repository;
        this.operator = operator;
        this.sales = sales;
    }

    public List<SaleService.Receipt> list(LocalDate from, LocalDate to, String method, String status, String product) {
        return queries.between(from, to, method, status, product).stream().map(SaleService::receipt).toList();
    }

    public SaleService.CreatedReceipt open(String id) {
        return sales.renew(new ObjectId(id), operator.id());
    }

    public SaleService.Receipt cancel(String id, String reason) {
        if (reason == null || reason.isBlank())
            throw new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Indica el motivo de anulación.");
        return SaleService.receipt(repository.cancel(new ObjectId(id), operator.id(), reason));
    }
}
