package com.farao_community.farao.data.crac_api;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface ThresholdAdder {

    /**
     * Set the unit for the threshold to add to cnec
     * @param unit: unit of the threshold
     * @return the {@code ThresholdAdder} instance
     */
    ThresholdAdder setUnit(Unit unit);

    /**
     * Set the value of the threshold to add to cnec
     * @param maxValue: value of threshold
     * @return the {@code ThresholdAdder} instance
     */
    ThresholdAdder setMaxValue(Double maxValue);

    /**
     * Set the side of the threshold to add to cnec
     * @param side: side of threshold
     * @return the {@code ThresholdAdder} instance
     */
    ThresholdAdder setSide(Side side);

    /**
     * Set the direction of the threshold to add to cnec
     * @param direction: direction of threshold
     * @return the {@code ThresholdAdder} instance
     */
    ThresholdAdder setDirection(Direction direction);

    /**
     * Add the new threshold to cnec
     * @return the {@code CnecAdder} instance
     */
    CnecAdder add();
}
