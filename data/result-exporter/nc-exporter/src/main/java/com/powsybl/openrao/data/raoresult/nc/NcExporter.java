package com.powsybl.openrao.data.raoresult.nc;

import com.google.auto.service.AutoService;
import com.powsybl.action.Action;
import com.powsybl.action.GeneratorAction;
import com.powsybl.action.LoadAction;
import com.powsybl.action.ShuntCompensatorPositionAction;
import com.powsybl.action.SwitchAction;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracCreationContext;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.data.raoresultapi.io.Exporter;
import org.apache.commons.lang3.NotImplementedException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;

@AutoService(Exporter.class)
public class NcExporter implements Exporter {
    private static final String RDF_RESOURCE = "rdf:resource";
    private static final String REMEDIAL_ACTION_SCHEDULE_PROFILE_KEYWORD = "RAS";
    private static final String REMEDIAL_ACTION_SCHEDULE = "nc:RemedialActionSchedule";
    private static final String PROPOSED_REMEDIAL_ACTION_SCHEDULE_STATUS_KIND = "RemedialActionScheduleStatusKind.proposed";

    @Override
    public String getFormat() {
        return "NC";
    }

    @Override
    public void exportData(RaoResult raoResult, CracCreationContext cracCreationContext, Properties properties, OutputStream outputStream) {
        if (cracCreationContext instanceof CsaProfileCracCreationContext ncCracCreationContext) {
            Document document = initXmlDocument();
            String timeStamp = ncCracCreationContext.getTimeStamp().format(DateTimeFormatter.ISO_DATE_TIME);

            Element rootRdfElement = createRootRdfElement(document);
            rootRdfElement.appendChild(writeProfileHeader(document, timeStamp));

            ncCracCreationContext.getCrac().getStates().forEach(
                state -> {
                    raoResult.getActivatedRangeActionsDuringState(state).forEach(rangeAction -> writeRemedialActionResult(document, rootRdfElement, rangeAction, state, raoResult, timeStamp));
                    raoResult.getActivatedNetworkActionsDuringState(state).forEach(networkAction -> writeRemedialActionResult(document, rootRdfElement, networkAction, state, raoResult, timeStamp));
                }
            );

            writeOutputXmlFile(document, outputStream);
        } else {
            throw new OpenRaoException("CRAC Creation Context is not NC-compliant.");
        }
    }

    @Override
    public void exportData(RaoResult raoResult, Crac crac, Properties properties, OutputStream outputStream) {
        throw new NotImplementedException("CracCreationContext is required for NC export.");
    }

