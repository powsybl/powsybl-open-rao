/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file.xlsx.handler;

import com.farao_community.farao.data.crac_file.xlsx.CellExcelReader;
import com.farao_community.farao.data.crac_file.xlsx.column.ExcelColumnInfo;
import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_file.xlsx.converter.Converter;
import com.farao_community.farao.data.crac_file.xlsx.model.BranchTimeSeries;
import com.farao_community.farao.data.crac_file.xlsx.model.TimesSeries;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.usermodel.XSSFComment;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
@Slf4j
@Data
public final class EntityExcelSheetHandler<T> implements CellExcelReader {

    private final ExcelColumnInfo rowNumberColumn;
    private final ExcelColumnInfo[] columns;
    private final List<T> entities;
    private final Class<T> type;
    private final int maxColIndex;
    private final boolean skipHeaderRow;
    private boolean isHeaderRow = false;
    private final TimesSeries timesSeries;
    private int currentRow = -1;
    private int currentCol = -1;
    private T entity;

    @Builder
    EntityExcelSheetHandler(Class<T> clazz, ExcelColumnInfo rowNumberColumn, ExcelColumnInfo[] columns, TimesSeries timesSeries, boolean skipHeaderRow) {
        Objects.requireNonNull(clazz);
        this.rowNumberColumn = rowNumberColumn;
        this.columns = columns;
        this.entities = new ArrayList<>();
        this.type = clazz;
        this.maxColIndex = columns.length - 1;
        this.skipHeaderRow = skipHeaderRow;
        this.timesSeries = timesSeries;
    }

    @Override
    public List<T> read(File file, String sheet) {
        return Collections.unmodifiableList(this.entities);
    }

    @Override
    public void startRow(int i) {
        currentRow = i;
        // skip the header row
        if (currentRow == 0) {
            isHeaderRow = true;
            return;
        } else {
            isHeaderRow = false;
        }
        try {
            entity = (T) type.newInstance();
            // Write to the field with the @RowNumber annotation here if it exists
            if (!Objects.isNull(rowNumberColumn)) {
                writeColumnField(entity, String.valueOf(i), rowNumberColumn, timesSeries, i);
            }
        } catch (InstantiationException | IllegalAccessException e) {
            throw new FaraoException(String.format("Failed to create and instance of '%s'", type.getName()), e);
        }
    }

    @Override
    public void endRow(int i) {
        if (Objects.nonNull(entity) && Objects.nonNull(entity.toString()) && !entity.toString().equals("")) {
            this.entities.add(entity);
            entity = null;
        }
    }

    @Override
    public void cell(String cellReference, String formattedValue, XSSFComment xssfComment) {
        // gracefully handle missing cellReference here in a similar way as XSSFCell does
        String cellRef = null;
        if (cellReference == null) {
            cellRef = new CellAddress(currentRow, currentCol).formatAsString();
        } else {
            cellRef = cellReference;
        }

        int column = new CellReference(cellRef).getCol();
        currentCol = column;

        if (column > maxColIndex) {
            log.warn(String.format("Invalid Column index found: '%s'", column));
            return;
        }

        ExcelColumnInfo currentColumnInfo = columns[column];

        if (Objects.isNull(entity) || Objects.isNull(formattedValue) || formattedValue.isEmpty()) {
            return;
        }
        writeColumnField(entity, formattedValue, currentColumnInfo, timesSeries, currentRow);
    }

    /**
     * Write the value read from the excel cell to a field
     */
    private void writeColumnField(T object, String formattedValue, ExcelColumnInfo currentColumnInfo, TimesSeries timesSeries, int rowNum) {
        String fieldName = currentColumnInfo.getFieldName();
        try {
            if (Objects.nonNull(timesSeries) && Objects.equals(timesSeries.getLabel(), currentColumnInfo.getName())) {
                ((BranchTimeSeries) object).setCurrentLimit(Float.valueOf(formattedValue));
            }
            Converter converter = (Converter) currentColumnInfo.getConverterClass().newInstance();
            Object value = null;
            value = converter.convert(formattedValue, rowNum);
            Field field = type.getDeclaredField(currentColumnInfo.getFieldName());
            boolean access = field.isAccessible();
            if (!access) {
                field.setAccessible(true);
            }
            field.set(entity, value);
            field.setAccessible(field.isAccessible() && access);
        } catch (IllegalArgumentException e) {
            throw new FaraoException(String.format("Failed to write value %s to field %s at row %s", formattedValue, fieldName, rowNum));
        } catch (InstantiationException | NoSuchFieldException | IllegalAccessException e) {
            log.error(String.format("Failed to set field: %s", fieldName), e);
        }
    }

    @Override
    public void headerFooter(String text, boolean b, String tagName) {
        // Skip, no headers or footers
    }
}
