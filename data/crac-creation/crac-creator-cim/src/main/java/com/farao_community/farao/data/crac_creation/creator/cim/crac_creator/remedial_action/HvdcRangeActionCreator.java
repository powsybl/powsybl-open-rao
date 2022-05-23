/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.remedial_action;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeActionAdder;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimConstants;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.FaraoImportException;
import com.farao_community.farao.data.crac_creation.creator.cim.xsd.RemedialActionRegisteredResource;
import com.farao_community.farao.data.crac_creation.creator.cim.xsd.RemedialActionSeries;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

import static com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimConstants.MEGAWATT_UNIT_SYMBOL;

/**
 * @author Godelaine de Montmorillon <godelaine.demontmorillon at rte-france.com>
 */
public class HvdcRangeActionCreator {
    private final String cimSerieId;
    private final Crac crac;
    private final Network network;
    private final List<RemedialActionSeries> storedHvdcRangeActions;
    private final List<Contingency> contingencies;
    private final List<String> invalidContingencies;
    private Set<FlowCnec> flowCnecs;
    private Country sharedDomain;

    public HvdcRangeActionCreator(String cimSerieId, Crac crac, Network network, List<RemedialActionSeries> storedHvdcRangeActions, List<Contingency> contingencies, List<String> invalidContingencies, Set<FlowCnec> flowCnecs, Country sharedDomain) {
        this.cimSerieId = cimSerieId;
        this.crac = crac;
        this.network = network;
        this.storedHvdcRangeActions = storedHvdcRangeActions;
        this.contingencies = contingencies;
        this.invalidContingencies = invalidContingencies;
        this.flowCnecs = flowCnecs;
        this.sharedDomain = sharedDomain;
    }

