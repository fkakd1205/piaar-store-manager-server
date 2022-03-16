package com.piaar_store_manager.server.service.excel_translator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.piaar_store_manager.server.handler.DateHandler;
import com.piaar_store_manager.server.model.excel_translator_data.dto.UploadExcelDataDetailDto;
import com.piaar_store_manager.server.model.excel_translator_data.dto.UploadExcelDataGetDto;
import com.piaar_store_manager.server.model.excel_translator_data.dto.UploadedDetailDto;
import com.piaar_store_manager.server.model.excel_translator_header.dto.ExcelTranslatorHeaderGetDto;
import com.piaar_store_manager.server.model.excel_translator_header.dto.UploadDetailDto;
import com.piaar_store_manager.server.model.excel_translator_header.entity.ExcelTranslatorHeaderEntity;
import com.piaar_store_manager.server.model.excel_translator_header.repository.ExcelTranslatorHeaderRepository;

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
public class ExcelTranslatorHeaderBusinessService {
    private ExcelTranslatorHeaderService excelTranslatorHeaderService;

    @Autowired
    public ExcelTranslatorHeaderBusinessService(
        ExcelTranslatorHeaderService excelTranslatorHeaderService
    ) {
        this.excelTranslatorHeaderService = excelTranslatorHeaderService;
    }

    /**
     * <b>DB Insert Related Method</b>
     * 엑셀 변환기를 생성해 타이틀 및 데이터 시작행을 저장한다.
     * 
     * 
     * @param dto : ExcelTranslatorHeaderGetDto
     * @return ExcelTranslatorHeaderGetDto
     * @see ExcelTranslatorHeaderEntity#toEntity
     * @see ExcelTranslatorHeaderService#saveOne
     * @see ExcelTranslatorHeaderGetDto#toDto
     */
    public ExcelTranslatorHeaderGetDto createTitle(ExcelTranslatorHeaderGetDto dto) {
        ExcelTranslatorHeaderEntity entity = excelTranslatorHeaderService.saveOne(ExcelTranslatorHeaderEntity.toEntity(dto));
        ExcelTranslatorHeaderGetDto savedDto = ExcelTranslatorHeaderGetDto.toDto(entity);
        return savedDto;
    }

    /**
     * <b>DB Select Related Method</b>
     * 엑셀 변환기 헤더 데이터를 조회한다.
     * 
     * @return List::ExcelTranslatorHeaderGetDto::
     * @see ExcelTranslatorHeaderService#searchList
     * @see ExcelTranslatorHeaderGetDto#toDto
     */
    public List<ExcelTranslatorHeaderGetDto> searchList() {
        List<ExcelTranslatorHeaderEntity> entities = excelTranslatorHeaderService.searchList();
        List<ExcelTranslatorHeaderGetDto> dtos = entities.stream().map(r -> ExcelTranslatorHeaderGetDto.toDto(r)).collect(Collectors.toList());
        return dtos;
    }

    /**
     * <b>DB Update Related Method</b>
     * 생성된 엑셀 변환기의 타이틀 및 데이터 시작행을 수정한다.
     * 
     * @param dto : ExcelTranslatorHeaderGetDto
     * @see ExcelTranslatorHeaderService#searchOne
     * @see excelTranslatorHeaderService#saveOne
     */
    public void changeOne(ExcelTranslatorHeaderGetDto dto) {
        ExcelTranslatorHeaderEntity entity = excelTranslatorHeaderService.searchOne(dto.getId());
        // entity.setId(dto.getId()).setUploadHeaderTitle(dto.getUploadHeaderTitle())
        //     .setDownloadHeaderTitle(dto.getDownloadHeaderTitle()).setUploadHeaderDetail(dto.getUploadHeaderDetail())
        //     .setDownloadHeaderDetail(dto.getDownloadHeaderDetail()).setRowStartNumber(dto.getRowStartNumber());

        entity.setId(dto.getId()).setUploadHeaderTitle(dto.getUploadHeaderTitle())
                .setDownloadHeaderTitle(dto.getDownloadHeaderTitle()).setRowStartNumber(dto.getRowStartNumber());

        excelTranslatorHeaderService.saveOne(entity);
    }

    /**
     * <b>DB Delete Related Method</b>
     * headerId에 대응하는 엑셀 변환기 데이터를 삭제한다.
     * 
     * @param dto : ExcelTranslatorHeaderGetDto
     * @see ExcelTranslatorHeaderRepository#findById
     * @see ExcelTranslatorHeaderRepository#save
     */
    public void deleteOne(Map<String, Object> query) {
        UUID headerId = UUID.fromString(query.get("headerId").toString());
        excelTranslatorHeaderService.destroyOne(headerId);
    }

