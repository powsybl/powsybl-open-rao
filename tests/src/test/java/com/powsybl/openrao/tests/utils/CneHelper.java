/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.tests.utils;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.corecneexporter.CoreCneExporter;
import com.powsybl.openrao.data.cracapi.CracCreationContext;
import com.powsybl.openrao.data.cracio.cim.craccreator.CimCracCreationContext;
import com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext.UcteCracCreationContext;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.data.swecneexporter.SweCneExporter;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Helper class for exporting and comparing CNE files
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class CneHelper {

    private static final double DEFAULT_DOUBLE_TOLERANCE = 1.1;
    private static final Map<String, Double> SPECIFIC_DOUBLE_TOLERANCE = Map.of("Z11", 1e-6);

    public enum CneVersion {
        CORE,
        SWE
    }

    private CneHelper() {
        // should not be instantiated
    }

    public static String exportCoreCne(CracCreationContext cracCreationContext, RaoResult raoResult, RaoParameters raoParameters) {
        if (!(cracCreationContext instanceof UcteCracCreationContext)) {
            throw new OpenRaoException("CORE CNE export can only handle UcteCracCreationContext");
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Properties properties = new Properties();
        fillPropertiesWithCoreCneExporterParameters(properties);
        fillPropertiesWithRaoParameters(properties, raoParameters);
        raoResult.write("CORE-CNE", cracCreationContext, properties, outputStream);
        return outputStream.toString();
    }

    private static void fillPropertiesWithCoreCneExporterParameters(Properties properties) {
        properties.setProperty("rao-result.export.core-cne.document-id", "22XCORESO------S-20211115-F299v1");
        properties.setProperty("rao-result.export.core-cne.revision-number", "1");
        properties.setProperty("rao-result.export.core-cne.domain-id", "10YDOM-REGION-1V");
        properties.setProperty("rao-result.export.core-cne.process-type", "A48");
        properties.setProperty("rao-result.export.core-cne.sender-id", "22XCORESO------S");
        properties.setProperty("rao-result.export.core-cne.sender-role", "A44");
        properties.setProperty("rao-result.export.core-cne.receiver-id", "17XTSO-CS------W");
        properties.setProperty("rao-result.export.core-cne.receiver-role", "A36");
        properties.setProperty("rao-result.export.core-cne.time-interval", "2021-10-30T22:00Z/2021-10-31T23:00Z");
    }

    private static void fillPropertiesWithRaoParameters(Properties properties, RaoParameters raoParameters) {
        switch (raoParameters.getObjectiveFunctionParameters().getType()) {
            case MAX_MIN_RELATIVE_MARGIN -> properties.setProperty("rao-result.export.core-cne.relative-positive-margins", "true");
            case MAX_MIN_MARGIN -> properties.setProperty("rao-result.export.core-cne.relative-positive-margins", "false");
        }
        if (raoParameters.getLoopFlowParameters().isPresent()) {
            properties.setProperty("rao-result.export.core-cne.with-loop-flows", "true");
        }
        if (raoParameters.getMnecParameters().isPresent()) {
            properties.setProperty("rao-result.export.core-cne.mnec-acceptable-margin-diminution", String.valueOf(raoParameters.getMnecParameters().get().getAcceptableMarginDecrease()));
        }
    }

    public static String exportSweCne(CracCreationContext cracCreationContext, RaoResult raoResult) {
        if (!(cracCreationContext instanceof CimCracCreationContext)) {
            throw new OpenRaoException("SWE CNE export can only handle CimCracCreationContext");
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Properties properties = new Properties();
        fillPropertiesWithSweCneExporterParameters(properties);
        raoResult.write("SWE-CNE", cracCreationContext, properties, outputStream);
        return outputStream.toString();
    }

    private static void fillPropertiesWithSweCneExporterParameters(Properties properties) {
        properties.setProperty("rao-result.export.swe-cne.document-id", "documentId");
        properties.setProperty("rao-result.export.swe-cne.revision-number", "3");
        properties.setProperty("rao-result.export.swe-cne.domain-id", "domainId");
        properties.setProperty("rao-result.export.swe-cne.process-type", "A48");
        properties.setProperty("rao-result.export.swe-cne.sender-id", "senderId");
        properties.setProperty("rao-result.export.swe-cne.sender-role", "A44");
        properties.setProperty("rao-result.export.swe-cne.receiver-id", "receiverId");
        properties.setProperty("rao-result.export.swe-cne.receiver-role", "A36");
        properties.setProperty("rao-result.export.swe-cne.time-interval", "2021-04-02T12:00:00Z/2021-04-02T13:00:00Z");
    }

    public static boolean isCoreCneValid(String cneContent) {
        return CoreCneExporter.validateCNESchema(cneContent);
    }

    public static boolean isSweCneValid(String cneContent) {
        return SweCneExporter.validateCNESchema(cneContent);
    }

    /**
     * Function to compare CNE files
     * @param expectedCneInputStream: input stream with the expected contents of the CNE file
     * @param actualCneInputStream: input stream with the actual contents of the CNE file
     * @param onlySimilarity: set to true to check only for similarity, ie ignore nodes' order in the document
     * @param cneVersion: CNE version, to apply specific compare rules
     * @throws AssertionError: if the files are not similar, with a list of the differences
     */
    public static void compareCneFiles(InputStream expectedCneInputStream, InputStream actualCneInputStream, boolean onlySimilarity, CneVersion cneVersion) throws AssertionError {
        DiffBuilder db = DiffBuilder
                .compare(Input.fromStream(expectedCneInputStream))
                .withTest(Input.fromStream(actualCneInputStream))
                .ignoreComments()
                .withDifferenceEvaluator(new DoubleElementDifferenceEvaluator("analogValues.value", DEFAULT_DOUBLE_TOLERANCE, SPECIFIC_DOUBLE_TOLERANCE));
        if (onlySimilarity) {
            db.checkForSimilar().withNodeMatcher(new DefaultNodeMatcher(new CneDocumentElementSelector()));
        }
        switch (cneVersion) {
            case CORE:
                db.withNodeFilter(CneHelper::shouldCompareNodeForCore);
                break;
            case SWE:
                db.withNodeFilter(CneHelper::shouldCompareNodeForSwe);
                break;
            default:
                throw new OpenRaoException(String.format("Unknown CNE version %s", cneVersion));
        }
        Diff d = db.build();

        if (d.hasDifferences()) {
            DefaultComparisonFormatter formatter = new DefaultComparisonFormatter();
            StringBuffer buffer = new StringBuffer();
            for (Difference ds : d.getDifferences()) {
                buffer.append(formatter.getDescription(ds.getComparison()) + "\n");
            }
            throw new AssertionError("There are XML differences in CNE files\n" + buffer.toString());
        }
        assertFalse(d.hasDifferences());
    }

    /**
     * This class tells XMLUnit which nodes it should compare in the CNE file, and which nodes it should ignore
     * For example, XMLUnit should ignore nodes with random content, and nodes containing the computation timestamp
     * This applies for CORE
     */
    private static boolean shouldCompareNodeForCore(Node node) {
        if (node.getNodeName().equals("mRID")) {
            // For the following fields, mRID is generated randomly as per the CNE specifications
            // We should not compare them with the test file
            return !node.getParentNode().getNodeName().equals("TimeSeries")
                && (!node.getParentNode().getNodeName().equals("Constraint_Series") || !getChildElementValue(node.getParentNode(), "businessType").equals("B56"));
        } else {
            return !(node.getNodeName().equals("createdDateTime"));
        }
    }

    /**
     * This class tells XMLUnit which nodes it should compare in the CNE file, and which nodes it should ignore
     * For example, XMLUnit should ignore nodes with random content, and nodes containing the computation timestamp
     * This applies for SWE
     */
    private static boolean shouldCompareNodeForSwe(Node node) {
        if (node.getNodeName().equals("mRID")) {
            // For the following fields, mRID is generated randomly as per the CNE specifications
            // We should not compare them with the test file
            return !node.getParentNode().getNodeName().equals("Constraint_Series");
        } else {
            return !(node.getNodeName().equals("createdDateTime"));
        }
    }

    /**
     * This class helps XMLUnit select nodes that can be compared, since some nodes do not have name or id attributes
     * It tells XMLUnit how nodes are unique depending on their content, in order for it to be able to ignore the sequence in the XML file
     * It is only used in case of an 'only similarity' comparison
     */
    private static class CneDocumentElementSelector implements ElementSelector {
        ByNameAndTextRecSelector byNameAndTextRecSelector = new ByNameAndTextRecSelector();

        @Override
        public boolean canBeCompared(Element e1, Element e2) {
            if (e1.getNodeName().equals("Constraint_Series") && e2.getNodeName().equals("Constraint_Series")) {
                return canBeComparedConstraintSeries(e1, e2);
            } else if (e1.getNodeName().equals("RemedialAction_Series") && e2.getNodeName().equals("RemedialAction_Series")) {
                return byNameAndTextRecSelector.canBeCompared(e1, e2);
            } else {
                return ElementSelectors.byName.canBeCompared(e1, e2);
            }
        }

        private boolean canBeComparedConstraintSeries(Element e1, Element e2) {
            String type1 = getChildElementValue(e1, "businessType");
            String type2 = getChildElementValue(e2, "businessType");
            if (!type1.equals(type2)) {
                return false;
            }
            switch (type1) {
                case "B54", "B57", "B88":
                    String cnecId1 = getChildElementValue(e1, "Monitored_Series", "RegisteredResource", "mRID");
                    String cnecId2 = getChildElementValue(e2, "Monitored_Series", "RegisteredResource", "mRID");
                    return cnecId1.equals(cnecId2);
                case "B56":
                    // check that the two nodes contain the same remedial actions (no other way)
                    return getB56RemedialActions(e1).equals(getB56RemedialActions(e2));
                default:
                    return true; // should not happen
            }
        }

        private Set<String> getB56RemedialActions(Element e) {
            Set<String> raNames = new HashSet<>();
            NodeList nodeList = e.getElementsByTagName("RemedialAction_Series");
            for (int i = 0; i < nodeList.getLength(); i++) {
                raNames.add(getChildElementValue(nodeList.item(i), "name"));
            }
            return raNames;
        }
    }

    private static String getChildElementValue(Node e, String... childTags) {
        Node currentNode = e;
        for (String childTag : childTags) {
            currentNode = getChildElement(currentNode, childTag);
        }
        return currentNode.getTextContent();
    }

    private static Node getChildElement(Node e, String childTag) {
        for (int i = 0; i < e.getChildNodes().getLength(); i++) {
            if (e.getChildNodes().item(i).getNodeName().equals(childTag)) {
                return e.getChildNodes().item(i);
            }
        }
        throw new IllegalArgumentException("Could not find child element " + childTag);
    }

    /**
     * This class is helpful in order to tolerate some differences in double values (which are rounded and can easily change when the RAO changes slightly)
     */
    private static class DoubleElementDifferenceEvaluator implements DifferenceEvaluator {
        private String elementName;
        private double defaultTolerance;
        private Map<String, Double> specificMeasurementTypeTolerance;

        public DoubleElementDifferenceEvaluator(String elementName, double defaultTolerance, Map<String, Double> specificMeasurementTypeTolerance) {
            this.elementName = elementName;
            this.defaultTolerance = defaultTolerance;
            this.specificMeasurementTypeTolerance = specificMeasurementTypeTolerance;
        }

        @Override
        public ComparisonResult evaluate(Comparison comparison, ComparisonResult outcome) {
            if (outcome == ComparisonResult.EQUAL) {
                return outcome; // only evaluate differences.
            }
            if (comparison.getType() == ComparisonType.CHILD_NODELIST_SEQUENCE) {
                return ComparisonResult.SIMILAR;
            }
            final Node controlNode = comparison.getControlDetails().getTarget();
            final Node testNode = comparison.getTestDetails().getTarget();
            if (controlNode != null && controlNode.getParentNode() instanceof Element
                    && testNode != null && testNode.getParentNode() instanceof Element) {
                Element controlElement = (Element) controlNode.getParentNode();
                Element testElement = (Element) testNode.getParentNode();
                if (controlElement.getNodeName().equals(elementName)) {
                    final double controlValue = Double.parseDouble(controlElement.getTextContent());
                    final double testValue = Double.parseDouble(testElement.getTextContent());
                    String measurementType = getMeasurementType(controlElement);
                    double tolerance = specificMeasurementTypeTolerance.containsKey(measurementType) ? specificMeasurementTypeTolerance.get(measurementType) : defaultTolerance;
                    if (Math.abs(controlValue - testValue) < tolerance) {
                        return ComparisonResult.EQUAL;
                    }
                }
            }
            return outcome;
        }

        private String getMeasurementType(Element analogValue) {
            String measurementType = null;
            Node sibling = analogValue.getPreviousSibling();
            while (sibling != null) {
                if (sibling.getNodeName().equals("measurementType")) {
                    measurementType = sibling.getTextContent();
                    break;
                }
                sibling = sibling.getPreviousSibling();
            }
            return measurementType;
        }
    }
}
