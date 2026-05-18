package com.innowise.userservice.mapper;

import com.innowise.userservice.dto.request.CreateCardRequest;
import com.innowise.userservice.dto.request.UpdateCardRequest;
import com.innowise.userservice.dto.response.CardResponse;
import com.innowise.userservice.entity.PaymentCard;
import com.innowise.userservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        imports = {UUID.class})
public interface CardMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", expression = "java(createUserReference(request.getUserId()))")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PaymentCard toEntity(CreateCardRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(UpdateCardRequest request, @MappingTarget PaymentCard card);

    @Mapping(target = "userId", source = "user.id")
    CardResponse toResponse(PaymentCard card);

    List<CardResponse> toResponseList(List<PaymentCard> cards);

    default User createUserReference(String userId) {
        if (userId == null) {
            return null;
        }
        User user = new User();
        user.setId(UUID.fromString(userId));
        return user;
    }
}
