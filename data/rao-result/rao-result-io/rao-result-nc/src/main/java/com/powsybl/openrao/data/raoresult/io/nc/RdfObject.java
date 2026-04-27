/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.nc;

import org.w3c.dom.Element;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class RdfObject {
    private final Element element;

    private final static String RESOURCE = "resource";

    public RdfObject(Element element) {
        this.element = element;
    }

    public RdfObject addValue(String fieldName, Namespace namespace, String value) {
        Element valueElement = element.getOwnerDocument().createElement(namespace.format(fieldName));
        valueElement.setTextContent(value);
        element.appendChild(valueElement);
        return this;
    }

    public RdfObject addResource(String fieldName, Namespace namespace, String reference) {
        Element valueElement = element.getOwnerDocument().createElement(namespace.format(fieldName));
        valueElement.setAttribute(Namespace.RDF.format(RESOURCE), reference);
        element.appendChild(valueElement);
        return this;
    }
}
