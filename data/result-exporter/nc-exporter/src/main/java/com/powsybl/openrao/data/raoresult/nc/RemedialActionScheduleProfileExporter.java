package com.powsybl.openrao.data.raoresult.nc;

import com.powsybl.action.Action;
import com.powsybl.action.GeneratorAction;
import com.powsybl.action.LoadAction;
import com.powsybl.action.PhaseTapChangerTapPositionAction;
import com.powsybl.action.ShuntCompensatorPositionAction;
import com.powsybl.action.SwitchAction;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public class RemedialActionScheduleProfileExporter implements NcProfileWriter {
    @Override
    public String getKeyword() {
        return "RAS";
    }

    @Override
    public void addProfileContent(Document document, Element rootRdfElement, RaoResult raoResult, CsaProfileCracCreationContext ncCracCreationContext) {
        ncCracCreationContext.getCrac().getStates().forEach(
            state -> {
                raoResult.getActivatedRangeActionsDuringState(state).forEach(rangeAction -> writeRemedialActionResult(document, rootRdfElement, rangeAction, state, raoResult, ncCracCreationContext));
                raoResult.getActivatedNetworkActionsDuringState(state).forEach(networkAction -> writeRemedialActionResult(document, rootRdfElement, networkAction, state, raoResult, ncCracCreationContext));
            }
        );
    }

    private static void writeRemedialActionResult(Document document, Element rootRdfElement, RemedialAction<?> remedialAction, State state, RaoResult raoResult, CsaProfileCracCreationContext ncCracCreationContext) {
        // Step 1: Create RemedialActionSchedule to indicate the application state of the remedial action
        String remedialActionScheduleMRid = generateRemedialActionScheduleMRid(remedialAction, state);
        Element remedialActionScheduleElement = writeRemedialActionScheduleElement(document, remedialActionScheduleMRid, remedialAction, state);
        rootRdfElement.appendChild(remedialActionScheduleElement);

        // Step 2: For each elementary action, create a GridStateIntensitySchedule and a GenericValueTimePoint to indicate the optimal set-point
        if (remedialAction instanceof RangeAction<?> rangeAction) {
            // no elementary action -> use remedial action directly
            String gridStateIntensityScheduleMRid = generateGridStateIntensityScheduleMRid(rangeAction.getId(), state);
            rootRdfElement.appendChild(writeGridStateIntensitySchedule(document, gridStateIntensityScheduleMRid, remedialActionScheduleMRid, rangeAction.getId()));
            rootRdfElement.appendChild(writeGenericValueTimePoint(document, gridStateIntensityScheduleMRid, state, getRangeActionSetPoint(rangeAction, state, raoResult), ncCracCreationContext));
        } else if (remedialAction instanceof NetworkAction networkAction) {
            for (Action elementaryAction : networkAction.getElementaryActions()) {
                String gridStateIntensityScheduleMRid = generateGridStateIntensityScheduleMRid(elementaryAction.getId(), state);
                rootRdfElement.appendChild(writeGridStateIntensitySchedule(document, gridStateIntensityScheduleMRid, remedialActionScheduleMRid, elementaryAction.getId()));
                rootRdfElement.appendChild(writeGenericValueTimePoint(document, gridStateIntensityScheduleMRid, state, getActionSetPoint(elementaryAction), ncCracCreationContext));
            }
        }
    }

    private static Number getRangeActionSetPoint(RangeAction<?> rangeAction, State state, RaoResult raoResult) {
        if (rangeAction instanceof PstRangeAction pstRangeAction) {
            return raoResult.getOptimizedTapOnState(state, pstRangeAction);
        }
        return raoResult.getOptimizedSetPointOnState(state, rangeAction);
    }

    private static Number getActionSetPoint(Action elementaryAction) {
        if (elementaryAction instanceof SwitchAction switchAction) {
            return switchAction.isOpen() ? 1 : 0;
        } else if (elementaryAction instanceof ShuntCompensatorPositionAction shuntCompensatorPositionAction) {
            return shuntCompensatorPositionAction.getSectionCount();
        } else if (elementaryAction instanceof GeneratorAction generatorAction) {
            return generatorAction.getActivePowerValue().orElseThrow();
        } else if (elementaryAction instanceof LoadAction loadAction) {
            return loadAction.getActivePowerValue().orElseThrow();
        } else if (elementaryAction instanceof PhaseTapChangerTapPositionAction tapPositionAction) {
            return tapPositionAction.getTapPosition();
        } else {
            throw new OpenRaoException("Unsupported elementary action type %s".formatted(elementaryAction.getClass().getSimpleName()));
        }
    }

    private static Element writeRemedialActionScheduleElement(Document document, String remedialActionScheduleMRid, RemedialAction<?> remedialAction, State state) {
        Element remedialActionScheduleElement = document.createElement("nc:RemedialActionSchedule");
        remedialActionScheduleElement.setAttribute("rdf:ID", NcProfileWriter.getMRidReference(remedialActionScheduleMRid));

        Element mRidElement = document.createElement("cim:IdentifiedObject.mRID");
        mRidElement.setTextContent(remedialActionScheduleMRid);
        remedialActionScheduleElement.appendChild(mRidElement);

        Element statusKindElement = document.createElement("nc:RemedialActionSchedule.statusKind");
        NcProfileWriter.setRdfResourceReference(statusKindElement, Namespace.NC.getUri() + "RemedialActionScheduleStatusKind.proposed");
        remedialActionScheduleElement.appendChild(statusKindElement);

        Element remedialActionElement = document.createElement("nc:RemedialActionSchedule.RemedialAction");
        NcProfileWriter.setRdfResourceReference(remedialActionElement, NcProfileWriter.getMRidReference(remedialAction.getId()));
        remedialActionScheduleElement.appendChild(remedialActionElement);

        state.getContingency().ifPresent(contingency -> {
            Element contingencyElement = document.createElement("nc:RemedialActionSchedule.Contingency");
            NcProfileWriter.setRdfResourceReference(contingencyElement, NcProfileWriter.getMRidReference(contingency.getId()));
            remedialActionScheduleElement.appendChild(contingencyElement);
        });

        return remedialActionScheduleElement;
    }

    private static Element writeGridStateIntensitySchedule(Document document, String gridStateIntensityScheduleMRid, String remedialActionScheduleMRid, String elementaryActionId) {
        Element gridStateIntensityScheduleElement = document.createElement("nc:GridStateIntensitySchedule");
        gridStateIntensityScheduleElement.setAttribute("rdf:ID", NcProfileWriter.getMRidReference(gridStateIntensityScheduleMRid));

        Element valueKindElement = document.createElement("nc:GridStateIntensitySchedule.valueKind");
        NcProfileWriter.setRdfResourceReference(valueKindElement, Namespace.NC.getUri() + "ValueOffsetKind.absolute");
        gridStateIntensityScheduleElement.appendChild(valueKindElement);

        Element interpolationKindElement = document.createElement("nc:BaseTimeSeries.interpolationKind");
        NcProfileWriter.setRdfResourceReference(interpolationKindElement, Namespace.NC.getUri() + "TimeSeriesInterpolationKind.none");
        gridStateIntensityScheduleElement.appendChild(interpolationKindElement);

        Element mRidElement = document.createElement("cim:IdentifiedObject.mRID");
        mRidElement.setTextContent(gridStateIntensityScheduleMRid);
        gridStateIntensityScheduleElement.appendChild(mRidElement);

        Element gridStateAlterationElement = document.createElement("nc:GridStateIntensitySchedule.GridStateAlteration");
        NcProfileWriter.setRdfResourceReference(gridStateAlterationElement, NcProfileWriter.getMRidReference(elementaryActionId));
        gridStateIntensityScheduleElement.appendChild(gridStateAlterationElement);

        Element remedialActionScheduleElement = document.createElement("nc:GenericValueSchedule.RemedialActionSchedule");
        NcProfileWriter.setRdfResourceReference(remedialActionScheduleElement, NcProfileWriter.getMRidReference(remedialActionScheduleMRid));
        gridStateIntensityScheduleElement.appendChild(remedialActionScheduleElement);

        return gridStateIntensityScheduleElement;
    }

    private static Element writeGenericValueTimePoint(Document document, String genericValueScheduleMRid, State state, Number setPoint, CsaProfileCracCreationContext ncCracCreationContext) {
        Element genericValueTimePointElement = document.createElement("nc:GenericValueTimePoint");

        Element atTimeElement = document.createElement("nc:GenericValueTimePoint.atTime");
        atTimeElement.setTextContent(NcProfileWriter.formatOffsetDateTime(getRemedialActionApplicationTimeStamp(ncCracCreationContext.getTimeStamp(), ncCracCreationContext.getInstantApplicationTimeMap(), state)));
        genericValueTimePointElement.appendChild(atTimeElement);

        Element valueElement = document.createElement("nc:GenericValueTimePoint.value");
        valueElement.setTextContent(String.valueOf(setPoint));
        genericValueTimePointElement.appendChild(valueElement);

        Element genericValueSchedule = document.createElement("nc:GenericValueTimePoint.GenericValueSchedule");
        NcProfileWriter.setRdfResourceReference(genericValueSchedule, NcProfileWriter.getMRidReference(genericValueScheduleMRid));
        genericValueTimePointElement.appendChild(genericValueSchedule);

        return genericValueTimePointElement;
    }

    private static OffsetDateTime getRemedialActionApplicationTimeStamp(OffsetDateTime timeStamp, Map<Instant, Integer> instantApplicationTimeMap, State state) {
        return timeStamp.plusSeconds(instantApplicationTimeMap.get(state.getInstant()));
    }

    private static String generateRemedialActionScheduleMRid(RemedialAction<?> remedialAction, State state) {
        return UUID.nameUUIDFromBytes("%s@%s".formatted(remedialAction.getId(), state.getId()).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static String generateGridStateIntensityScheduleMRid(String elementaryActionId, State state) {
        return UUID.nameUUIDFromBytes("%s@%s::set-point".formatted(elementaryActionId, state.getId()).getBytes(StandardCharsets.UTF_8)).toString();
    }
}
