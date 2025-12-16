import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ONE-FILE AUTOMATION - CHANGE ONE LINE, RUN ONCE, GET EVERYTHING!
 * 
 * HOW TO USE:
 * 1. Change ENTITY_NAME and ENTITY_PACKAGE below (lines 18-19)
 * 2. Run: java GenerateTablePage.java
 * 3. Done! Controller + View created, ready to use
 * 
 * EXAMPLE:
 *   For Game API at /mvc/games/read
 *   - ENTITY_NAME = "Game"
 *   - ENTITY_PACKAGE = "com.open.spring.mvc.rpg.games"
 *   
 *   Creates GameMvcController.java + games/read.html
 *   Visit: http://localhost:8080/mvc/games/read
 */
public class GenerateTablePage {
    
    // ═══════════════════════════════════════════════════════════════════
    // ✏️  EDIT THESE LINES - Everything else is automatic!
    // ═══════════════════════════════════════════════════════════════════
    static final String ENTITY_NAME = "Resume";                                  // ← Your entity class name
    static final String ENTITY_PACKAGE = "com.open.spring.mvc.resume";          // ← Where your entity class is
    static final String PAGE_NAME = "Resumes";                                   // ← Creates /mvc/resumes/read
    // ═══════════════════════════════════════════════════════════════════
    
    // Auto-computed (don't touch)
    static final String PAGE_LOWER = PAGE_NAME.toLowerCase();
    static final String REPO_NAME = ENTITY_NAME + "JpaRepository";
    static final String CONTROLLER_PACKAGE = "com.open.spring.mvc." + PAGE_LOWER;
    static final String BASE_PATH = "/mvc/" + PAGE_LOWER;
    static final String PROJECT_ROOT = System.getProperty("user.dir");
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║     AUTO TABLE PAGE GENERATOR                              ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Entity:          " + ENTITY_NAME);
        System.out.println("Entity Package:  " + ENTITY_PACKAGE);
        System.out.println("Controller Pkg:  " + CONTROLLER_PACKAGE);
        System.out.println("URL Path:        " + BASE_PATH + "/read");
        System.out.println();
        System.out.println("Generating files...");
        System.out.println("═".repeat(60));
        
