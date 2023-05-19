/*
 * Copyright (c) 2008-2023, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.platform.demos.banking.cva.cvastp;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.function.FunctionEx;
import com.hazelcast.jet.datamodel.Tuple3;

/**
 * <p>Prepare job output, converting from a two-dimensional array
 * to an Excel spreadsheet to download
 * </p>
 */
public class XlstFileAsByteArray {

    private static final Logger LOGGER = LoggerFactory.getLogger(XlstFileAsByteArray.class);

    // Cell Width, each unit is 1/256th of char, so 40 chars wide
    private static final int CELL_WIDTH = 256 * 40;
    private static final short FONT_HEIGHT = 16;

    /**
     * <p>A function to convert a tuple3 of job name, timestamp,
     * a two-dimensional array of cells for the Excel workbook
     * into the Excel format.
     * </p>
     * <p>Create an anonymous workbook, attach a single sheet with the
     * data in it, and turn into a "{@code byte[]}" to store in the
     * grid for later downloading.
     * </p>
     */
    public static final FunctionEx<Tuple3<String, Long, Object[][]>, byte[]>
        CONVERT_TUPLE3_TO_BYTE_ARRAY =
                (Tuple3<String, Long, Object[][]> tuple3) -> {

                // Extract from the tuples
                String jobName = tuple3.f0();
                long timestamp = tuple3.f1();
                Object[][] content = tuple3.f2();

                // Create XLST content
                XSSFWorkbook workbook = new XSSFWorkbook();
                addDataSheet(workbook, jobName, timestamp, content);

                // Format for grid storage
                byte[] bytes = null;
                try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                    workbook.write(byteArrayOutputStream);
                    bytes = byteArrayOutputStream.toByteArray();
                    workbook.close();
                    return bytes;
                } catch (Exception e) {
                    LOGGER.error(jobName, e);
                    return "".getBytes(StandardCharsets.UTF_8);
                }
            };


    /**
     * <p>Add a single sheet to the Excel workbook, one row per CVA.
     * </p>
     *
     * @param workbook An instance of {@link org.apache.poi.ss.usermodel.Workbook}
     * @param jobName The Jet job creating this sheet
     * @param timestamp Launch time of the Jet job
     * @param content 2-D array with header
     */
    private static void addDataSheet(XSSFWorkbook workbook,
            String jobName,
            long timestamp,
            Object[][] content) {

        Instant instant = Instant.ofEpochMilli(timestamp);
        LocalDateTime localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();

        // ":" and "/" not allowed in Excel sheet name
        String sheetName = "CVA "
                + localDateTime.getHour() + "_"
                + localDateTime.getMinute()
                + " - "
                + localDateTime.getDayOfMonth() + "_"
                + localDateTime.getMonth() + "_"
                + localDateTime.getYear();

        Sheet sheet = workbook.createSheet(sheetName);

        CellStyle headerStyle = createCellStyle(workbook, true);
        CellStyle normalStyle = createCellStyle(workbook, false);

        addHeaderRow(workbook, sheet, headerStyle, content[0]);
        addDataRows(workbook, sheet, normalStyle, content);

        workbook.setActiveSheet(0);
    }

    /**
     * <p>Create a style for header and non-header cells.
     * </p>
     *
     * @param workbook Workbook to add style to
     * @param header Is the header row or not
     * @return
     */
    public static CellStyle createCellStyle(XSSFWorkbook workbook, boolean header) {
        XSSFFont font = workbook.createFont();
        if (header) {
            font.setFontName("Arial");
        } else {
            font.setFontName("Courier");
        }
        font.setFontHeightInPoints(FONT_HEIGHT);
        font.setBold(true);

        CellStyle cellStyle = workbook.createCellStyle();
        if (header) {
            cellStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        } else {
            cellStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        }
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cellStyle.setFont(font);

        return cellStyle;
    }

    /**
     * <p>Add a header row for the given columns.
     * </p>
     *
     * @param workbook The only workbook
     * @param sheet The only sheet in the workbook
     * @param cellStyle To style the cells in the row
     * @param columns A list of strings
     */
    public static void addHeaderRow(Workbook workbook, Sheet sheet, CellStyle cellStyle, Object[] columns) {
        Row header = sheet.createRow(0);
        for (int i = 0 ; i < columns.length; i++) {
            sheet.setColumnWidth(i, CELL_WIDTH);
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i].toString());
            cell.setCellStyle(cellStyle);
        }
    }

    /**
     * <p>Add rows for each value in the given content, ignoring
     * the first row as that is handled by {@link #addHeaderRow(Workbook, Sheet, CellStyle, Object[])}
     * </p>
     *
     * @param workbook The only workbook
     * @param sheet The only sheet in the workbook
     * @param cellStyle To style the cells in each row
     * @param content A 2 dimension array
     */
    public static void addDataRows(Workbook workbook, Sheet sheet, CellStyle cellStyle,
            Object[][] content) {

        for (int i = 1 ; i < content.length; i++) {
            Row row = sheet.createRow(i);

            Object[] fields = content[i];
            for (int j = 0 ; j < fields.length; j++) {
                Cell cell = row.createCell(j);
                cell.setCellStyle(cellStyle);
                if (fields[j] instanceof Double) {
                    cell.setCellValue(((Double) fields[j]).doubleValue());
                } else {
                    cell.setCellValue(fields[j].toString());
                }
            }
        }
    }


}
