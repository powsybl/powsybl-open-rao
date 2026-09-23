package com.powsybl.openrao.commons;

public class AreaPairBoundary {
    private final String areaLeft;
    private final String areaRight;

    public AreaPairBoundary(String areaLeft, String areaRight) {
        this.areaLeft = areaLeft;
        this.areaRight = areaRight;
    }

    public static AreaPairBoundary of(String area1, String area2) {
        return new AreaPairBoundary(area1, area2);
    }

    public String getAreaLeft() {
        return areaLeft;
    }

    public String getAreaRight() {
        return areaRight;
    }

    /** True if 'area' is the canonical "from" side of this border, by area id ordering. */
    public boolean isFromSide(String area) {
        return areaLeft.compareTo(areaRight) < 0
                ? areaLeft.equals(area)
                : areaRight.equals(area);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AreaPairBoundary that)) {
            return false;
        }
        return areaLeft.equals(that.areaLeft) && areaRight.equals(that.areaRight)
                || areaLeft.equals(that.areaRight) && areaRight.equals(that.areaLeft);
    }

    @Override
    public int hashCode() {
        // order-independent hash
        return areaLeft.hashCode() + areaRight.hashCode();
    }

    @Override
    public String toString() {
        return areaLeft + "/" + areaRight;
    }
}
