package com.example.demo.controller;

import com.example.demo.dto.ProductDTO;
import com.example.demo.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;

    // 获取热门产品
    @GetMapping("/popular")
    public ResponseEntity<List<ProductDTO>> getPopularProducts() {
        List<ProductDTO> products = productService.getPopularProducts();
        return ResponseEntity.ok(products);
    }
}