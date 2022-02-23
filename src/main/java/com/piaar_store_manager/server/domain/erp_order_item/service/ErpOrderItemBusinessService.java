package com.piaar_store_manager.server.domain.erp_order_item.service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.piaar_store_manager.server.domain.erp_order_item.dto.ErpOrderItemDto;
import com.piaar_store_manager.server.domain.erp_order_item.entity.ErpOrderItemEntity;
import com.piaar_store_manager.server.domain.erp_order_item.repository.ErpOrderItemRepository;
import com.piaar_store_manager.server.domain.erp_order_item.vo.ErpOrderItemVo;
import com.piaar_store_manager.server.exception.AccessDeniedException;
import com.piaar_store_manager.server.exception.FileUploadException;
import com.piaar_store_manager.server.exception.InvalidUserException;
import com.piaar_store_manager.server.handler.DateHandler;
import com.piaar_store_manager.server.service.user.UserService;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ErpOrderItemBusinessService {
    private ErpOrderItemRepository erpOrderItemRepository;
    private ErpOrderItemService erpOrderItemService;
    private UserService userService;

    @Autowired
    public ErpOrderItemBusinessService(
        ErpOrderItemRepository erpOrderItemRepository,
        ErpOrderItemService erpOrderItemService,
        UserService userService
    ) {
        this.erpOrderItemRepository = erpOrderItemRepository;
        this.erpOrderItemService = erpOrderItemService;
        this.userService = userService;
    }

    // Excel file extension.
    private final List<String> EXTENSIONS_EXCEL = Arrays.asList("xlsx", "xls");

    private final Integer PIAAR_ERP_ORDER_ITEM_SIZE = 40;
    private final Integer PIAAR_ERP_ORDER_MEMO_START_INDEX = 20;

    private final List<String> PIAAR_ERP_ORDER_HEADER_NAME_LIST = Arrays.asList(
            "피아르 고유번호",
            "주문번호1",
            "주문번호2",
            "주문번호3",
            "상품명",
            "옵션명",
            "수량",
            "수취인명",
            "전화번호1",
            "전화번호2",
            "주소",
            "우편번호",
            "배송방식",
            "배송메세지",
            "상품고유번호1",
            "상품고유번호2",
            "옵션고유번호1",
            "옵션고유번호2",
            "피아르 상품코드",
            "피아르 옵션코드",
            "관리메모1",
            "관리메모2",
            "관리메모3",
            "관리메모4",
            "관리메모5",
            "관리메모6",
            "관리메모7",
            "관리메모8",
            "관리메모9",
            "관리메모10",
            "관리메모11",
            "관리메모12",
            "관리메모13",
            "관리메모14",
            "관리메모15",
            "관리메모16",
            "관리메모17",
            "관리메모18",
            "관리메모19",
            "관리메모20");

    /**
     * <b>Extension Check</b>
     * <p>
     * 
     * @param file : MultipartFile
     * @throws FileUploadException
     */
    public void isExcelFile(MultipartFile file) {
        String extension = FilenameUtils.getExtension(file.getOriginalFilename().toLowerCase());

        if (EXTENSIONS_EXCEL.contains(extension)) {
            return;
        }
        throw new FileUploadException("This is not an excel file.");
    }

    /**
     * <b>Upload Excel File</b>
     * <p>
     * 엑셀 파일을 업로드하여 화면에 출력한다.
     * 
     * @param file : MultipartFile
     * @return List::ErpOrderItemVo::
     * @throws IOException
     * @see ErpOrderItemBusinessService#getErpOrderItemForm
     */
    public List<ErpOrderItemVo> uploadErpOrderItem(MultipartFile file) {
        if (!userService.isUserLogin()) {
            throw new InvalidUserException("로그인이 필요한 서비스 입니다.");
        }

        if (!userService.isManager()) {
            throw new AccessDeniedException("접근 권한이 없습니다.");
        }

        Workbook workbook = null;
        try {
            workbook = WorkbookFactory.create(file.getInputStream());
        } catch (IOException e) {
            throw new IllegalArgumentException();
        }

        Sheet sheet = workbook.getSheetAt(0);

        List<ErpOrderItemVo> vos = this.getErpOrderItemForm(sheet);
        return vos;
    }

    private List<ErpOrderItemVo> getErpOrderItemForm(Sheet worksheet) {
        List<ErpOrderItemVo> itemVos = new ArrayList<>();

        Row firstRow = worksheet.getRow(0);
        // 피아르 엑셀 양식 검사
        for (int i = 0; i < PIAAR_ERP_ORDER_ITEM_SIZE; i++) {
            Cell cell = firstRow.getCell(i);
            String headerName = cell != null ? cell.getStringCellValue() : null;
            // 지정된 양식이 아니라면
            if (!PIAAR_ERP_ORDER_HEADER_NAME_LIST.get(i).equals(headerName)) {
                throw new IllegalArgumentException();
            }
        }

        for (int i = 1; i < worksheet.getPhysicalNumberOfRows(); i++) {
            Row row = worksheet.getRow(i);
            if (row == null)
                break;

            Object cellValue = new Object();
            List<String> customManagementMemo = new ArrayList<>();

            // type check and data setting of managementMemo1~20.
            for (int j = PIAAR_ERP_ORDER_MEMO_START_INDEX; j < PIAAR_ERP_ORDER_ITEM_SIZE; j++) {
                Cell cell = row.getCell(j);

                if (cell == null || cell.getCellType().equals(CellType.BLANK)) {
                    cellValue = "";
                } else if (cell.getCellType().equals(CellType.NUMERIC)) {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        Instant instant = Instant.ofEpochMilli(cell.getDateCellValue().getTime());
                        LocalDateTime date = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
                        // yyyy-MM-dd'T'HH:mm:ss -> yyyy-MM-dd HH:mm:ss로 변경
                        String newDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        cellValue = newDate;
                    } else {
                        cellValue = cell.getNumericCellValue();
                    }
                } else {
                    cellValue = cell.getStringCellValue();
                }
                customManagementMemo.add(cellValue.toString());
            }

            ErpOrderItemVo excelVo = ErpOrderItemVo.builder()
                    .uniqueCode(UUID.randomUUID())
                    .orderNumber1(row.getCell(1) != null ? row.getCell(1).getStringCellValue() : "")
                    .orderNumber2(row.getCell(2) != null ? row.getCell(2).getStringCellValue() : "")
                    .orderNumber3(row.getCell(3) != null ? row.getCell(3).getStringCellValue() : "")
                    .prodName(row.getCell(4) != null ? row.getCell(4).getStringCellValue() : "")
                    .optionName(row.getCell(5) != null ? row.getCell(5).getStringCellValue() : "")
                    .unit(row.getCell(6) != null ? (int) row.getCell(6).getNumericCellValue() : 0)
                    .receiver(row.getCell(7) != null ? row.getCell(7).getStringCellValue() : "")
                    .receiverContact1(row.getCell(8) != null ? row.getCell(8).getStringCellValue() : "")
                    .receiverContact2(row.getCell(9) != null ? row.getCell(9).getStringCellValue() : "")
                    .destination(row.getCell(10) != null ? row.getCell(10).getStringCellValue() : "")
                    .zipCode(row.getCell(11) != null ? row.getCell(11).getStringCellValue() : "")
                    .transportType(row.getCell(12) != null ? row.getCell(12).getStringCellValue() : "")
                    .deliveryMessage(row.getCell(13) != null ? row.getCell(13).getStringCellValue() : "")
                    .prodUniqueNumber1(row.getCell(14) != null ? row.getCell(14).getStringCellValue() : "")
                    .prodUniqueNumber2(row.getCell(15) != null ? row.getCell(15).getStringCellValue() : "")
                    .optionUniqueNumber1(row.getCell(16) != null ? row.getCell(16).getStringCellValue() : "")
                    .optionUniqueNumber2(row.getCell(17) != null ? row.getCell(17).getStringCellValue() : "")
                    .prodCode(row.getCell(18) != null ? row.getCell(18).getStringCellValue() : "")
                    .optionCode(row.getCell(19) != null ? row.getCell(19).getStringCellValue() : "")
                    .managementMemo1(customManagementMemo.get(0))
                    .managementMemo2(customManagementMemo.get(1))
                    .managementMemo3(customManagementMemo.get(2))
                    .managementMemo4(customManagementMemo.get(3))
                    .managementMemo5(customManagementMemo.get(4))
                    .managementMemo6(customManagementMemo.get(5))
                    .managementMemo7(customManagementMemo.get(6))
                    .managementMemo8(customManagementMemo.get(7))
                    .managementMemo9(customManagementMemo.get(8))
                    .managementMemo10(customManagementMemo.get(9))
                    .managementMemo11(customManagementMemo.get(10))
                    .managementMemo12(customManagementMemo.get(11))
                    .managementMemo13(customManagementMemo.get(12))
                    .managementMemo14(customManagementMemo.get(13))
                    .managementMemo15(customManagementMemo.get(14))
                    .managementMemo16(customManagementMemo.get(15))
                    .managementMemo17(customManagementMemo.get(16))
                    .managementMemo18(customManagementMemo.get(17))
                    .managementMemo19(customManagementMemo.get(18))
                    .managementMemo20(customManagementMemo.get(19))
                    .build();
                    
            itemVos.add(excelVo);
        }
        return itemVos;
    }

    public void saveItemList(List<ErpOrderItemDto> orderItemDtos) {
        if (!userService.isUserLogin()) {
            throw new InvalidUserException("로그인이 필요한 서비스 입니다.");
        }

        if (!userService.isManager()) {
            throw new AccessDeniedException("접근 권한이 없습니다.");
        }

        UUID USER_ID = userService.getUserId();

        List<ErpOrderItemEntity> orderItemEntities = orderItemDtos.stream()
                .map(r -> {
                    r.setId(UUID.randomUUID())
                            .setUniqueCode(UUID.randomUUID())
                            .setSoldYn("n")
                            .setReleasedYn("n")
                            .setStockReflectedYn("n")
                            .setCreatedAt(DateHandler.getCurrentLocalDateTime())
                            .setCreatedBy(USER_ID);

                    return ErpOrderItemEntity.toEntity(r);
                }).collect(Collectors.toList());

        erpOrderItemService.saveItemList(orderItemEntities);
    }
    
}
