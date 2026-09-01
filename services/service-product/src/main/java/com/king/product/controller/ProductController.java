package com.king.product.controller;

import com.king.product.bean.Product;
import com.king.product.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {
    @Autowired
    private ProductService productService;
    @GetMapping("/product/{id}")
    public Product getproduct(@PathVariable("id") long productid, HttpServletRequest request) {
        String header = request.getHeader("X-Token");

        System.out.println("hello....header====["+header+"]");
        Product product=productService.getById(productid);
        return product;
    }
}
