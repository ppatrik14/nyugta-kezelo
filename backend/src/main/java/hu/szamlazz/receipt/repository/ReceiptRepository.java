package hu.szamlazz.receipt.repository;

import hu.szamlazz.receipt.entity.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Nyugta repository – adatbázis műveletek a Receipt entitásra.
 */
@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    /**
     * Nyugták listázása létrehozás dátuma szerint csökkenő sorrendben.
     */
    List<Receipt> findAllByOrderByCreatedAtDesc();
}
