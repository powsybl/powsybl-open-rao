/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.intertemporalconstraints;

import com.powsybl.openrao.commons.OpenRaoException;

import java.util.Optional;

/**
 * Set of physical and operational constraints that apply on a generator.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class GeneratorConstraints {
    private final String generatorId;
    private final Double leadTime;
    private final Double lagTime;
    private final Double minUpTime;
    private final Double maxUpTime;
    private final Double minOffTime;
    private final Double upwardPowerGradient;
    private final Double downwardPowerGradient;

    private GeneratorConstraints(String generatorId, Double leadTime, Double lagTime, Double minUpTime, Double maxUpTime, Double minOffTime, Double upwardPowerGradient, Double downwardPowerGradient) {
        this.generatorId = generatorId;
        this.leadTime = leadTime;
        this.lagTime = lagTime;
        this.minUpTime = minUpTime;
        this.maxUpTime = maxUpTime;
        this.minOffTime = minOffTime;
        this.upwardPowerGradient = upwardPowerGradient;
        this.downwardPowerGradient = downwardPowerGradient;
    }

    /**
     * Get the id of the generator on which the constraints apply.
     *
     * @return if of the generator
     */
    public String getGeneratorId() {
        return generatorId;
    }

    /**
     * Get the lead time of the generator, i.e. the time required by the power to go from 0 to pMin, in hours.
     *
     * @return lead time of the generator
     */
    public Optional<Double> getLeadTime() {
        return Optional.ofNullable(leadTime);
    }

    /**
     * Get the lag time of the generator, i.e. the time required by the power to go from pMin to 0, in hours.
     *
     * @return lag time of the generator
     */
    public Optional<Double> getLagTime() {
        return Optional.ofNullable(lagTime);
    }

    /**
     * Get the minUp time the generator, i.e. the minimum time during which the generator must be operated with a power greater than pMin, in hours.
     *
     * @return minUp time of the generator
     */
    public Optional<Double> getMinUpTime() {
        return Optional.ofNullable(minUpTime);
    }

    /**
     * Get the maxUp time the generator, i.e. the maximum time during which the generator can be operated with a power greater than pMin, in hours.
     *
     * @return maxUp time of the generator
     */
    public Optional<Double> getMaxUpTime() {
        return Optional.ofNullable(maxUpTime);
    }

    /**
     * Get the minOff time the generator, i.e. the minimum time during which the generator must be kept shutdown, in hours.
     *
     * @return minOff time of the generator
     */
    public Optional<Double> getMinOffTime() {
        return Optional.ofNullable(minOffTime);
    }

    /**
     * Get the upward power gradient of the generator in MW/hours.
     * It only applies when the generator is on, i.e. when its power is greater than pMin.
     * Its value is always positive.
     *
     * @return upward power gradient of the generator
     */
    public Optional<Double> getUpwardPowerGradient() {
        return Optional.ofNullable(upwardPowerGradient);
    }

    /**
     * Get the downward power gradient of the generator in MW/hours.
     * It only applies when the generator is on, i.e. when its power is greater than pMin.
     * Its value is always negative.
     *
     * @return downward power gradient of the generator
     */
    public Optional<Double> getDownwardPowerGradient() {
        return Optional.ofNullable(downwardPowerGradient);
    }

    public static GeneratorConstraintsBuilder create() {
        return new GeneratorConstraintsBuilder();
    }

    public static final class GeneratorConstraintsBuilder {
        private String generatorId;
        private Double leadTime;
        private Double lagTime;
        private Double minUpTime;
        private Double maxUpTime;
        private Double minOffTime;
        private Double upwardPowerGradient;
        private Double downwardPowerGradient;

        private GeneratorConstraintsBuilder() {
        }

        public GeneratorConstraintsBuilder withGeneratorId(String generatorId) {
            this.generatorId = generatorId;
            return this;
        }

        public GeneratorConstraintsBuilder withLeadTime(Double leadTime) {
            this.leadTime = leadTime;
            return this;
        }

        public GeneratorConstraintsBuilder withLagTime(Double lagTime) {
            this.lagTime = lagTime;
            return this;
        }

        public GeneratorConstraintsBuilder withMinUpTime(Double minUpTime) {
            this.minUpTime = minUpTime;
            return this;
        }

        public GeneratorConstraintsBuilder withMaxUpTime(Double maxUpTime) {
            this.maxUpTime = maxUpTime;
            return this;
        }

        public GeneratorConstraintsBuilder withMinOffTime(Double minOffTime) {
            this.minOffTime = minOffTime;
            return this;
        }

        public GeneratorConstraintsBuilder withUpwardPowerGradient(Double upwardPowerGradient) {
            this.upwardPowerGradient = upwardPowerGradient;
            return this;
        }

        public GeneratorConstraintsBuilder withDownwardPowerGradient(Double downwardPowerGradient) {
            this.downwardPowerGradient = downwardPowerGradient;
            return this;
        }

        public GeneratorConstraints build() {
            if (generatorId == null) {
                throw new OpenRaoException("The id of the generator is mandatory.");
            }
            if (leadTime != null && leadTime < 0) {
                throw new OpenRaoException("The lead time of the generator must be positive.");
            }
            if (lagTime != null && lagTime < 0) {
                throw new OpenRaoException("The lag time of the generator must be positive.");
            }
            if (minUpTime != null && minUpTime < 0) {
                throw new OpenRaoException("The minUp time of the generator must be positive.");
            }
            if (maxUpTime != null) {
                if (minUpTime != null && maxUpTime < minUpTime) {
                    throw new OpenRaoException("The maxUp time of the generator must be greater than its minUp time.");
                }
                if (maxUpTime < 0) {
                    throw new OpenRaoException("The maxUp time of the generator must be positive.");
                }
            }
            if (minOffTime != null && minOffTime < 0) {
                throw new OpenRaoException("The minOff time of the generator must be positive.");
            }
            if (upwardPowerGradient != null && upwardPowerGradient < 0) {
                throw new OpenRaoException("The upward power gradient of the generator must be positive.");
            }
            if (downwardPowerGradient != null && downwardPowerGradient > 0) {
                throw new OpenRaoException("The downward power gradient of the generator must be negative.");
            }
            return new GeneratorConstraints(generatorId, leadTime, lagTime, minUpTime, maxUpTime, minOffTime, upwardPowerGradient, downwardPowerGradient);
        }
    }
}
