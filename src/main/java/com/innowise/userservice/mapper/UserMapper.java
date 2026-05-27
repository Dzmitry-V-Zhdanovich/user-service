package com.innowise.userservice.mapper;

import com.innowise.userservice.dto.request.CreateUserRequest;
import com.innowise.userservice.dto.request.UpdateUserRequest;
import com.innowise.userservice.dto.response.UserResponse;
import com.innowise.userservice.dto.response.UserWithCardsResponse;
import com.innowise.userservice.entity.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    User toEntity(CreateUserRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "paymentCards", ignore = true)
    void updateEntityFromRequest(UpdateUserRequest request, @MappingTarget User user);

    UserResponse toResponse(User user);

    List<UserResponse> toResponseList(List<User> users);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromUser(@MappingTarget User target, User source);

    @Mapping(target = "cardsCount", source = "cards", qualifiedByName = "mapCardsCount")
    UserResponse toResponse(UserWithCardsResponse cachedUser);

    @Named("mapCardsCount")
    default int mapCardsCount(List<?> cards) {
        return cards != null ? cards.size() : 0;
    }
}
