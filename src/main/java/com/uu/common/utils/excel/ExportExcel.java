/**
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.uu.common.utils.excel;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.uu.common.utils.Encodes;
import com.uu.common.utils.Reflections;
import com.uu.common.utils.excel.annotation.ExcelField;
import com.uu.modules.sys.utils.DictUtils;

/**
 * 导出Excel文件（导出“XLSX”格式，支持大数据量导出 @see org.apache.poi.ss.SpreadsheetVersion）
 * 
 * @author admin
 * @version 2013-04-21
 */
public class ExportExcel {

	private static Logger log = LoggerFactory.getLogger(ExportExcel.class);

	/**
	 * 工作薄对象
	 */
	private SXSSFWorkbook wb;

	/**
	 * 工作表对象
	 */
	private Sheet sheet;
	
	private Sheet sheet2;

	/**
	 * 样式列表
	 */
	private Map<String, CellStyle> styles;

	/**
	 * 当前行号
	 */
	private int rownum;

	/**
	 * 注解列表（Object[]{ ExcelField, Field/Method }）
	 */
	List<Object[]> annotationList = Lists.newArrayList();

	/**
	 * 构造函数
	 * 
	 * @param title
	 *            表格标题，传“空值”，表示无标题
	 * @param cls
	 *            实体对象，通过annotation.ExportField获取标题
	 */
	public ExportExcel(String title, Class<?> cls) {
		this(title, cls, 1);
	}

