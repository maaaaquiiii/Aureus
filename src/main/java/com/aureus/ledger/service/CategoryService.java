package com.aureus.ledger.service;

import com.aureus.ledger.api.CategoryResponse;
import com.aureus.ledger.domain.CategoryRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll()
                .stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getIcon(), c.getColor()))
                .toList();
    }
}