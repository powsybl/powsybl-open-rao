/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.data.crac.api.range.ConnectedArea;
import com.powsybl.openrao.data.crac.api.range.StandardRange;

import java.util.List;
import java.util.Objects;

/**
 * @author Pedro Tobarra {@literal <pedro.tobarra at artelys.com>}
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
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConnectedAreaImpl otherConnectedArea = (ConnectedAreaImpl) o;
        return area.equals(otherConnectedArea.area) && borderRanges.equals(otherConnectedArea.borderRanges);
    }

    @Override
    public int hashCode() {
        return Objects.hash(area, borderRanges);
    }
}
