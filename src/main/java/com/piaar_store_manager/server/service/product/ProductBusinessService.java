package com.piaar_store_manager.server.service.product;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.piaar_store_manager.server.handler.DateHandler;
import com.piaar_store_manager.server.model.option_package.entity.OptionPackageEntity;
import com.piaar_store_manager.server.model.product.dto.ProductCreateReqDto;
import com.piaar_store_manager.server.model.product.dto.ProductGetDto;
import com.piaar_store_manager.server.model.product.dto.ProductJoinResDto;
import com.piaar_store_manager.server.model.product.entity.ProductEntity;
import com.piaar_store_manager.server.model.product.proj.ProductProj;
import com.piaar_store_manager.server.model.product_option.dto.ProductOptionGetDto;
import com.piaar_store_manager.server.model.product_option.entity.ProductOptionEntity;
import com.piaar_store_manager.server.service.option_package.OptionPackageService;
import com.piaar_store_manager.server.service.product_option.ProductOptionService;

import com.piaar_store_manager.server.service.user.UserService;
import com.piaar_store_manager.server.utils.CustomUniqueKeyUtils;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductBusinessService {
    private final ProductService productService;
    private final ProductOptionService productOptionService;
    private final OptionPackageService optionPackageService;
    private final UserService userService;

    public ProductGetDto searchOne(Integer productCid) {
        ProductEntity entity = productService.searchOne(productCid);
        return ProductGetDto.toDto(entity);
    }

    public List<ProductGetDto> searchList() {
        List<ProductEntity> entities = productService.searchList();
        List<ProductGetDto> dtos = entities.stream().map(entity -> ProductGetDto.toDto(entity)).collect(Collectors.toList());
        return dtos;
    }

    /**
     * <b>DB Select Related Method</b>
     * <p>
     * Product cid 값과 상응되는 데이터를 조회한다.
     * Product와 Many To One JOIN(m2oj) 연관관계에 놓여있는 user, category 조회한다.
     *
     * @param productCid : Integer
     * @return ProductJoinResDto
     * @see ProductService#searchOneM2OJ
     * @see ProductJoinResDto#toDto
     */
    public ProductJoinResDto searchOneM2OJ(Integer productCid) {
        ProductProj productProj = productService.searchOneM2OJ(productCid);
        return ProductJoinResDto.toDto(productProj);
    }

    /**
     * <b>DB Select Related Method</b>
     * <p>
     * Product cid 값과 상응되는 데이터를 조회한다.
     * 해당 Product와 연관관계에 놓여있는 Full JOIN(fj) 상태를 조회한다.
     *
     * @param productCid : Integer
     * @return ProductJoinResDto
     * @see ProductOptionService#searchListByProduct
     * @see ProductJoinResDto#toDto
     */
    public ProductJoinResDto searchOneFJ(Integer productCid) {
        ProductProj productProj = productService.searchOneM2OJ(productCid);
        List<ProductOptionEntity> optionEntities = productOptionService.searchListByProduct(productProj.getProduct().getCid());
        List<ProductOptionGetDto> optionDtos = optionEntities.stream().map(r -> ProductOptionGetDto.toDto(r)).collect(Collectors.toList());
        ProductJoinResDto resDto = ProductJoinResDto.toDto(productProj);
        resDto.setOptions(optionDtos);
        return resDto;
    }

    /**
     * <b>DB Select Related Method</b>
     * <p>
     * Category cid에 대응하는 Product 데이터를 모두 조회한다.
     * 
     * @return List::ProductGetDto::
     * @see ProductService#searchListByCategory
     * @see ProductGetDto#toDto
     */
    public List<ProductGetDto> searchListByCategory(Integer categoryCid) {
        List<ProductEntity> entities = productService.searchListByCategory(categoryCid);
        List<ProductGetDto> productDtos = entities.stream().map(entity -> ProductGetDto.toDto(entity)).collect(Collectors.toList());
        return productDtos;
    }
    
    /**
     * <b>DB Select Related Method</b>
     * <p>
     * Product 데이터를 모두 조회한다.
     * 해당 Product와 연관관계에 놓여있는 Many To One JOIN(m2oj) 상태를 조회한다.
     *
     * @return List::ProductJoinResDto::
     * @see ProductService#searchProjList
     * @see ProductJoinResDto#toDto
     */
    public List<ProductJoinResDto> searchListM2OJ() {
        List<ProductProj> productProjs = productService.searchProjList();
        List<ProductJoinResDto> resDtos = productProjs.stream().map(proj -> ProductJoinResDto.toDto(proj)).collect(Collectors.toList());
        return resDtos;
    }

    /**
     * <b>DB Select Related Method</b>
     * <p>
     * Product 데이터를 모두 조회한다.
     * 해당 Product와 연관관계에 놓여있는 모든 Full JOIN(fj) 상태를 조회한다.
     *
     * @return List::ProductJoinResDto::
     * @see ProductService#searchProjList
     * @see ProductOptionService#searchListByProductList
     * @see ProductJoinResDto#toDto
     */
    public List<ProductJoinResDto> searchListFJ(){
        List<ProductProj> productProjs = productService.searchProjList();
        List<Integer> productCids = productProjs.stream().map(r -> r.getProduct().getCid()).collect(Collectors.toList());
        List<ProductOptionGetDto> optionGetDtos = productOptionService.searchListByProductList(productCids);
        
        List<ProductJoinResDto> joinResDto = productProjs.stream().map(r -> {
            List<ProductOptionGetDto> optionDtosByProductCid = new ArrayList<>();

            optionGetDtos.stream().forEach(option -> {
                if(r.getProduct().getCid().equals(option.getProductCid())) {
                    optionDtosByProductCid.add(option);
                }
            });

            ProductJoinResDto resDto = ProductJoinResDto.toDto(r);
            resDto.setOptions(optionDtosByProductCid);

            return resDto;
        }).collect(Collectors.toList());

        return joinResDto;
    }

    /**
     * <b>DB Select Related Method</b>
     * <p>
     * Product 데이터를 모두 조회한다.
     * 해당 Product와 연관관계에 놓여있는 모든 Full JOIN(fj) 상태를 조회한다.
     * 재고관리 여부가 true인 데이터를 추출한다.
     * 옵션cid값에 대응하는 입출고수량, 총 재고수량를 구한다.
     * 총 재고수량을 이용해 상품옵션의 stockUnit값을 업데이트한다.
     *
     * @return List::ProductJoinResDto::
     * @see ProductService#searchProjList
     * @see ProductOptionService#searchListByProductList
     * @see ProductJoinResDto#toDto
     */
//    public List<ProductJoinResDto> searchStockListFJ(){
    public List<ProductGetDto.FullJoin> searchStockListFJ(){
        List<ProductProj> productProjs = productService.searchProjList();
        List<ProductProj> stockManagementProductProjs = new ArrayList<>();
        List<Integer> productCids = new ArrayList<>();

        for(ProductProj proj : productProjs){
            // 재고관리 상품 여부
            if(proj.getProduct().getStockManagement()){
                productCids.add(proj.getProduct().getCid());
                stockManagementProductProjs.add(proj);
            }
        }


        List<ProductOptionGetDto> optionGetDtos = productOptionService.searchListByProductList(productCids);


//        List<ProductJoinResDto> joinResDto = stockManagementProductProjs.stream().map(r -> {
//            List<ProductOptionGetDto> optionDtosByProduct = new ArrayList<>();
//
//            optionGetDtos.stream().forEach(option -> {
//                if(r.getProduct().getCid().equals(option.getProductCid())) {
//                    option.setStockUnit(option.getStockSumUnit());
//                    optionDtosByProduct.add(option);
//                }
//            });
//
//            ProductJoinResDto resDto = ProductJoinResDto.toDto(r);
//            resDto.setOptions(optionDtosByProduct);
//            return resDto;
//        }).collect(Collectors.toList());

        List<ProductGetDto.FullJoin> joinResDto = stockManagementProductProjs.stream().map(r -> {
            List<ProductOptionGetDto> optionDtosByProduct = new ArrayList<>();

            optionGetDtos.stream().forEach(option -> {
                if(r.getProduct().getCid().equals(option.getProductCid())) {
                    option.setStockUnit(option.getStockSumUnit());
                    optionDtosByProduct.add(option);
                }
            });

            ProductGetDto.FullJoin productFJDto = ProductGetDto.FullJoin.toDto(r);
            productFJDto.setOptions(optionDtosByProduct);
            return productFJDto;
        }).collect(Collectors.toList());

        return joinResDto;
    }

    /**
     * <b>DB Insert Related Method</b>
     * <p>
     * Product 내용을 한개 등록한다.
     * Product에 등록된 매입총합게를 해당 옵션들에게도 반영한다.
     * 
     * @param productGetDto : ProductGetDto
     * @param userId : UUID
     * @see ProductEntity#toEntity
     * @see ProductGetDto#toDto
     */
    public ProductGetDto createOne(ProductGetDto productGetDto, UUID userId) {
        productGetDto.setCode(CustomUniqueKeyUtils.generateKey()).setCreatedAt(DateHandler.getCurrentDate2()).setCreatedBy(userId)
            .setUpdatedAt(DateHandler.getCurrentDate2()).setUpdatedBy(userId);

        ProductEntity entity = productService.createOne(ProductEntity.toEntity(productGetDto));
        ProductGetDto dto = ProductGetDto.toDto(entity);
        return dto;
    }

    /**
     * <b>DB Insert Related Method</b>
     * <p>
     * Product 내용을 여러개 등록한다.
     * 
     * @param productGetDto : ProductGetDto
     * @param userId : UUID
     * @see ProductEntity#toEntity
     * @see ProductService#createList
     * @see ProductGetDto#toDto
     */
    public List<ProductGetDto> createList(List<ProductGetDto> productGetDto, UUID userId) {
        List<ProductEntity> productEntities = productGetDto.stream().map(r -> {
            r.setCreatedAt(DateHandler.getCurrentDate2()).setCreatedBy(userId)
                .setUpdatedAt(DateHandler.getCurrentDate2()).setUpdatedBy(userId);

            return ProductEntity.toEntity(r);
        }).collect(Collectors.toList());

        List<ProductEntity> entities = productService.createList(productEntities);
        List<ProductGetDto> dtos = entities.stream().map(entity -> ProductGetDto.toDto(entity)).collect(Collectors.toList());
        return dtos;
    }

    /**
     * <b>DB Insert Related Method</b>
     * <p>
     * Product와 ProductOption 내용을 한개 등록한다.
     */
    @Transactional
    public void createPAO(ProductCreateReqDto reqDto) {
        UUID USER_ID = userService.getUserId();
        // product save
        ProductGetDto savedProductDto = this.createOne(reqDto.getProductDto(), USER_ID);

        List<ProductOptionEntity> entities = reqDto.getOptionDtos().stream().map(r -> {
            r.setCode(CustomUniqueKeyUtils.generateKey()).setCreatedAt(DateHandler.getCurrentDate2()).setCreatedBy(USER_ID)
                .setUpdatedAt(DateHandler.getCurrentDate2()).setUpdatedBy(USER_ID).setProductCid(savedProductDto.getCid());

            // 패키지 상품 여부
            if(reqDto.getPackageDtos().size() > 0) {
                r.setPackageYn("y");
            }else {
                r.setPackageYn("n");
            }

            // 상품에 등록된 totalPurchasePrice가 있다면 옵션에 동일한 값 부여
            if(r.getTotalPurchasePrice() == 0) {
                r.setTotalPurchasePrice(savedProductDto.getDefaultTotalPurchasePrice());
            }
            
            return ProductOptionEntity.toEntity(r);
        }).collect(Collectors.toList());

        System.out.println(entities);
        // option save
        productOptionService.createList(entities);

        List<OptionPackageEntity> optionPackageEntities = reqDto.getPackageDtos().stream().map(r -> {
            r.setCreatedAt(LocalDateTime.now()).setCreatedBy(USER_ID)
                .setUpdatedAt(LocalDateTime.now()).setUpdatedBy(USER_ID);

            return OptionPackageEntity.toEntity(r);
        }).collect(Collectors.toList());

        // option package save
        optionPackageService.saveListAndModify(optionPackageEntities);
    }

    /**
     * <b>DB Insert Related Method</b>
     * <p>
     * Product와 ProductOption 내용을 여러개 등록한다.
     * 
     * @param productCreateReqDtos : List::ProductCreateReqDto::
     * @param userId : UUID
     * @see ProductService#createOne
     * @see productOptionService#createList
     */
    @Transactional
    public void createPAOList(List<ProductCreateReqDto> productCreateReqDtos){
        productCreateReqDtos.stream().forEach(r -> this.createPAO(r));
    }

    /**
     * <b>DB Delete Related Method</b>
     * <p>
     * Product cid 값과 상응되는 데이터를 삭제한다.
     * 
     * @param productCid : Integer
     * @see ProductService#destroyOne
     */
    public void destroyOne(Integer productCid) {
        productService.destroyOne(productCid);
    }

    /**
     * <b>DB Update Related Method</b>
     * <p>
     * Product cid 값과 상응되는 데이터를 업데이트한다.
     * Product에 대응되는 옵션들을 조회해서 그것ㄷ
     * 
     * @param productDto : ProductGetDto
     * @see ProductService#searchOne
     * @see ProductService#createOne
     */
    public void changeOne(ProductGetDto productDto) {
        UUID USER_ID = userService.getUserId();

        ProductEntity productEntity = productService.searchOne(productDto.getCid());
        productEntity.setCode(productDto.getCode()).setManufacturingCode(productDto.getManufacturingCode())
                .setNaverProductCode(productDto.getNaverProductCode()).setDefaultName(productDto.getDefaultName())
                .setManagementName(productDto.getManagementName()).setImageUrl(productDto.getImageUrl())
                .setImageFileName(productDto.getImageFileName()).setPurchaseUrl(productDto.getPurchaseUrl()).setMemo(productDto.getMemo())
                .setHsCode(productDto.getHsCode()).setStyle(productDto.getStyle())
                .setTariffRate(productDto.getTariffRate()).setDefaultWidth(productDto.getDefaultWidth())
                .setDefaultLength(productDto.getDefaultLength()).setDefaultHeight(productDto.getDefaultHeight())
                .setDefaultQuantity(productDto.getDefaultQuantity()).setDefaultWeight(productDto.getDefaultWeight())
                .setDefaultTotalPurchasePrice(productDto.getDefaultTotalPurchasePrice())
                .setUpdatedAt(DateHandler.getCurrentDate2()).setUpdatedBy(USER_ID)
                .setStockManagement(productDto.getStockManagement())
                .setProductCategoryCid(productDto.getProductCategoryCid());

        // 옵션들의 매입총합계를 변경한다.
        productService.createOne(productEntity);

        this.changeOptionTotalPurchasePrice(productEntity.getCid(), productEntity.getDefaultTotalPurchasePrice());
    }

    public void changeOptionTotalPurchasePrice(Integer productCid, Integer totalPurchasePrice) {
        List<ProductOptionEntity> optionEntities = productOptionService.searchListByProduct(productCid);
        optionEntities.stream().forEach(r -> r.setTotalPurchasePrice(totalPurchasePrice));

        productOptionService.createList(optionEntities);
    }

    /**
     * <b>DB Update Related Method</b>
     * <p>
     * 각 상품마다 ProductOption cid 값과 상응되는 데이터를 업데이트한다.
     * 
     * @param productCreateReqDtos : List::ProductCreateReqDto::
     * @param userId :: UUID
     * @see ProductBusinessService#changeOne
     * @see ProductOptionService#changeOne
     */
    @Transactional
    public void changePAOList(List<ProductCreateReqDto> productCreateReqDtos) {
        productCreateReqDtos.stream().forEach(req -> {
            this.changeOne(req.getProductDto());
            req.getOptionDtos().stream().forEach(option -> productOptionService.changeOne(option));
        });
    }
    
    /**
     * <b>DB Update Related Method</b>
     * <p>
     * Product id 값과 상응되는 데이터의 일부분을 업데이트한다.
     * 
     * @param productDto : ProductGetDto
     * @param userId : UUID
     * @see ProductService#searchOne
     * @see ProductService#createOne
     */
    public void patchOne(ProductGetDto productDto) {
        UUID USER_ID = userService.getUserId();

        ProductEntity productEntity = productService.searchOne(productDto.getCid());
        
        if (productDto.getCode() != null) {
            productEntity.setCode(productDto.getCode());
        }
        if (productDto.getManufacturingCode() != null) {
            productEntity.setManufacturingCode(productDto.getManufacturingCode());
        }
        if (productDto.getNaverProductCode() != null) {
            productEntity.setNaverProductCode(productDto.getNaverProductCode());
        }
        if (productDto.getDefaultName() != null) {
            productEntity.setDefaultName(productDto.getDefaultName());
        }
        if (productDto.getManagementName() != null) {
            productEntity.setManagementName(productDto.getManagementName());
        }
        if (productDto.getImageUrl() != null) {
            productEntity.setImageUrl(productDto.getImageUrl());
        }
        if (productDto.getImageFileName() != null) {
            productEntity.setImageFileName(productDto.getImageFileName());
        }
        if (productDto.getPurchaseUrl() != null) {
            productEntity.setPurchaseUrl(productDto.getPurchaseUrl());
        }
        if (productDto.getMemo() != null) {
            productEntity.setMemo(productDto.getMemo());
        }
        if (productDto.getHsCode() != null) {
            productEntity.setHsCode(productDto.getHsCode());
        }
        if (productDto.getStyle() != null) {
            productEntity.setStyle(productDto.getStyle());
        }
        if (productDto.getTariffRate() != null) {
            productEntity.setTariffRate(productDto.getTariffRate());
        }
        if (productDto.getDefaultWidth() != null) {
            productEntity.setDefaultWidth(productDto.getDefaultWidth());
        }
        if (productDto.getDefaultLength() != null) {
            productEntity.setDefaultLength(productDto.getDefaultLength());
        }
        if (productDto.getDefaultHeight() != null) {
            productEntity.setDefaultHeight(productDto.getDefaultHeight());
        }
        if (productDto.getDefaultQuantity() != null) {
            productEntity.setDefaultQuantity(productDto.getDefaultQuantity());
        }
        if (productDto.getDefaultWeight() != null) {
            productEntity.setDefaultWeight(productDto.getDefaultWeight());
        }
        if (productDto.getDefaultTotalPurchasePrice() != null) {
            productEntity.setDefaultTotalPurchasePrice(productDto.getDefaultTotalPurchasePrice());
        }
        if (productDto.getStockManagement() != null) {
            productEntity.setStockManagement(productDto.getStockManagement());
        }
        if (productDto.getProductCategoryCid() != null) {
            productEntity.setProductCategoryCid(productDto.getProductCategoryCid());
        }
        productEntity.setUpdatedAt(DateHandler.getCurrentDate2()).setUpdatedBy(USER_ID);
        productService.createOne(productEntity);
    }
}
