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

        NodeList manualGskBlocks = element.getElementsByTagName("ManualGSKBlock");
        for (int i = 0; i < manualGskBlocks.getLength(); i++) {
            if (manualGskBlocks.item(i).getNodeType() == Node.ELEMENT_NODE) {
                String businessType = ((Element) element.getElementsByTagName("BusinessType").item(0)).getAttribute("v");
                Element manualGlskBlockElement = (Element) manualGskBlocks.item(i);
                this.glskShiftKeys.add(new CseGlskShiftKey(manualGlskBlockElement, businessType, pointInterval, subjectDomainmRID));
            }
        }

        NodeList propGskBlocks = element.getElementsByTagName("PropGSKBlock");
        for (int i = 0; i < propGskBlocks.getLength(); i++) {
            if (propGskBlocks.item(i).getNodeType() == Node.ELEMENT_NODE) {
                String businessType = ((Element) element.getElementsByTagName("BusinessType").item(0)).getAttribute("v");
                Element propGlskBlockElement = (Element) propGskBlocks.item(i);
                this.glskShiftKeys.add(new CseGlskShiftKey(propGlskBlockElement, businessType, pointInterval, subjectDomainmRID));
            }
        }

        NodeList propLskBlocks = element.getElementsByTagName("PropLSKBlock");
        for (int i = 0; i < propLskBlocks.getLength(); i++) {
            if (propLskBlocks.item(i).getNodeType() == Node.ELEMENT_NODE) {
                String businessType = ((Element) element.getElementsByTagName("BusinessType").item(0)).getAttribute("v");
                Element propGlskBlockElement = (Element) propLskBlocks.item(i);
                this.glskShiftKeys.add(new CseGlskShiftKey(propGlskBlockElement, businessType, pointInterval, subjectDomainmRID));
            }
        }

        NodeList reserveGskBlocks = element.getElementsByTagName("ReserveGSKBlock");
        for (int i = 0; i < reserveGskBlocks.getLength(); i++) {
            if (reserveGskBlocks.item(i).getNodeType() == Node.ELEMENT_NODE) {

                String businessType = ((Element) element.getElementsByTagName("BusinessType").item(0)).getAttribute("v");
                Element reserveGskBlockElement = (Element) reserveGskBlocks.item(i);
                this.glskShiftKeys.add(new CseGlskShiftKey(reserveGskBlockElement, businessType, pointInterval, subjectDomainmRID));
            }
        }

        NodeList meritOrderGskBlocks = element.getElementsByTagName("MeritOrderGSKBlock");
        for (int i = 0; i < meritOrderGskBlocks.getLength(); i++) {
            if (meritOrderGskBlocks.item(i).getNodeType() == Node.ELEMENT_NODE) {
                String businessType = ((Element) element.getElementsByTagName("BusinessType").item(0)).getAttribute("v");
                Element meritOrderGskBlockElement = (Element) meritOrderGskBlocks.item(i);
                // TODO, import down merit order block when UpDownScalable available
                Element upBlockElement = (Element) meritOrderGskBlockElement.getElementsByTagName("Up").item(0);
                NodeList nodesList = upBlockElement.getElementsByTagName("Node");
                for (int j = 0; j < nodesList.getLength(); j++) {
                    glskShiftKeys.add(new CseGlskShiftKey(meritOrderGskBlockElement, businessType, pointInterval, subjectDomainmRID, j));
                }
            }
        }
    }
}
