/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file.xlsx.column;

import lombok.Builder;
import lombok.Data;

/**
 * Information about a Column in a Java Bean
 *
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
@Data
public class ExcelColumnInfo {

    private final String name;

    private final String fieldName;

    private final int position;

    private final String dataFormat;

    private final Class<?> type;

    private final Class<?> converterClass;

    private final Class<?> validatorClass;

    @Builder
    public ExcelColumnInfo(String name, String fieldName, int position, String dataFormat, Class<?> type, Class<?> converterClass, Class<?> validatorClass) {
        this.name = name;
        this.fieldName = fieldName;
        this.position = position;
        this.dataFormat = dataFormat;
        this.type = type;
        this.converterClass = converterClass;
        this.validatorClass = validatorClass;
    }
}
