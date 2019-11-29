/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file.xlsx.column;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
@Builder
@Data
public final class ExcelColumnMapping {
    private final ExcelColumnInfo rowNumberInfo;
    private final List<ExcelColumnInfo> columns;
}
