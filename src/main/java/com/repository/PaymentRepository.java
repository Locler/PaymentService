package com.repository;

import com.entity.Payment;
import com.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByUserId(Long userId);

    List<Payment> findByOrderId(Long orderId);

    List<Payment> findByStatus(PaymentStatus status);

    @Query("""
        select sum(p.paymentAmount)
        from Payment p
        where p.userId = :userId
          and p.createdAt between :from and :to
    """)
    BigDecimal sumForUserByDateRange(
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
        select sum(p.paymentAmount)
        from Payment p
        where p.createdAt between :from and :to
    """)
    BigDecimal sumForAllByDateRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
