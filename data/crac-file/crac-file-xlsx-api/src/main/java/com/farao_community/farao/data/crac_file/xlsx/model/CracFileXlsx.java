/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
package com.farao_community.farao.data.crac_file.xlsx.model;

import com.farao_community.farao.data.crac_file.xlsx.annotation.ExcelColumn;
import com.farao_community.farao.data.crac_file.xlsx.annotation.ExcelRowNumber;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Business object of the CRAC file
 */

@Builder
@Value
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CracFileXlsx {
    @ExcelRowNumber
    private int rowNumber;
    @ExcelColumn(position = 3)
    private final String id;
    @ExcelColumn(position = 3)
    private final String name;
    @ExcelColumn(position = 3)
    private final String sourceFormat;
    @ExcelColumn(position = 3)
    private final String description;
    private final PreContingencyXlsx preContingency;
    private final List<ContingencyXlsx> contingencies;
}
