/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.iidm.network.Generator;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.timecoupledconstraints.GeneratorConstraints;
import com.powsybl.openrao.data.timecoupledconstraints.TimeCoupledConstraints;
import com.powsybl.openrao.raoapi.RaoInput;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class TimeCoupledConstraintsChecker {
    private TimeCoupledConstraintsChecker() {
    }

    public static boolean check(TemporalData<RaoInput> inputs, TimeCoupledConstraints timeCoupledConstraints, TemporalData<RaoResult> raoResults) {
        Map<String, TemporalData<Generator>> generators = new HashMap<>();
        Map<String, GeneratorConstraints> constraintsPerGenerator = new HashMap<>();
        Map<String, TemporalData<Double>> powersPerGenerator = new HashMap<>();

        for (GeneratorConstraints constraints : timeCoupledConstraints.getGeneratorConstraints()) {
            constraintsPerGenerator.put(constraints.getGeneratorId(), constraints);
        }

        for (OffsetDateTime timestamp : inputs.getTimestamps()) {
            RaoInput raoInput = inputs.getData(timestamp).orElseThrow();
            RaoResult raoResult = raoResults.getData(timestamp).orElseThrow();
            Crac crac = raoInput.getCrac();
            for (String generatorId : constraintsPerGenerator.keySet()) {
                generators.computeIfAbsent(generatorId, id -> new TemporalDataImpl<>())
                    .put(timestamp, raoInput.getNetwork().getGenerator(generatorId));
            }
            for (RangeAction<?> rangeAction : crac.getRangeActions(crac.getPreventiveState())) {
                if (rangeAction instanceof InjectionRangeAction injectionRangeAction) {
                    double setPoint = raoResult.getOptimizedSetPointOnState(crac.getPreventiveState(), injectionRangeAction);
                    injectionRangeAction.getInjectionDistributionKeys().forEach(
                        (networkElement, key) -> {
                            powersPerGenerator.computeIfAbsent(networkElement.getId(), id -> new TemporalDataImpl<>())
                                .put(timestamp, setPoint * key);
                        }
                    );
                }
            }
        }

        for (String generatorId : generators.keySet()) {
            if (!checkGeneratorConstraints(generatorId, generators.get(generatorId), powersPerGenerator.getOrDefault(generatorId, new TemporalDataImpl<>()), constraintsPerGenerator.get(generatorId))) {
                return false;
            }
        }

        return true;
    }

    private static boolean checkGeneratorConstraints(String generatorId, TemporalData<Generator> generatorPerTimestamp, TemporalData<Double> setPointPerTimestamp, GeneratorConstraints generatorConstraints) {
        OffsetDateTime firstTimestamp = generatorPerTimestamp.getTimestamps().getFirst();
        OffsetDateTime previousTimestamp = firstTimestamp;
        Generator previousGenerator = generatorPerTimestamp.getData(firstTimestamp).orElseThrow();
        double previousSetPoint = setPointPerTimestamp.getData(firstTimestamp).orElseThrow();

        boolean wasOff = previousSetPoint == 0.0;
        boolean wasOn = !wasOff;

        double minOffTime = 0.0;
        if (generatorConstraints.getLeadTime().isPresent()) {
            minOffTime += generatorConstraints.getLeadTime().get();
        }
        if (generatorConstraints.getLagTime().isPresent()) {
            minOffTime += generatorConstraints.getLagTime().get();
        }

        double offStreak = 0.0;

        for (OffsetDateTime timestamp : generatorPerTimestamp.getTimestamps()) {
            double timeElapsed = previousTimestamp.until(timestamp, ChronoUnit.SECONDS) / 3600.0;

            Generator generator = generatorPerTimestamp.getData(timestamp).orElseThrow();
            double setPoint = setPointPerTimestamp.getData(timestamp).orElseThrow();

            // 1. Check maxP
            if (setPoint > generator.getMaxP()) {
                OpenRaoLoggerProvider.BUSINESS_WARNS.warn("Generator {} has a set-point of {} MW at {} which is greater than its maximum power of {} MW", generatorId, setPoint, timestamp, generator.getMaxP());
            }

            // 2. Check minP
            if (setPoint < generator.getMinP() && setPoint > 0.0) {
                OpenRaoLoggerProvider.BUSINESS_WARNS.warn("Generator {} has a non-null set-point of {} MW at {} which is lower than its minimum power of {} MW", generatorId, setPoint, timestamp, generator.getMinP());
            }

            // 3. Check gradients -> only if both powers are above the minimum power
            if (timestamp != firstTimestamp && setPoint >= generator.getMinP() && previousSetPoint >= previousGenerator.getMinP()) {
                double powerVariation = setPoint - previousSetPoint;
                double gradient = powerVariation / timeElapsed;

                if (generatorConstraints.getUpwardPowerGradient().isPresent()) {
                    if (gradient > generatorConstraints.getUpwardPowerGradient().get()) {
                        OpenRaoLoggerProvider.BUSINESS_WARNS.warn("Generator {} has a variation of {} MW between timestamps {} and {} which is greater than its maximum power gradient of {} MW/h", generatorId, powerVariation, previousTimestamp, timestamp, generatorConstraints.getUpwardPowerGradient().get());
                    }
                }

                if (generatorConstraints.getDownwardPowerGradient().isPresent()) {
                    if (gradient < generatorConstraints.getDownwardPowerGradient().get()) {
                        OpenRaoLoggerProvider.BUSINESS_WARNS.warn("Generator {} has a variation of {} MW between timestamps {} and {} which is lower than its maximum power gradient of {} MW/h", generatorId, powerVariation, previousTimestamp, timestamp, generatorConstraints.getDownwardPowerGradient().get());
                    }
                }
            }

            // 4. Check start-up allowed
            if (!generatorConstraints.isStartUpAllowed() && wasOff && setPoint > generator.getMinP()) {
                OpenRaoLoggerProvider.BUSINESS_WARNS.warn("Generator {} was switched on at {} whereas it should not be started up.", generatorId, timestamp);
            }

            // 5. Check shutdown allowed
            if (!generatorConstraints.isShutDownAllowed() && wasOn && setPoint == 0.0) {
                OpenRaoLoggerProvider.BUSINESS_WARNS.warn("Generator {} was switched off at {} whereas it should not be shut down.", generatorId, timestamp);
            }

            // 6. Check lead and lag times
            if (setPoint == 0.0 && previousSetPoint == 0.0) {
                offStreak += timeElapsed;
            } else if (setPoint > 0.0 && previousSetPoint == 0.0) {
                if (offStreak < minOffTime) {
                    OpenRaoLoggerProvider.BUSINESS_WARNS.warn("Generator {} was switched on at {} after {} of off time, which is less than the required lead+lag times of {}.", generatorId, timestamp, offStreak, minOffTime);
                }
                offStreak = 0.0;
            }

            previousTimestamp = timestamp;
            previousSetPoint = setPoint;
            previousGenerator = generator;
            wasOff = wasOff || setPoint == 0.0;
            wasOn = !wasOff;
        }

        return true;
    }
}
