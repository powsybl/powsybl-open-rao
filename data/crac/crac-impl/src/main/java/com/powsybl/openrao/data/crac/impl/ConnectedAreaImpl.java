/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.data.crac.api.range.StandardRange;
import com.powsybl.openrao.data.crac.api.rangeaction.ConnectedArea;

import java.util.List;
import java.util.Objects;

/**
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
public class ConnectedAreaImpl implements ConnectedArea {

    private final String area;
    private final List<StandardRange> borderRanges;

    ConnectedAreaImpl(String area, List<StandardRange> borderRanges) {
        this.area = area;
        this.borderRanges = borderRanges;
    }

    @Override
    public String getArea() {
        return area;
    }

    @Override
    public List<StandardRange> getBorderRanges() {
        return borderRanges;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConnectedArea otherConnectedArea)) {
            return false;
        }
        return this.area.equals(otherConnectedArea.getArea()) && this.borderRanges.equals(otherConnectedArea.getBorderRanges());
    }

    @Override
    public int hashCode() {
        return Objects.hash(area, borderRanges);
    }
}
