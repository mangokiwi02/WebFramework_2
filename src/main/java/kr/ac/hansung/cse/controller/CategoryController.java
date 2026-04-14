package kr.ac.hansung.cse.controller;


import jakarta.validation.Valid;
import kr.ac.hansung.cse.exception.DuplicateCategoryException;
import kr.ac.hansung.cse.model.CategoryForm;
import kr.ac.hansung.cse.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping // GET /categories → 목록
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        return "categoryList"; }

    @GetMapping("/create") // GET → 등록 폼 표시
    public String showCreateForm(Model model) {
        model.addAttribute("categoryForm", new CategoryForm());
        return "categoryForm"; }

    @PostMapping("/create") // POST → 등록 처리
    public String createCategory(
            @Valid @ModelAttribute CategoryForm categoryForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) return "categoryForm"; // 검증 실패
        try {
            categoryService.createCategory(categoryForm.getName());
            redirectAttributes.addFlashAttribute("successMessage", "등록 완료");
        } catch (DuplicateCategoryException e) {
            // 중복 예외 → BindingResult에 필드 오류 등록 후 폼 재표시
            bindingResult.rejectValue("name", "duplicate", e.getMessage());
            return "categoryForm"; }
        return "redirect:/categories"; }

    @PostMapping("/{id}/delete")
    public String deleteCategory(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("successMessage", "삭제 완료");
        } catch (IllegalStateException e) {
            // 연결된 상품 있을 때 → Flash로 오류 메시지 전달
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }  return "redirect:/categories"; } }
