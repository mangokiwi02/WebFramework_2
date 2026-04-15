package kr.ac.hansung.cse.service;

import kr.ac.hansung.cse.model.Product;
import kr.ac.hansung.cse.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * =====================================================================
 * ProductService - 비즈니스 로직 계층 (Service Layer)
 * =====================================================================
 *
 * Service 계층의 역할:
 *   - 비즈니스 규칙(Business Logic)을 담당합니다.
 *   - Controller와 Repository 사이를 중재합니다.
 *   - 여러 Repository 호출을 조합하는 복합 작업을 처리합니다.
 *   - 트랜잭션 경계를 정의합니다.
 *
 * @Service: @Component의 특수화입니다.
 *   - Spring이 이 클래스를 빈으로 등록합니다.
 *   - 비즈니스 로직 담당 클래스임을 의미적으로 명확히 표현합니다.
 *
 * [의존성 주입(Dependency Injection)]
 * 이 클래스는 ProductRepository에 의존합니다.
 * Spring IoC 컨테이너가 ProductRepository 빈을 생성하여 주입해 줍니다.
 *
 * 생성자 주입(Constructor Injection)을 권장하는 이유:
 *   1. 의존성이 명시적으로 드러납니다.
 *   2. 불변(final) 필드로 선언 가능합니다.
 *   3. 단위 테스트 시 목(Mock) 객체 주입이 용이합니다.
 *   4. 순환 의존성을 컴파일 타임에 감지할 수 있습니다.
 *
 * [@Transactional 상세 설명]
 * 클래스 레벨에 선언 시 모든 public 메서드에 트랜잭션이 적용됩니다.
 *
 * 트랜잭션 전파(Propagation):
 *   - REQUIRED (기본값): 기존 트랜잭션이 있으면 참여, 없으면 새로 시작
 *   - REQUIRES_NEW: 항상 새 트랜잭션 시작 (기존 트랜잭션 일시 중단)
 *   - SUPPORTS: 트랜잭션이 있으면 참여, 없으면 없이 실행
 *
 * readOnly = true:
 *   - 읽기 전용 최적화: Hibernate의 더티 체킹(변경 감지)을 비활성화합니다.
 *   - DB 드라이버 레벨에서 읽기 전용 연결로 설정할 수 있어 성능이 향상됩니다.
 *   - 읽기 메서드에는 반드시 readOnly = true를 명시하는 것을 권장합니다.
 */
@Service
@Transactional(readOnly = true) // 클래스 기본값: 읽기 전용 트랜잭션
public class ProductService {

    // final 선언: 생성자 주입 후 변경 불가 (불변성 보장)
    private final ProductRepository productRepository;

    /**
     * 생성자 주입 (Constructor Injection)
     * Spring 4.3+: 생성자가 하나뿐이면 @Autowired를 생략할 수 있습니다.
     * DbConfig의 @ComponentScan이 이 클래스를 발견하고
     * ProductRepository 빈을 찾아 자동으로 주입합니다.
     */
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * 모든 상품 조회
     * readOnly = true (클래스 레벨 설정 상속): 읽기 전용 트랜잭션
     */
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    /**
     * ID로 상품 조회
     * Optional을 그대로 반환하여 Controller가 null 처리를 명시적으로 하도록 강제합니다.
     */
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    /**
     * 새 상품 등록
     *
     * @Transactional: readOnly 기본값을 false로 오버라이드합니다.
     *                 쓰기 작업에는 반드시 readOnly = false가 필요합니다.
     *                 DB 변경 작업이 포함되므로 트랜잭션이 필수입니다.
     *
     * [비즈니스 규칙 예시]
     * 실제 프로젝트에서는 이 위치에 비즈니스 규칙을 추가합니다:
     *   - 가격이 0 이하이면 예외 발생
     *   - 이미 존재하는 상품명이면 예외 발생
     *   - 재고 관련 로직 처리 등
     */
    @Transactional // readOnly = false (쓰기 가능)
    public Product createProduct(Product product) {
        // 비즈니스 유효성 검사 예시
        if (product.getPrice() != null && product.getPrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("상품 가격은 0 이상이어야 합니다.");
        }
        return productRepository.save(product);
    }

    /**
     * 상품 수정
     */
    @Transactional
    public Product updateProduct(Product product) {
        return productRepository.update(product);
    }

    /**
     * 상품 삭제
     */
    @Transactional
    public void deleteProduct(Long id) {
        productRepository.delete(id);
    }

    public List<Product> searchByName(String keyword) {
        return productRepository.findByNameContaining(keyword);
    }

    public List<Product> searchByCategoryName(String categoryName) {
        return productRepository.findByCategoryName(categoryName);
    }
}
