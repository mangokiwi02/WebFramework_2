package kr.ac.hansung.cse.controller;

import kr.ac.hansung.cse.model.Product;
import kr.ac.hansung.cse.service.CategoryService;
import kr.ac.hansung.cse.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * =====================================================================
 * ProductController - 웹 요청 처리 계층 (Controller Layer)
 * =====================================================================
 *
 * MVC 패턴에서 Controller의 역할:
 *   1. HTTP 요청을 받아 적절한 Service 메서드를 호출합니다.
 *   2. Service로부터 받은 결과를 Model에 담아 View에 전달합니다.
 *   3. 어떤 View를 렌더링할지 결정하여 뷰 이름을 반환합니다.
 *   (비즈니스 로직은 Service에 위임, 데이터 접근은 Repository에 위임)
 *
 * @Controller  : @Component의 특수화입니다.
 *   - WebConfig의 @ComponentScan이 이 클래스를 발견하고 빈으로 등록합니다.
 *   - 반환값을 뷰 이름으로 해석합니다. (@RestController는 JSON/XML 반환)
 *
 * @RequestMapping("/products"): 이 컨트롤러의 모든 메서드에 /products 경로 접두사 적용
 *
 * [요청 처리 흐름]
 * Browser → Tomcat → DispatcherServlet → HandlerMapping
 *   → ProductController → ProductService → ProductRepository
 *   → EntityManager → MySQL
 *   ↓ (역방향)
 * MySQL → EntityManager → ProductRepository → ProductService
 *   → ProductController (Model에 데이터 담기)
 *   → ThymeleafViewResolver → HTML 렌더링 → Browser
 */
@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    /**
     * 생성자 주입 (Constructor Injection)
     * WebConfig의 @ComponentScan → Servlet Context 빈으로 등록
     * ProductService는 Root Context(DbConfig)의 빈이지만,
     * 자식 컨텍스트(Servlet Context)가 부모 빈을 참조할 수 있습니다.
     */
    public ProductController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /products - 상품 목록 조회
    // ─────────────────────────────────────────────────────────────────

    /**
     * 모든 상품 목록을 조회하여 productList 뷰로 전달합니다.
     *
     * @param model : Spring MVC가 제공하는 Map 형태의 데이터 컨테이너입니다.
     *                model.addAttribute("key", value)로 View에 데이터를 전달합니다.
     *                Thymeleaf에서 ${key}로 접근합니다.
     *
     * @return "productList" : ThymeleafViewResolver가 이 문자열을 받아
     *                         /WEB-INF/views/productList.html 파일을 렌더링합니다.
     */
    @GetMapping // (또는 @GetMapping("/products"))
    public String listProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categoryName, // 💡 Long categoryId 에서 변경!
            Model model) {

        List<Product> products;

        if (keyword != null && !keyword.isBlank()) {
            products = productService.searchByName(keyword);
        } else if (categoryName != null && !categoryName.isBlank()) {
            products = productService.searchByCategoryName(categoryName); // 💡 변경됨
        } else {
            products = productService.getAllProducts();
        }

        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryName", categoryName); // 💡 화면에 선택 상태 유지를 위해 전달

        return "productList"; // (혹은 폴더 구조에 따라 "products/productList")
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /products/{id} - 상품 상세 조회
    // ─────────────────────────────────────────────────────────────────

    /**
     * 특정 ID의 상품 상세 정보를 조회합니다.
     *
     * @PathVariable : URL 경로의 변수를 메서드 파라미터로 바인딩합니다.
     *                 예) GET /products/1 → id = 1L
     */
    @GetMapping("/{id}")
    public String productDetail(@PathVariable Long id, Model model) {
        // Optional.orElseThrow(): 상품이 없으면 예외 발생
        // 실제 운영에서는 @ExceptionHandler나 @ControllerAdvice로 에러 페이지를 처리합니다.
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 상품 ID: " + id));

        model.addAttribute("product", product);
        return "productDetail"; // → /WEB-INF/views/productDetail.html
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /products/create - 상품 생성 폼 표시
    // ─────────────────────────────────────────────────────────────────

    /**
     * 빈 Product 객체를 Model에 담아 폼을 표시합니다.
     *
     * Thymeleaf의 th:object 와 th:field 를 사용한 폼 바인딩을 위해
     * 빈 Product 객체를 미리 Model에 담아 두어야 합니다.
     */
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        // 빈 Product 객체: 폼의 초기값으로 사용됩니다.
        model.addAttribute("product", new Product("", "", java.math.BigDecimal.ZERO, ""));
        return "productForm"; // → /WEB-INF/views/productForm.html
    }

    // ─────────────────────────────────────────────────────────────────
    // POST /products/create - 상품 생성 처리
    // ─────────────────────────────────────────────────────────────────

    /**
     * 폼 데이터를 받아 새 상품을 저장하고 목록 페이지로 리다이렉트합니다.
     *
     * @ModelAttribute Product product:
     *   - HTTP 요청 파라미터(form 데이터)를 Product 객체에 자동 바인딩합니다.
     *   - 요청 파라미터 name → product.name, price → product.price 등
     *   - Spring MVC의 데이터 바인딩(DataBinder)이 처리합니다.
     *
     * @RedirectAttributes:
     *   - 리다이렉트 시 데이터를 Flash 속성으로 전달합니다.
     *   - Flash 속성은 HTTP 세션에 임시 저장 후 리다이렉트 후 자동 삭제됩니다.
     *   - 성공/실패 메시지를 리다이렉트 후 페이지에 표시할 때 사용합니다.
     *
     * [PRG 패턴 - Post-Redirect-Get]
     * POST 처리 후 바로 View를 반환하면 브라우저 새로고침 시 중복 제출 발생.
     * 해결책: POST 처리 → Redirect → GET 요청
     * "redirect:/products" → DispatcherServlet이 302 응답 → 브라우저가 GET /products 요청
     */
    @PostMapping("/create")
    public String createProduct(@ModelAttribute Product product,
                                RedirectAttributes redirectAttributes) {
        productService.createProduct(product);

        // Flash 속성: 리다이렉트 후 한 번만 표시되는 메시지
        redirectAttributes.addFlashAttribute("successMessage",
                "'" + product.getName() + "' 상품이 성공적으로 등록되었습니다.");

        // PRG 패턴: POST 후 GET으로 리다이렉트
        return "redirect:/products";
    }
}
