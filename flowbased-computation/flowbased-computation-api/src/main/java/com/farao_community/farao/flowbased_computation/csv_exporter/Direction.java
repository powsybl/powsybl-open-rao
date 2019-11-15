/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.csv_exporter;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
enum Direction {
    DIRECT("DIRECT", 1),
    OPPOSITE("OPPOSITE", -1);

    private String name;
    private int sign;

    public String getName() {
        return name;
    }

    public int getSign() {
        return  sign;
    }

    Direction(String name, int sign) {
        this.name = name;
        this.sign = sign;
    }
}
