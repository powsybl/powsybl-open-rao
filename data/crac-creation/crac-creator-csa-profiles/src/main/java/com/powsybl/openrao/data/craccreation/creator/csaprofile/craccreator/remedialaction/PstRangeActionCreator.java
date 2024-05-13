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
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.constants.PropertyReference;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.constants.RelativeDirectionKind;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.constants.ValueOffsetKind;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.StaticPropertyRange;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.nc.TapPositionAction;
import com.powsybl.openrao.data.craccreation.util.OpenRaoImportException;
import com.powsybl.openrao.data.craccreation.util.iidm.IidmPstHelper;
import com.powsybl.iidm.network.Network;

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

    public PstRangeActionAdder getPstRangeActionAdder(Map<String, Set<TapPositionAction>> linkedTapPositionActions, Map<String, Set<StaticPropertyRange>> linkedStaticPropertyRanges, String remedialActionId, String elementaryActionsAggregatorId, List<String> alterations) {
        PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction().withId(remedialActionId);

        if (linkedTapPositionActions.containsKey(elementaryActionsAggregatorId)) {
            if (linkedTapPositionActions.get(elementaryActionsAggregatorId).size() > 1) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action %s will not be imported because several TapPositionActions were defined for the same PST Range Action when only one is expected", remedialActionId));
            }
            TapPositionAction nativeTapPositionAction = linkedTapPositionActions.get(elementaryActionsAggregatorId).iterator().next();
            Set<StaticPropertyRange> linkedStaticPropertyRangesToTapPositionAction = new HashSet<>();
            if (linkedStaticPropertyRanges.containsKey(nativeTapPositionAction.mrid())) {
                linkedStaticPropertyRangesToTapPositionAction = linkedStaticPropertyRanges.get(nativeTapPositionAction.mrid());
            }
            addTapPositionElementaryAction(linkedStaticPropertyRangesToTapPositionAction, remedialActionId, pstRangeActionAdder, nativeTapPositionAction);
        }

        return pstRangeActionAdder;
    }

    private void addTapPositionElementaryAction(Set<StaticPropertyRange> linkedStaticPropertyRangesToTapPositionAction, String remedialActionId, PstRangeActionAdder pstRangeActionAdder, TapPositionAction nativeTapPositionAction) {
        if (!nativeTapPositionAction.normalEnabled()) {
            throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO, String.format("Remedial action %s will not be imported because the field normalEnabled in TapPositionAction is set to false", remedialActionId));
        }
        CsaProfileCracUtils.checkPropertyReference(remedialActionId, "TapPositionAction", PropertyReference.TAP_CHANGER, nativeTapPositionAction.propertyReference());
        IidmPstHelper iidmPstHelper = new IidmPstHelper(nativeTapPositionAction.tapChangerId(), network);
        if (!iidmPstHelper.isValid()) {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, "Remedial action " + remedialActionId + " will not be imported because " + iidmPstHelper.getInvalidReason());
        }

        pstRangeActionAdder
            .withNetworkElement(nativeTapPositionAction.tapChangerId())
            .withInitialTap(iidmPstHelper.getInitialTap())
            .withTapToAngleConversionMap(iidmPstHelper.getTapToAngleConversionMap());

        if (!linkedStaticPropertyRangesToTapPositionAction.isEmpty()) {
            Optional<Integer> normalValueUp = Optional.empty();
            Optional<Integer> normalValueDown = Optional.empty();
            for (StaticPropertyRange nativeStaticPropertyRange : linkedStaticPropertyRangesToTapPositionAction) {
                CsaProfileCracUtils.checkPropertyReference(remedialActionId, "StaticPropertyRange", PropertyReference.TAP_CHANGER, nativeStaticPropertyRange.propertyReference());

                if (!ValueOffsetKind.ABSOLUTE.toString().equals(nativeStaticPropertyRange.valueKind())) {
                    throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because StaticPropertyRange has wrong value of valueKind, the only allowed value is absolute");
                } else {
                    int normalValue = (int) nativeStaticPropertyRange.normalValue();
                    if (RelativeDirectionKind.DOWN.toString().equals(nativeStaticPropertyRange.direction())) {
                        normalValueDown.ifPresent(value -> {
                            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, "Remedial action " + remedialActionId + " will not be imported because there is more than ONE StaticPropertyRange with direction RelativeDirectionKind.down");
                        });
                        normalValueDown = Optional.of(normalValue);
                    } else if (RelativeDirectionKind.UP.toString().equals(nativeStaticPropertyRange.direction())) {
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
