/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.crac_creation.creator.csa_profile.crac_creator.remedial_action;

import com.powsybl.open_rao.data.crac_api.Crac;
import com.powsybl.open_rao.data.crac_api.range.RangeType;
import com.powsybl.open_rao.data.crac_api.range.TapRangeAdder;
import com.powsybl.open_rao.data.crac_api.range_action.PstRangeActionAdder;
import com.powsybl.open_rao.data.crac_creation.creator.api.ImportStatus;
import com.powsybl.open_rao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileConstants;
import com.powsybl.open_rao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracUtils;
import com.powsybl.open_rao.data.crac_creation.util.FaraoImportException;
import com.powsybl.open_rao.data.crac_creation.util.iidm.IidmPstHelper;
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

    public PstRangeActionAdder getPstRangeActionAdder(Map<String, Set<PropertyBag>> linkedTapPositionActions, Map<String, Set<PropertyBag>> linkedStaticPropertyRanges, String gridStateAlterationId, String targetRaId) {
        PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction().withId(targetRaId);
        if (linkedTapPositionActions.containsKey(gridStateAlterationId)) {
            for (PropertyBag tapPositionActionPropertyBag : linkedTapPositionActions.get(gridStateAlterationId)) {
                Set<PropertyBag> linkedStaticPropertyRangesFoTapPositionAction = new HashSet<>();
                if (linkedStaticPropertyRanges.containsKey(tapPositionActionPropertyBag.getId("mRID"))) {
                    linkedStaticPropertyRangesFoTapPositionAction = linkedStaticPropertyRanges.get(tapPositionActionPropertyBag.getId("mRID"));
                }
                addTapPositionElementaryAction(linkedStaticPropertyRangesFoTapPositionAction, gridStateAlterationId, targetRaId, pstRangeActionAdder, tapPositionActionPropertyBag);
            }
        }
        return pstRangeActionAdder;
    }

    private void addTapPositionElementaryAction(Set<PropertyBag> linkedStaticPropertyRangesFoTapPositionAction, String gridStateAlterationId, String targetRaId, PstRangeActionAdder pstRangeActionAdder, PropertyBag tapPositionActionPropertyBag) {
        CsaProfileCracUtils.checkNormalEnabled(tapPositionActionPropertyBag, gridStateAlterationId, "TapPositionAction");
        CsaProfileCracUtils.checkPropertyReference(tapPositionActionPropertyBag, gridStateAlterationId, "TapPositionAction", CsaProfileConstants.PropertyReference.TAP_CHANGER.toString());
        String rawId = tapPositionActionPropertyBag.get(CsaProfileConstants.TAP_CHANGER);
        String tapChangerId = rawId.substring(rawId.lastIndexOf("#_") + 2).replace("+", " ");
        IidmPstHelper iidmPstHelper = new IidmPstHelper(tapChangerId, network);
        if (!iidmPstHelper.isValid()) {
            throw new FaraoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + targetRaId + " will not be imported because " + iidmPstHelper.getInvalidReason());
        }

        pstRangeActionAdder
            .withNetworkElement(tapChangerId)
            .withInitialTap(iidmPstHelper.getInitialTap())
            .withTapToAngleConversionMap(iidmPstHelper.getTapToAngleConversionMap());

        if (!linkedStaticPropertyRangesFoTapPositionAction.isEmpty()) {
            Optional<Integer> normalValueUp = Optional.empty();
            Optional<Integer> normalValueDown = Optional.empty();
            for (PropertyBag staticPropertyRangePropertyBag : linkedStaticPropertyRangesFoTapPositionAction) {
                CsaProfileCracUtils.checkPropertyReference(staticPropertyRangePropertyBag, gridStateAlterationId, "StaticPropertyRange", CsaProfileConstants.PropertyReference.TAP_CHANGER.toString());
                String valueKind = staticPropertyRangePropertyBag.get(CsaProfileConstants.STATIC_PROPERTY_RANGE_VALUE_KIND);

                if (!valueKind.equals(CsaProfileConstants.ValueOffsetKind.ABSOLUTE.toString())) {
                    throw new FaraoImportException(ImportStatus.NOT_YET_HANDLED_BY_FARAO, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + targetRaId + " will not be imported because StaticPropertyRange has wrong value of valueKind, the only allowed value is 'absolute'");
                } else {
                    String direction = staticPropertyRangePropertyBag.get(CsaProfileConstants.STATIC_PROPERTY_RANGE_DIRECTION);
                    int normalValue = (int) Float.parseFloat(staticPropertyRangePropertyBag.get(CsaProfileConstants.NORMAL_VALUE));
                    if (direction.equals(CsaProfileConstants.RelativeDirectionKind.DOWN.toString())) {
                        normalValueDown = Optional.of(normalValue);
                    } else if (direction.equals(CsaProfileConstants.RelativeDirectionKind.UP.toString())) {
                        normalValueUp = Optional.of(normalValue);
                    } else {
                        throw new FaraoImportException(ImportStatus.NOT_YET_HANDLED_BY_FARAO, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + targetRaId + " will not be imported because StaticPropertyRange has wrong value of direction, the only allowed values are RelativeDirectionKind.up and RelativeDirectionKind.down");
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
