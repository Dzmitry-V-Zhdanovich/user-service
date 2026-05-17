package com.innowise.userservice.repository;

import com.innowise.userservice.entity.PaymentCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, UUID>, JpaSpecificationExecutor<PaymentCard> {

    List<PaymentCard> findByUserId(UUID userId);

    Page<PaymentCard> findByUserId(UUID userId, Pageable pageable);

    List<PaymentCard> findByUserIdAndActiveTrue(UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE PaymentCard pc SET pc.active = :active WHERE pc.id = :cardId")
    void setActiveStatus(@Param("cardId") UUID cardId, @Param("active") Boolean active);

    @Modifying
    @Transactional
    @Query("UPDATE PaymentCard pc SET pc.active = false WHERE pc.user.id = :userId")
    void deactivateAllByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(pc) FROM PaymentCard pc WHERE pc.user.id = :userId AND pc.active = true")
    int countActiveCardsByUserId(@Param("userId") UUID userId);

    int countByUserId(UUID userId);

    boolean existsByNumber(String number);

    Optional<PaymentCard> findByNumber(String number);
}
