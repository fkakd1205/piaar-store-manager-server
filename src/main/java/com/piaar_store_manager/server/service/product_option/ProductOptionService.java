package com.piaar_store_manager.server.service.product_option;

import com.piaar_store_manager.server.handler.DateHandler;
import com.piaar_store_manager.server.model.product.dto.ProductGetDto;
import com.piaar_store_manager.server.model.product_category.dto.ProductCategoryGetDto;
import com.piaar_store_manager.server.model.product_option.dto.ProductOptionGetDto;
import com.piaar_store_manager.server.model.product_option.dto.ProductOptionJoinResDto;
import com.piaar_store_manager.server.model.product_option.dto.ReceiveReleaseSumOnlyDto;
import com.piaar_store_manager.server.model.product_option.entity.ProductOptionEntity;
import com.piaar_store_manager.server.model.product_option.proj.ProductOptionProj;
import com.piaar_store_manager.server.model.product_option.repository.ProductOptionRepository;
import com.piaar_store_manager.server.model.product_receive.repository.ProductReceiveRepository;
import com.piaar_store_manager.server.model.product_release.repository.ProductReleaseRepository;
import com.piaar_store_manager.server.model.user.dto.UserGetDto;
import com.piaar_store_manager.server.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.Tuple;

@Service
public class ProductOptionService {

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Autowired
    private ProductReceiveRepository productReceiveRepository;

    @Autowired
    private ProductReleaseRepository productReleaseRepository;

    @Autowired
    private DateHandler dateHandler;

    @Autowired
    private UserService userService;

    /**
     * <b>DB Select Related Method</b>
     * <p>
     * ProductOption cid 값과 상응되는 데이터를 조회한다.
     * 
     * @param productOptionCid : Integer
     * @return ProductOptionGetDto
     * @see ProductOptionRepository#findById
     * @see ProductOptionGetDto#toDto
     */
    public ProductOptionGetDto searchOne(Integer productOptionCid) {
        Optional<ProductOptionEntity> productOptionEntityOpt = productOptionRepository.findById(productOptionCid);
        ProductOptionGetDto productOptionDto = new ProductOptionGetDto();

        if (productOptionEntityOpt.isPresent()) {
            productOptionDto = ProductOptionGetDto.toDto(productOptionEntityOpt.get());
        } else {
            throw new NullPointerException();
        }

        return productOptionDto;
    }

    /**
     * <b>DB Select Related Method</b>
     * <p>
     * ProductOption cid 값과 상응되는 데이터를 조회한다.
     * 해당 ProductOption와 연관관계에 놓여있는 Many To One JOIN(m2oj) 상태를 조회한다.
     *
     * @param productOptionCid : Integer
     * @return ProductOptionJoinResDto
     * @see ProductOptionRepository#selectByCid
     * @see ProductGetDto#toDto
     * @see UserService#getDtoByEntity
     * @see ProductCategoryGetDto#toDto
     * @see ProductOptionGetDto#toDto
     */
    public ProductOptionJoinResDto searchOneM2OJ(Integer productOptionCid) {
        ProductOptionJoinResDto productOptionResDto = new ProductOptionJoinResDto();

        Optional<ProductOptionProj> productOptionProjOpt = productOptionRepository.selectByCid(productOptionCid);

        if (productOptionProjOpt.isPresent()) {
            ProductGetDto productGetDto = ProductGetDto.toDto(productOptionProjOpt.get().getProduct());
            UserGetDto userGetDto = userService.getDtoByEntity(productOptionProjOpt.get().getUser());
            ProductCategoryGetDto categoryGetDto = ProductCategoryGetDto.toDto(productOptionProjOpt.get().getCategory());
            ProductOptionGetDto productOptionGetDto = ProductOptionGetDto.toDto(productOptionProjOpt.get().getProductOption());

            productOptionResDto.setProduct(productGetDto).setUser(userGetDto).setCategory(categoryGetDto)
                    .setOption(productOptionGetDto);
        } else {
            throw new NullPointerException();
        }
        return productOptionResDto;
    }

