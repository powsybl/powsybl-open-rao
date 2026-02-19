/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.fbconstraint;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.RemedialActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeActionAdder;
import com.powsybl.openrao.data.crac.io.commons.ucte.UcteGeneratorHelper;
import com.powsybl.openrao.data.crac.io.commons.ucte.UcteNetworkAnalyzer;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.ActionsSetType;
import com.powsybl.openrao.virtualhubs.HvdcLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class HvdcLineRemedialActionAdder {
    private static final String DELIMITER = "_";

    private final Map<String, String> nodeToStationMap;

    private final String fromNodeName;
    private final String toNodeName;
    private final ComplexVariantReader fromComplexVariantReader;
    private final ComplexVariantReader toComplexVariantReader;
    private final UcteGeneratorHelper fromGeneratorHelper;
    private final UcteGeneratorHelper toGeneratorHelper;

    private boolean isValid;

    HvdcLineRemedialActionAdder(final HvdcLine hvdcLine, final UcteNetworkAnalyzer ucteNetworkAnalyzer, final Map<String, ComplexVariantReader> complexVariantReadersByElementId, final Map<String, String> nodeToStationMap) {
        this.nodeToStationMap = nodeToStationMap;

        this.fromNodeName = hvdcLine.from();
        this.toNodeName = hvdcLine.to();
        this.fromComplexVariantReader = complexVariantReadersByElementId.get(fromNodeName);
        this.toComplexVariantReader = complexVariantReadersByElementId.get(toNodeName);
        this.fromGeneratorHelper = new UcteGeneratorHelper(fromNodeName, ucteNetworkAnalyzer);
        this.toGeneratorHelper = new UcteGeneratorHelper(toNodeName, ucteNetworkAnalyzer);

        this.isValid = true;
    }

    void add(final Crac crac, final FbConstraintCreationContext creationContext) {
        checkDataValidity();
        addRangeAction(crac);
        addComplexVariantCreationContexts(creationContext);
    }

    private void checkDataValidity() {
        // If an element of the HVDC line (as defined in VirtualHubs input file) doesn't exist in the CRAC or is invalid,
        // immediately invalidate the adder and return: the HVDC line will be ignored
        if (fromComplexVariantReader == null || toComplexVariantReader == null || !fromComplexVariantReader.isComplexVariantValid() || !toComplexVariantReader.isComplexVariantValid()) {
            isValid = false;
            return;
        }

        if (!fromGeneratorHelper.isValid()) {
            fromComplexVariantReader.invalidateOnInvalidGenerator(fromGeneratorHelper.getImportStatus(), fromGeneratorHelper.getDetail());
            isValid = false;
        }
        if (!toGeneratorHelper.isValid()) {
            toComplexVariantReader.invalidateOnInvalidGenerator(toGeneratorHelper.getImportStatus(), toGeneratorHelper.getDetail());
            isValid = false;
        }

        // Usage rules validity must be checked only if complex variant readers are valid:
        // - complex variant reader invalidity reason is more important than generator helper invalidity reason
        // - if at least one complex variant reader is already invalid, the HVDC line will be ignored anyway
        if (fromComplexVariantReader.isComplexVariantValid() && toComplexVariantReader.isComplexVariantValid()) {
            checkUsageRules();
        }
    }

    private void checkUsageRules() {
        final ActionsSetType fromActionsSetType = fromComplexVariantReader.getComplexVariant().getActionsSet().getFirst();
        final ActionsSetType toActionsSetType = toComplexVariantReader.getComplexVariant().getActionsSet().getFirst();

        if (fromActionsSetType.isPreventive() != toActionsSetType.isPreventive()) {
            fromComplexVariantReader.invalidateOnInconsistencyOnState("preventive");
            toComplexVariantReader.invalidateOnInconsistencyOnState("preventive");
            isValid = false;
        }

        if (fromActionsSetType.isCurative() != toActionsSetType.isCurative()) {
            fromComplexVariantReader.invalidateOnInconsistencyOnState("curative");
            toComplexVariantReader.invalidateOnInconsistencyOnState("curative");
            isValid = false;
        }
    }

    private void addRangeAction(final Crac crac) {
        // HVDC line must be ignored if data is invalid
        if (isValid) {
            final String raId = fromComplexVariantReader.getComplexVariant().getId() + DELIMITER + toComplexVariantReader.getComplexVariant().getId();
            final String raName = fromComplexVariantReader.getComplexVariant().getName() + DELIMITER + toComplexVariantReader.getComplexVariant().getName();
            // TODO Waiting for confirmation of the concatenated-operator format
            final String raOperator = fromComplexVariantReader.getComplexVariant().getTsoOrigin() + DELIMITER + toComplexVariantReader.getComplexVariant().getTsoOrigin();
            // groupId elements must be sorted for the generators alignment to work
            final String raGroupId = Stream.of(fromNodeName, toNodeName)
                .map(nodeToStationMap::get)
                .sorted()
                .collect(Collectors.joining(DELIMITER));

            final InjectionRangeActionAdder injectionRangeActionAdder = crac.newInjectionRangeAction()
                .withId(raId)
                .withName(raName)
                .withOperator(raOperator)
                .withGroupId(raGroupId)
                .withNetworkElementAndKey(-1., fromGeneratorHelper.getGeneratorId())
                .withNetworkElementAndKey(1., toGeneratorHelper.getGeneratorId())
                .withInitialSetpoint(toGeneratorHelper.getCurrentP());

            addRangeToRangeAction(injectionRangeActionAdder);
            addUsageRules(injectionRangeActionAdder, crac);

            injectionRangeActionAdder.add();
        }
    }

    private void addRangeToRangeAction(final InjectionRangeActionAdder injectionRangeActionAdder) {
        // The range defined in the network should always be wider than the range defined in the CRAC, so we can only consider the range of the CRAC
        final ActionReader.HvdcRange fromHvdcRange = fromComplexVariantReader.getActionReaders().getFirst().getHvdcRange();
        final ActionReader.HvdcRange toHvdcRange = toComplexVariantReader.getActionReaders().getFirst().getHvdcRange();
        // Theoretically, if from-range is [a;b] and to-range is [c;d], then we should have a=-d and b=-c.
        // As distribution key associated to "from" is -1 and "to" is +1, we use the to-range as remedial action range.
        // In case of inconsistent data, we use the most restrictive combination.
        injectionRangeActionAdder.newRange()
            .withMin(Math.max(-fromHvdcRange.max(), toHvdcRange.min()))
            .withMax(Math.min(-fromHvdcRange.min(), toHvdcRange.max()))
            .add();
    }

    private void addUsageRules(final RemedialActionAdder<?> remedialActionAdder, final Crac crac) {
        // As checkUsageRules() has been used previously, we know that isPreventive/isCurative booleans are aligned between "from" and "to" complex variants
        final ActionsSetType action = fromComplexVariantReader.getComplexVariant().getActionsSet().getFirst();

        if (action.isPreventive()) {
            remedialActionAdder.newOnInstantUsageRule()
                .withInstant(crac.getPreventiveInstant().getId())
                .add();
        }

        final List<String> fromAfterCoList = fromComplexVariantReader.getAfterCoList();
        final List<String> toAfterCoList = toComplexVariantReader.getAfterCoList();

        if (action.isCurative() && !Objects.isNull(fromAfterCoList)) {
            // We use the intersection of contingencies lists
            final List<String> afterContingencies = new ArrayList<>(fromAfterCoList);
            afterContingencies.retainAll(toAfterCoList);

            for (String contingency : afterContingencies) {
                remedialActionAdder.newOnContingencyStateUsageRule()
                    .withContingency(contingency)
                    .withInstant(crac.getInstant(InstantKind.CURATIVE).getId())
                    .add();
            }
        }
    }

    private void addComplexVariantCreationContexts(final FbConstraintCreationContext creationContext) {
        if (fromComplexVariantReader != null) {
            creationContext.addComplexVariantCreationContext(fromComplexVariantReader.getComplexVariantCreationContext());
        }
        if (toComplexVariantReader != null) {
            creationContext.addComplexVariantCreationContext(toComplexVariantReader.getComplexVariantCreationContext());
        }
    }
}
