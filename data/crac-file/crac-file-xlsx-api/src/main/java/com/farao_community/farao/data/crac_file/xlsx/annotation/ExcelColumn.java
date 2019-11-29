/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file.xlsx.annotation;

import com.farao_community.farao.data.crac_file.xlsx.converter.NoopConverter;
import com.farao_community.farao.data.crac_file.xlsx.validator.NoopValidater;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A Column in the excel workbook
 *
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
@Target({ElementType.FIELD, ElementType.LOCAL_VARIABLE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelColumn {
    /**
     * @return Name of the column in the excelWorkbook - this must be the value in that column
     */
    String name() default "";

    /**
     * @return Position of the column. Starts with the 0 index.
     */
    int position();

    /**
     * @return Format for the value of the column, iff applicable
     */
    String dataFormat() default "";

    /**
     * @return Converter to use to convert the string value to the field type's value
     */
    Class<?> convertorClass() default NoopConverter.class;

    /**
     * @return validater to use to validate the string value to the field type's value
     */
    Class<?> validatorClass() default NoopValidater.class;

}
