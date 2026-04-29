/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.nc.profiles;

import com.powsybl.action.Action;
import com.powsybl.action.GeneratorAction;
import com.powsybl.action.LoadAction;
import com.powsybl.action.PhaseTapChangerTapPositionAction;
import com.powsybl.action.ShuntCompensatorPositionAction;
import com.powsybl.action.SwitchAction;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationContext;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.io.nc.Namespace;
import com.powsybl.openrao.data.raoresult.io.nc.RdfElement;
import com.powsybl.openrao.data.raoresult.io.nc.XmlHelper;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class RemedialActionScheduleProfile extends AbstractNcProfile {
    public RemedialActionScheduleProfile() {
        super("RAS");
    }

    @Override
    public void fillContent(RaoResult raoResult, NcCracCreationContext ncCracCreationContext) {
        ncCracCreationContext.getCrac().getStates().forEach(
            state -> {
                raoResult.getActivatedRangeActionsDuringState(state).forEach(rangeAction -> writeRemedialActionResult(rangeAction, state, raoResult, ncCracCreationContext));
                raoResult.getActivatedNetworkActionsDuringState(state).forEach(networkAction -> writeRemedialActionResult(networkAction, state, raoResult, ncCracCreationContext));
            }
        );
    }

    private void writeRemedialActionResult(RemedialAction<?> remedialAction, State state, RaoResult raoResult, NcCracCreationContext ncCracCreationContext) {
        // Step 1: Create RemedialActionSchedule to indicate the application state of the remedial action
        String remedialActionScheduleMRid = XmlHelper.generateStaticUUID("RemedialActionSchedule", remedialAction.getId(), state.getId());
        addRemedialActionSchedule(remedialAction, state, remedialActionScheduleMRid);

        // Step 2: For each elementary action, create a GridStateIntensitySchedule and a GenericValueTimePoint to indicate the optimal set-point
        if (remedialAction instanceof RangeAction<?> rangeAction) {
            // no elementary action -> use remedial action directly
            String gridStateIntensityScheduleMRid = XmlHelper.generateStaticUUID("GridStateIntensitySchedule", remedialAction.getId(), state.getId());
            addGridStateIntensitySchedule(gridStateIntensityScheduleMRid, remedialActionScheduleMRid, rangeAction.getId());
            addGenericValueTimePoint(gridStateIntensityScheduleMRid, getRangeActionSetPoint(rangeAction, state, raoResult), ncCracCreationContext.getTimeStamp());
        } else if (remedialAction instanceof NetworkAction networkAction) {
            for (Action elementaryAction : networkAction.getElementaryActions()) {
                String gridStateIntensityScheduleMRid = XmlHelper.generateStaticUUID("GridStateIntensitySchedule", elementaryAction.getId(), state.getId());
                addGridStateIntensitySchedule(gridStateIntensityScheduleMRid, remedialActionScheduleMRid, elementaryAction.getId());
                addGenericValueTimePoint(gridStateIntensityScheduleMRid, getActionSetPoint(elementaryAction), ncCracCreationContext.getTimeStamp());
            }
        }
    }

    private void addRemedialActionSchedule(RemedialAction<?> remedialAction, State state, String remedialActionScheduleMRid) {
        RdfElement remedialActionSchedule = addRdfElement("RemedialActionSchedule", Namespace.NC, remedialActionScheduleMRid)
            .addAttribute("IdentifiedObject.mRID", Namespace.CIM, remedialActionScheduleMRid)
            .addResource("RemedialActionSchedule.statusKind", Namespace.NC, Namespace.NC.getURI() + "RemedialActionScheduleStatusKind.proposed")
            .addResource("RemedialActionSchedule.RemedialAction", Namespace.NC, XmlHelper.getMRidReference(remedialAction.getId()));
        if (state.getContingency().isPresent()) {
            remedialActionSchedule.addResource("RemedialActionSchedule.Contingency", Namespace.NC, XmlHelper.getMRidReference(state.getContingency().orElseThrow().getId()));
        }
    }

    private void addGridStateIntensitySchedule(String gridStateIntensityScheduleMRid, String remedialActionScheduleMRid, String elementaryActionId) {
        addRdfElement("GridStateIntensitySchedule", Namespace.NC, gridStateIntensityScheduleMRid)
            .addAttribute("IdentifiedObject.mRID", Namespace.CIM, gridStateIntensityScheduleMRid)
            .addResource("GridStateIntensitySchedule.valueKind", Namespace.NC, Namespace.NC.getURI() + "ValueOffsetKind.absolute")
            .addResource("GridStateIntensitySchedule.interpolationKind", Namespace.NC, Namespace.NC.getURI() + "TimeSeriesInterpolationKind.none")
            .addResource("GridStateIntensitySchedule.GridStateAlteration", Namespace.NC, XmlHelper.getMRidReference(elementaryActionId))
            .addResource("GenericValueSchedule.RemedialActionSchedule", Namespace.NC, XmlHelper.getMRidReference(remedialActionScheduleMRid));
    }

    private void addGenericValueTimePoint(String genericValueScheduleMRid, Number setPoint, OffsetDateTime timestamp) {
        addRdfElement("GenericValueTimePoint", Namespace.NC, XmlHelper.generateStaticUUID("GenericValueTimePoint", XmlHelper.formatOffsetDateTime(timestamp), String.valueOf(setPoint), genericValueScheduleMRid))
            .addAttribute("GenericValueTimePoint.atTime", Namespace.NC, XmlHelper.formatOffsetDateTime(timestamp))
            .addAttribute("GenericValueTimePoint.value", Namespace.NC, String.valueOf(setPoint))
            .addResource("GenericValueTimePoint.GenericValueSchedule", Namespace.NC, XmlHelper.getMRidReference(genericValueScheduleMRid));
    }

    private static Number getRangeActionSetPoint(RangeAction<?> rangeAction, State state, RaoResult raoResult) {
        if (rangeAction instanceof PstRangeAction pstRangeAction) {
            return raoResult.getOptimizedTapOnState(state, pstRangeAction);
        }
        return raoResult.getOptimizedSetPointOnState(state, rangeAction);
    }

    private static Number getActionSetPoint(Action elementaryAction) {
        return switch (elementaryAction) {
            case SwitchAction switchAction -> switchAction.isOpen() ? 1 : 0;
            case ShuntCompensatorPositionAction shuntCompensatorPositionAction ->
                shuntCompensatorPositionAction.getSectionCount();
            case GeneratorAction generatorAction -> generatorAction.getActivePowerValue().orElseThrow();
            case LoadAction loadAction -> loadAction.getActivePowerValue().orElseThrow();
            case PhaseTapChangerTapPositionAction tapPositionAction -> tapPositionAction.getTapPosition();
            default ->
                throw new OpenRaoException("Unsupported elementary action type %s".formatted(elementaryAction.getClass().getSimpleName()));
        };
    }

    private static String generateGenericValueTimePointMRid(OffsetDateTime timestamp, Number value, String scheduleMRid) {
        return UUID.nameUUIDFromBytes("%S@%s::%s".formatted(scheduleMRid, timestamp.format(DateTimeFormatter.ISO_DATE_TIME), value).getBytes(StandardCharsets.UTF_8)).toString();
    }
}
