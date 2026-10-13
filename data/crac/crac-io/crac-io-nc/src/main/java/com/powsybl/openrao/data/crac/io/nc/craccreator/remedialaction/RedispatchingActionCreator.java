/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.remedialaction;

import com.google.common.annotations.Beta;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeActionAdder;
import com.powsybl.openrao.data.crac.io.commons.OpenRaoImportException;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracUtils;
import com.powsybl.openrao.data.crac.io.nc.objects.GlskBidActionDistribution;
import com.powsybl.openrao.data.crac.io.nc.objects.GlskSchedule;
import com.powsybl.openrao.data.crac.io.nc.objects.ParticipationFactorTimePoint;
import com.powsybl.openrao.data.crac.io.nc.objects.PowerBidSchedule;
import com.powsybl.openrao.data.crac.io.nc.objects.RedispatchRemedialAction;
import com.powsybl.openrao.data.crac.io.nc.objects.SynchronousMachine;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Redispatching remedial actions importer from NC profiles v2.2.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@Beta
public class RedispatchingActionCreator {
    private final Crac crac;
    private final Network network;

    // TODO: include PowerBidScheduleTimePoints (what to do with p and price?)

    public RedispatchingActionCreator(Crac crac, Network network) {
        this.crac = crac;
        this.network = network;
        OpenRaoLoggerProvider.BUSINESS_WARNS.warn("OpenRAO NC redispatching importer is still in Beta version and only works with NC profiles v2.2.");
    }

    public InjectionRangeActionAdder getInjectionRangeActionAdder(RedispatchRemedialAction nativeRedispatchRemedialAction,
                                                                  Set<PowerBidSchedule> nativePowerBidSchedules,
                                                                  Map<String, Set<GlskBidActionDistribution>> glskBidActionDistributionsPerPowerBidSchedule,
                                                                  Set<GlskSchedule> glskSchedules,
                                                                  Map<String, Set<ParticipationFactorTimePoint>> participationFactorTimePointsPerGlskSchedule,
                                                                  Map<String, Set<SynchronousMachine>> synchronousMachinesPerGeneratingUnit) {
        if (!nativeRedispatchRemedialAction.normalAvailable()) {
            throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO, "Redispatching action %s ignored because it is not available.".formatted(nativeRedispatchRemedialAction.mrid()));
        }

        // 1. instantiate the adder with the basic remedial action's data
        InjectionRangeActionAdder injectionRangeActionAdder = crac.newInjectionRangeAction();
        injectionRangeActionAdder.withId(nativeRedispatchRemedialAction.mrid());
        injectionRangeActionAdder.withName(nativeRedispatchRemedialAction.getUniqueName());
        if (nativeRedispatchRemedialAction.operator() != null) {
            injectionRangeActionAdder.withOperator(NcCracUtils.getTsoNameFromUrl(nativeRedispatchRemedialAction.operator()));
        }
        if (!nativeRedispatchRemedialAction.isManual()) {
            throw new OpenRaoImportException(ImportStatus.NOT_YET_HANDLED_BY_OPEN_RAO, "Redispatching action %s ignored because OpenRAO does not support automatic redispatching.".formatted(nativeRedispatchRemedialAction.mrid()));
        }

