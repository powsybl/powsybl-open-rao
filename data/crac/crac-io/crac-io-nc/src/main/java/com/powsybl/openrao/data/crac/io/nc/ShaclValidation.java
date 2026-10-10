/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc;

import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.triplestore.api.PropertyBag;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.validation.ReportEntry;
import org.apache.jena.shacl.validation.Severity;
import org.apache.jena.sparql.graph.GraphFactory;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class ShaclValidation {
    private static final List<String> SHACL_FILES = List.of(
        "/shacl/Contingency-AP-Con-Simple-SHACL.ttl",
        "/shacl/Contingency-AP-Con-Complex-SHACL.ttl",
        "/shacl/DatasetMetadata-AP-Con-SGACL.ttl",
        "/shacl/Contingency-PowSyBl-SHACL.ttl"
    );

    private ShaclValidation() {
    }

    public static Map<String, PropertyBag> validate(Path dataPath) {
        Graph shapesGraph = GraphFactory.createDefaultGraph();
        SHACL_FILES.forEach(shacl -> RDFDataMgr.read(shapesGraph, String.valueOf(ShaclValidation.class.getResource(shacl))));
        Shapes shapes = Shapes.parse(shapesGraph);

        Graph dataGraph = RDFDataMgr.loadGraph(String.valueOf(dataPath));

        ValidationReport report = ShaclValidator.get().validate(shapes, dataGraph);

        Set<String> invalidObjects = new HashSet<>();
        for (ReportEntry entry : report.getEntries()) {
            logShaclViolation(entry);
            invalidObjects.add(entry.focusNode().getLocalName());
        }
        report.getEntries().forEach(ShaclValidation::logShaclViolation);
        return getObjects(dataGraph, invalidObjects);
    }

    private static void logShaclViolation(ReportEntry entry) {
        Severity level = entry.severity();
        String sourceProfile = Arrays.stream(entry.focusNode().getNameSpace().split("/")).toList().getLast();
        String rule = entry.source().getLocalName();
        String focusNode = entry.focusNode().getLocalName();
        String message = "[%s] {%s%s}: %s".formatted(rule, sourceProfile, focusNode, entry.message());
        if (level == Severity.Violation) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.error(message);
        } else if (level == Severity.Warning) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.warn(message);
        } else if (level == Severity.Info) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info(message);
        }
    }

    private static Map<String, PropertyBag> getObjects(Graph dataGraph, Set<String> invalidObjects) {
        Map<String, PropertyBag> propertyBags = new HashMap<>();
        dataGraph.stream().forEach(
            triple -> {
                String mRID = getValue(triple.getSubject());
                String objectType = getValue(triple.getPredicate());
                String value = getValue(triple.getObject());
                if (!invalidObjects.contains(mRID)) {
                    propertyBags.computeIfAbsent(mRID, k -> new PropertyBag(List.of(objectType), false)).putIfAbsent(objectType, value);
                }
            }
        );
        return propertyBags;
    }

    private static String getValue(Node node) {
        return node.isLiteral() ? node.getLiteral().getLexicalForm() : Arrays.stream(node.getURI().split("#")).toList().getLast();
    }
}
