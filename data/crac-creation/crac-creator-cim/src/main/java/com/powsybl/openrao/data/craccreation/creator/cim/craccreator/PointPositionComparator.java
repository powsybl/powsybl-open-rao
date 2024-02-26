package com.powsybl.openrao.data.craccreation.creator.cim.craccreator;

import com.powsybl.openrao.data.craccreation.creator.cim.xsd.Point;

import java.util.Comparator;

public class PointPositionComparator implements Comparator<Point> {

    @Override
    public int compare(Point point1, Point point2) {
        return Integer.compare(point1.getPosition(), point2.getPosition());
    }
}
