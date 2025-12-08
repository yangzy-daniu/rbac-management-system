package com.example.demo.service;

import com.example.demo.dto.ProductDTO;
import com.example.demo.entity.Product;
import com.example.demo.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    // 获取热门产品列表（按月销量排序）
    public List<ProductDTO> getPopularProducts() {
        List<Product> products = productRepository.findPopularProducts();
        return products.stream()
                .limit(10) // 取前10个
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 更新产品销量（可以在订单创建时调用，但这里不强制关联）
    public void updateProductSales(String productName, Integer quantity) {
        // 这里可以留空，如果需要关联，后面再实现
    }

    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setCategory(product.getCategory());
        dto.setSalesCount(product.getSalesCount());
        dto.setMonthlySales(product.getMonthlySales());
        dto.setGrowthRate(product.getGrowthRate());
        dto.setRankPosition(product.getRankPosition());
        dto.setStatus(product.getStatus());
        dto.setImageUrl(product.getImageUrl());
        return dto;
    }
}