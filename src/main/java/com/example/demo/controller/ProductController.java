package com.example.demo.controller;

import com.example.demo.dto.CategorySalesDTO;
import com.example.demo.dto.ProductDTO;
import com.example.demo.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // 获取热门产品
    @GetMapping("/popular")
    public ResponseEntity<List<ProductDTO>> getPopularProducts() {
        List<ProductDTO> products = productService.getPopularProducts();
        return ResponseEntity.ok(products);
    }

    // 获取品类销售数据
    @GetMapping("/category-sales")
    public ResponseEntity<List<CategorySalesDTO>> getCategorySales(
            @RequestParam(defaultValue = "sales") String type) {
        List<CategorySalesDTO> data = productService.getCategorySales(type);
        return ResponseEntity.ok(data);
    }
}