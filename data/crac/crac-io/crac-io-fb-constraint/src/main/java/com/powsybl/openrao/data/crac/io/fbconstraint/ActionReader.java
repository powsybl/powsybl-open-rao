/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.fbconstraint;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.crac.api.networkaction.SingleNetworkElementActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeActionAdder;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.ActionType;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.RangeType;
import com.powsybl.openrao.data.crac.io.commons.OpenRaoImportException;
import com.powsybl.openrao.data.crac.io.commons.ucte.UcteNetworkAnalyzer;
import com.powsybl.openrao.data.crac.io.commons.ucte.UctePstHelper;
import com.powsybl.openrao.data.crac.io.commons.ucte.UcteTopologicalElementHelper;

import jakarta.xml.bind.JAXBElement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class ActionReader {

    private static final String PST_TYPE = "PSTTAP";
    private static final String TOPO_TYPE = "STATUS";
    private static final String HVDC_TYPE = "HVDCSETPOINT";

    private static final String NODE_CATEGORY = "node";
    private static final String RANGE_CATEGORY = "range";
    private static final String RELATIVERANGE_CATEGORY = "relativeRange";
    private static final String HVDCGROUPID_CATEGORY = "HVDCGroupId";
    private static final String PSTGROUPID_CATEGORY = "PSTGroupId";

    private final ActionType action;

    private boolean isActionValid = true;
    private String invalidActionReason = "";

    private ActionTypeEnum type;
    private String networkElementId;
    private String nativeNetworkElementId;
    private String groupId;

    // useful only for PSTs
    private UctePstHelper uctePstHelper;
    private List<PstRange> pstRanges;
    private boolean isInverted = false;
    private String inversionMessage = null;

    // useful only for topo
    private com.powsybl.openrao.data.crac.api.networkaction.ActionType topologicalActionType;
    private Identifiable<?> networkElement;

    // useful only for HVDCs
    private HvdcRange hvdcRange;

    record PstRange(int minTap, int maxTap, com.powsybl.openrao.data.crac.api.range.RangeType relativeOrAbsolute) {
    }

    record HvdcRange(int min, int max) {
    }

    ActionTypeEnum getType() {
        return type;
    }

    boolean isActionValid() {
        return isActionValid;
    }

    String getInvalidActionReason() {
        return invalidActionReason;
    }

    ActionReader(final ActionType action, final UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        this.action = action;

        if (HVDC_TYPE.equals(action.getType())) {
            interpretHvdcSetpointWithNetwork();
        } else {
            interpretWithNetwork(ucteNetworkAnalyzer);
        }
    }

    void addAction(PstRangeActionAdder pstRangeActionAdder) {
        if (!type.equals(ActionTypeEnum.PST)) {
            throw new OpenRaoException(String.format("This method is only applicable for Action of type %s", PST_TYPE));
        }

        pstRangeActionAdder.withNetworkElement(networkElementId)
            .withInitialTap(uctePstHelper.getInitialTap())
            .withTapToAngleConversionMap(uctePstHelper.getTapToAngleConversionMap());

        for (PstRange range : pstRanges) {
            pstRangeActionAdder.newTapRange()
                .withRangeType(range.relativeOrAbsolute)
                .withMaxTap(range.maxTap)
                .withMinTap(range.minTap)
                .add();
        }

        if (!Objects.isNull(groupId)) {
            pstRangeActionAdder.withGroupId(groupId);
        }
    }

    void addAction(NetworkActionAdder networkActionAdder, String remedialActionId) {
        if (!type.equals(ActionTypeEnum.TOPO)) {
            throw new OpenRaoException(String.format("This method is only applicable for Action of type %s", TOPO_TYPE));
        }

        SingleNetworkElementActionAdder<?> actionAdder;
        if (networkElement.getType() == IdentifiableType.SWITCH) {
            actionAdder = networkActionAdder.newSwitchAction().withActionType(topologicalActionType);
        } else if (networkElement instanceof Branch) {
            actionAdder = networkActionAdder.newTerminalsConnectionAction().withActionType(topologicalActionType);
        } else {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "FlowBasedConstraint topological action " + remedialActionId + " should be on branch or on switch, not on " + networkElement.getType());
        }
        actionAdder.withNetworkElement(networkElementId).add();
    }

    boolean isInverted() {
        return isInverted;
    }

    String getInversionMessage() {
        return inversionMessage;
    }

    String getNativeNetworkElementId() {
        return nativeNetworkElementId;
    }

    String getNetworkElementId() {
        return networkElementId;
    }

    String getGroupId() {
        return groupId;
    }

    HvdcRange getHvdcRange() {
        return hvdcRange;
    }

    com.powsybl.openrao.data.crac.api.networkaction.ActionType getActionType() {
        return topologicalActionType;
    }

    private void interpretHvdcSetpointWithNetwork() {
        final Iterator<Serializable> actionTypeIterator = action.getContent().stream()
            .filter(serializable -> serializable.getClass() != String.class)
            .iterator();

        while (actionTypeIterator.hasNext()) {
            try {
                final JAXBElement<?> jaxbElement = (JAXBElement<?>) actionTypeIterator.next();
                final String elementCategory = jaxbElement.getName().getLocalPart();
                switch (elementCategory) {
                    case NODE_CATEGORY -> {
                        final ActionType.Node node = (ActionType.Node) jaxbElement.getValue();
                        networkElementId = node.getCode();
                    }
                    case RANGE_CATEGORY -> {
                        final RangeType rangeType = (RangeType) jaxbElement.getValue();
                        this.hvdcRange = new HvdcRange(rangeType.getMin().intValue(), rangeType.getMax().intValue());
                    }
                    case HVDCGROUPID_CATEGORY -> groupId = (String) jaxbElement.getValue();
                    default -> throw new ClassCastException();
                }
            } catch (final ClassCastException e) {
                invalidate("action's elementCategory not recognized");
                break;
            }
        }

        this.type = ActionTypeEnum.HVDC;
    }

    private void interpretWithNetwork(UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        // check first element of the action, which is assumed to be a branch
        Iterator<?> actionTypeIterator = action.getContent().stream().filter(serializable -> serializable.getClass() != String.class).iterator();
        ActionType.Branch branch = (ActionType.Branch) ((JAXBElement<?>) actionTypeIterator.next()).getValue();

        this.nativeNetworkElementId = String.format("%1$-8s %2$-8s %3$s", branch.getFrom(), branch.getTo(), branch.getOrder() != null ? branch.getOrder() : branch.getElementName());

        switch (action.getType()) {
            case PST_TYPE -> interpretAsPstRangeAction(branch, actionTypeIterator, ucteNetworkAnalyzer);
            case TOPO_TYPE -> interpretAsTopologicalAction(branch, actionTypeIterator, ucteNetworkAnalyzer);
            default -> invalidate(String.format("action of type %s is not handled", action.getType()));
        }
    }

    private void interpretAsPstRangeAction(ActionType.Branch branch, Iterator<?> actionTypeIterator, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        this.type = ActionTypeEnum.PST;
        this.uctePstHelper = new UctePstHelper(branch.getFrom(), branch.getTo(), branch.getOrder(), branch.getElementName(), ucteNetworkAnalyzer);

        if (!uctePstHelper.isValid()) {
            invalidate(uctePstHelper.getInvalidReason());
            return;
        }

        this.networkElementId = uctePstHelper.getIdInNetwork();
        this.isInverted = !uctePstHelper.isInvertedInNetwork(); // POWSYBL convention actually inverts transformers usually
        if (this.isInverted) {
            inversionMessage = "PST was inverted in order to match the element in the network";
        }
        getPstRangesAndGroupId(actionTypeIterator, isInverted);
    }

    private void interpretAsTopologicalAction(ActionType.Branch branch, Iterator<?> actionTypeIterator, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        this.type = ActionTypeEnum.TOPO;
        UcteTopologicalElementHelper ucteElementHelper = new UcteTopologicalElementHelper(branch.getFrom(), branch.getTo(), branch.getOrder(), branch.getElementName(), ucteNetworkAnalyzer);

        if (!ucteElementHelper.isValid()) {
            invalidate(ucteElementHelper.getInvalidReason());
        }

        this.networkElementId = ucteElementHelper.getIdInNetwork();
        this.networkElement = ucteElementHelper.getIidmIdentifiable();
        getActionType(actionTypeIterator);
    }

    private void getPstRangesAndGroupId(Iterator<?> actionTypeIterator, boolean shouldInvertRanges) {
        pstRanges = new ArrayList<>();

        while (actionTypeIterator.hasNext()) {
            try {
                JAXBElement<?> jaxbElement = (JAXBElement<?>) actionTypeIterator.next();
                String elementCategory = jaxbElement.getName().getLocalPart();
                if (elementCategory.equals(RELATIVERANGE_CATEGORY) || elementCategory.equals(RANGE_CATEGORY)) {
                    pstRanges.add(getRangesFromJaxbElement(jaxbElement, shouldInvertRanges));
                } else if (elementCategory.equals(PSTGROUPID_CATEGORY)) {
                    groupId = (String) jaxbElement.getValue();
                }
            } catch (ClassCastException e) {
                invalidate("action's elementCategory not recognized");
                break;
            }
        }
    }

    private PstRange getRangesFromJaxbElement(JAXBElement<?> jaxbElementRange, boolean shouldInvert) {
        final String rangeCategory = jaxbElementRange.getName().getLocalPart();
        final RangeType rangeType = (RangeType) jaxbElementRange.getValue();
        final int minTap = shouldInvert ? -rangeType.getMax().intValue() : rangeType.getMin().intValue();
        final int maxTap = shouldInvert ? -rangeType.getMin().intValue() : rangeType.getMax().intValue();
        com.powsybl.openrao.data.crac.api.range.RangeType relativeOrAbsolute = null;
        if (rangeCategory.equals(RELATIVERANGE_CATEGORY)) {
            relativeOrAbsolute = com.powsybl.openrao.data.crac.api.range.RangeType.RELATIVE_TO_PREVIOUS_INSTANT;
        } else if (rangeCategory.equals(RANGE_CATEGORY)) {
            relativeOrAbsolute = com.powsybl.openrao.data.crac.api.range.RangeType.ABSOLUTE;
        } else {
            invalidate(String.format("unknown type of range category %s", rangeCategory));
        }
        return new PstRange(minTap, maxTap, relativeOrAbsolute);
    }

    private void getActionType(Iterator<?> actionTypeIterator) {
        String actionAsString = ((JAXBElement<?>) actionTypeIterator.next()).getValue().toString();

        if (!actionAsString.equals("OPEN") && !actionAsString.equals("CLOSE")) {
            invalidate(String.format("unknown topological action: %s", actionAsString));
        }

        topologicalActionType = com.powsybl.openrao.data.crac.api.networkaction.ActionType.valueOf(actionAsString);
    }

    private void invalidate(String reason) {
        this.isActionValid = false;
        this.invalidActionReason = reason;
    }
}
