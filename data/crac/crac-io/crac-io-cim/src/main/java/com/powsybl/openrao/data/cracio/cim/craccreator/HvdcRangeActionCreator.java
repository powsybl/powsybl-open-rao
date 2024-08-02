/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.cim.craccreator;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.rangeaction.HvdcRangeActionAdder;
import com.powsybl.openrao.data.cracio.commons.api.ImportStatus;
import com.powsybl.openrao.data.cracio.commons.OpenRaoImportException;
import com.powsybl.openrao.data.cracio.cim.parameters.CimCracCreationParameters;
import com.powsybl.openrao.data.cracio.cim.parameters.RangeActionSpeed;
import com.powsybl.openrao.data.cracio.cim.xsd.RemedialActionRegisteredResource;
import com.powsybl.openrao.data.cracio.cim.xsd.RemedialActionSeries;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.openrao.data.cracio.cim.craccreator.CimConstants.MEGAWATT_UNIT_SYMBOL;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class HvdcRangeActionCreator {
    private final Crac crac;
    private final Network network;
    private final List<Contingency> contingencies;
    private final List<String> invalidContingencies;
    private final Set<FlowCnec> flowCnecs;
    private final AngleCnec angleCnec;
    private final Country sharedDomain;
    private final CimCracCreationParameters cimCracCreationParameters;
    private final Map<String, HvdcRangeActionAdder> hvdcRangeActionAdders = new HashMap<>();
    private final Map<String, List<Integer>> rangeMin = new HashMap<>();
    private final Map<String, List<Integer>> rangeMax = new HashMap<>();
    private final Map<String, Boolean> isDirectionInverted = new HashMap<>();
    private final List<String> raSeriesIds = new ArrayList<>();
    private final Map<String, OpenRaoImportException> exceptions = new HashMap<>();

    public HvdcRangeActionCreator(Crac crac, Network network, List<Contingency> contingencies, List<String> invalidContingencies, Set<FlowCnec> flowCnecs, AngleCnec angleCnec, Country sharedDomain, CimCracCreationParameters cimCracCreationParameters) {
        this.crac = crac;
        this.network = network;
        this.contingencies = contingencies;
        this.invalidContingencies = invalidContingencies;
        this.flowCnecs = flowCnecs;
        this.angleCnec = angleCnec;
        this.sharedDomain = sharedDomain;
        this.cimCracCreationParameters = cimCracCreationParameters;
    }

    public void addDirection(RemedialActionSeries remedialActionSeries) {
        raSeriesIds.add(remedialActionSeries.getMRID());

        try {
            if (remedialActionSeries.getRegisteredResource().size() != 4) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("%s registered resources were defined in HVDC instead of 4", remedialActionSeries.getRegisteredResource().size()));
            }

            Set<String> networkElementIds = new HashSet<>();
            Boolean isRemedialActionSeriesInverted = null;

            for (RemedialActionRegisteredResource registeredResource : remedialActionSeries.getRegisteredResource()) {
                // Ignore PMode registered resource
                if (registeredResource.getMarketObjectStatusStatus().equals(CimConstants.MarketObjectStatus.PMODE.getStatus())) {
                    continue;
                }

                checkRegisteredResource(registeredResource);

                String networkElementId = registeredResource.getMRID().getValue();
                if (networkElementIds.contains(networkElementId)) {
                    throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "HVDC RemedialAction_Series contains multiple RegisteredResources with the same mRID");
                }
                networkElementIds.add(networkElementId);

                hvdcRangeActionAdders.putIfAbsent(networkElementId, initHvdcRangeActionAdder(registeredResource));

                boolean isRegisteredResourceInverted = readHvdcRange(
                    networkElementId,
                    registeredResource.getResourceCapacityMinimumCapacity().intValue(),
                    registeredResource.getResourceCapacityMaximumCapacity().intValue(),
                    registeredResource.getInAggregateNodeMRID().getValue(),
                    registeredResource.getOutAggregateNodeMRID().getValue());

                if (Objects.nonNull(isRemedialActionSeriesInverted) && !isRemedialActionSeriesInverted.equals(isRegisteredResourceInverted)) {
                    throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "HVDC registered resources reference lines in opposite directions");
                } else {
                    isRemedialActionSeriesInverted = isRegisteredResourceInverted;
                }
            }

            Boolean finalIsRemedialActionSeriesInverted = isRemedialActionSeriesInverted;
            if (isDirectionInverted.values().stream().anyMatch(inverted -> inverted.equals(finalIsRemedialActionSeriesInverted))) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "HVDC line should be defined in the opposite direction");
            } else {
                isDirectionInverted.put(remedialActionSeries.getMRID(), isRemedialActionSeriesInverted);
            }
        } catch (OpenRaoImportException e) {
            exceptions.put(remedialActionSeries.getMRID(), e);
        }
    }

    public Set<RemedialActionSeriesCreationContext> add() {
        if (raSeriesIds.size() != 2) {
            return raSeriesIds.stream().map(id ->
                RemedialActionSeriesCreationContext.notImported(id, ImportStatus.INCONSISTENCY_IN_DATA, String.format("Expected 2 registered resources but found %s", raSeriesIds.size()))
            ).collect(Collectors.toSet());
        }

        if (!exceptions.isEmpty()) {
            Set<RemedialActionSeriesCreationContext> setFromExceptions = exceptions.entrySet().stream().map(
                entry -> RemedialActionSeriesCreationContext.notImported(entry.getKey(), entry.getValue().getImportStatus(), entry.getValue().getMessage())
            ).collect(Collectors.toSet());
            // Complete for those who dd not throw an exception but could not be imported because of the others
            raSeriesIds.stream().filter(key -> !exceptions.containsKey(key)).map(
                id -> RemedialActionSeriesCreationContext.notImported(id, ImportStatus.INCONSISTENCY_IN_DATA, "Other RemedialActionSeries in the same HVDC Series failed")
            ).forEach(setFromExceptions::add);
            return setFromExceptions;
        }

        Set<String> createdRaIds = new HashSet<>();
        String groupId = hvdcRangeActionAdders.keySet().stream().sorted().collect(Collectors.joining(" + "));
        for (Map.Entry<String, List<Integer>> rangeEntry : rangeMin.entrySet()) {
            String neId = rangeEntry.getKey();
            try {
                Pair<Integer, Integer> minMax = concatenateHvdcRanges(rangeEntry.getValue(), rangeMax.get(neId));
                String remedialActionId = String.join(" + ", raSeriesIds) + " - " + neId;
                hvdcRangeActionAdders.get(neId)
                    .withId(remedialActionId).withName(remedialActionId)
                    .withOperator(CimConstants.readOperator(remedialActionId))
                    .withGroupId(groupId)
                    .newRange().withMin(minMax.getLeft()).withMax(minMax.getRight()).add()
                    .add();
                createdRaIds.add(remedialActionId);
            } catch (OpenRaoImportException e) {
                return raSeriesIds.stream().map(id ->
                    RemedialActionSeriesCreationContext.notImported(id, e.getImportStatus(), e.getMessage())
                ).collect(Collectors.toSet());
            } catch (OpenRaoException e) {
                return raSeriesIds.stream().map(id ->
                        RemedialActionSeriesCreationContext.notImported(id, ImportStatus.INCONSISTENCY_IN_DATA, e.getMessage())).collect(Collectors.toSet());
            }
        }

        if (invalidContingencies.isEmpty()) {
            return raSeriesIds.stream().map(id -> RemedialActionSeriesCreationContext.importedHvdcRa(id, createdRaIds, false, isDirectionInverted.get(id), "")).collect(Collectors.toSet());
        } else {
            String contingencyList = StringUtils.join(invalidContingencies, ", ");
            return raSeriesIds.stream().map(id -> RemedialActionSeriesCreationContext.importedHvdcRa(id, createdRaIds, true, isDirectionInverted.get(id), String.format("Contingencies %s were not imported", contingencyList))).collect(Collectors.toSet());
        }

    }

    private HvdcRangeActionAdder initHvdcRangeActionAdder(RemedialActionRegisteredResource registeredResource) {
        HvdcRangeActionAdder hvdcRangeActionAdder = crac.newHvdcRangeAction();
        String hvdcId = registeredResource.getMRID().getValue();
        checkHvdcNetworkElement(hvdcId);
        hvdcRangeActionAdder.withNetworkElement(hvdcId);

        // Speed
        if (cimCracCreationParameters != null && cimCracCreationParameters.getRangeActionSpeedSet() != null) {
            for (RangeActionSpeed rangeActionSpeed : cimCracCreationParameters.getRangeActionSpeedSet()) {
                if (rangeActionSpeed.getRangeActionId().equals(hvdcId)) {
                    hvdcRangeActionAdder.withSpeed(rangeActionSpeed.getSpeed());
                }
            }
        }

        // Usage rules
        RemedialActionSeriesCreator.addUsageRules(crac, CimConstants.ApplicationModeMarketObjectStatus.AUTO.getStatus(), hvdcRangeActionAdder, contingencies, invalidContingencies, flowCnecs, angleCnec, sharedDomain);

        return hvdcRangeActionAdder;
    }

    // Return type : Pair two integers :
    // -- the integers are the min and max Hvdc concatenated range.
    private Pair<Integer, Integer> concatenateHvdcRanges(List<Integer> min, List<Integer> max) {
        if (min.get(1) < min.get(0)) {
            if (max.get(1) < min.get(0)) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "HVDC range mismatch");
            } else {
                return Pair.of(min.get(1), Math.max(max.get(0), max.get(1)));
            }
        } else {
            if (min.get(1) > max.get(0)) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "HVDC range mismatch");
            } else {
                return Pair.of(min.get(0), Math.max(max.get(0), max.get(1)));
            }
        }
    }

    // Throws an exception if the RegisteredResource is ill defined
    private void checkRegisteredResource(RemedialActionRegisteredResource registeredResource) {
        // Check MarketObjectStatus
        String marketObjectStatusStatus = registeredResource.getMarketObjectStatusStatus();
        if (Objects.isNull(marketObjectStatusStatus)) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA, "Missing marketObjectStatusStatus");
        }
        if (!marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.ABSOLUTE.getStatus())) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong marketObjectStatusStatus: %s", marketObjectStatusStatus));
        }
        // Check unit
        String unit = registeredResource.getResourceCapacityUnitSymbol();
        if (Objects.isNull(unit)) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA, "Missing unit");
        }
        if (!registeredResource.getResourceCapacityUnitSymbol().equals(MEGAWATT_UNIT_SYMBOL)) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong unit: %s", unit));
        }
        // Check that min/max are defined
        if (Objects.isNull(registeredResource.getResourceCapacityMinimumCapacity()) || Objects.isNull(registeredResource.getResourceCapacityMaximumCapacity())) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA, "Missing min or max resource capacity");
        }
    }

    /**
     * @param networkElement - HVDC line name
     * @param minCapacity
     * @param maxCapacity
     * @param inNode - The area of the related oriented border study where the energy flows INTO.
     * @param outNode - The area of the related oriented border study where the energy comes FROM.
     * @return - the boolean indicates whether the Hvdc line is inverted
     */
    private boolean readHvdcRange(String networkElement, int minCapacity, int maxCapacity, String inNode, String outNode) {
        HvdcLine hvdcLine = network.getHvdcLine(networkElement);
        boolean isInverted;
        int min;
        int max;
        if (Objects.isNull(hvdcLine)) {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Not a HVDC line");
        }
        String from = hvdcLine.getConverterStation1().getTerminal().getVoltageLevel().getId();
        String to = hvdcLine.getConverterStation2().getTerminal().getVoltageLevel().getId();

        if (Objects.isNull(inNode) || Objects.isNull(outNode)) {
            throw new OpenRaoImportException(ImportStatus.INCOMPLETE_DATA, "Missing HVDC in or out aggregate nodes");
        }
        if (inNode.equals(to) && outNode.equals(from)) {
            isInverted = false;
            min = minCapacity;
            max = maxCapacity;
        } else if (inNode.equals(from) && outNode.equals(to)) {
            isInverted = true;
            min = -maxCapacity;
            max = -minCapacity;
        } else {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Wrong HVDC inAggregateNode/outAggregateNode");
        }

        if (rangeMin.containsKey(networkElement)) {
            rangeMin.get(networkElement).add(min);
        } else {
            List<Integer> list = new ArrayList<>();
            list.add(min);
            rangeMin.put(networkElement, list);
        }

        if (rangeMax.containsKey(networkElement)) {
            rangeMax.get(networkElement).add(max);
        } else {
            List<Integer> list = new ArrayList<>();
            list.add(max);
            rangeMax.put(networkElement, list);
        }
        return isInverted;
    }

    private void checkHvdcNetworkElement(String networkElement) {
        if (Objects.isNull(network.getHvdcLine(networkElement))) {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Not a HVDC line");
        }
        if (Objects.isNull(network.getHvdcLine(networkElement).getExtension(HvdcAngleDroopActivePowerControl.class))) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "HVDC does not have a HvdcAngleDroopActivePowerControl extension");
        }
        if (!network.getHvdcLine(networkElement).getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled()) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "HvdcAngleDroopActivePowerControl extension is not enabled");
        }
    }
}
