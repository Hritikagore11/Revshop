package revshop.product_service.product.service;

import org.springframework.stereotype.Service;
import revshop.product_service.product.model.Category;
import revshop.product_service.product.repository.CategoryRepository;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category createCategory(Category category) {

        if (category.getName() == null ||
                category.getName().trim().isEmpty()) {

            throw new RuntimeException("Category name is required");
        }

        if (categoryRepository.existsByNameIgnoreCase(category.getName())) {
            throw new RuntimeException("Category already exists");
        }

        category.setName(category.getName().trim());

        return categoryRepository.save(category);
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Category not found"));
    }

    public Category updateCategory(Long id, Category updatedCategory) {

        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Category not found"));

        if (updatedCategory.getName() == null ||
                updatedCategory.getName().trim().isEmpty()) {

            throw new RuntimeException("Category name is required");
        }

        existingCategory.setName(updatedCategory.getName().trim());

        return categoryRepository.save(existingCategory);
    }

    public void deleteCategory(Long id) {

        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Category not found");
        }

        categoryRepository.deleteById(id);
    }
}