/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.range.RangeType;
import com.powsybl.openrao.data.cracapi.range.TapRangeAdder;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeActionAdder;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.craccreation.util.OpenRaoImportException;
import com.powsybl.openrao.data.craccreation.util.iidm.IidmPstHelper;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBag;

import java.util.*;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class PstRangeActionCreator {
    private final Crac crac;
    private final Network network;

    public PstRangeActionCreator(Crac crac, Network network) {
        this.crac = crac;
        this.network = network;
    }

    public PstRangeActionAdder getPstRangeActionAdder(Map<String, Set<PropertyBag>> linkedTapPositionActions, Map<String, Set<PropertyBag>> linkedStaticPropertyRanges, String remedialActionId, String elementaryActionsAggregatorId, List<String> alterations) {
        PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction().withId(remedialActionId);

        if (linkedTapPositionActions.containsKey(elementaryActionsAggregatorId)) {
            if (linkedTapPositionActions.get(elementaryActionsAggregatorId).size() > 1) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action %s will not be imported because several TapPositionActions were defined for the same PST Range Action when only one is expected", remedialActionId));
            }
            PropertyBag tapPositionActionPropertyBag = linkedTapPositionActions.get(elementaryActionsAggregatorId).iterator().next();
            Set<PropertyBag> linkedStaticPropertyRangesToTapPositionAction = new HashSet<>();
            if (linkedStaticPropertyRanges.containsKey(tapPositionActionPropertyBag.getId(CsaProfileConstants.MRID))) {
                linkedStaticPropertyRangesToTapPositionAction = linkedStaticPropertyRanges.get(tapPositionActionPropertyBag.getId(CsaProfileConstants.MRID));
            }
            addTapPositionElementaryAction(linkedStaticPropertyRangesToTapPositionAction, remedialActionId, pstRangeActionAdder, tapPositionActionPropertyBag);
        }

        return pstRangeActionAdder;
    }

    private void addTapPositionElementaryAction(Set<PropertyBag> linkedStaticPropertyRangesToTapPositionAction, String remedialActionId, PstRangeActionAdder pstRangeActionAdder, PropertyBag tapPositionActionPropertyBag) {
        Optional<String> normalEnabledOpt = Optional.ofNullable(tapPositionActionPropertyBag.get(CsaProfileConstants.NORMAL_ENABLED));
        if (normalEnabledOpt.isPresent() && !Boolean.parseBoolean(normalEnabledOpt.get())) {
            throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO, String.format("Remedial action %s will not be imported because the field normalEnabled in TapPositionAction is set to false", remedialActionId));
        }
        CsaProfileCracUtils.checkPropertyReference(tapPositionActionPropertyBag, remedialActionId, "TapPositionAction", CsaProfileConstants.PropertyReference.TAP_CHANGER.toString());
        String tapChangerId = tapPositionActionPropertyBag.getId(CsaProfileConstants.TAP_CHANGER).replace("+", " ");
        IidmPstHelper iidmPstHelper = new IidmPstHelper(tapChangerId, network);
        if (!iidmPstHelper.isValid()) {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Remedial action " + remedialActionId + " will not be imported because " + iidmPstHelper.getInvalidReason());
        }

        pstRangeActionAdder
            .withNetworkElement(tapChangerId)
            .withInitialTap(iidmPstHelper.getInitialTap())
            .withTapToAngleConversionMap(iidmPstHelper.getTapToAngleConversionMap());

        if (!linkedStaticPropertyRangesToTapPositionAction.isEmpty()) {
            Optional<Integer> normalValueUp = Optional.empty();
            Optional<Integer> normalValueDown = Optional.empty();
            for (PropertyBag staticPropertyRangePropertyBag : linkedStaticPropertyRangesToTapPositionAction) {
                CsaProfileCracUtils.checkPropertyReference(staticPropertyRangePropertyBag, remedialActionId, "StaticPropertyRange", CsaProfileConstants.PropertyReference.TAP_CHANGER.toString());
                String valueKind = staticPropertyRangePropertyBag.get(CsaProfileConstants.STATIC_PROPERTY_RANGE_VALUE_KIND);

                if (!valueKind.equals(CsaProfileConstants.ValueOffsetKind.ABSOLUTE.toString())) {
                    throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because StaticPropertyRange has wrong value of valueKind, the only allowed value is absolute");
                } else {
                    String direction = staticPropertyRangePropertyBag.get(CsaProfileConstants.STATIC_PROPERTY_RANGE_DIRECTION);
                    int normalValue = (int) Float.parseFloat(staticPropertyRangePropertyBag.get(CsaProfileConstants.NORMAL_VALUE));
                    if (direction.equals(CsaProfileConstants.RelativeDirectionKind.DOWN.toString())) {
                        normalValueDown.ifPresent(value -> {
                            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because there is more than ONE StaticPropertyRange with direction RelativeDirectionKind.down");
                        });
                        normalValueDown = Optional.of(normalValue);
                    } else if (direction.equals(CsaProfileConstants.RelativeDirectionKind.UP.toString())) {
                        normalValueUp.ifPresent(value -> {
                            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because there is more than ONE StaticPropertyRange with direction RelativeDirectionKind.up");
                        });
                        normalValueUp = Optional.of(normalValue);
                    } else {
                        throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because StaticPropertyRange has wrong value of direction, the only allowed values are RelativeDirectionKind.up and RelativeDirectionKind.down");
                    }
                }
            }
            TapRangeAdder tapRangeAdder = pstRangeActionAdder.newTapRange().withRangeType(RangeType.ABSOLUTE);
            normalValueDown.ifPresent(tapRangeAdder::withMinTap);
            normalValueUp.ifPresent(tapRangeAdder::withMaxTap);
            tapRangeAdder.add();
        }
    }

}
