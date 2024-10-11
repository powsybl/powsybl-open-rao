package com.powsybl.openrao.data.raoresult.nc;

import com.google.auto.service.AutoService;
import com.powsybl.action.Action;
import com.powsybl.action.GeneratorAction;
import com.powsybl.action.LoadAction;
import com.powsybl.action.PhaseTapChangerTapPositionAction;
import com.powsybl.action.ShuntCompensatorPositionAction;
import com.powsybl.action.SwitchAction;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracCreationContext;
import com.powsybl.openrao.data.cracapi.Instant;
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
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

@AutoService(Exporter.class)
public class NcExporter implements Exporter {
    @Override
    public String getFormat() {
        return "NC";
    }

    @Override
    public void exportData(RaoResult raoResult, CracCreationContext cracCreationContext, Properties properties, OutputStream outputStream) {
        if (cracCreationContext instanceof CsaProfileCracCreationContext ncCracCreationContext) {
            Document document = initXmlDocument();
            OffsetDateTime timeStamp = ncCracCreationContext.getTimeStamp();

            Element rootRdfElement = createRootRdfElement(document);
            rootRdfElement.appendChild(writeProfileHeader(document, timeStamp));

            writeRemedialActionScheduleProfileContent(raoResult, ncCracCreationContext, document, timeStamp, rootRdfElement, ncCracCreationContext.getInstantApplicationTimeMap());

            writeOutputXmlFile(document, outputStream);
        } else {
            throw new OpenRaoException("CRAC Creation Context is not NC-compliant.");
        }
    }

    @Override
    public void exportData(RaoResult raoResult, Crac crac, Properties properties, OutputStream outputStream) {
        throw new NotImplementedException("CracCreationContext is required for NC export.");
    }

