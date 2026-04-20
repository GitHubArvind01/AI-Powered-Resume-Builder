package com.resumeai.aiservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.factory.Mappers;

import com.resumeai.aiservice.dto.AiRequestDTO;
import com.resumeai.aiservice.entity.AiRequest;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AiRequestMapper {

	AiRequestMapper INSTANCE = Mappers.getMapper(AiRequestMapper.class);

	AiRequestDTO toDTO(AiRequest entity);

	AiRequest toEntity(AiRequestDTO dto);
}

