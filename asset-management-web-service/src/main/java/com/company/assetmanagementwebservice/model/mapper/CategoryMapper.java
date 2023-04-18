package com.company.assetmanagementwebservice.model.mapper;

import com.company.assetmanagementwebservice.entity.Category;
import com.company.assetmanagementwebservice.model.dto.CategoryDTO;

public class CategoryMapper {
	
	//map from Category to CategoryDTO
	public CategoryDTO fromEntity(Category category) {
		CategoryDTO dto = new CategoryDTO();
		dto.setId(category.getId());
		dto.setPrefix(category.getPrefix());
		dto.setName(category.getName());
		return dto;
	}
	
	//map from CategoryDTO to Category
	public Category fromDTO(CategoryDTO payload) {
		Category category = new Category();
		category.setId(payload.getId());
		category.setPrefix(payload.getPrefix());
		category.setName(payload.getName());
		return category;
	}
}