    private static Document initXmlDocument() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            return docBuilder.newDocument();
        } catch (ParserConfigurationException e) {
            throw new OpenRaoException("Could not initialize output XML file. Reason: %s".formatted(e.getMessage()));
        }
    }

    private static Element createRootRdfElement(Document document) {
        Element rootRdfElement = document.createElementNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf:RDF");
        Arrays.stream(Namespace.values()).forEach(namespace -> rootRdfElement.setAttribute("xmlns:%s".formatted(namespace.getKeyword()), namespace.getUri()));
        document.appendChild(rootRdfElement);
        return rootRdfElement;
    }

    private Element writeProfileHeader(Document document, String timeStamp) {
        Element header = document.createElement("md:FullModel");

        Element startDate = document.createElement("dcat:startDate");
        startDate.setTextContent(timeStamp);
        header.appendChild(startDate);

        Element endDate = document.createElement("dcat:endDate");
        endDate.setTextContent(timeStamp);
        header.appendChild(endDate);

        Element keyword = document.createElement("dcat:keyword");
        keyword.setTextContent(REMEDIAL_ACTION_SCHEDULE_PROFILE_KEYWORD);
        header.appendChild(keyword);

        return header;
    }

    private void writeRemedialActionResult(Document document, Element rootRdfElement, RemedialAction<?> remedialAction, State state, RaoResult raoResult, String timeStamp) {
        // Step 1: Create RemedialActionSchedule to indicate the application state of the remedial action
        String remedialActionScheduleMRid = generateRemedialActionScheduleMRid(remedialAction, state);
        Element remedialActionScheduleElement = writeRemedialActionScheduleElement(document, remedialActionScheduleMRid, remedialAction, state);
        rootRdfElement.appendChild(remedialActionScheduleElement);

        // Step 2: For each elementary action, create a GridStateIntensitySchedule and a GenericValueTimePoint to indicate the optimal set-point
        if (remedialAction instanceof RangeAction<?> rangeAction) {
            // no elementary action -> use remedial action directly
            String gridStateIntensityScheduleMRid = generateGridStateIntensityScheduleMRid(rangeAction.getId(), state);
            rootRdfElement.appendChild(writeGridStateIntensitySchedule(document, gridStateIntensityScheduleMRid, remedialActionScheduleMRid, rangeAction.getId()));
            rootRdfElement.appendChild(writeGenericValueTimePoint(document, gridStateIntensityScheduleMRid, rangeAction instanceof PstRangeAction pstRangeAction ? raoResult.getOptimizedTapOnState(state, pstRangeAction) : raoResult.getOptimizedSetPointOnState(state, rangeAction), rangeAction instanceof PstRangeAction, timeStamp));
        } else if (remedialAction instanceof NetworkAction networkAction) {
            for (Action elementaryAction : networkAction.getElementaryActions()) {
                String gridStateIntensityScheduleMRid = generateGridStateIntensityScheduleMRid(elementaryAction.getId(), state);
                rootRdfElement.appendChild(writeGridStateIntensitySchedule(document, gridStateIntensityScheduleMRid, remedialActionScheduleMRid, elementaryAction.getId()));
                rootRdfElement.appendChild(writeGenericValueTimePoint(document, gridStateIntensityScheduleMRid, getActionSetpoint(elementaryAction), elementaryAction instanceof SwitchAction || elementaryAction instanceof ShuntCompensatorPositionAction, timeStamp));
            }
        }
    }

    private static double getActionSetpoint(Action elementaryAction) {
        double setPoint;
        if (elementaryAction instanceof SwitchAction switchAction) {
            setPoint = switchAction.isOpen() ? 1 : 0;
        } else if (elementaryAction instanceof ShuntCompensatorPositionAction shuntCompensatorPositionAction) {
            setPoint = shuntCompensatorPositionAction.getSectionCount();
        } else if (elementaryAction instanceof GeneratorAction generatorAction) {
            setPoint = generatorAction.getActivePowerValue().orElseThrow();
        } else if (elementaryAction instanceof LoadAction loadAction) {
            setPoint = loadAction.getActivePowerValue().orElseThrow();
        } else {
            throw new OpenRaoException("Unsupported elementary action type %s".formatted(elementaryAction.getClass().getSimpleName()));
        }
        return setPoint;
    }

    private Element writeRemedialActionScheduleElement(Document document, String remedialActionScheduleMRid, RemedialAction<?> remedialAction, State state) {
        Element remedialActionScheduleElement = document.createElement(REMEDIAL_ACTION_SCHEDULE);
        remedialActionScheduleElement.setAttribute("rdf:ID", "#_%s".formatted(remedialActionScheduleMRid));

        Element mRidElement = document.createElement("cim:IdentifiedObject.mRID");
        mRidElement.setTextContent(remedialActionScheduleMRid);
        remedialActionScheduleElement.appendChild(mRidElement);

        Element statusKindElement = document.createElement("nc:RemedialActionSchedule.statusKind");
        statusKindElement.setAttribute(RDF_RESOURCE, Namespace.NC.getUri() + PROPOSED_REMEDIAL_ACTION_SCHEDULE_STATUS_KIND);
        remedialActionScheduleElement.appendChild(statusKindElement);

        Element remedialActionElement = document.createElement("nc:RemedialActionSchedule.RemedialAction");
        remedialActionElement.setAttribute(RDF_RESOURCE, "#_%s".formatted(remedialAction.getId()));
        remedialActionScheduleElement.appendChild(remedialActionElement);

        state.getContingency().ifPresent(contingency -> {
            Element contingencyElement = document.createElement("nc:RemedialActionSchedule.Contingency");
            contingencyElement.setAttribute(RDF_RESOURCE, "#_%s".formatted(contingency.getId()));
            remedialActionScheduleElement.appendChild(contingencyElement);
        });

        return remedialActionScheduleElement;
    }

    private Element writeGridStateIntensitySchedule(Document document, String gridStateIntensityScheduleMRid, String remedialActionScheduleMRid, String elementaryActionId) {
        Element gridStateIntensityScheduleElement = document.createElement("nc:GridStateIntensitySchedule");
        gridStateIntensityScheduleElement.setAttribute("rdf:ID", "#_%s".formatted(gridStateIntensityScheduleMRid));

        Element valueKindElement = document.createElement("nc:GridStateIntensitySchedule.valueKind");
        valueKindElement.setAttribute(RDF_RESOURCE, Namespace.NC.getUri() + "ValueOffsetKind.absolute");
        gridStateIntensityScheduleElement.appendChild(valueKindElement);

        Element interpolationKindElement = document.createElement("nc:BaseTimeSeries.interpolationKind");
        interpolationKindElement.setAttribute(RDF_RESOURCE, Namespace.NC.getUri() + "TimeSeriesInterpolationKind.none");
        gridStateIntensityScheduleElement.appendChild(interpolationKindElement);

        Element mRidElement = document.createElement("cim:IdentifiedObject.mRID");
        mRidElement.setTextContent(gridStateIntensityScheduleMRid);
        gridStateIntensityScheduleElement.appendChild(mRidElement);

        Element gridStateAlterationElement = document.createElement("nc:GridStateIntensitySchedule.GridStateAlteration");
        gridStateAlterationElement.setAttribute(RDF_RESOURCE, "#_%s".formatted(elementaryActionId));
        gridStateIntensityScheduleElement.appendChild(gridStateAlterationElement);

        Element remedialActionScheduleElement = document.createElement("nc:GenericValueSchedule.RemedialActionSchedule");
        remedialActionScheduleElement.setAttribute(RDF_RESOURCE, "#_%s".formatted(remedialActionScheduleMRid));
        gridStateIntensityScheduleElement.appendChild(remedialActionScheduleElement);

        return gridStateIntensityScheduleElement;
    }

    private Element writeGenericValueTimePoint(Document document, String genericValueScheduleMRid, double setPoint, boolean setPointAsInt, String timeStamp) {
        Element genericValueTimePointElement = document.createElement("nc:GenericValueTimePoint");

        // TODO: for CRAs, use curative instant post-outage time as an offset to identify curative batch
        // TODO: add curative instants post-outage time in CRAC Creation Context
        Element atTimeElement = document.createElement("nc:GenericValueTimePoint.atTime");
        atTimeElement.setTextContent(timeStamp);
        genericValueTimePointElement.appendChild(atTimeElement);

        Element valueElement = document.createElement("nc:GenericValueTimePoint.value");
        valueElement.setTextContent(setPointAsInt ? String.valueOf((int) setPoint) : String.valueOf(setPoint));
        genericValueTimePointElement.appendChild(valueElement);

        Element genericValueSchedule = document.createElement("nc:GenericValueTimePoint.GenericValueSchedule");
        genericValueSchedule.setAttribute(RDF_RESOURCE, "#_%s".formatted(genericValueScheduleMRid));
        genericValueTimePointElement.appendChild(genericValueSchedule);

        return genericValueTimePointElement;
    }

    private static String generateRemedialActionScheduleMRid(RemedialAction<?> remedialAction, State state) {
        return UUID.nameUUIDFromBytes("%s@%s".formatted(remedialAction.getId(), state.getId()).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static String generateGridStateIntensityScheduleMRid(String elementaryActionId, State state) {
        return UUID.nameUUIDFromBytes("%s@%s::set-point".formatted(elementaryActionId, state.getId()).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static void writeOutputXmlFile(Document document, OutputStream outputStream) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(document), new StreamResult(outputStream));
        } catch (TransformerException e) {
            throw new OpenRaoException("Could not write output XML file. Reason: %s".formatted(e.getMessage()));
        }
    }
}
