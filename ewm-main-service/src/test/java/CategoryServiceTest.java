

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.Main;
import ru.practicum.dto.CategoryDto;
import ru.practicum.dto.NewCategoryDto;
import ru.practicum.service.CategoryService;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Main.class)
@Transactional
@ActiveProfiles("test")
class CategoryServiceTest {

    @Autowired
    private CategoryService categoryService;

    @Test
    void addCategory() {
        NewCategoryDto newCategory = new NewCategoryDto();
        newCategory.setName("Test Category");

        CategoryDto saved = categoryService.addCategory(newCategory);

        assertNotNull(saved.getId());
        assertEquals("Test Category", saved.getName());
    }

    @Test
    void getCategory() {
        NewCategoryDto newCategory = new NewCategoryDto();
        newCategory.setName("Test Category");
        CategoryDto saved = categoryService.addCategory(newCategory);

        CategoryDto found = categoryService.getCategory(saved.getId());

        assertEquals(saved.getId(), found.getId());
        assertEquals("Test Category", found.getName());
    }
}