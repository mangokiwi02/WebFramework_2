package kr.ac.hansung.cse.repository;

import jakarta.persistence.EntityManager;

import kr.ac.hansung.cse.model.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CategoryRepository {

    private final EntityManager em;

    public Category save(Category category) {
        em.persist(category);
        return category;
    }

    public List<Category> findAll() {
        return em.createQuery("select c from Category c", Category.class)
                .getResultList();
    }

    // 이름으로 카테고리 찾기 (중복 검사용)
    public Optional<Category> findByName(String name) {
        List<Category> r = em.createQuery(
                        "SELECT c FROM Category c WHERE c.name = :name", Category.class)
                .setParameter("name", name)
                .getResultList();
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    // 삭제 전 연결된 상품이 있는지 확인 (카운트 쿼리)
    public long countProductsByCategoryId(Long categoryId) {
        return em.createQuery(
                        "SELECT COUNT(p) FROM Product p WHERE p.category.id = :id", Long.class)
                .setParameter("id", categoryId)
                .getSingleResult();
    }

    public void delete(Long id) {
        Category c = em.find(Category.class, id);
        if (c != null) {
            em.remove(c);
        }
    }
}