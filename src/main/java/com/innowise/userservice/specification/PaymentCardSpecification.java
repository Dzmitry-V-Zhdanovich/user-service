package com.innowise.userservice.specification;

import com.innowise.userservice.entity.PaymentCard;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.UUID;

public class PaymentCardSpecification {

    public static Specification<PaymentCard> byUserId(UUID userId) {
        return (root, query, criteriaBuilder) -> {
            if (userId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("user").get("id"), userId);
        };
    }

    public static Specification<PaymentCard> isActive(Boolean active) {
        return (root, query, criteriaBuilder) -> {
            if (active == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("active"), active);
        };
    }

    public static Specification<PaymentCard> numberContains(String number) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(number)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(root.get("number"), "%" + number + "%");
        };
    }

    public static Specification<PaymentCard> filter(UUID userId, Boolean active, String number) {
        return Specification.where(byUserId(userId))
                .and(isActive(active))
                .and(numberContains(number));
    }
}
