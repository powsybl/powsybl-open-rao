/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.cse;

import com.farao_community.farao.data.glsk.api.AbstractGlskPoint;
import org.threeten.extra.Interval;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Objects;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class CseGlskPoint extends AbstractGlskPoint {
    /**
     * @param element Dom element
     */
    public CseGlskPoint(Element element) {
        Objects.requireNonNull(element);
        this.position = 1;
        this.pointInterval = Interval.parse(((Element) element.getElementsByTagName("TimeInterval").item(0)).getAttribute("v"));
        this.subjectDomainmRID = ((Element) element.getElementsByTagName("Area").item(0)).getAttribute("v");
        this.curveType = "A03";
        this.glskShiftKeys = new ArrayList<>();

        String businessType = ((Element) element.getElementsByTagName("BusinessType").item(0)).getAttribute("v");
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) childNode;
                switch (childElement.getNodeName()) {
                    case "ManualGSKBlock":
                    case "PropGSKBlock":
                    case "PropLSKBlock":
                    case "ReserveGSKBlock":
                        importStandardBlock(childElement, businessType);
                        break;
                    case "MeritOrderGSKBlock":
                        importMeritOrderBlock(childElement, businessType);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void importMeritOrderBlock(Element blockElement, String businessType) {
        // Merit order scaling is not symmetrical, a feature not yet released of PowSyBl.
        // TODO : create UpDownScalable with up block and down block when available in PowSYBl
        Element upBlockElement = (Element) blockElement.getElementsByTagName("Up").item(0);
        NodeList nodesList = upBlockElement.getElementsByTagName("Node");
        for (int j = 0; j < nodesList.getLength(); j++) {
            glskShiftKeys.add(new CseGlskShiftKey(blockElement, businessType, pointInterval, subjectDomainmRID, j));
        }

    }

    private void importStandardBlock(Element blockElement, String businessType) {
        this.glskShiftKeys.add(new CseGlskShiftKey(blockElement, businessType, pointInterval, subjectDomainmRID));
    }
}