    /**
     * <b>Data Processing Related Method</b>
     * 업로드된 엑셀 파일을 읽는다.
     * 
     * @param file : MultipartFile
     * @param dto : ExcelTranslatorHeaderGetDto
     * @return List::UploadExcelDataGetDto::
     */
    public List<UploadExcelDataGetDto> uploadExcelFile(MultipartFile file, ExcelTranslatorHeaderGetDto dto) {
        Workbook workbook = null;
        try{
            workbook = WorkbookFactory.create(file.getInputStream());
        } catch (IOException e) {
            throw new IllegalArgumentException();
        }

        Sheet sheet = workbook.getSheetAt(0);
        List<UploadExcelDataGetDto> excelDto = this.getUploadedExcelForm(sheet, dto);
        return excelDto;
    }

    /**
     * <b>Data Processing Related Method</b>
     * 업로드된 엑셀 파일을 데이터 시작 행부터 읽어 Dto로 반환한다.
     * 
     * @param worksheet : Sheet
     * @param dto : ExcelTranslatorHeaderGetDto
     * @return List::UploadExcelDataGetDto::
     */
    private List<UploadExcelDataGetDto> getUploadedExcelForm(Sheet worksheet, ExcelTranslatorHeaderGetDto dto) {
        List<UploadExcelDataGetDto> dtos = new ArrayList<>();
        List<UploadDetailDto> uploadDetailDtos = dto.getUploadHeaderDetail().getDetails();

        Row headerRow = worksheet.getRow(dto.getRowStartNumber()-1);
        // 저장된 양식이 존재하는데 지정양식과 다른 엑셀이 업로드된 경우
        if(uploadDetailDtos.size() != 0 && uploadDetailDtos.size() != headerRow.getLastCellNum()) {
            throw new IllegalArgumentException();
        }

        // 저장된 데이터 시작행부터 엑셀을 읽는다.
        for(int i = dto.getRowStartNumber()-1; i < worksheet.getPhysicalNumberOfRows(); i++) {
            Row row = worksheet.getRow(i);
            List<UploadedDetailDto> uploadedDetailDtos = new ArrayList<>();

            for(int j = 0; j < row.getLastCellNum(); j++) {
                Cell cell = row.getCell(j);
                Object cellObj = new Object();

                if(cell == null || cell.getCellType().equals(CellType.BLANK)) {
                    cellObj = "";
                } else if (cell.getCellType().equals(CellType.STRING)) {
                    cellObj = cell.getStringCellValue();
                } else if (cell.getCellType().equals(CellType.NUMERIC)) {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        cellObj = DateHandler.getUtcDate(cell.getDateCellValue());
                    } else {
                        cellObj = cell.getNumericCellValue();
                    }
                }

                // if(uploadDetailDtos.size() > 0 && i == dto.getRowStartNumber()-1) {
                //     // 등록된 양식과 다른 엑셀 헤더값이 있다면
                //     if(!uploadDetailDtos.get(j).getHeaderName().equals(cellObj.toString())){
                //         System.out.println(cellObj.toString());
                //         throw new IllegalArgumentException();
                //     }
                // }

                UploadedDetailDto detailDto = UploadedDetailDto.builder().id(UUID.randomUUID()).colData(cellObj).cellType(cellObj.getClass().getSimpleName()).build();  
                uploadedDetailDtos.add(detailDto);
            }
            UploadExcelDataDetailDto uploadedData = UploadExcelDataDetailDto.builder().details(uploadedDetailDtos).build();
            UploadExcelDataGetDto dataDto = UploadExcelDataGetDto.builder().id(UUID.randomUUID()).uploadedData(uploadedData).build();
            dtos.add(dataDto);
        }
        return dtos;
    }

    /**
     * <b>DB Update Related Method</b>
     * 엑셀 변환기 헤더 데이터의 업로드 헤더 상세를 업데이트한다.
     * 
     * @param dto : ExcelTranslatorHeaderGetDto
     * @see ExcelTranslatorHeaderService#searchOne
     * @see ExcelTranslatorHeaderService#saveOne
     */
    public void updateUploadHeaderDetailOfExcelTranslator(ExcelTranslatorHeaderGetDto dto) {
        ExcelTranslatorHeaderEntity entity = excelTranslatorHeaderService.searchOne(dto.getId());
        entity.setUploadHeaderDetail(dto.getUploadHeaderDetail());

        excelTranslatorHeaderService.saveOne(entity);
    }

    /**
     * <b>DB Update Related Method</b>
     * 엑셀 변환기 헤더 데이터의 다운로드 헤더 상세를 업데이트한다.
     * 
     * @param dto : ExcelTranslatorHeaderGetDto
     * @see ExcelTranslatorHeaderService#searchOne
     * @see ExcelTranslatorHeaderService#saveOne
     */
    public void updateDownloadHeaderDetailOfExcelTranslator(ExcelTranslatorHeaderGetDto dto) {
        ExcelTranslatorHeaderEntity entity = excelTranslatorHeaderService.searchOne(dto.getId());
        entity.setDownloadHeaderDetail(dto.getDownloadHeaderDetail());
        
        excelTranslatorHeaderService.saveOne(entity);
    }
}

