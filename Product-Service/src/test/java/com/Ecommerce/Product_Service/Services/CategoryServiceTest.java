package com.Ecommerce.Product_Service.Services;

import com.Ecommerce.Product_Service.Entities.Category;
import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Repositories.CategoryRepository;
import com.Ecommerce.Product_Service.Repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Category Service Tests")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category rootCategory;
    private Category subCategory;
    private Product testProduct;
    private UUID rootCategoryId;
    private UUID subCategoryId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        rootCategoryId = UUID.randomUUID();
        subCategoryId = UUID.randomUUID();
        productId = UUID.randomUUID();

        rootCategory = createTestCategory(rootCategoryId, "Electronics", null, 0);
        subCategory = createTestCategory(subCategoryId, "Smartphones", rootCategoryId, 1);
        testProduct = createTestProduct();
    }

    @Test
    @DisplayName("Should find all categories")
    void findAllCategories_ShouldReturnAllCategories() {
        // Given
        List<Category> expectedCategories = Arrays.asList(rootCategory, subCategory);
        when(categoryRepository.findAll()).thenReturn(expectedCategories);

        // When
        List<Category> actualCategories = categoryService.findAllCategories();

        // Then
        assertThat(actualCategories).hasSize(2);
        assertThat(actualCategories).containsExactlyElementsOf(expectedCategories);
        verify(categoryRepository).findAll();
    }

    @Test
    @DisplayName("Should find category by ID")
    void findCategoryById_WhenCategoryExists_ShouldReturnCategory() {
        // Given
        when(categoryRepository.findById(rootCategoryId)).thenReturn(Optional.of(rootCategory));

        // When
        Optional<Category> result = categoryService.findCategoryById(rootCategoryId);

        // Then
        assertTrue(result.isPresent());
        assertThat(result.get()).isEqualTo(rootCategory);
        verify(categoryRepository).findById(rootCategoryId);
    }

    @Test
    @DisplayName("Should return empty when category not found")
    void findCategoryById_WhenCategoryNotExists_ShouldReturnEmpty() {
        // Given
        when(categoryRepository.findById(rootCategoryId)).thenReturn(Optional.empty());

        // When
        Optional<Category> result = categoryService.findCategoryById(rootCategoryId);

        // Then
        assertTrue(result.isEmpty());
        verify(categoryRepository).findById(rootCategoryId);
    }

    @Test
    @DisplayName("Should find root categories")
    void findRootCategories_ShouldReturnCategoriesWithoutParent() {
        // Given
        List<Category> rootCategories = Arrays.asList(rootCategory);
        when(categoryRepository.findByParentIdIsNull()).thenReturn(rootCategories);

        // When
        List<Category> result = categoryService.findRootCategories();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(rootCategory);
        verify(categoryRepository).findByParentIdIsNull();
    }

    @Test
    @DisplayName("Should find subcategories")
    void findSubcategories_ShouldReturnChildCategories() {
        // Given
        List<Category> subcategories = Arrays.asList(subCategory);
        when(categoryRepository.findByParentId(rootCategoryId)).thenReturn(subcategories);

        // When
        List<Category> result = categoryService.findSubcategories(rootCategoryId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(subCategory);
        verify(categoryRepository).findByParentId(rootCategoryId);
    }

    @Test
    @DisplayName("Should find categories by level")
    void findCategoriesByLevel_ShouldReturnCategoriesAtSpecificLevel() {
        // Given
        List<Category> levelZeroCategories = Arrays.asList(rootCategory);
        when(categoryRepository.findByLevel(0)).thenReturn(levelZeroCategories);

        // When
        List<Category> result = categoryService.findCategoriesByLevel(0);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLevel()).isEqualTo(0);
        verify(categoryRepository).findByLevel(0);
    }

    @Test
    @DisplayName("Should add category successfully")
    void addCategory_WithValidData_ShouldCreateCategory() {
        // Given
        Category newCategory = createTestCategory(null, "New Category", null, null);
        Category savedCategory = createTestCategory(UUID.randomUUID(), "New Category", null, 0);

        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        // When
        Category result = categoryService.addCategory(newCategory);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("New Category");
        assertThat(result.getLevel()).isEqualTo(0);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("Should throw exception when adding category with invalid data")
    void addCategory_WithInvalidData_ShouldThrowException() {
        // Given
        Category invalidCategory = new Category();
        invalidCategory.setName(""); // Invalid empty name

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            categoryService.addCategory(invalidCategory);
        });

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("Should update category successfully")
    void updateCategory_WithValidData_ShouldUpdateCategory() {
        // Given
        Category updatedData = new Category();
        updatedData.setName("Updated Name");
        updatedData.setDescription("Updated Description");

        when(categoryRepository.findById(rootCategoryId)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(rootCategory);

        // When
        Optional<Category> result = categoryService.updateCategory(rootCategoryId, updatedData);

        // Then
        assertTrue(result.isPresent());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("Should delete category successfully")
    void deleteCategory_WhenCategoryHasNoConstraints_ShouldDeleteCategory() {
        // Given
        Category categoryToDelete = createTestCategory(rootCategoryId, "To Delete", null, 0);
        categoryToDelete.setProducts(new ArrayList<>()); // Empty products list

        when(categoryRepository.findById(rootCategoryId)).thenReturn(Optional.of(categoryToDelete));
        when(categoryRepository.findByParentId(rootCategoryId)).thenReturn(new ArrayList<>()); // No subcategories

        // When
        categoryService.deleteCategory(rootCategoryId);

        // Then
        verify(categoryRepository).deleteById(rootCategoryId);
    }

    @Test
    @DisplayName("Should throw exception when deleting category with products")
    void deleteCategory_WhenCategoryHasProducts_ShouldThrowException() {
        // Given
        rootCategory.setProducts(Arrays.asList(testProduct));
        when(categoryRepository.findById(rootCategoryId)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.findByParentId(rootCategoryId)).thenReturn(new ArrayList<>());

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            categoryService.deleteCategory(rootCategoryId);
        });

        verify(categoryRepository, never()).deleteById(rootCategoryId);
    }

    @Test
    @DisplayName("Should add product to category")
    void addProductToCategory_WithValidIds_ShouldAddProduct() {
        // Given
        when(categoryRepository.findById(rootCategoryId)).thenReturn(Optional.of(rootCategory));
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(categoryRepository.save(any(Category.class))).thenReturn(rootCategory);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        Optional<Category> result = categoryService.addProductToCategory(rootCategoryId, productId);

        // Then
        assertTrue(result.isPresent());
        verify(categoryRepository).save(any(Category.class));
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should remove product from category")
    void removeProductFromCategory_WithValidIds_ShouldRemoveProduct() {
        // Given
        rootCategory.getProducts().add(testProduct);
        testProduct.getCategories().add(rootCategory);

        when(categoryRepository.findById(rootCategoryId)).thenReturn(Optional.of(rootCategory));
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(categoryRepository.save(any(Category.class))).thenReturn(rootCategory);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        Optional<Category> result = categoryService.removeProductFromCategory(rootCategoryId, productId);

        // Then
        assertTrue(result.isPresent());
        verify(categoryRepository).save(any(Category.class));
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should calculate category level correctly")
    void calculateCategoryLevel_WithParent_ShouldReturnCorrectLevel() {
        // Given
        when(categoryRepository.findById(rootCategoryId)).thenReturn(Optional.of(rootCategory));

        // When
        int level = categoryService.calculateCategoryLevel(rootCategoryId);

        // Then
        assertThat(level).isEqualTo(1); // Parent level (0) + 1
    }

    @Test
    @DisplayName("Should return 0 for root category level")
    void calculateCategoryLevel_WithNullParent_ShouldReturnZero() {
        // When
        int level = categoryService.calculateCategoryLevel(null);

        // Then
        assertThat(level).isEqualTo(0);
    }

    @Test
    @DisplayName("Should check if category can be moved")
    void canMoveCategory_WithValidMove_ShouldReturnTrue() {
        // Given
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        // When
        boolean canMove = categoryService.canMoveCategory(sourceId, targetId);

        // Then
        assertTrue(canMove);
    }

    @Test
    @DisplayName("Should prevent circular reference when moving category")
    void canMoveCategory_WhenWouldCreateCircle_ShouldReturnFalse() {
        // Given - trying to move category to be its own parent
        UUID categoryId = UUID.randomUUID();

        // When
        boolean canMove = categoryService.canMoveCategory(categoryId, categoryId);

        // Then
        assertFalse(canMove);
    }

    @Test
    @DisplayName("Should check if category has subcategories")
    void hasSubcategories_WhenCategoryHasChildren_ShouldReturnTrue() {
        // Given
        when(categoryRepository.findByParentId(rootCategoryId)).thenReturn(Arrays.asList(subCategory));

        // When
        boolean hasSubcategories = categoryService.hasSubcategories(rootCategoryId);

        // Then
        assertTrue(hasSubcategories);
    }

    @Test
    @DisplayName("Should find categories with products")
    void findCategoriesWithProducts_ShouldReturnOnlyCategoriesWithProducts() {
        // Given
        rootCategory.setProducts(Arrays.asList(testProduct));
        subCategory.setProducts(new ArrayList<>());

        when(categoryRepository.findAll()).thenReturn(Arrays.asList(rootCategory, subCategory));

        // When
        List<Category> result = categoryService.findCategoriesWithProducts();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(rootCategory);
    }

    @Test
    @DisplayName("Should find empty categories")
    void findEmptyCategories_ShouldReturnCategoriesWithoutProducts() {
        // Given
        rootCategory.setProducts(Arrays.asList(testProduct));
        subCategory.setProducts(new ArrayList<>());

        when(categoryRepository.findAll()).thenReturn(Arrays.asList(rootCategory, subCategory));

        // When
        List<Category> result = categoryService.findEmptyCategories();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(subCategory);
    }

    private Category createTestCategory(UUID id, String name, UUID parentId, Integer level) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setParentId(parentId);
        category.setLevel(level);
        category.setDescription("Test description for " + name);
        category.setCreatedAt(LocalDateTime.now());
        category.setProducts(new ArrayList<>());
        return category;
    }

    private Product createTestProduct() {
        Product product = new Product();
        product.setId(productId);
        product.setName("Test Product");
        product.setCategories(new ArrayList<>());
        return product;
    }
}