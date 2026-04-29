/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.nc.profiles;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationContext;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.io.nc.Namespace;
import com.powsybl.openrao.data.raoresult.io.nc.RdfElement;
import com.powsybl.openrao.data.raoresult.io.nc.XmlHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.Arrays;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public abstract class AbstractNcProfile {
    protected final String keyword;
    protected final Document document;
    protected final Element rootRdfElement;

    protected AbstractNcProfile(String keyword) {
        this.keyword = keyword;
        this.document = XmlHelper.initXmlDocument();
        this.rootRdfElement = this.document.createElementNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf:RDF");
        Arrays.stream(Namespace.values()).forEach(namespace -> this.rootRdfElement.setAttribute("xmlns:%s".formatted(namespace.getKeyword()), namespace.getURI()));
        this.document.appendChild(this.rootRdfElement);
    }

    public AbstractNcProfile fill(OffsetDateTime startDateTime, RaoResult raoResult, NcCracCreationContext ncCracCreationContext) {
        fillHeader(startDateTime);
        fillContent(raoResult, ncCracCreationContext);
        return this;
    }

    private void fillHeader(OffsetDateTime startDateTime) {
        OffsetDateTime now = OffsetDateTime.now();
        addRdfElement("FullModel", Namespace.MD)
            .addAttribute("generatedAtTime", Namespace.PROV, XmlHelper.formatOffsetDateTime(now))
            .addAttribute("issued", Namespace.DCTERMS, XmlHelper.formatOffsetDateTime(now))
            .addAttribute("startDate", Namespace.DCAT, XmlHelper.formatOffsetDateTime(startDateTime))
            .addAttribute("endDate", Namespace.DCAT, XmlHelper.formatOffsetDateTime(startDateTime.plusHours(1)))
            .addAttribute("keyword", Namespace.DCAT, keyword)
            .addResource("accessRights", Namespace.DCTERMS, "http://energy.referencedata.eu/Confidentiality/Restricted");
    }

    protected abstract void fillContent(RaoResult raoResult, NcCracCreationContext ncCracCreationContext);

    protected RdfElement addRdfElement(String name, Namespace namespace) {
        Element element = document.createElement(namespace.format(name));
        rootRdfElement.appendChild(element);
        return new RdfElement(element);
    }

    protected RdfElement addRdfElement(String name, Namespace namespace, String id) {
        Element element = document.createElement(namespace.format(name));
        element.setAttribute(Namespace.RDF.format("ID"), "_" + id);
        rootRdfElement.appendChild(element);
        return new RdfElement(element);
    }

    public void write(OutputStream outputStream) {
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
