/*
 * Copyright (c) 2008-2020, Hazelcast, Inc. All Rights Reserved.
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.function.FunctionEx;
import com.hazelcast.jet.datamodel.Tuple4;

/**
 * <p>Prepare job output as an Excel spreadsheet to download
 */
public class XlstFileAsByteArray {

    private static final Logger LOGGER = LoggerFactory.getLogger(XlstFileAsByteArray.class);

    /**
     * <p>Columns to add to the spreadsheet from the CVAs (all of them!)
     * and their pretty-print labels.
     * </p>
     */
    private static final List<String> CVA_COLUMNS =
            List.of("counterparty", "cva");
    private static final List<String> CVA_COLUMNS_LABELS =
            List.of("CounterParty Code", "CVA");

    /**
     * <p>Columns to add to the spreadsheet from the counterparty CDS JSON,
     * and their pretty-print labels.
     * </p>
     */
    private static final List<String> CP_CDS_COLUMNS =
            List.of("shortname", "date", "redcode", "tier");
    private static final List<String> CP_CDS_COLUMNS_LABELS =
            List.of("Name", "Date", "Red Code", "Tier");

    /**
     * <p>All columns in the spreadsheet.
     * </p>
     */
    private static final List<String> COLUMNS =
            Stream.concat(CVA_COLUMNS.stream(), CP_CDS_COLUMNS.stream()).collect(Collectors.toList());
    private static final List<String> COLUMNS_LABELS =
            Stream.concat(CVA_COLUMNS_LABELS.stream(), CP_CDS_COLUMNS_LABELS.stream()).collect(Collectors.toList());


    // Cell Width, each unit is 1/256th of char, so 40 chars wide
    private static final int CELL_WIDTH = 256 * 40;
    private static final short FONT_HEIGHT = 16;

    /**
     * <p>A function to convert a tuple4 of job name, timestamp,
     * a sorted list of CVAs and a sorted list of the corresponding
     * counterparty CDSes into an Excel spreadsheet.
     * </p>
     * <p>Create an anonymous workbook, attach a single sheet with the
     * data in it, and turn into a "{@code byte[]}" to store in the
     * grid for later downloading.
     * </p>
     */
    public static final FunctionEx<Tuple4<String, Long, List<Entry<String, Double>>,
        List<Entry<String, HazelcastJsonValue>>>, byte[]>
        CONVERT_TUPLES_TO_BYTE_ARRAY =
                (Tuple4<String, Long, List<Entry<String, Double>>, List<Entry<String, HazelcastJsonValue>>> tuple4) -> {

                // Extract from the tuples
                String jobName = tuple4.f0();
                long timestamp = tuple4.f1();
                List<Entry<String, Double>> cvaList = tuple4.f2();
                List<Entry<String, HazelcastJsonValue>> cpCdsList = tuple4.f3();

                // Create XLST content
                XSSFWorkbook workbook = new XSSFWorkbook();
                addDataSheet(workbook, jobName, timestamp, cvaList, cpCdsList);

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
     * @param cvaList All CVAs, in order
     * @param cpCdsList Counterparties, in same order as CVAs
     */
    private static void addDataSheet(XSSFWorkbook workbook,
            String jobName,
            long timestamp,
            List<Entry<String, Double>> cvaList,
            List<Entry<String, HazelcastJsonValue>> cpCdsList) {

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

        addHeaderRow(workbook, sheet, headerStyle);
        addDataRows(workbook, sheet, normalStyle, cvaList, cpCdsList);

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
     * <p>Add a header row for the expected columns.
     * </p>
     *
     * @param workbook The only workbook
     * @param sheet The only sheet in the workbook
     * @param cellStyle To style the cells in the row
     */
    public static void addHeaderRow(Workbook workbook, Sheet sheet, CellStyle cellStyle) {
        Row header = sheet.createRow(0);
        for (int i = 0 ; i < COLUMNS.size(); i++) {
            sheet.setColumnWidth(i, CELL_WIDTH);
            Cell cell = header.createCell(i);
            cell.setCellValue(COLUMNS_LABELS.get(i));
            cell.setCellStyle(cellStyle);
        }
    }

    /**
     * <p>Add rows for each value in the input list.
     * </p>
     *
     * @param workbook The only workbook
     * @param sheet The only sheet in the workbook
     * @param cellStyle To style the cells in each row
     * @param cvaList Main input list
     * @param cpCdsList Side input list, should be in same order and same length as main
     */
    public static void addDataRows(Workbook workbook, Sheet sheet, CellStyle cellStyle,
            List<Entry<String, Double>> cvaList,
            List<Entry<String, HazelcastJsonValue>> cpCdsList) {

        int offsetForHeader = 1;

        for (int i = 0 ; i < cvaList.size(); i++) {
            int rowCount = i + offsetForHeader;
            Row row = sheet.createRow(rowCount);

            Object[] fields = getFields(cvaList.get(i), cpCdsList.get(i));
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

    /**
     * <p>Extract the required data fields. For the CVA entry, it's both fields.
     * For the Counterparty CDS, it's the named fields in the JSON Object.
     * </p>
     *
     * @param cvaEntry Counterparty code and amount pair
     * @param cpCdsEntry Counterparty code and JSON
     * @return
     */
    private static Object[] getFields(Entry<String, Double> cvaEntry, Entry<String, HazelcastJsonValue> cpCdsEntry) {

        List<Object> result = new ArrayList<>();
        result.add(cvaEntry.getKey());
        result.add(cvaEntry.getValue());

        if (!cvaEntry.getKey().equals(cpCdsEntry.getKey())) {
            // Should never occur unless someone changes the sort ordering
            LOGGER.error("Key mismatch, '{}'!='{}'", cvaEntry.getKey(), cpCdsEntry.getKey());
            return result.toArray();
        }

        // For easier field lookup
        String jsonStr = cpCdsEntry.getValue().toString();
        JSONObject json = null;
        try {
            json = new JSONObject(jsonStr);
        } catch (JSONException e) {
            LOGGER.error(cpCdsEntry.getKey(), e);
            for (int i = 0 ; i < CP_CDS_COLUMNS.size(); i++) {
                result.add("?");
            }
            return result.toArray();
        }

        // Find the named fields
        for (String fieldName : CP_CDS_COLUMNS) {
            try {
                Object field = json.get(fieldName);
                if (field instanceof String) {
                    result.add(field);
                } else {
                    LOGGER.error("{},{} field type {} not handled", cpCdsEntry.getKey(), fieldName, field.getClass());
                }
            } catch (JSONException e) {
                LOGGER.error(cpCdsEntry.getKey() + "," + fieldName, e);
                result.add("?");
            }
        }

        return result.toArray();
    }

}
