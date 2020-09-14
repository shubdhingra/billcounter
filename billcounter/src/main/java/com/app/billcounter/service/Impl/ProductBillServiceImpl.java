package com.app.billcounter.service.Impl;

import com.app.billcounter.dao.ProductBillDao;
import com.app.billcounter.dao.entity.ProductEntity;
import com.app.billcounter.dao.impl.ProductBillDaoImpl;
import com.app.billcounter.domain.BillResource;
import com.app.billcounter.domain.ProductResource;
import com.app.billcounter.service.ProductBillService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductBillServiceImpl implements ProductBillService {

    @Autowired
    private ProductBillDaoImpl productBillDao;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public BillResource getBillByProductId(Set<String> productIds) {
        // fetch all products from Db
        List<ProductEntity> productListFromDb = productBillDao.getProductsByIds(productIds);
        //convert entity list to resource list
        List<ProductResource> productResourceList =
                productListFromDb
                        .stream()
                        .map(prod -> modelMapper.map(prod, ProductResource.class))
                        .collect(Collectors.toList());

        //set taxRate as per category
        productResourceList.forEach(obj -> {
            float taxRate = mapTaxRateToCategory(obj.getTaxCategory());
            obj.setTaxRate(taxRate);
            obj.setTaxAmount(obj.getPrice() * (taxRate / 100));
        });

        return prepareFinalBill(productResourceList);
    }

    private BillResource prepareFinalBill(List<ProductResource> productResourceList) {
        BillResource finalBill = new BillResource();

        finalBill.setProducts(productResourceList);
        float totalCostWithoutTax = calculateTotalAmountWithoutTax(productResourceList);
        float totalTax = calculateTotalTax(productResourceList);
        finalBill.setTotalCostWithoutTax(totalCostWithoutTax);
        finalBill.setTotalCostWithTax(totalCostWithoutTax + totalTax);
        finalBill.setTotalTax(totalTax);
        return finalBill;
    }

    private float mapTaxRateToCategory(String taxCategory) {
        float taxRate = 0;
        if (taxCategory.equalsIgnoreCase("A")) taxRate = 10;
        else if (taxCategory.equalsIgnoreCase("B")) taxRate = 20;
        else taxRate = 0;
        return taxRate;
    }

    private float calculateTotalAmountWithoutTax(List<ProductResource> productResourceList) {
        // calculate total amount
        float totalAmountWithoutTax = 0;
        totalAmountWithoutTax = (float)
                productResourceList.stream().mapToDouble(o -> o.getPrice()).sum();

        return totalAmountWithoutTax;
    }

    private float calculateTotalTax(List<ProductResource> productResourceList) {
        return (float) productResourceList.stream().mapToDouble(o -> o.getTaxAmount()).sum();
    }

}