    private static String formatOffsetDateTime(OffsetDateTime offsetDateTime) {
        return offsetDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
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

    private static Element writeProfileHeader(Document document, OffsetDateTime timeStamp) {
        Element header = document.createElement("md:FullModel");

        Element startDate = document.createElement("dcat:startDate");
        startDate.setTextContent(formatOffsetDateTime(timeStamp));
        header.appendChild(startDate);

        Element endDate = document.createElement("dcat:endDate");
        endDate.setTextContent(formatOffsetDateTime(timeStamp.plusHours(1)));
        header.appendChild(endDate);

        Element keyword = document.createElement("dcat:keyword");
        keyword.setTextContent("RAS");
        header.appendChild(keyword);

        return header;
    }

    private static void writeRemedialActionScheduleProfileContent(RaoResult raoResult, CsaProfileCracCreationContext ncCracCreationContext, Document document, OffsetDateTime timeStamp, Element rootRdfElement, Map<Instant, Integer> instantApplicationTimeMap) {
        ncCracCreationContext.getCrac().getStates().forEach(
            state -> {
                raoResult.getActivatedRangeActionsDuringState(state).forEach(rangeAction -> writeRemedialActionResult(document, rootRdfElement, rangeAction, state, raoResult, timeStamp.plusSeconds(instantApplicationTimeMap.get(state.getInstant()))));
                raoResult.getActivatedNetworkActionsDuringState(state).forEach(networkAction -> writeRemedialActionResult(document, rootRdfElement, networkAction, state, raoResult, timeStamp.plusSeconds(instantApplicationTimeMap.get(state.getInstant()))));
            }
        );
    }

    private static void writeRemedialActionResult(Document document, Element rootRdfElement, RemedialAction<?> remedialAction, State state, RaoResult raoResult, OffsetDateTime timeStamp) {
        // Step 1: Create RemedialActionSchedule to indicate the application state of the remedial action
        String remedialActionScheduleMRid = generateRemedialActionScheduleMRid(remedialAction, state);
        Element remedialActionScheduleElement = writeRemedialActionScheduleElement(document, remedialActionScheduleMRid, remedialAction, state);
        rootRdfElement.appendChild(remedialActionScheduleElement);

        // Step 2: For each elementary action, create a GridStateIntensitySchedule and a GenericValueTimePoint to indicate the optimal set-point
        if (remedialAction instanceof RangeAction<?> rangeAction) {
            // no elementary action -> use remedial action directly
            String gridStateIntensityScheduleMRid = generateGridStateIntensityScheduleMRid(rangeAction.getId(), state);
            rootRdfElement.appendChild(writeGridStateIntensitySchedule(document, gridStateIntensityScheduleMRid, remedialActionScheduleMRid, rangeAction.getId()));
            rootRdfElement.appendChild(writeGenericValueTimePoint(document, gridStateIntensityScheduleMRid, getRangeActionSetPoint(rangeAction, state, raoResult), timeStamp));
        } else if (remedialAction instanceof NetworkAction networkAction) {
            for (Action elementaryAction : networkAction.getElementaryActions()) {
                String gridStateIntensityScheduleMRid = generateGridStateIntensityScheduleMRid(elementaryAction.getId(), state);
                rootRdfElement.appendChild(writeGridStateIntensitySchedule(document, gridStateIntensityScheduleMRid, remedialActionScheduleMRid, elementaryAction.getId()));
                rootRdfElement.appendChild(writeGenericValueTimePoint(document, gridStateIntensityScheduleMRid, getActionSetPoint(elementaryAction), timeStamp));
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
        remedialActionScheduleElement.setAttribute("rdf:ID", getMRidReference(remedialActionScheduleMRid));

        Element mRidElement = document.createElement("cim:IdentifiedObject.mRID");
        mRidElement.setTextContent(remedialActionScheduleMRid);
        remedialActionScheduleElement.appendChild(mRidElement);

        Element statusKindElement = document.createElement("nc:RemedialActionSchedule.statusKind");
        setRdfResourceReference(statusKindElement, Namespace.NC.getUri() + "RemedialActionScheduleStatusKind.proposed");
        remedialActionScheduleElement.appendChild(statusKindElement);

        Element remedialActionElement = document.createElement("nc:RemedialActionSchedule.RemedialAction");
        setRdfResourceReference(remedialActionElement, getMRidReference(remedialAction.getId()));
        remedialActionScheduleElement.appendChild(remedialActionElement);

        state.getContingency().ifPresent(contingency -> {
            Element contingencyElement = document.createElement("nc:RemedialActionSchedule.Contingency");
            setRdfResourceReference(contingencyElement, getMRidReference(contingency.getId()));
            remedialActionScheduleElement.appendChild(contingencyElement);
        });

        return remedialActionScheduleElement;
    }

    private static Element writeGridStateIntensitySchedule(Document document, String gridStateIntensityScheduleMRid, String remedialActionScheduleMRid, String elementaryActionId) {
        Element gridStateIntensityScheduleElement = document.createElement("nc:GridStateIntensitySchedule");
        gridStateIntensityScheduleElement.setAttribute("rdf:ID", getMRidReference(gridStateIntensityScheduleMRid));

        Element valueKindElement = document.createElement("nc:GridStateIntensitySchedule.valueKind");
        setRdfResourceReference(valueKindElement, Namespace.NC.getUri() + "ValueOffsetKind.absolute");
        gridStateIntensityScheduleElement.appendChild(valueKindElement);

        Element interpolationKindElement = document.createElement("nc:BaseTimeSeries.interpolationKind");
        setRdfResourceReference(interpolationKindElement, Namespace.NC.getUri() + "TimeSeriesInterpolationKind.none");
        gridStateIntensityScheduleElement.appendChild(interpolationKindElement);

        Element mRidElement = document.createElement("cim:IdentifiedObject.mRID");
        mRidElement.setTextContent(gridStateIntensityScheduleMRid);
        gridStateIntensityScheduleElement.appendChild(mRidElement);

        Element gridStateAlterationElement = document.createElement("nc:GridStateIntensitySchedule.GridStateAlteration");
        setRdfResourceReference(gridStateAlterationElement, getMRidReference(elementaryActionId));
        gridStateIntensityScheduleElement.appendChild(gridStateAlterationElement);

        Element remedialActionScheduleElement = document.createElement("nc:GenericValueSchedule.RemedialActionSchedule");
        setRdfResourceReference(remedialActionScheduleElement, getMRidReference(remedialActionScheduleMRid));
        gridStateIntensityScheduleElement.appendChild(remedialActionScheduleElement);

        return gridStateIntensityScheduleElement;
    }

    private static Element writeGenericValueTimePoint(Document document, String genericValueScheduleMRid, Number setPoint, OffsetDateTime timeStamp) {
        Element genericValueTimePointElement = document.createElement("nc:GenericValueTimePoint");

        // TODO: for CRAs, use curative instant post-outage time as an offset to identify curative batch
        // TODO: add curative instants post-outage time in CRAC Creation Context
        Element atTimeElement = document.createElement("nc:GenericValueTimePoint.atTime");
        atTimeElement.setTextContent(formatOffsetDateTime(timeStamp));
        genericValueTimePointElement.appendChild(atTimeElement);

        Element valueElement = document.createElement("nc:GenericValueTimePoint.value");
        valueElement.setTextContent(String.valueOf(setPoint));
        genericValueTimePointElement.appendChild(valueElement);

        Element genericValueSchedule = document.createElement("nc:GenericValueTimePoint.GenericValueSchedule");
        setRdfResourceReference(genericValueSchedule, getMRidReference(genericValueScheduleMRid));
        genericValueTimePointElement.appendChild(genericValueSchedule);

        return genericValueTimePointElement;
    }

    private static String generateRemedialActionScheduleMRid(RemedialAction<?> remedialAction, State state) {
        return UUID.nameUUIDFromBytes("%s@%s".formatted(remedialAction.getId(), state.getId()).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static String generateGridStateIntensityScheduleMRid(String elementaryActionId, State state) {
        return UUID.nameUUIDFromBytes("%s@%s::set-point".formatted(elementaryActionId, state.getId()).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static void setRdfResourceReference(Element element, String reference) {
        element.setAttribute("rdf:resource", reference);
    }

    private static String getMRidReference(String mRid) {
        return "#_%s".formatted(mRid);
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