	/**
	 * 订单统计模块，自定义构造函数
	 * 
	 * @param title
	 *            表格标题，传“空值”，表示无标题
	 * @param cls
	 *            实体对象，通过annotation.ExportField获取标题
	 */
	public ExportExcel(String title, Class<?> cls, String listSize, String sumMoney) {
		getAnnotationField(cls, 1, new int[0]);// Get annotation field
		getAnnotationMethod(cls, 1, new int[0]);// Get annotation method
		fieldSorting();// Field sorting
		List<String> headerList = initializeHeaderList(1);// Initialize
		initializeTitle(title, headerList);
		Row row = this.addRow();
		this.addCell(row, 0, "总计订单数", "header1");
		this.addCell(row, 1, StringUtils.EMPTY);
		this.addCell(row, 2, listSize, "data2");
		this.addCell(row, 3, StringUtils.EMPTY);
		this.addCell(row, 4, StringUtils.EMPTY);
		this.addCell(row, 5, "总计金额", "header1");
		this.addCell(row, 6, StringUtils.EMPTY);
		this.addCell(row, 7, sumMoney, "data2");
		this.addCell(row, 8, StringUtils.EMPTY);
		this.addCell(row, 9, StringUtils.EMPTY);
		sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 1));
		sheet.addMergedRegion(new CellRangeAddress(1, 1, 2, 4));
		sheet.addMergedRegion(new CellRangeAddress(1, 1, 5, 6));
		sheet.addMergedRegion(new CellRangeAddress(1, 1, 7, 9));
		initializeHeader(title, headerList);
	}

	/**
	 * 构造函数
	 * 
	 * @param title
	 *            表格标题，传“空值”，表示无标题
	 * @param cls
	 *            实体对象，通过annotation.ExportField获取标题
	 * @param type
	 *            导出类型（1:导出数据；2：导出模板）
	 * @param groups
	 *            导入分组
	 */
	public ExportExcel(String title, Class<?> cls, int type, int... groups) {
		getAnnotationField(cls, type, groups);// Get annotation field
		getAnnotationMethod(cls, type, groups);// Get annotation method
		fieldSorting();// Field sorting
		List<String> headerList = initializeHeaderList(type);// Initialize
		initializeTitle(title, headerList);
		initializeHeader(title, headerList);
	}

	/**
	 * 根据注解生成表格头
	 * 
	 * @Description
	 * @param type
	 * @return
	 * @return List<String>
	 * @author yuxiaoyu
	 * @date 2016年4月5日 下午5:05:42
	 */
	private List<String> initializeHeaderList(int type) {
		List<String> headerList = Lists.newArrayList();
		for (Object[] os : annotationList) {
			String t = ((ExcelField) os[0]).title();
			// 如果是导出，则去掉注释
			if (type == 1) {
				String[] ss = StringUtils.split(t, "**", 2);
				if (ss.length == 2) {
					t = ss[0];
				}
			}
			headerList.add(t);
		}
		return headerList;
	}

	/**
	 * 列排序
	 * 
	 * @Description
	 * @return void
	 * @author yuxiaoyu
	 * @date 2016年4月5日 下午5:06:07
	 */
	private void fieldSorting() {
		Collections.sort(annotationList, new Comparator<Object[]>() {
			public int compare(Object[] o1, Object[] o2) {
				return new Integer(((ExcelField) o1[0]).sort()).compareTo(new Integer(((ExcelField) o2[0]).sort()));
			};
		});
	}

	/**
	 * 获取ExcelField注解的方法
	 * 
	 * @Description
	 * @param cls
	 * @param type
	 * @param groups
	 * @return void
	 * @author yuxiaoyu
	 * @date 2016年4月5日 下午5:06:33
	 */
	private void getAnnotationMethod(Class<?> cls, int type, int... groups) {
		Method[] ms = cls.getDeclaredMethods();
		for (Method m : ms) {
			ExcelField ef = m.getAnnotation(ExcelField.class);
			if (ef != null && (ef.type() == 0 || ef.type() == type)) {
				if (groups != null && groups.length > 0) {
					boolean inGroup = false;
					for (int g : groups) {
						if (inGroup) {
							break;
						}
						for (int efg : ef.groups()) {
							if (g == efg) {
								inGroup = true;
								annotationList.add(new Object[] { ef, m });
								break;
							}
						}
					}
				} else {
					annotationList.add(new Object[] { ef, m });
				}
			}
		}
	}

	/**
	 * 获取ExcelField注解的成员便量
	 * 
	 * @Description
	 * @param cls
	 * @param type
	 * @param groups
	 * @return void
	 * @author yuxiaoyu
	 * @date 2016年4月5日 下午5:07:19
	 */
	private void getAnnotationField(Class<?> cls, int type, int... groups) {
		Field[] fs = cls.getDeclaredFields();
		for (Field f : fs) {
			ExcelField ef = f.getAnnotation(ExcelField.class);
			if (ef != null && (ef.type() == 0 || ef.type() == type)) {
				if (groups != null && groups.length > 0) {
					boolean inGroup = false;
					for (int g : groups) {
						if (inGroup) {
							break;
						}
						for (int efg : ef.groups()) {
							if (g == efg) {
								inGroup = true;
								annotationList.add(new Object[] { ef, f });
								break;
							}
						}
					}
				} else {
					annotationList.add(new Object[] { ef, f });
				}
			}
		}
	}

	/**
	 * 构造函数
	 * 
	 * @param title
	 *            表格标题，传“空值”，表示无标题
	 * @param headers
	 *            表头数组
	 */
	public ExportExcel(String title, String[] headers) {
		initializeTitle(title, Lists.newArrayList(headers));
		initializeHeader(title, Lists.newArrayList(headers));
	}

	/**
	 * 构造函数
	 * 
	 * @param title
	 *            表格标题，传“空值”，表示无标题
	 * @param headerList
	 *            表头列表
	 */
	public ExportExcel(String title, List<String> headerList) {
		initializeTitle(title, headerList);
		initializeHeader(title, headerList);
	}

	/**
	 * 添加一行
	 * 
	 * @return 行对象
	 */
	public void addSheet(String title, String[] headers) {
		this.sheet2 = wb.createSheet(title);
		rownum = 0;
		List<String> headerList = Lists.newArrayList(headers);
		// Create title
		if (StringUtils.isNotBlank(title)) {
			Row titleRow = sheet2.createRow(rownum++);
			titleRow.setHeightInPoints(30);
			Cell titleCell = titleRow.createCell(0);
			titleCell.setCellStyle(styles.get("title"));
			titleCell.setCellValue(title);
			sheet2.addMergedRegion(new CellRangeAddress(titleRow.getRowNum(), titleRow.getRowNum(), titleRow.getRowNum(), headerList.size() - 1));
		}
		// Create header
		if (headerList == null) {
			throw new RuntimeException("headerList not null!");
		}
		Row headerRow = sheet2.createRow(rownum++);
		headerRow.setHeightInPoints(16);
		for (int i = 0; i < headerList.size(); i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellStyle(styles.get("header"));
			String[] ss = StringUtils.split(headerList.get(i), "**", 2);
			if (ss.length == 2) {
				cell.setCellValue(ss[0]);
				Comment comment = this.sheet2.createDrawingPatriarch().createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 5, 6));
				comment.setString(new XSSFRichTextString(ss[1]));
				cell.setCellComment(comment);
			} else {
				cell.setCellValue(headerList.get(i));
			}
			sheet2.autoSizeColumn(i);
		}
		for (int i = 0; i < headerList.size(); i++) {
			int colWidth = sheet2.getColumnWidth(i) * 2;
			sheet2.setColumnWidth(i, colWidth < 3000 ? 3000 : colWidth);
		}
		log.debug("Initialize success.");
	}

	/**
	 * 初始化函数
	 * 
	 * @param title
	 *            表格标题，传“空值”，表示无标题
	 * @param headerList
	 *            表头列表
	 */
	private void initializeHeader(String title, List<String> headerList) {
		// Create header
		if (headerList == null) {
			throw new RuntimeException("headerList not null!");
		}
		Row headerRow = sheet.createRow(rownum++);
		headerRow.setHeightInPoints(16);
		for (int i = 0; i < headerList.size(); i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellStyle(styles.get("header"));
			String[] ss = StringUtils.split(headerList.get(i), "**", 2);
			if (ss.length == 2) {
				cell.setCellValue(ss[0]);
				Comment comment = this.sheet.createDrawingPatriarch().createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 5, 6));
				comment.setString(new XSSFRichTextString(ss[1]));
				cell.setCellComment(comment);
			} else {
				cell.setCellValue(headerList.get(i));
			}
			sheet.autoSizeColumn(i);
		}
		for (int i = 0; i < headerList.size(); i++) {
			int colWidth = sheet.getColumnWidth(i) * 2;
			sheet.setColumnWidth(i, colWidth < 3000 ? 3000 : colWidth);
		}
		log.debug("Initialize success.");
	}

	private void initializeTitle(String title, List<String> headerList) {
		this.wb = new SXSSFWorkbook(500);
		this.sheet = wb.createSheet(title);
		this.styles = createStyles(wb);
		// Create title
		if (StringUtils.isNotBlank(title)) {
			Row titleRow = sheet.createRow(rownum++);
			titleRow.setHeightInPoints(30);
			Cell titleCell = titleRow.createCell(0);
			titleCell.setCellStyle(styles.get("title"));
			titleCell.setCellValue(title);
			sheet.addMergedRegion(new CellRangeAddress(titleRow.getRowNum(), titleRow.getRowNum(), titleRow.getRowNum(), headerList.size() - 1));
		}
	}

	/**
	 * 创建表格样式
	 * 
	 * @param wb
	 *            工作薄对象
	 * @return 样式列表
	 */
	private Map<String, CellStyle> createStyles(Workbook wb) {
		Map<String, CellStyle> styles = new HashMap<String, CellStyle>();

		CellStyle style = wb.createCellStyle();
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		Font titleFont = wb.createFont();
		titleFont.setFontName("Arial");
		titleFont.setFontHeightInPoints((short) 16);
		titleFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
		style.setFont(titleFont);
		styles.put("title", style);

		style = wb.createCellStyle();
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
		style.setBorderTop(CellStyle.BORDER_THIN);
		style.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
		style.setBorderBottom(CellStyle.BORDER_THIN);
		style.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
		Font dataFont = wb.createFont();
		dataFont.setFontName("Arial");
		dataFont.setFontHeightInPoints((short) 10);
		style.setFont(dataFont);
		styles.put("data", style);

		style = wb.createCellStyle();
		style.cloneStyleFrom(styles.get("data"));
		style.setAlignment(CellStyle.ALIGN_LEFT);
		styles.put("data1", style);

		style = wb.createCellStyle();
		style.cloneStyleFrom(styles.get("data"));
		style.setAlignment(CellStyle.ALIGN_CENTER);
		styles.put("data2", style);

		style = wb.createCellStyle();
		style.cloneStyleFrom(styles.get("data"));
		style.setAlignment(CellStyle.ALIGN_RIGHT);
		styles.put("data3", style);

		style = wb.createCellStyle();
		style.cloneStyleFrom(styles.get("data"));
		style.setAlignment(CellStyle.ALIGN_CENTER);
		Font headerFont = wb.createFont();
		headerFont.setFontName("Arial");
		headerFont.setFontHeightInPoints((short) 10);
		headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
		style.setFont(headerFont);
		styles.put("header1", style);

		style = wb.createCellStyle();
		style.cloneStyleFrom(styles.get("data"));
		// style.setWrapText(true);
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		headerFont = wb.createFont();
		headerFont.setFontName("Arial");
		headerFont.setFontHeightInPoints((short) 10);
		headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
		headerFont.setColor(IndexedColors.WHITE.getIndex());
		style.setFont(headerFont);
		styles.put("header", style);

		return styles;
	}

	/**
	 * 添加一行
	 * 
	 * @return 行对象
	 */
	public Row addRow() {
		return sheet.createRow(rownum++);
	}

	/**
	 * 添加一行
	 * 
	 * @return 行对象
	 */
	public Row addRowToSheet2() {
		return sheet2.createRow(rownum++);
	}

	/**
	 * 添加一个单元格，指定样式
	 * @Description
	 * @param row
	 * @param column
	 * @param val
	 * @param styleType
	 * @return
	 * @return Cell
	 * @author yuxiaoyu
	 * @date 2016年4月5日 下午2:54:52
	 */
	public Cell addCell(Row row, int column, String val, String styleType) {
		Cell cell = row.createCell(column);
		CellStyle style = styles.get(styleType);
		if (val == null) {
			cell.setCellValue(StringUtils.EMPTY);
		} else if (val instanceof String) {
			cell.setCellValue((String) val);
		}
		cell.setCellStyle(style);
		return cell;
	}

	/**
	 * 添加一个单元格
	 * 
	 * @param row
	 *            添加的行
	 * @param column
	 *            添加列号
	 * @param val
	 *            添加值
	 * @return 单元格对象
	 */
	public Cell addCell(Row row, int column, Object val) {
		return this.addCell(row, column, val, 0, Class.class);
	}

	/**
	 * 添加一个单元格
	 * 
	 * @param row
	 *            添加的行
	 * @param column
	 *            添加列号
	 * @param val
	 *            添加值
	 * @param align
	 *            对齐方式（1：靠左；2：居中；3：靠右）
	 * @return 单元格对象
	 */
	public Cell addCell(Row row, int column, Object val, int align, Class<?> fieldType) {
		Cell cell = row.createCell(column);
		CellStyle style = styles.get("data" + (align >= 1 && align <= 3 ? align : StringUtils.EMPTY));
		try {
			if (val == null) {
				cell.setCellValue(StringUtils.EMPTY);
			} else if (val instanceof String) {
				cell.setCellValue((String) val);
			} else if (val instanceof Integer) {
				cell.setCellValue((Integer) val);
			} else if (val instanceof Long) {
				cell.setCellValue((Long) val);
			} else if (val instanceof Double) {
				cell.setCellValue((Double) val);
			} else if (val instanceof Float) {
				cell.setCellValue((Float) val);
			} else if (val instanceof Number) {
				cell.setCellValue(null == val ? StringUtils.EMPTY : val.toString());
			} else if (val instanceof Date) {
				DataFormat format = wb.createDataFormat();
				style = wb.createCellStyle();
				style.cloneStyleFrom(styles.get("data"));
				style.setAlignment(CellStyle.ALIGN_CENTER);
				style.setDataFormat(format.getFormat("yyyy-MM-dd"));
				cell.setCellValue((Date) val);
			} else {
				if (fieldType != Class.class) {
					cell.setCellValue((String) fieldType.getMethod("setValue", Object.class).invoke(null, val));
				} else {
					cell.setCellValue((String) Class
							.forName(
									this.getClass().getName()
											.replaceAll(this.getClass().getSimpleName(), "fieldtype." + val.getClass().getSimpleName() + "Type"))
							.getMethod("setValue", Object.class).invoke(null, val));
				}
			}
		} catch (Exception ex) {
			log.info("Set cell value [" + row.getRowNum() + "," + column + "] error: " + ex.toString());
			cell.setCellValue(val.toString());
		}
		cell.setCellStyle(style);
		return cell;
	}

	/**
	 * 添加数据（通过annotation.ExportField添加数据）
	 * 
	 * @return list 数据列表
	 */
	public <E> ExportExcel setDataList(List<E> list) {
		for (E e : list) {
			int colunm = 0;
			Row row = this.addRow();
			StringBuilder sb = new StringBuilder();
			for (Object[] os : annotationList) {
				ExcelField ef = (ExcelField) os[0];
				Object val = null;
				// Get entity value
				try {
					if (StringUtils.isNotBlank(ef.value())) {
						val = Reflections.invokeGetter(e, ef.value());
					} else {
						if (os[1] instanceof Field) {
							val = Reflections.invokeGetter(e, ((Field) os[1]).getName());
						} else if (os[1] instanceof Method) {
							val = Reflections.invokeMethod(e, ((Method) os[1]).getName(), new Class[] {}, new Object[] {});
						}
					}
					// If is dict, get dict label
					if (StringUtils.isNotBlank(ef.dictType())) {
						val = DictUtils.getDictLabel(val == null ? "" : val.toString(), ef.dictType(), "");
					}
				} catch (Exception ex) {
					// Failure to ignore
					log.info(ex.toString());
					val = "";
				}
				this.addCell(row, colunm++, val, ef.align(), ef.fieldType());
				sb.append(val + ", ");
			}
			log.debug("Write success: [" + row.getRowNum() + "] " + sb.toString());
		}
		return this;
	}

	/**
	 * 输出数据流
	 * 
	 * @param os
	 *            输出数据流
	 */
	public ExportExcel write(OutputStream os) throws IOException {
		wb.write(os);
		return this;
	}

	/**
	 * 输出到客户端
	 * 
	 * @param fileName
	 *            输出文件名
	 */
	public ExportExcel write(HttpServletResponse response, String fileName) throws IOException {
		response.reset();
		response.setContentType("application/octet-stream; charset=utf-8");
		response.setHeader("Content-Disposition", "attachment; filename=" + Encodes.urlEncode(fileName));
		write(response.getOutputStream());
		return this;
	}

	/**
	 * 输出到文件
	 * 
	 * @param fileName
	 *            输出文件名
	 */
	public ExportExcel writeFile(String name) throws FileNotFoundException, IOException {
		FileOutputStream os = new FileOutputStream(name);
		this.write(os);
		return this;
	}

	/**
	 * 清理临时文件
	 */
	public ExportExcel dispose() {
		wb.dispose();
		return this;
	}

	// /**
	// * 导出测试
	// */
	// public static void main(String[] args) throws Throwable {
	//
	// List<String> headerList = Lists.newArrayList();
	// for (int i = 1; i <= 10; i++) {
	// headerList.add("表头"+i);
	// }
	//
	// List<String> dataRowList = Lists.newArrayList();
	// for (int i = 1; i <= headerList.size(); i++) {
	// dataRowList.add("数据"+i);
	// }
	//
	// List<List<String>> dataList = Lists.newArrayList();
	// for (int i = 1; i <=1000000; i++) {
	// dataList.add(dataRowList);
	// }
	//
	// ExportExcel ee = new ExportExcel("表格标题", headerList);
	//
	// for (int i = 0; i < dataList.size(); i++) {
	// Row row = ee.addRow();
	// for (int j = 0; j < dataList.get(i).size(); j++) {
	// ee.addCell(row, j, dataList.get(i).get(j));
	// }
	// }
	//
	// ee.writeFile("target/export.xlsx");
	//
	// ee.dispose();
	//
	// log.debug("Export success.");
	//
	// }

}