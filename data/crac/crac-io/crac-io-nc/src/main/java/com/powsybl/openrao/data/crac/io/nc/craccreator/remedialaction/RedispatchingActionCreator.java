/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.remedialaction;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.data.crac.io.commons.OpenRaoImportException;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.MarketRegulationDirectionKind;
import com.powsybl.openrao.data.crac.io.nc.objects.*;
import com.powsybl.openrao.data.crac.io.nc.parameters.NcCracCreationParameters;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class RedispatchingActionCreator {

    private final Crac crac;
    private final Network network;
    private final NcCracCreationParameters ncCracCreationParameters;

    public RedispatchingActionCreator(Crac crac, Network network, NcCracCreationParameters ncParameters) {
        this.crac = crac;
        this.network = network;
        this.ncCracCreationParameters = ncParameters;
    }

    public InjectionRangeActionAdder getRedispatchingActionAdder(RedispatchRemedialAction redispatchRemedialAction,
                                                                 String remedialActionId,
                                                                 Set<PowerShiftKeyStrategy> linkedPowerShiftKeyStrategies,
                                                                 Map<String, Set<PowerShiftKeyDistribution>> linkedPskDistributionWithPskStrategies,
                                                                 Map<String, Set<PowerShiftKeySchedule>> linkedPowerShiftKeySchedulesWithMrid,
                                                                 Map<String, Set<ParticipationFactorTimePoint>> linkedParticipationFactorTimePointsWithPskSchedule,
                                                                 Set<PowerBidSchedule> powerBidSchedules,
                                                                 Map<String, Set<PowerBidScheduleTimePoint>> linkedPowerBidScheduleTimePointWithPowerBidSchedule) {

        InjectionRangeActionAdder injectionRangeActionAdder = crac.newInjectionRangeAction().withId(remedialActionId).withName(redispatchRemedialAction.name());

        addNetworkElementAndDistributionKey(remedialActionId,
            linkedPowerShiftKeyStrategies,
            linkedPskDistributionWithPskStrategies,
            linkedPowerShiftKeySchedulesWithMrid,
            linkedParticipationFactorTimePointsWithPskSchedule,
            ncCracCreationParameters.getTimestamp(),
            injectionRangeActionAdder);

        addCostAndRange(remedialActionId,
            injectionRangeActionAdder,
            powerBidSchedules,
            linkedPowerBidScheduleTimePointWithPowerBidSchedule,
            ncCracCreationParameters.getTimestamp());

        return injectionRangeActionAdder;
    }

    private static void addCostAndRange(String remedialActionId,
                                        InjectionRangeActionAdder injectionRangeActionAdder,
                                        Set<PowerBidSchedule> powerBidSchedules,
                                        Map<String, Set<PowerBidScheduleTimePoint>> linkedPowerBidScheduleTimePointWithPowerBidSchedule,
                                        OffsetDateTime dateTime) {

        if (powerBidSchedules == null || powerBidSchedules.isEmpty()) {
            throw new OpenRaoImportException(
                ImportStatus.INCONSISTENCY_IN_DATA,
                String.format("Redispatching action %s will not be imported because it has no power bid schedule", remedialActionId)
            );
        }

        PowerBidSchedule powerBidSchedule = powerBidSchedules.iterator().next();

        injectionRangeActionAdder.withActivationCost(powerBidSchedule.activationCost());

        VariationDirection variationDirection;
        if (MarketRegulationDirectionKind.UP.matches(powerBidSchedule.direction())) {
            variationDirection = VariationDirection.UP;
        } else if (MarketRegulationDirectionKind.DOWN.matches(powerBidSchedule.direction())) {
            variationDirection = VariationDirection.DOWN;
        } else {
            throw new OpenRaoImportException(
                ImportStatus.INCONSISTENCY_IN_DATA,
                String.format("Redispatching action %s will not be imported because its market regulation direction %s is not supported", remedialActionId, powerBidSchedule.direction().toString())
            );
        }
        // TODO: check that currency is euros ? convert if not the case ? check that all the actions' cost are given in same currency ?

        if (linkedPowerBidScheduleTimePointWithPowerBidSchedule == null || !linkedPowerBidScheduleTimePointWithPowerBidSchedule.containsKey(powerBidSchedule.mrid())) {
            throw new OpenRaoImportException(
                ImportStatus.INCONSISTENCY_IN_DATA,
                String.format("Redispatching action %s will not be imported because it has no power bid schedule time point", remedialActionId)
            );
        }
        // TODO: intertemporal not supported for now, could also just filter to get the correct one...
        if (linkedPowerBidScheduleTimePointWithPowerBidSchedule.get(powerBidSchedule.mrid()).size() > 1) {
            throw new OpenRaoImportException(
                ImportStatus.INCONSISTENCY_IN_DATA,
                String.format("Redispatching action %s will not be imported because it has multiple power bid schedule time points", remedialActionId)
            );
        }

        PowerBidScheduleTimePoint powerBidScheduleTimePoint = linkedPowerBidScheduleTimePointWithPowerBidSchedule.get(powerBidSchedule.mrid()).iterator().next();

        // Check bid validity
        if (powerBidScheduleTimePoint.atTime().equals(dateTime)) {
            throw new OpenRaoImportException(
                ImportStatus.INCONSISTENCY_IN_DATA,
                String.format("Redispatching action %s will not be imported because powerBidScheduleTimePoint %s is not valid for dateTime %s",
                    remedialActionId, powerBidScheduleTimePoint.mrid(), dateTime)
            );
        }

        injectionRangeActionAdder.withVariationCost(powerBidScheduleTimePoint.price(), variationDirection);
        double p = powerBidScheduleTimePoint.p();

        // TODO: this is not going to work redispatching action here is considered a bid aka a variation of a generator or a load
        if (p > 0) {
            injectionRangeActionAdder.newRange()
                .withMin(0)
                .withMax(p)
                .add();
        } else {
            injectionRangeActionAdder.newRange()
                .withMin(p)
                .withMax(0)
                .add();
        }

    }

    private static void addNetworkElementAndDistributionKey(String remedialActionId,
                                                            Set<PowerShiftKeyStrategy> linkedPowerShiftKeyStrategies,
                                                            Map<String, Set<PowerShiftKeyDistribution>> linkedPskDistributionWithPskStrategies,
                                                            Map<String, Set<PowerShiftKeySchedule>> linkedPowerShiftKeySchedulesWithMrid,
                                                            Map<String, Set<ParticipationFactorTimePoint>> linkedParticipationFactorTimePointsWithPskSchedule,
                                                            OffsetDateTime dateTime,
                                                            InjectionRangeActionAdder injectionRangeActionAdder) {
        if (linkedPowerShiftKeyStrategies == null || linkedPowerShiftKeyStrategies.isEmpty()) {
            throw new OpenRaoImportException(
                ImportStatus.INCONSISTENCY_IN_DATA,
                String.format("Redispatching action %s will not be imported because no power shift strategy were found",
                    remedialActionId)
            );
        }

        for (PowerShiftKeyStrategy powerShiftKeyStrategy : linkedPowerShiftKeyStrategies) {

            String powerShiftKeyStrategyId = powerShiftKeyStrategy.mrid();
            // Find the associated powerShiftKeyDistribution to the powerShiftKeyStrategy
            Set<PowerShiftKeyDistribution> powerShiftKeyDistributionSet = linkedPskDistributionWithPskStrategies.get(powerShiftKeyStrategyId);
            for (PowerShiftKeyDistribution powerShiftKeyDistribution : powerShiftKeyDistributionSet) {
                String powerShiftKeyScheduleId = powerShiftKeyDistribution.powerShiftKeySchedule();

                if (linkedPowerShiftKeySchedulesWithMrid == null || !linkedPowerShiftKeySchedulesWithMrid.containsKey(powerShiftKeyScheduleId)) {
                    throw new OpenRaoImportException(
                        ImportStatus.INCONSISTENCY_IN_DATA,
                        String.format("Redispatching action %s will not be imported because no power shift key schedule found for power shift key strategy %s",
                            remedialActionId, powerShiftKeyStrategyId)
                    );
                }

                if (linkedPowerShiftKeySchedulesWithMrid.get(powerShiftKeyScheduleId).size() > 1) {
                    throw new OpenRaoImportException(
                        ImportStatus.INCONSISTENCY_IN_DATA,
                        String.format("Redispatching action %s will not be imported because more than one power shift key schedule found with id %s",
                            remedialActionId, powerShiftKeyScheduleId)
                    );
                }
                PowerShiftKeySchedule powerShiftKeySchedule = linkedPowerShiftKeySchedulesWithMrid.get(powerShiftKeyScheduleId).iterator().next();
                String generatorId = powerShiftKeySchedule.generatingUnit();

                if (!linkedParticipationFactorTimePointsWithPskSchedule.containsKey(powerShiftKeySchedule.mrid())) {
                    throw new OpenRaoImportException(
                        ImportStatus.INCONSISTENCY_IN_DATA,
                        String.format("Redispatching action %s will not be imported because no participationFactorTimePoint found for powerShiftKeySchedule %s",
                            remedialActionId, powerShiftKeySchedule.mrid())
                    );
                }

                Set<ParticipationFactorTimePoint> participationFactorTimePoints = linkedParticipationFactorTimePointsWithPskSchedule.get(powerShiftKeySchedule.mrid());
                //TODO: authorize once we support intertemporal
                // We can have several participationFactorTimePoints for same powerShiftKeySchedule (one per time point)
                if (participationFactorTimePoints.size() > 1) {
                    throw new OpenRaoImportException(
                        ImportStatus.INCONSISTENCY_IN_DATA,
                        String.format("Redispatching action %s will not be imported because more than one participationFactorTimePoint found for powerShiftKeySchedule %s",
                            remedialActionId, powerShiftKeySchedule.mrid())
                    );
                }

                ParticipationFactorTimePoint participationFactorTimePoint = participationFactorTimePoints.iterator().next();
                double distributionKey = participationFactorTimePoint.participationFactor();

                // Check bid validity
                if (participationFactorTimePoint.atTime().equals(dateTime)) {
                    throw new OpenRaoImportException(
                        ImportStatus.INCONSISTENCY_IN_DATA,
                        String.format("Redispatching action %s will not be imported because powerShiftKeySchedule %s is not valid for dateTime %s",
                            remedialActionId, powerShiftKeySchedule.mrid(), dateTime)
                    );
                }

                // add distribution key associated to network element
                injectionRangeActionAdder.withNetworkElementAndKey(distributionKey, generatorId);
            }

        }
    }

}
