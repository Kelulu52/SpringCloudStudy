package com.king.product.service.impl;
import java.math.BigDecimal;

import com.king.product.bean.Product;
import com.king.product.service.ProductService;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl implements ProductService {

    @Override
    public Product getById(long productid) {
        Product product = new Product();
        product.setId(productid);
        product.setPrice(new BigDecimal("100"));
        product.setProductName("苹果+"+productid);
        product.setNum(2);
        return product;
    }
}
