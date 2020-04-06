/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file.xlsx.handler;

import com.farao_community.farao.data.crac_file.xlsx.ExcelReaderUtil;
import com.farao_community.farao.data.crac_file.xlsx.annotation.ExcelColumn;
import com.farao_community.farao.data.crac_file.xlsx.annotation.ExcelRowNumber;
import com.farao_community.farao.data.crac_file.xlsx.column.ExcelColumnInfo;
import com.farao_community.farao.data.crac_file.xlsx.column.ExcelColumnMapping;
import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_file.xlsx.converter.NoopConverter;
import com.farao_community.farao.data.crac_file.xlsx.model.TimesSeries;
import io.vavr.control.Validation;
import lombok.Builder;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
public class EntityHandler<T> {

    private final EntityExcelSheetHandler<T> entitySheetHandler;
    private final String sheetName;
    private final boolean skipHeaderRow;
    private final TimesSeries timesSeries;

    @Builder
    public EntityHandler(Class<T> clazz, String sheetName, TimesSeries timesSeries, boolean skipHeaderRow) {
        Objects.requireNonNull(clazz);
        this.sheetName = sheetName;
        this.skipHeaderRow = skipHeaderRow;
        this.timesSeries = timesSeries;
        this.entitySheetHandler = createSheetHandler(clazz);
    }

    private EntityExcelSheetHandler<T> createSheetHandler(Class<T> clazz) {
        ExcelColumnMapping columnMapping = readColumnInfoViaReflection(clazz);
        final ExcelColumnInfo rowNumberColumn = columnMapping.getRowNumberInfo();
        final List<ExcelColumnInfo> list = columnMapping.getColumns();
        final ExcelColumnInfo[] columns = new ExcelColumnInfo[list.size()];
        int index = 0;
        for (ExcelColumnInfo columnInfo : list) {
            index = columnInfo.getPosition();
            if (index > columns.length - 1) {
                throw new FaraoException(
                        String.format("Column index out of range. index=%s columnCount=%s. Ensure there @Column annotations for all indexes from 0 to %s",
                                index, columns.length, columns.length - 1));
            }
            if (!Objects.isNull(columns[index])) {
                throw new FaraoException(String.format("Cannot map two columns to the same index: '%s'", index));
            }
            columns[index] = columnInfo;
        }
        return EntityExcelSheetHandler
                .builder()
                .clazz((Class) clazz)
                .skipHeaderRow(skipHeaderRow)
                .columns(columns)
                .rowNumberColumn(rowNumberColumn)
                .timesSeries(timesSeries)
                .build();
    }

    private ExcelColumnMapping readColumnInfoViaReflection(Class<?> clazz) {
        Field[] fieldArray = clazz.getDeclaredFields();
        ArrayList<ExcelColumnInfo> list = new ArrayList<>(fieldArray.length);
        ExcelColumnInfo rowNumberColumn = null;
        for (Field field : fieldArray) {
            ExcelRowNumber rowNumberAnnotation = field.getAnnotation(ExcelRowNumber.class);

            if (!Objects.isNull(rowNumberAnnotation)) {
                rowNumberColumn = ExcelColumnInfo
                        .builder()
                        .fieldName(field.getName())
                        .position(-1).dataFormat(null)
                        .type(Integer.class)
                        .converterClass(NoopConverter.class)
                        .validatorClass(Validation.class)
                        .build();
                continue;
            }

            ExcelColumn annotation = field.getAnnotation(ExcelColumn.class);
            if (Objects.nonNull(annotation)) {
                Class<?> converter = annotation.convertorClass();
                Class<?> validater = annotation.validatorClass();
                list.add(new ExcelColumnInfo(
                        annotation.name().trim(),
                        field.getName(),
                        annotation.position(),
                        annotation.dataFormat(),
                        field.getType(),
                        converter, validater));
            }
        }

        if (list.isEmpty()) {
            throw new FaraoException(String.format("Class %s does not have @Column annotations", clazz.getName()));
        }
        list.trimToSize();
        return ExcelColumnMapping.builder().rowNumberInfo(rowNumberColumn).columns(list).build();
    }

    /**
     * Returns the extracted entities as an immutable list.
     */
    public List<T> readAsList() {
        return Collections.unmodifiableList(this.entitySheetHandler.read(null, sheetName));
    }

    public void process(InputStream inputStream) throws FaraoException {
        ExcelReaderUtil.process(inputStream, sheetName, this.entitySheetHandler);
    }
}
