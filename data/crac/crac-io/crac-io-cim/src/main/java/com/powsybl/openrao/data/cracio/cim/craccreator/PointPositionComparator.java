/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.cim.craccreator;

import com.powsybl.openrao.data.cracio.cim.xsd.Point;

import java.util.Comparator;

/**
 * @author Marc Schwitzguebel {@literal <marc.schwitzguebel at rte-france.com>}
 */
public class PointPositionComparator implements Comparator<Point> {

    @Override
    public int compare(Point point1, Point point2) {
        return Integer.compare(point1.getPosition(), point2.getPosition());
    }
}
