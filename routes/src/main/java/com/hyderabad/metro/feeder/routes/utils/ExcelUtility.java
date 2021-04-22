package com.hyderabad.metro.feeder.routes.utils;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class ExcelUtility {
	
	private final static Logger LOGGER = Logger.getLogger(ExcelUtility.class.getName());
	
	public List<List<Object>> readData(String workSheetName, Integer startRow, Integer endRow, 
			Integer startColumn, Integer endColumn) {
		
		FileInputStream fis = null;
		List<List<Object>> data = new ArrayList<List<Object>>();
		try {
			fis = new FileInputStream(new ClassPathResource("/templates/Matrix.xlsx").getFile());
			Workbook workbook = WorkbookFactory.create(fis);
			Sheet sheet = workbook.getSheet(workSheetName);
			Row row = null;
			
			for(int rowIndex= startRow; rowIndex<=endRow; rowIndex++) {
				row = sheet.getRow(rowIndex);
				List<Object> rowData = new ArrayList<Object>();
				for(int columnIndex = startColumn; columnIndex<= endColumn; columnIndex++) {
					Cell cell = row.getCell(columnIndex);
					rowData.add(this.getCellValue(cell));
				}
				data.add(rowData);
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, e.getMessage());
		} finally {
			try {
				fis.close();
			}catch (Exception e) {
				LOGGER.log(Level.WARNING, e.getMessage());
			}
		}		
		return data;		
	}
	
	private Object getCellValue(Cell cell) {
		
		Object value = null;
		if(cell == null) {
			value = "";
			return value;
		}
		
		switch (cell.getCellType()) {
		case _NONE:
			value = "NONE";			
			break;
		case BLANK:
			value = "";
			break;
		case BOOLEAN:
			value = (Boolean) cell.getBooleanCellValue();
			break;
		case NUMERIC:
			if(DateUtil.isCellDateFormatted(cell)) {
				value = cell.getDateCellValue();
				break;
			} else if (Math.floor(cell.getNumericCellValue()) == cell.getNumericCellValue()) {
				value = (Integer) new Double(cell.getNumericCellValue()).intValue();
				break;
			} else {
				value = (Double) cell.getNumericCellValue();
				break;
			}
		case STRING:
			value = cell.getRichStringCellValue().getString();
			break;
		case ERROR:
			value = (Byte) cell.getErrorCellValue();
			break;
		case FORMULA:
			value = cell.getCellFormula();
			break;
		default:
			value = "";
			break;
		}		
		return value;
	}

}


