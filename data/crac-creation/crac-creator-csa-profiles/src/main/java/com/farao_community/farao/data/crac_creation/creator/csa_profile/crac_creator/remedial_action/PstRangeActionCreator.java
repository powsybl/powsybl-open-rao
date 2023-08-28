/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.range.RangeType;
import com.farao_community.farao.data.crac_api.range.TapRangeAdder;
import com.farao_community.farao.data.crac_api.range_action.PstRangeActionAdder;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileConstants;
import com.farao_community.farao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracUtils;
import com.farao_community.farao.data.crac_creation.util.FaraoImportException;
import com.farao_community.farao.data.crac_creation.util.iidm.IidmPstHelper;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.PropertyBag;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

    public PstRangeActionAdder getPstRangeActionAdder(Map<String, Set<PropertyBag>> linkedTapPositionActions, Map<String, Set<PropertyBag>> linkedStaticPropertyRanges, String remedialActionId) {
        PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction().withId(remedialActionId);
        if (linkedTapPositionActions.containsKey(remedialActionId)) {
            for (PropertyBag tapPositionActionPropertyBag : linkedTapPositionActions.get(remedialActionId)) {
                Set<PropertyBag> linkedStaticPropertyRangesFoTapPositionAction = new HashSet<>();
                if (linkedStaticPropertyRanges.containsKey(tapPositionActionPropertyBag.getId("mRID"))) {
                    linkedStaticPropertyRangesFoTapPositionAction = linkedStaticPropertyRanges.get(tapPositionActionPropertyBag.getId("mRID"));
                }
                addTapPositionElementaryAction(linkedStaticPropertyRangesFoTapPositionAction, remedialActionId, pstRangeActionAdder, tapPositionActionPropertyBag);
            }
        }
        return pstRangeActionAdder;
    }

    private void addTapPositionElementaryAction(Set<PropertyBag> linkedStaticPropertyRangesFoTapPositionAction, String remedialActionId, PstRangeActionAdder pstRangeActionAdder, PropertyBag tapPositionActionPropertyBag) {
        CsaProfileCracUtils.checkNormalEnabled(tapPositionActionPropertyBag, remedialActionId, "TapPositionAction");
        CsaProfileCracUtils.checkPropertyReference(tapPositionActionPropertyBag, remedialActionId, "TapPositionAction", CsaProfileConstants.PROPERTY_REFERENCE_TAP_POSITION);
        String rawId = tapPositionActionPropertyBag.get(CsaProfileConstants.TAP_CHANGER);
        String tapChangerId = rawId.substring(rawId.lastIndexOf("_") + 1);
        IidmPstHelper iidmPstHelper = new IidmPstHelper(tapChangerId, network);
        if (!iidmPstHelper.isValid()) {
            throw new FaraoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Remedial Action: " + remedialActionId + " will not be imported because " + iidmPstHelper.getInvalidReason());
        }

        pstRangeActionAdder
                .withNetworkElement(tapChangerId)
                .withInitialTap(iidmPstHelper.getInitialTap())
                .withTapToAngleConversionMap(iidmPstHelper.getTapToAngleConversionMap());

        if (!linkedStaticPropertyRangesFoTapPositionAction.isEmpty()) {
            Optional<Integer> normalValueUp = Optional.empty();
            Optional<Integer> normalValueDown = Optional.empty();
            for (PropertyBag staticPropertyRangePropertyBag : linkedStaticPropertyRangesFoTapPositionAction) {
                CsaProfileCracUtils.checkPropertyReference(staticPropertyRangePropertyBag, remedialActionId, "StaticPropertyRange", CsaProfileConstants.PROPERTY_REFERENCE_TAP_POSITION);
                String valueKind = staticPropertyRangePropertyBag.get(CsaProfileConstants.STATIC_PROPERTY_RANGE_VALUE_KIND);

                if (!valueKind.equals(CsaProfileConstants.VALUE_KIND_ABSOLUTE)) {
                    throw new FaraoImportException(ImportStatus.NOT_YET_HANDLED_BY_FARAO, "Remedial Action: " + remedialActionId + " will not be imported because StaticPropertyRange has wrong value of valueKind, the only allowed value is 'absolute'");
                } else {
                    String direction = staticPropertyRangePropertyBag.get(CsaProfileConstants.STATIC_PROPERTY_RANGE_DIRECTION);
                    int normalValue = (int) Float.parseFloat(staticPropertyRangePropertyBag.get(CsaProfileConstants.NORMAL_VALUE));
                    if (direction.equals(CsaProfileConstants.DIRECTION_DOWN)) {
                        normalValueDown = Optional.of(normalValue);
                    } else if (direction.equals(CsaProfileConstants.DIRECTION_UP)) {
                        normalValueUp = Optional.of(normalValue);
                    } else {
                        throw new FaraoImportException(ImportStatus.NOT_YET_HANDLED_BY_FARAO, "Remedial Action: " + remedialActionId + " will not be imported because StaticPropertyRange has wrong value of direction, the only allowed values are RelativeDirectionKind.up and RelativeDirectionKind.down");
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