    public Set<RemedialActionSeriesCreationContext> createAndAddHvdcRemedialActionSeries() {
        try {
            checkHvdcRangeActionValidity();

            RemedialActionSeries hvdcRangeActionDirection1 = storedHvdcRangeActions.get(0);
            RemedialActionSeries hvdcRangeActionDirection2 = storedHvdcRangeActions.get(1);
            // Two adders are created, based on hvdcRangeActionDirection1, with range information from hvdcRangeActionDirection2.
            HvdcRangeActionAdder hvdcRangeActionAdder1 = crac.newHvdcRangeAction();
            HvdcRangeActionAdder hvdcRangeActionAdder2 = crac.newHvdcRangeAction();

            //------------------    1) Read hvdcRangeActionDirection1       ------------------
            String groupId = "";
            String hvdcRangeActionDirection1Id1 = "";
            String hvdcRangeActionDirection1Id2 = "";
            int direction1minRange1 = 0;
            int direction1minRange2 = 0;
            int direction1maxRange1 = 0;
            int direction1maxRange2 = 0;
            boolean isDirection1Inverted = false;
            boolean direction1hvdc1IsDefined = false;
            for (RemedialActionRegisteredResource registeredResource : hvdcRangeActionDirection1.getRegisteredResource()) {
                // Ignore PMode registered resource
                if (registeredResource.getMarketObjectStatusStatus().equals(CimConstants.MarketObjectStatus.PMODE.getStatus())) {
                    continue;
                }
                checkRegisteredResource(registeredResource);

                // Add range action
                if (!direction1hvdc1IsDefined) {
                    // common function
                    hvdcRangeActionDirection1Id1 = registeredResource.getMRID().getValue();
                    Pair<Boolean, List<Integer>> hvdcRegisteredResource1Status = readHvdcRegisteredResource(hvdcRangeActionAdder1, registeredResource);

                    isDirection1Inverted = hvdcRegisteredResource1Status.getLeft();

                    direction1minRange1 = hvdcRegisteredResource1Status.getRight().get(0);
                    direction1maxRange1 = hvdcRegisteredResource1Status.getRight().get(1);

                    groupId += hvdcRangeActionDirection1Id1;
                    direction1hvdc1IsDefined = true;
                } else {
                    hvdcRangeActionDirection1Id2 = registeredResource.getMRID().getValue();
                    if (hvdcRangeActionDirection1Id2.equals(hvdcRangeActionDirection1Id1)) {
                        throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Wrong id : HVDC registered resource has an id already defined");
                    }
                    Pair<Boolean, List<Integer>> hvdcRegisteredResource2Status = readHvdcRegisteredResource(hvdcRangeActionAdder2, registeredResource);
                    if (Boolean.valueOf(!isDirection1Inverted).equals(hvdcRegisteredResource2Status.getLeft())) {
                        throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "HVDC registered resources reference lines in opposite directions");
                    }
                    direction1minRange2 = hvdcRegisteredResource2Status.getRight().get(0);
                    direction1maxRange2 = hvdcRegisteredResource2Status.getRight().get(1);

                    groupId += " + " + hvdcRangeActionDirection1Id2;
                    hvdcRangeActionAdder1.withGroupId(groupId);
                    hvdcRangeActionAdder2.withGroupId(groupId);
                }
            }

            //------------------    2) Read hvdcRangeActionDirection2       ------------------
            // Only keep range data
            String hvdcRangeActionDirection2Id1 = "";
            String hvdcRangeActionDirection2Id2 = "";
            int direction2minRange1 = 0;
            int direction2minRange2 = 0;
            int direction2maxRange1 = 0;
            int direction2maxRange2 = 0;
            boolean direction2hvdc1IsDefined = false;
            for (RemedialActionRegisteredResource registeredResource : hvdcRangeActionDirection2.getRegisteredResource()) {
                // Ignore PMode registered resource
                if (registeredResource.getMarketObjectStatusStatus().equals(CimConstants.MarketObjectStatus.PMODE.getStatus())) {
                    continue;
                }
                checkRegisteredResource(registeredResource);

                if (!direction2hvdc1IsDefined) {
                    hvdcRangeActionDirection2Id1 = registeredResource.getMRID().getValue();
                    // READ RANGE
                    Pair<Boolean, List<Integer>> hvdcRange1Direction2 = defineHvdcRange(hvdcRangeActionDirection2Id1,
                        registeredResource.getResourceCapacityMinimumCapacity().intValue(),
                        registeredResource.getResourceCapacityMaximumCapacity().intValue(),
                        registeredResource.getInAggregateNodeMRID().getValue(),
                        registeredResource.getOutAggregateNodeMRID().getValue());
                    if (Boolean.valueOf(isDirection1Inverted).equals(hvdcRange1Direction2.getLeft())) {
                        throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "HVDC line should be defined in the opposite direction");
                    }
                    direction2minRange1 = hvdcRange1Direction2.getRight().get(0);
                    direction2maxRange1 = hvdcRange1Direction2.getRight().get(1);

                    direction2hvdc1IsDefined = true;
                } else {
                    hvdcRangeActionDirection2Id2 = registeredResource.getMRID().getValue();
                    if (hvdcRangeActionDirection2Id2.equals(hvdcRangeActionDirection2Id1)) {
                        throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Wrong id : HVDC registered resource has an id already defined");
                    }
                    // Read range
                    Pair<Boolean, List<Integer>> hvdcRange2Direction2 = defineHvdcRange(hvdcRangeActionDirection2Id2,
                        registeredResource.getResourceCapacityMinimumCapacity().intValue(),
                        registeredResource.getResourceCapacityMaximumCapacity().intValue(),
                        registeredResource.getInAggregateNodeMRID().getValue(),
                        registeredResource.getOutAggregateNodeMRID().getValue());
                    if (Boolean.valueOf(isDirection1Inverted).equals(hvdcRange2Direction2.getLeft())) {
                        throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "HVDC registered resources reference lines in opposite directions");
                    }
                    direction2minRange2 = hvdcRange2Direction2.getRight().get(0);
                    direction2maxRange2 = hvdcRange2Direction2.getRight().get(1);
                }
            }

            //------------------    3) Concatenate hvdcRangeActionDirection1 and hvdcRangeActionDirection2      ------------------
            // hvdcRangeActionDirection1 and hvdcRangeActionDirection2 both contain 2 hvdc remedial actions
            // They must be matched by id and then have their ranges concatenated.
            int hvdc1minRange;
            int hvdc1maxRange;
            int hvdc2minRange;
            int hvdc2maxRange;

            if (hvdcRangeActionDirection2Id1.equals(hvdcRangeActionDirection1Id1)) {
                List<Integer> concatenatedHvdc1Range = concatenateHvdcRanges(
                    direction1minRange1, direction2minRange1,
                    direction1maxRange1, direction2maxRange1);
                hvdc1minRange = concatenatedHvdc1Range.get(0);
                hvdc1maxRange = concatenatedHvdc1Range.get(1);

                // If hvdcRangeActionDirection2Id1 and hvdcRangeActionDirection1Id1 have the same id,
                // then hvdcRangeActionDirection2Id2 and hvdcRangeActionDirection1Id2 must also have a shared (and different) id.
                if (hvdcRangeActionDirection2Id2.equals(hvdcRangeActionDirection1Id2)) {
                    List<Integer> concatenatedHvdc2Range = concatenateHvdcRanges(
                        direction1minRange2, direction2minRange2,
                        direction1maxRange2, direction2maxRange2);
                    hvdc2minRange = concatenatedHvdc2Range.get(0);
                    hvdc2maxRange = concatenatedHvdc2Range.get(1);
                } else {
                    throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "HVDC ID mismatch");
                }
            } else if (hvdcRangeActionDirection2Id1.equals(hvdcRangeActionDirection1Id2)) {
                List<Integer> concatenatedHvdcRange = concatenateHvdcRanges(
                    direction1minRange2, direction2minRange1,
                    direction1maxRange2, direction2maxRange1);
                hvdc2minRange = concatenatedHvdcRange.get(0);
                hvdc2maxRange = concatenatedHvdcRange.get(1);

                // If hvdcRangeActionDirection2Id1 and hvdcRangeActionDirection1Id2 have the same id,
                // then hvdcRangeActionDirection2Id2 and hvdcRangeActionDirection1Id1 must also have a shared (and different) id.
                if (hvdcRangeActionDirection2Id2.equals(hvdcRangeActionDirection1Id1)) {
                    List<Integer> concatenatedHvdc1Range = concatenateHvdcRanges(
                        direction1minRange1, direction2minRange2,
                        direction1maxRange1, direction2maxRange2);
                    hvdc1minRange = concatenatedHvdc1Range.get(0);
                    hvdc1maxRange = concatenatedHvdc1Range.get(1);
                } else {
                    throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "HVDC ID mismatch");
                }
            } else {
                throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "HVDC ID mismatch");
            }

            //------------------    4) Add hvdcRangeActionAdder1 and hvdcRangeActionAdder2 with concatenated range   ------------------
            hvdcRangeActionAdder1.newRange().withMin(hvdc1minRange).withMax(hvdc1maxRange).add();
            hvdcRangeActionAdder2.newRange().withMin(hvdc2minRange).withMax(hvdc2maxRange).add();
            // ID
            String id = hvdcRangeActionDirection1.getMRID() + " + " + hvdcRangeActionDirection2.getMRID() + " - ";
            String hvdc1Id = id + hvdcRangeActionDirection1Id1;
            String hvdc2Id = id + hvdcRangeActionDirection1Id2;
            Set<String> createdIds = Set.of(hvdc1Id, hvdc2Id);
            hvdcRangeActionAdder1.withId(hvdc1Id).withName(hvdc1Id);
            hvdcRangeActionAdder2.withId(hvdc2Id).withName(hvdc2Id);

            Set<RemedialActionSeriesCreationContext> hvdcRemedialActionSeriesCreationContexts = new HashSet<>();
            if (invalidContingencies.isEmpty()) {
                hvdcRemedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.importedHvdcRa(hvdcRangeActionDirection1.getMRID(), createdIds, false, isDirection1Inverted, ""));
                hvdcRemedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.importedHvdcRa(hvdcRangeActionDirection2.getMRID(), createdIds, false, !isDirection1Inverted, ""));
            } else {
                String contingencyList = StringUtils.join(invalidContingencies, ", ");
                hvdcRemedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.importedHvdcRa(hvdcRangeActionDirection1.getMRID(), createdIds, true, isDirection1Inverted, String.format("Contingencies %s not defined in B55s", contingencyList)));
                hvdcRemedialActionSeriesCreationContexts.add(RemedialActionSeriesCreationContext.importedHvdcRa(hvdcRangeActionDirection2.getMRID(), createdIds, true, !isDirection1Inverted, String.format("Contingencies %s not defined in B55s", contingencyList)));
            }

            hvdcRangeActionAdder1.add();
            hvdcRangeActionAdder2.add();
            return hvdcRemedialActionSeriesCreationContexts;
        } catch (FaraoImportException e) {
            return Set.of(RemedialActionSeriesCreationContext.notImported(cimSerieId, e.getImportStatus(), e.getMessage()));
        }
    }

    // Return type : Pair of a list of booleans and a list of two integers :
    // -- the booleans indicate :
    // -------- * whether the Hvdc range is ill defined (boolean set to false when an issue is detected)
    // -------- * whether the Hvdc line is inverted
    // -- the integers are the min and max Hvdc range.
    private Pair<Boolean, List<Integer>> readHvdcRegisteredResource(HvdcRangeActionAdder hvdcRangeActionAdder, RemedialActionRegisteredResource registeredResource) {
        String hvdcId = registeredResource.getMRID().getValue();
        checkHvdcNetworkElement(hvdcId);
        hvdcRangeActionAdder.withNetworkElement(hvdcId);

        // Usage rules
        RemedialActionSeriesCreator.addUsageRules(CimConstants.ApplicationModeMarketObjectStatus.AUTO.getStatus(), hvdcRangeActionAdder, contingencies, invalidContingencies, flowCnecs, sharedDomain);

        return defineHvdcRange(hvdcId,
            registeredResource.getResourceCapacityMinimumCapacity().intValue(),
            registeredResource.getResourceCapacityMaximumCapacity().intValue(),
            registeredResource.getInAggregateNodeMRID().getValue(),
            registeredResource.getOutAggregateNodeMRID().getValue());
    }

    // Return type : Pair of one boolean and a list of two integers :
    // -- the boolean indicates whether the Hvdc ranges can be concatenated or not (boolean set to false when an issue is detected),
    // -- the integers are the min and max Hvdc concatenated range.
    private List<Integer> concatenateHvdcRanges(int min1, int min2, int max1, int max2) {
        if (min2 < min1) {
            if (max2 < min1) {
                throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "HVDC range mismatch");
            } else {
                return List.of(min2, Math.max(max1, max2));
            }
        } else {
            if (min2 > max1) {
                throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "HVDC range mismatch");
            } else {
                return List.of(min1, Math.max(max1, max2));
            }
        }
    }

    // Throws an exception if the RegisteredResource is ill defined
    private void checkRegisteredResource(RemedialActionRegisteredResource registeredResource) {
        // Check MarketObjectStatus
        String marketObjectStatusStatus = registeredResource.getMarketObjectStatusStatus();
        if (Objects.isNull(marketObjectStatusStatus)) {
            throw new FaraoImportException(ImportStatus.INCOMPLETE_DATA, "Missing marketObjectStatusStatus");
        }
        if (!marketObjectStatusStatus.equals(CimConstants.MarketObjectStatus.ABSOLUTE.getStatus())) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong marketObjectStatusStatus: %s", marketObjectStatusStatus));
        }
        // Check unit
        String unit = registeredResource.getResourceCapacityUnitSymbol();
        if (Objects.isNull(unit)) {
            throw new FaraoImportException(ImportStatus.INCOMPLETE_DATA, "Missing unit");
        }
        if (!registeredResource.getResourceCapacityUnitSymbol().equals(MEGAWATT_UNIT_SYMBOL)) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Wrong unit: %s", unit));
        }
        // Check that min/max are defined
        if (Objects.isNull(registeredResource.getResourceCapacityMinimumCapacity()) || Objects.isNull(registeredResource.getResourceCapacityMaximumCapacity())) {
            throw new FaraoImportException(ImportStatus.INCOMPLETE_DATA, "Missing min or max resource capacity");
        }
    }

    // Return type : Pair of one boolean and a list of two integers :
    // -- the boolean indicates whether the Hvdc line is inverted
    // -- the integers are the min and max Hvdc range.
    private Pair<Boolean, List<Integer>> defineHvdcRange(String networkElement, int minCapacity, int maxCapacity, String inNode, String outNode) {
        HvdcLine hvdcLine = network.getHvdcLine(networkElement);
        if (Objects.isNull(hvdcLine)) {
            throw new FaraoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Not a HVDC line");
        }
        String from = hvdcLine.getConverterStation1().getTerminal().getBusBreakerView().getBus().getId();
        String to = hvdcLine.getConverterStation2().getTerminal().getBusBreakerView().getBus().getId();

        if (Objects.isNull(inNode) || Objects.isNull(outNode)) {
            throw new FaraoImportException(ImportStatus.INCOMPLETE_DATA, "Missing HVDC in or out aggregate nodes");
        }
        if (inNode.equals(from) && outNode.equals(to)) {
            return Pair.of(false, List.of(minCapacity, maxCapacity));
        } else if (inNode.equals(to) && outNode.equals(from)) {
            return Pair.of(true, List.of(-maxCapacity, -minCapacity));
        } else {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Wrong HVDC inAggregateNode/outAggregateNode");
        }
    }

    private void checkHvdcNetworkElement(String networkElement) {
        if (Objects.isNull(network.getHvdcLine(networkElement))) {
            throw new FaraoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Not a HVDC line");
        }
        if (Objects.isNull(network.getHvdcLine(networkElement).getExtension(HvdcAngleDroopActivePowerControl.class))) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "HVDC does not have a HvdcAngleDroopActivePowerControl extension");
        }
        if (!network.getHvdcLine(networkElement).getExtension(HvdcAngleDroopActivePowerControl.class).isEnabled()) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "HvdcAngleDroopActivePowerControl extension is not enabled");
        }
    }

    private void checkHvdcRangeActionValidity() {
        // Two hvdc range actions have been defined :
        if (storedHvdcRangeActions.size() != 2) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("%s HVDC remedial actions were defined instead of 2", storedHvdcRangeActions.size()));
        }
        // Each range action must contain 4 registered resources
        if (storedHvdcRangeActions.get(0).getRegisteredResource().size() != 4) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("%s registered resources were defined in HVDC instead of 4", storedHvdcRangeActions.get(0).getRegisteredResource().size()));
        }
        if (storedHvdcRangeActions.get(1).getRegisteredResource().size() != 4) {
            throw new FaraoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("%s registered resources were defined in HVDC instead of 4", storedHvdcRangeActions.get(1).getRegisteredResource().size()));
        }
    }
}
