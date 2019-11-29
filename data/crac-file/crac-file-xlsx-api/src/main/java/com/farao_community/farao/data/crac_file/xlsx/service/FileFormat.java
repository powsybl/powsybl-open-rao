/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
package com.farao_community.farao.data.crac_file.xlsx.service;

import lombok.Getter;

import java.util.Arrays;

/**
 * Enum File Format
 */
public enum FileFormat {
    XLS("application/vnd.ms-excel"),
    XLSX("application/vnd.ms-excel"),
    TIKA_OOXML("application/x-tika-ooxml"),
    UNKNOWN("unknown");

    @Getter
    private final String label;

    FileFormat(String extension) {
        this.label = extension;
    }

    public static FileFormat fromLabel(String value) {
        return Arrays.stream(FileFormat.values())
                .filter(e -> e.label.equalsIgnoreCase(value)).findFirst()
                .orElse(FileFormat.UNKNOWN);
    }
}