    /**
     * <b>DB Select Related Method</b>
     * <p>
     * ProductOption 데이터를 모두 조회한다.
     *
     * @return List::ProductOptionGetDto::
     * @see ProductOptionRepository#findAll
     * @see ProductOptionGetDto#toDto
     */
    public List<ProductOptionGetDto> searchList() {
        List<ProductOptionEntity> productOptionEntities = productOptionRepository.findAll();
        List<ProductOptionGetDto> productOptionDto = new ArrayList<>();

        for (ProductOptionEntity optionEntity : productOptionEntities) {
            productOptionDto.add(ProductOptionGetDto.toDto(optionEntity));
        }
        return productOptionDto;
    }

    /**
     * <b>DB Select Related Method</b>
     * <p>
     * ProductOption 데이터를 모두 조회한다.
     *
     * @return List::ProductOptionGetDto::
     * @see ProductOptionRepository#findAll
     * @see ProductOptionGetDto#toDto
     */
    public List<ProductOptionGetDto> searchListByProduct(Integer productCid) {
        List<ProductOptionEntity> productOptionEntities = productOptionRepository.findByProductCid(productCid);
        List<ProductOptionGetDto> productOptionDto = new ArrayList<>();
        
        // TODO : ReceiveReleaseSumOnlyDto 사용
        for (ProductOptionEntity optionEntity : productOptionEntities) {
            ProductOptionGetDto dto = ProductOptionGetDto.toDto(optionEntity);
            
            Integer receiveStockUnit = productReceiveRepository.sumByProductOptionCid(optionEntity.getCid());
            Integer releaseStockUnit = productReleaseRepository.sumByProductOptionCid(optionEntity.getCid());

            if(receiveStockUnit == null) receiveStockUnit = 0;
            if(releaseStockUnit == null) releaseStockUnit = 0;

            dto.setStockUnit(receiveStockUnit- releaseStockUnit);
            productOptionDto.add(dto);
        }

        return productOptionDto;
    }


    /**
     * <b>DB Select Related Method</b>
     * <p>
     * ProductOption 데이터를 모두 조회한다.
     * 해당 ProductOption와 연관관계에 놓여있는 Many To One JOIN(m2oj) 상태를 조회한다.
     *
     * @return List::ProductOptionJoinResDto::
     * @see ProductOptionRepository#selectAll
     */
    public List<ProductOptionJoinResDto> searchListM2OJ() {
        List<ProductOptionJoinResDto> productOptionJoinResDtos = new ArrayList<>();
        List<ProductOptionProj> productOptionProjs = productOptionRepository.selectAll();
        
        for (ProductOptionProj projOptionOpt : productOptionProjs) {
            productOptionJoinResDtos.add(this.searchOneM2OJ(projOptionOpt.getProductOption().getCid()));
        }
        return productOptionJoinResDtos;
    }

    /**
     * <b>DB Insert Related Method</b>
     * <p>
     * ProductOption 내용을 한개 등록한다.
     * 
     * @param productOptionGetDto : ProductOptionGetDto
     * @param userId : UUID
     * @see ProductOptionEntity#toEntity
     * @see ProductOptionRepository#save
     */
    public void createOne(ProductOptionGetDto dto, UUID userId) {
        dto.setCreatedAt(dateHandler.getCurrentDate()).setCreatedBy(userId)
            .setUpdatedAt(dateHandler.getCurrentDate()).setUpdatedBy(userId);

        ProductOptionEntity entity = ProductOptionEntity.toEntity(dto);
        productOptionRepository.save(entity);
    }

    /**
     * <b>DB Insert Related Method</b>
     * <p>
     * Product에 종속되는 옵션(ProductOption)을 한개 등록한다.
     * 
     * @param productOptionGetDto : ProductOptionGetDto
     * @param userId : UUID
     * @param productCid : Integer
     * @see ProductOptionEntity#toEntity
     * @see ProductOptionRepository#save
     */
    public ProductOptionEntity createOne(ProductOptionGetDto dto, UUID userId, Integer productCid) {
        dto.setCreatedAt(dateHandler.getCurrentDate()).setCreatedBy(userId)
            .setUpdatedAt(dateHandler.getCurrentDate()).setUpdatedBy(userId)
            .setProductCid(productCid);

        ProductOptionEntity entity = ProductOptionEntity.toEntity(dto);
        return productOptionRepository.save(entity);
    }

