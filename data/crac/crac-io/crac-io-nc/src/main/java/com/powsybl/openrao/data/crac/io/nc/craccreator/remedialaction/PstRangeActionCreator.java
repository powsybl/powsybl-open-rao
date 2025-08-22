/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.remedialaction;

import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracUtils;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.PropertyReference;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.RelativeDirectionKind;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.ValueOffsetKind;
import com.powsybl.openrao.data.crac.io.nc.objects.StaticPropertyRange;
import com.powsybl.openrao.data.crac.io.nc.objects.TapPositionAction;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.range.TapRangeAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeActionAdder;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import com.powsybl.openrao.data.crac.io.commons.OpenRaoImportException;
import com.powsybl.openrao.data.crac.io.commons.iidm.IidmPstHelper;
import com.powsybl.iidm.network.Network;

import java.util.*;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class PstRangeActionCreator {
    private final Crac crac;
    private final Network network;
    private final Map<String, String> pstPerTapChanger;

    public PstRangeActionCreator(Crac crac, Network network, Map<String, String> pstPerTapChanger) {
        this.crac = crac;
        this.network = network;
        this.pstPerTapChanger = pstPerTapChanger;
    }

    public PstRangeActionAdder getPstRangeActionAdder(boolean isGroup, String elementaryActionsAggregatorId, TapPositionAction nativeTapPositionAction, Map<String, Set<StaticPropertyRange>> linkedStaticPropertyRanges, String remedialActionId) {
        PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction().withId(remedialActionId);
        Set<StaticPropertyRange> linkedStaticPropertyRangesToTapPositionAction = new HashSet<>();
        if (linkedStaticPropertyRanges.containsKey(nativeTapPositionAction.mrid())) {
            linkedStaticPropertyRangesToTapPositionAction = linkedStaticPropertyRanges.get(nativeTapPositionAction.mrid());
        }
        addTapPositionElementaryAction(isGroup, elementaryActionsAggregatorId, linkedStaticPropertyRangesToTapPositionAction, remedialActionId, pstRangeActionAdder, nativeTapPositionAction);
        return pstRangeActionAdder;
    }

    private void addTapPositionElementaryAction(boolean isGroup, String elementaryActionsAggregatorId, Set<StaticPropertyRange> linkedStaticPropertyRangesToTapPositionAction, String remedialActionId, PstRangeActionAdder pstRangeActionAdder, TapPositionAction nativeTapPositionAction) {
        if (!nativeTapPositionAction.normalEnabled()) {
            throw new OpenRaoImportException(ImportStatus.NOT_FOR_RAO, String.format("Remedial action %s will not be imported because the field normalEnabled in TapPositionAction is set to false", remedialActionId));
        }
        NcCracUtils.checkPropertyReference(remedialActionId, "TapPositionAction", PropertyReference.TAP_CHANGER, nativeTapPositionAction.propertyReference());
        if (!pstPerTapChanger.containsKey(nativeTapPositionAction.tapChangerId())) {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, String.format("Remedial action %s will not be imported because no PowerTransformer was found in the network for TapChanger %s", remedialActionId, nativeTapPositionAction.tapChangerId()));
        }
        String pstId = pstPerTapChanger.get(nativeTapPositionAction.tapChangerId());
        IidmPstHelper iidmPstHelper = new IidmPstHelper(pstId, network);
        if (!iidmPstHelper.isValid()) {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, String.format("Remedial action %s will not be imported because %s", remedialActionId, iidmPstHelper.getInvalidReason()));
        }

        if (isGroup) {
            pstRangeActionAdder.withGroupId(elementaryActionsAggregatorId);
        }
        pstRangeActionAdder
            .withNetworkElement(pstId)
            .withInitialTap(iidmPstHelper.getInitialTap())
            .withTapToAngleConversionMap(iidmPstHelper.getTapToAngleConversionMap());

        if (!linkedStaticPropertyRangesToTapPositionAction.isEmpty()) {
            Optional<Integer> normalValueUp = Optional.empty();
            Optional<Integer> normalValueDown = Optional.empty();
            for (StaticPropertyRange nativeStaticPropertyRange : linkedStaticPropertyRangesToTapPositionAction) {
                NcCracUtils.checkPropertyReference(remedialActionId, "StaticPropertyRange", PropertyReference.TAP_CHANGER, nativeStaticPropertyRange.propertyReference());

                if (!ValueOffsetKind.ABSOLUTE.toString().equals(nativeStaticPropertyRange.valueKind())) {
                    throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action %s will not be imported because StaticPropertyRange has wrong value of valueKind, the only allowed value is absolute", remedialActionId));
                } else {
                    int normalValue = (int) nativeStaticPropertyRange.normalValue();
                    if (RelativeDirectionKind.DOWN.toString().equals(nativeStaticPropertyRange.direction())) {
                        normalValueDown.ifPresent(value -> {
                            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action %s will not be imported because there is more than ONE StaticPropertyRange with direction RelativeDirectionKind.down", remedialActionId));
                        });
                        normalValueDown = Optional.of(normalValue);
                    } else if (RelativeDirectionKind.UP.toString().equals(nativeStaticPropertyRange.direction())) {
                        normalValueUp.ifPresent(value -> {
                            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action %s will not be imported because there is more than ONE StaticPropertyRange with direction RelativeDirectionKind.up", remedialActionId));
                        });
                        normalValueUp = Optional.of(normalValue);
                    } else {
                        throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, String.format("Remedial action %s will not be imported because StaticPropertyRange has wrong value of direction, the only allowed values are RelativeDirectionKind.up and RelativeDirectionKind.down", remedialActionId));
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