        // 2. add information related to the cost
        if (nativePowerBidSchedules.isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA, "Redispatching action %s ignored because it has no associated PowerBidSchedule.".formatted(nativeRedispatchRemedialAction.mrid()));
        }
        if (nativePowerBidSchedules.size() > 1) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA, "Redispatching action %s ignored because it has more than one associated PowerBidSchedule.".formatted(nativeRedispatchRemedialAction.mrid()));
        }
        PowerBidSchedule nativePowerBidSchedule = nativePowerBidSchedules.iterator().next();
        if (nativePowerBidSchedule.startupCost() > 0.0) {
            injectionRangeActionAdder.withActivationCost(nativePowerBidSchedule.startupCost());
        }

        // 3. retrieve the involved generators and their distribution key
        Set<GlskBidActionDistribution> glskBidActionDistributions = glskBidActionDistributionsPerPowerBidSchedule.getOrDefault(nativePowerBidSchedule.mrid(), Set.of());
        if (glskBidActionDistributions.isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA, "Redispatching action %s ignored because PowerBidSchedule %s has no associated GlskBidActionDistribution.".formatted(nativeRedispatchRemedialAction.mrid(), nativePowerBidSchedule.mrid()));
        }
        Map<String, GlskSchedule> glskSchedulesPerId = glskSchedules.stream().collect(Collectors.toMap(GlskSchedule::mrid, Function.identity()));
        boolean hasNetworkElements = false;
        double maximumSetPoint = Double.MAX_VALUE;
        for (GlskBidActionDistribution glskBidActionDistribution : glskBidActionDistributions) {
            GlskSchedule glskSchedule = glskSchedulesPerId.get(glskBidActionDistribution.glskSchedule());
            if (glskSchedule == null) {
                OpenRaoLoggerProvider.BUSINESS_WARNS.warn("GlskBidActionDistribution {} is missing a GlskSchedule.", glskBidActionDistribution.mrid());
            } else {
                Set<SynchronousMachine> synchronousMachines = synchronousMachinesPerGeneratingUnit.getOrDefault(glskSchedule.generatingUnit(), Set.of());
                if (synchronousMachines.isEmpty()) {
                    throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA, "Redispatching action %s ignored because no synchronous machine in the network is based on generating unit %s.".formatted(nativeRedispatchRemedialAction.mrid(), glskSchedule.generatingUnit()));
                }
                SynchronousMachine synchronousMachine = synchronousMachines.iterator().next();
                Identifiable<?> generatorOrLoad = network.getIdentifiable(synchronousMachine.mrid());
                if (generatorOrLoad == null) {
                    throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA, "Redispatching action %s ignored because synchronous machine %s is missing from the network.".formatted(nativeRedispatchRemedialAction.mrid(), synchronousMachine.mrid()));
                }
                if (!(generatorOrLoad instanceof Generator) && !(generatorOrLoad instanceof Load)) {
                    throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA, "Redispatching action %s ignored because synchronous machine %s is neither a generator nor a load.".formatted(nativeRedispatchRemedialAction.mrid(), synchronousMachine.mrid()));
                }
                Set<ParticipationFactorTimePoint> participationFactorTimePoints = participationFactorTimePointsPerGlskSchedule.getOrDefault(glskSchedule.mrid(), Set.of());
                if (participationFactorTimePoints.isEmpty()) {
                    throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA, "Redispatching action %s ignored because GlskSchedule %s has no associated ParticipationFactorTimePoint.".formatted(nativeRedispatchRemedialAction.mrid(), glskSchedule.mrid()));
                }
                if (participationFactorTimePoints.size() > 1) {
                    throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA, "Redispatching action %s ignored because GlskSchedule %s has more than one associated ParticipationFactorTimePoint.".formatted(nativeRedispatchRemedialAction.mrid(), glskSchedule.mrid()));
                }

                double participationFactor = participationFactorTimePoints.iterator().next().participationFactor();
                injectionRangeActionAdder.withNetworkElementAndKey(participationFactor, generatorOrLoad.getId());
                hasNetworkElements = true;
                if (generatorOrLoad instanceof Generator generator && participationFactor != 0.0) {
                    double relativeMaxP = Math.abs(generator.getMaxP() / participationFactor);
                    maximumSetPoint = Math.min(maximumSetPoint, relativeMaxP);
                }
            }
        }

        if (!hasNetworkElements) {
            throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO, "Redispatching action %s ignored because it has no associated network element.".formatted(nativeRedispatchRemedialAction.mrid()));
        }

        // 4. add range
        // TODO: use min and max energy coming from profiles? or use static property ranges?
        injectionRangeActionAdder.newRange()
            .withRangeType(RangeType.ABSOLUTE)
            .withMin(0.0)
            .withMax(maximumSetPoint)
            .add();

        // 5. return adder
        return injectionRangeActionAdder;
    }

}
