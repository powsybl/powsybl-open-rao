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

import lombok.Getter;

public enum TimesSeries {
    TIME_0030("1"),
    TIME_0130("2"),
    TIME_0230("3"),
    TIME_0330("4"),
    TIME_0430("5"),
    TIME_0530("6"),
    TIME_0630("7"),
    TIME_0730("8"),
    TIME_0830("9"),
    TIME_0930("10"),
    TIME_1030("11"),
    TIME_1130("12"),
    TIME_1230("13"),
    TIME_1330("14"),
    TIME_1430("15"),
    TIME_1530("16"),
    TIME_1630("17"),
    TIME_1730("18"),
    TIME_1830("19"),
    TIME_1930("20"),
    TIME_2030("21"),
    TIME_2130("22"),
    TIME_2230("23"),
    TIME_2330("24");

    @Getter
    private final String label;

    TimesSeries(String label) {
        this.label = label;
    }
}
