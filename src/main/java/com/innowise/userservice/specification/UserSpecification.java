package com.innowise.userservice.specification;

import com.innowise.userservice.entity.User;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class UserSpecification {

    public static Specification<User> nameContains(String name) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(name)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")),
                    "%" + name.toLowerCase() + "%"
            );
        };
    }

    public static Specification<User> surnameContains(String surname) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(surname)) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("surname")),
                    "%" + surname.toLowerCase() + "%"
            );
        };
    }

    public static Specification<User> isActive(Boolean active) {
        return (root, query, criteriaBuilder) -> {
            if (active == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("active"), active);
        };
    }

    public static Specification<User> filterByNameAndSurname(String name, String surname) {
        return Specification.where(nameContains(name))
                .and(surnameContains(surname));
    }

    public static Specification<User> fullFilter(String name, String surname, Boolean active) {
        return Specification.where(nameContains(name))
                .and(surnameContains(surname))
                .and(isActive(active));
    }
}
