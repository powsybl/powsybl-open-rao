/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.timecoupledconstraints;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.commons.logs.RaoBusinessLogs;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Optional;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

/**
 * Set of physical and operational constraints that apply on a generator.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class GeneratorConstraints implements TimeCoupledConstraints {
    private final String generatorId;
    private final Double leadTime;
    private final Double lagTime;
    private final Double upwardPowerGradient;
    private final Double downwardPowerGradient;
    private final boolean isShutDownAllowed;
    private final boolean isStartUpAllowed;

    private GeneratorConstraints(String generatorId, Double leadTime, Double lagTime, Double upwardPowerGradient, Double downwardPowerGradient, Boolean isShutDownAllowed, Boolean isStartUpAllowed) {
        this.generatorId = generatorId;
        this.leadTime = leadTime;
        this.lagTime = lagTime;
        this.upwardPowerGradient = upwardPowerGradient;
        this.downwardPowerGradient = downwardPowerGradient;
        this.isShutDownAllowed = isShutDownAllowed;
        this.isStartUpAllowed = isStartUpAllowed;
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

    /**
     * Get the boolean indicating if the generator can be started up.
     *
     * @return true if generator can be started up
     */
    public boolean isStartUpAllowed() {
        return isStartUpAllowed;
    }

    /**
     * Get the boolean indicating if the generator can be shut down.
     *
     * @return true if generator can be shut down
     */
    public boolean isShutDownAllowed() {
        return isShutDownAllowed;
    }

    public static GeneratorConstraintsBuilder create() {
        return new GeneratorConstraintsBuilder();
    }

    @Override
    public boolean validate(TemporalData<Network> networks) {
        TemporalData<Generator> generators = networks.map(network -> getGeneratorOrThrow(generatorId, network));
        TemporalData<Double> setPoints = generators.map(Generator::getTargetP);
        TemporalData<Double> minimalPowers = generators.map(Generator::getMinP);
        TemporalData<Double> maximalPowers = generators.map(Generator::getMaxP);

        Iterator<OffsetDateTime> dateTimeIterator = generators.getTimestamps().iterator();
        OffsetDateTime currentDateTime = dateTimeIterator.next();
        while (dateTimeIterator.hasNext()) {
            OffsetDateTime nextDateTime = dateTimeIterator.next();
            double currentSetPoint = setPoints.getData(currentDateTime).orElseThrow();
            if (currentSetPoint > maximalPowers.getData(currentDateTime).orElseThrow()) {
                BUSINESS_WARNS.warn("Power of generator {} at timestamp {} is over its maximal power. Generator will be discarded.", generatorId, currentDateTime);
                return false;
            }
            double powerVariation = setPoints.getData(nextDateTime).orElseThrow() - currentSetPoint;
            double timestampDuration = currentDateTime.until(nextDateTime, ChronoUnit.SECONDS) / 3600.0;
            if (upwardPowerGradient != null && powerVariation > upwardPowerGradient * timestampDuration) {
                BUSINESS_WARNS.warn("Power variation of generator {} between timestamps {} and {} is greater than the permitted power gradient. Generator will be discarded.", generatorId, currentDateTime, nextDateTime);
                return false;
            }
            if (downwardPowerGradient != null && powerVariation < downwardPowerGradient * timestampDuration) {
                BUSINESS_WARNS.warn("Power variation of generator {} between timestamps {} and {} is lower than the permitted power gradient. Generator will be discarded.", generatorId, currentDateTime, nextDateTime);
                return false;
            }
            currentDateTime = nextDateTime;
        }
        return true;
    }

    private static Generator getGeneratorOrThrow(String generatorId, Network network) {
        Generator generator = network.getGenerator(generatorId);
        if (generator == null) {
            throw new OpenRaoException("Generator " + generatorId + " not found in network.");
        }
        return generator;
    }

    public static final class GeneratorConstraintsBuilder {
        private String generatorId;
        private Double leadTime;
        private Double lagTime;
        private Double upwardPowerGradient;
        private Double downwardPowerGradient;
        private boolean isShutDownAllowed = true;
        private boolean isStartUpAllowed = true;

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

        public GeneratorConstraintsBuilder withUpwardPowerGradient(Double upwardPowerGradient) {
            this.upwardPowerGradient = upwardPowerGradient;
            return this;
        }

        public GeneratorConstraintsBuilder withDownwardPowerGradient(Double downwardPowerGradient) {
            this.downwardPowerGradient = downwardPowerGradient;
            return this;
        }

        public GeneratorConstraintsBuilder withShutDownAllowed(boolean isShutDownAllowed) {
            this.isShutDownAllowed = isShutDownAllowed;
            return this;
        }

        public GeneratorConstraintsBuilder withStartUpAllowed(boolean isStartUpAllowed) {
            this.isStartUpAllowed = isStartUpAllowed;
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
            if (upwardPowerGradient != null && upwardPowerGradient < 0) {
                throw new OpenRaoException("The upward power gradient of the generator must be positive.");
            }
            if (downwardPowerGradient != null && downwardPowerGradient > 0) {
                throw new OpenRaoException("The downward power gradient of the generator must be negative.");
            }
            return new GeneratorConstraints(generatorId, leadTime, lagTime, upwardPowerGradient, downwardPowerGradient, isShutDownAllowed, isStartUpAllowed);
        }
    }
}
