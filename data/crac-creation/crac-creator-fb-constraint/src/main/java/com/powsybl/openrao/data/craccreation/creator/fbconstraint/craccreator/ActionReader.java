/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.fbconstraint.craccreator;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeActionAdder;
import com.powsybl.openrao.data.craccreation.creator.fbconstraint.xsd.ActionType;
import com.powsybl.openrao.data.craccreation.creator.fbconstraint.xsd.RangeType;
import com.powsybl.openrao.data.craccreation.util.ucte.UcteNetworkAnalyzer;
import com.powsybl.openrao.data.craccreation.util.ucte.UctePstHelper;
import com.powsybl.openrao.data.craccreation.util.ucte.UcteTopologicalElementHelper;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
class ActionReader {

    private static final String PST = "PSTTAP";
    private static final String TOPO = "STATUS";

    private final ActionType action;

    private boolean isActionValid = true;
    private String invalidActionReason = "";

    private String networkElementId;
    private Type type;
    private String nativeNetworkElementId;

    //useful only for PSTs
    private UctePstHelper uctePstHelper;
    private List<ActionReader.Range> ranges;
    private String groupId;
    private boolean isInverted = false;
    private String inversionMessage = null;

    // useful only for topo
    private com.powsybl.openrao.data.cracapi.networkaction.ActionType topologicalActionType;

    enum Type {
        TOPO,
        PST
    }

    private static class Range {
        private int minTap;
        private int maxTap;
        private com.powsybl.openrao.data.cracapi.range.RangeType relativeOrAbsolute;
    }

    Type getType() {
        return type;
    }

    boolean isActionValid() {
        return isActionValid;
    }

    String getInvalidActionReason() {
        return invalidActionReason;
    }

    ActionReader(ActionType action, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        this.action = action;

        interpretWithNetwork(ucteNetworkAnalyzer);
    }

    void addAction(PstRangeActionAdder pstRangeActionAdder) {

        if (!type.equals(Type.PST)) {
            throw new OpenRaoException(String.format("This method is only applicable for Action of type %s", PST));
        }

        pstRangeActionAdder.withNetworkElement(networkElementId)
            .withInitialTap(uctePstHelper.getInitialTap())
            .withTapToAngleConversionMap(uctePstHelper.getTapToAngleConversionMap());

        for (Range range : ranges) {
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

    void addAction(NetworkActionAdder networkActionAdder) {

        if (!type.equals(Type.TOPO)) {
            throw new OpenRaoException(String.format("This method is only applicable for Action of type %s", TOPO));
        }

        // networkElementId is a branch (not a switch)
        networkActionAdder.newTerminalsConnectionAction()
            .withNetworkElement(networkElementId)
            .withActionType(topologicalActionType)
            .add();
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

    com.powsybl.openrao.data.cracapi.networkaction.ActionType getActionType() {
        return topologicalActionType;
    }

    private void interpretWithNetwork(UcteNetworkAnalyzer ucteNetworkAnalyzer) {

        // check first element of the action, which is assumed to be a branch
        Iterator<?> actionTypeIterator = action.getContent().stream().filter(serializable -> serializable.getClass() != String.class).iterator();
        ActionType.Branch branch = (ActionType.Branch) ((JAXBElement<?>) actionTypeIterator.next()).getValue();

        this.nativeNetworkElementId = String.format("%1$-8s %2$-8s %3$s", branch.getFrom(), branch.getTo(), branch.getOrder() != null ? branch.getOrder() : branch.getElementName());

        switch (action.getType()) {
            case PST:
                interpretAsPstRangeAction(branch, actionTypeIterator, ucteNetworkAnalyzer);
                break;
            case TOPO:
                interpretAsTopologicalAction(branch, actionTypeIterator, ucteNetworkAnalyzer);
                break;
            default:
                invalidate(String.format("action of type %s is not handled", action.getType()));
        }
    }

    private void interpretAsPstRangeAction(ActionType.Branch branch, Iterator<?> actionTypeIterator, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        this.type = Type.PST;
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
        getRangesAndGroupId(actionTypeIterator, isInverted);
    }

    private void interpretAsTopologicalAction(ActionType.Branch branch, Iterator<?> actionTypeIterator, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        this.type = Type.TOPO;
        UcteTopologicalElementHelper ucteElementHelper = new UcteTopologicalElementHelper(branch.getFrom(), branch.getTo(), branch.getOrder(), branch.getElementName(), ucteNetworkAnalyzer);

        if (!ucteElementHelper.isValid()) {
            invalidate(ucteElementHelper.getInvalidReason());
        }

        this.networkElementId = ucteElementHelper.getIdInNetwork();
        getActionType(actionTypeIterator);
    }

    private void getRangesAndGroupId(Iterator<?> actionTypeIterator, boolean shouldInvertRanges) {
        ranges = new ArrayList<>();

        while (actionTypeIterator.hasNext()) {
            try {
                JAXBElement<?> jaxbElement = (JAXBElement<?>) actionTypeIterator.next();
                String elementCategory = jaxbElement.getName().getLocalPart();
                if (elementCategory.equals("relativeRange") || elementCategory.equals("range")) {
                    ranges.add(getRangesFromJaxbElement(jaxbElement, shouldInvertRanges));
                } else if (elementCategory.equals("PSTGroupId")) {
                    groupId = (String) jaxbElement.getValue();
                }
            } catch (ClassCastException e) {
                invalidate("action's elementCategory not recognized");
                break;
            }
        }
    }

    private ActionReader.Range getRangesFromJaxbElement(JAXBElement<?> jaxbElementRange, boolean shouldInvert) {
        Range range = new Range();
        String rangeCategory = jaxbElementRange.getName().getLocalPart();
        RangeType rangeType = (RangeType) jaxbElementRange.getValue();
        range.minTap = shouldInvert ? -rangeType.getMax().intValue() : rangeType.getMin().intValue();
        range.maxTap = shouldInvert ? -rangeType.getMin().intValue() : rangeType.getMax().intValue();
        if (rangeCategory.equals("relativeRange")) {
            range.relativeOrAbsolute = com.powsybl.openrao.data.cracapi.range.RangeType.RELATIVE_TO_PREVIOUS_INSTANT;
        } else if (rangeCategory.equals("range")) {
            range.relativeOrAbsolute = com.powsybl.openrao.data.cracapi.range.RangeType.ABSOLUTE;
        } else {
            invalidate(String.format("unknown type of range category %s", rangeCategory));
        }
        return range;
    }

    private void getActionType(Iterator<?> actionTypeIterator) {
        String actionAsString = ((JAXBElement<?>) actionTypeIterator.next()).getValue().toString();

        if (!actionAsString.equals("OPEN") && !actionAsString.equals("CLOSE")) {
            invalidate(String.format("unknown topological action: %s", actionAsString));
        }

        topologicalActionType = com.powsybl.openrao.data.cracapi.networkaction.ActionType.valueOf(actionAsString);
    }

    private void invalidate(String reason) {
        this.isActionValid = false;
        this.invalidActionReason = reason;
    }
}