    /**
     * <b>DB Select Related Method</b>
     * <p>
     * ProductOption cid 값과 상응되는 데이터를 삭제한다.
     * 
     * @param productOptionCid : Integer
     * @see ProductOptionRepository#findById
     * @see ProductOptionRepository#delete
     */
    public void destroyOne(Integer productOptionCid) {
        productOptionRepository.findById(productOptionCid).ifPresent(productOption -> {
            productOptionRepository.delete(productOption);
        });
    }

    /**
     * <b>DB Update Related Method</b>
     * <p>
     * ProductOption cid 값과 상응되는 데이터를 업데이트한다.
     * 
     * @param productOptionDto : ProductOptionGetDto
     * @param userId : UUID
     * @see ProductOptionRepository#findById
     * @see ProductOptionRepository#save
     */
    public void changeOne(ProductOptionGetDto productOptionDto, UUID userId) {
        productOptionRepository.findById(productOptionDto.getCid()).ifPresentOrElse(productOptionEntity -> {
            productOptionEntity.setCode(productOptionDto.getCode())
                    .setNosUniqueCode(productOptionDto.getNosUniqueCode())
                    .setDefaultName(productOptionDto.getDefaultName())
                    .setManagementName(productOptionDto.getManagementName())
                    .setNosUniqueCode(productOptionDto.getNosUniqueCode())
                    .setSalesPrice(productOptionDto.getSalesPrice()).setStockUnit(productOptionDto.getStockUnit())
                    .setStatus(productOptionDto.getStatus()).setMemo(productOptionDto.getMemo())
                    .setImageUrl(productOptionDto.getImageUrl()).setImageFileName(productOptionDto.getImageFileName())
                    .setColor(productOptionDto.getColor()).setUnitCny(productOptionDto.getUnitCny())
                    .setUnitKrw(productOptionDto.getUnitKrw()).setUpdatedAt(dateHandler.getCurrentDate()).setUpdatedBy(userId)
                    .setProductCid(productOptionDto.getProductCid());

            productOptionRepository.save(productOptionEntity);
        }, null);
    }

    /**
     * <b>DB Update Related Method</b>
     * <p>
     * ProductOption cid 값과 상응되는 데이터의 일부분을 업데이트한다.
     * 
     * @param productOptionDto : ProductOptionGetDto
     * @param userId : UUID
     * @see ProductOptionRepository#findById
     * @see ProductOptionRepository#save
     */
    public void patchOne(ProductOptionGetDto productOptionDto, UUID userId) {
        productOptionRepository.findById(productOptionDto.getCid()).ifPresentOrElse(productOptionEntity -> {
            if (productOptionDto.getCode() != null) {
                productOptionEntity.setCode(productOptionDto.getCode());
            }
            if (productOptionDto.getNosUniqueCode() != null) {
                productOptionEntity.setNosUniqueCode(productOptionDto.getNosUniqueCode());
            }
            if (productOptionDto.getDefaultName() != null) {
                productOptionEntity.setDefaultName(productOptionDto.getDefaultName());
            }
            if (productOptionDto.getManagementName() != null) {
                productOptionEntity.setManagementName(productOptionDto.getManagementName());
            }
            if (productOptionDto.getSalesPrice() != null) {
                productOptionEntity.setSalesPrice(productOptionDto.getSalesPrice());
            }
            if (productOptionDto.getStockUnit() != null) {
                productOptionEntity.setStockUnit(productOptionDto.getStockUnit());
            }
            if (productOptionDto.getStatus() != null) {
                productOptionEntity.setStatus(productOptionDto.getStatus());
            }
            if (productOptionDto.getMemo() != null) {
                productOptionEntity.setMemo(productOptionDto.getMemo());
            }
            if (productOptionDto.getImageUrl() != null) {
                productOptionEntity.setImageUrl(productOptionDto.getImageUrl());
            }
            if (productOptionDto.getImageFileName() != null) {
                productOptionEntity.setImageFileName(productOptionDto.getImageFileName());
            }
            if (productOptionDto.getColor() != null) {
                productOptionEntity.setColor(productOptionDto.getColor());
            }
            if (productOptionDto.getUnitCny() != null) {
                productOptionEntity.setUnitCny(productOptionDto.getUnitCny());
            }
            if (productOptionDto.getUnitKrw() != null) {
                productOptionEntity.setUnitKrw(productOptionDto.getUnitKrw());
            }
            if (productOptionDto.getProductCid() != null) {
                productOptionEntity.setProductCid(productOptionDto.getProductCid());
            }

            productOptionEntity.setUpdatedAt(dateHandler.getCurrentDate()).setUpdatedBy(userId);

            productOptionRepository.save(productOptionEntity);
        }, null);
    }

