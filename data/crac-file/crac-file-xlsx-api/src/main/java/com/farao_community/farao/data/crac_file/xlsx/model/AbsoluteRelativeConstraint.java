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

public enum AbsoluteRelativeConstraint {
    ABS("ABSOLUTE"),
    REL("RELATIVE");

    @Getter
    private final String label;

    private AbsoluteRelativeConstraint(String label) {
        this.label = label;
    }
}