        try {
            String controller = createController();
            System.out.println("✓ Created: " + controller);
            
            String view = createView();
            System.out.println("✓ Created: " + view);
            
            System.out.println("═".repeat(60));
            System.out.println();
            System.out.println("╔════════════════════════════════════════════════════════════╗");
            System.out.println("║  ✓ SUCCESS! Everything ready to use.                      ║");
            System.out.println("╚════════════════════════════════════════════════════════════╝");
            System.out.println();
            System.out.println("Next steps:");
            System.out.println("1. Make sure " + REPO_NAME + " exists");
            System.out.println("2. Restart Spring Boot");
            System.out.println("3. Visit: http://localhost:8585" + BASE_PATH + "/read");
            System.out.println();
            System.out.println("The page will show ALL fields from " + ENTITY_NAME + " automatically!");
            
        } catch (IOException e) {
            System.err.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    static String createController() throws IOException {
        String className = PAGE_NAME + "MvcController";
        String repoVar = Character.toLowerCase(REPO_NAME.charAt(0)) + REPO_NAME.substring(1);
        
        String code = String.format("""
package %s;

import %s.%s;
import %s.%s;
import com.open.spring.mvc.table.TableConfig;
import com.open.spring.mvc.table.TableConfigBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Auto-generated controller for %s (displays %s entity data)
 * Uses reflection to discover all fields automatically
 */
@Controller
@RequestMapping("%s")
public class %s {

    @Autowired
    private %s %s;

    @GetMapping("/read")
    public String readView(Model model) {
        // AUTO-MAGIC: Points at %s.class, discovers ALL fields automatically!
        TableConfig tableConfig = TableConfigBuilder.fromEntity(%s.class)
                .withEntityName("%s")
                .withDisplayNames("%s", "%ss")
                .withTableId("%sTable")
                .withPaths("%s/edit", "%s/delete")
                .withCreateNew("%s/new", "Create New %s")
                .withMaxVisibleColumns(6)
                .build();
        
        model.addAttribute("tableConfig", tableConfig);
        model.addAttribute("list", %s.findAll());
        return "%s/read";
    }
}
""",
            CONTROLLER_PACKAGE,
            ENTITY_PACKAGE, ENTITY_NAME,
            ENTITY_PACKAGE, REPO_NAME,
            PAGE_NAME, ENTITY_NAME,
            BASE_PATH,
            className,
            REPO_NAME, repoVar,
            ENTITY_NAME,
            ENTITY_NAME,
            PAGE_LOWER,
            ENTITY_NAME, ENTITY_NAME,
            PAGE_LOWER,
            BASE_PATH, BASE_PATH,
            BASE_PATH, ENTITY_NAME,
            repoVar,
            PAGE_LOWER
        );
        
        String packagePath = CONTROLLER_PACKAGE.replace('.', '/');
        Path dirPath = Paths.get(PROJECT_ROOT, "src/main/java", packagePath);
        Files.createDirectories(dirPath);
        
        Path filePath = dirPath.resolve(className + ".java");
        Files.writeString(filePath, code);
        
        return "src/main/java/" + packagePath + "/" + className + ".java";
    }
    
    static String createView() throws IOException {
        String entityPlural = ENTITY_NAME + "s";
        
        String code = String.format("""
<!DOCTYPE HTML>
<html xmlns:layout="http://www.w3.org/1999/xhtml" xmlns:th="http://www.w3.org/1999/xhtml"
      layout:decorate="~{layouts/base}" lang="en">

<th:block layout:fragment="title" th:remove="tag">%s List</th:block>

<th:block layout:fragment="body" th:remove="tag">
    <div sec:authorize="hasRole('ROLE_ADMIN')">
        <!-- AUTO-GENERATED TABLE - ONE LINE RENDERS EVERYTHING! -->
        <th:block th:replace="~{fragments/auto-table :: render(config=${tableConfig}, list=${list})}"></th:block>
    </div>
    <div sec:authorize="!hasRole('ROLE_ADMIN')" class="container">
        <div class="alert alert-danger">Access denied: administrators only.</div>
    </div>
</th:block>

<!-- Overlay for update page -->
<th:block layout:fragment="overlay-body" th:remove="tag">
    <div id="overlay-Container"></div>
</th:block>

<!-- Generic scripts - work with ANY entity! -->
<th:block layout:fragment="script" th:remove="tag">
    <script sec:authorize="hasRole('ROLE_ADMIN')" type="module" th:src="@{/js/entity/entity-extraction.js}"></script>
    <script sec:authorize="hasRole('ROLE_ADMIN')" type="module" th:src="@{/js/entity/entity-importation.js}"></script>
    <script type="text/javascript" th:src="@{/js/read-filter.js}"></script>
    <script type="text/javascript" th:src="@{/js/read-overlay.js}"></script>
    
    <!-- Inline column toggle - guaranteed to work! -->
    <script>
        document.addEventListener('DOMContentLoaded', function() {
            const table = document.querySelector('.table.table-sm.table-striped');
            const toggleButtons = document.querySelectorAll('.toggle-column');
            
            toggleButtons.forEach(button => {
                button.addEventListener('click', function() {
                    const columnIndex = parseInt(this.getAttribute('data-column'));
                    const isActive = this.classList.contains('active');
                    
                    // Toggle all cells in this column
                    const headers = table.querySelectorAll('thead th');
                    const rows = table.querySelectorAll('tbody tr');
                    
                    if (headers[columnIndex]) {
                        headers[columnIndex].style.display = isActive ? 'none' : '';
                    }
                    
                    rows.forEach(row => {
                        const cells = row.querySelectorAll('td');
                        if (cells[columnIndex]) {
                            cells[columnIndex].style.display = isActive ? 'none' : '';
                        }
                    });
                    
                    // Toggle button state
                    this.classList.toggle('active');
                    this.classList.toggle('inactive');
                });
            });
        });
    </script>
</th:block>

</html>
""", entityPlural);
        
        Path dirPath = Paths.get(PROJECT_ROOT, "src/main/resources/templates", PAGE_LOWER);
        Files.createDirectories(dirPath);
        
        Path filePath = dirPath.resolve("read.html");
        Files.writeString(filePath, code);
        
        return "src/main/resources/templates/" + PAGE_LOWER + "/read.html";
    }
}