    /**
     * <b>DB Update Related Method</b>
     * <p>
     * 입고되는 상품의 ProductOption cid 값과 상응되는 데이터의 내용을 업데이트한다.
     * 
     * @param optionCid : Integer
     * @param userId : UUID
     * @param receiveUnit : Integer
     * @see ProductOptionRepository#findById
     * @see ProductOptionRepository#save
     */
    public void updateReceiveProductUnit(Integer optionCid, UUID userId, Integer receiveUnit){
        productOptionRepository.findById(optionCid).ifPresentOrElse(productOptionEntity -> {
            productOptionEntity.setStockUnit(productOptionEntity.getStockUnit() + receiveUnit)
                               .setUpdatedAt(dateHandler.getCurrentDate())
                               .setUpdatedBy(userId);

            productOptionRepository.save(productOptionEntity);
        }, null);
    }

    /**
     * <b>DB Update Related Method</b>
     * <p>
     * 출고되는 상품의 ProductOption cid 값과 상응되는 데이터의 내용을 업데이트한다.
     * 
     * @param optionCid : Integer
     * @param userId : UUID
     * @param releaseUnit : Integer
     * @see ProductOptionRepository#findById
     * @see ProductOptionRepository#save
     */
    public void updateReleaseProductUnit(Integer optionCid, UUID userId, Integer releaseUnit){
        productOptionRepository.findById(optionCid).ifPresentOrElse(productOptionEntity -> {
            productOptionEntity.setStockUnit(productOptionEntity.getStockUnit() - releaseUnit)
                               .setUpdatedAt(dateHandler.getCurrentDate())
                               .setUpdatedBy(userId);

            productOptionRepository.save(productOptionEntity);
        }, null);
    }

    public List<ProductOptionGetDto> searchListByProductCids(List<Integer> cids){
        List<ProductOptionEntity> entities = productOptionRepository.selectAllByProductCids(cids);

        return entities.stream().map(r->{
            ProductOptionGetDto dto = ProductOptionGetDto.toDto(r);
            return dto;
        }).collect(Collectors.toList());
    }

    public List<ReceiveReleaseSumOnlyDto> sumStockUnit(List<Integer> cids) {
        List<Tuple> stockUnitTuple = productOptionRepository.sumStockUnitByOption(cids);
        List<ReceiveReleaseSumOnlyDto> stockUnitByOption = new ArrayList<>();

        stockUnitByOption = stockUnitTuple.stream().map(r -> {
            ReceiveReleaseSumOnlyDto dto = ReceiveReleaseSumOnlyDto.builder()
                    .optionCid(r.get("cid", Integer.class))
                    .receivedSum(r.get("receivedSum", BigDecimal.class) != null ? r.get("receivedSum", BigDecimal.class).intValue() : 0)
                    .releasedSum(r.get("releasedSum", BigDecimal.class) != null ? r.get("releasedSum", BigDecimal.class).intValue() : 0)
                    .build();

            return dto;
        }).collect(Collectors.toList());

        return stockUnitByOption;
    }
}
