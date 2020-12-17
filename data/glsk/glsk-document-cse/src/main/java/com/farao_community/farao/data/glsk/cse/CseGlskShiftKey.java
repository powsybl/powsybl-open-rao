/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.cse;

import com.farao_community.farao.data.glsk.api.AbstractGlskRegisteredResource;
import com.farao_community.farao.data.glsk.api.AbstractGlskShiftKey;
import com.farao_community.farao.data.glsk.api.GlskException;
import org.threeten.extra.Interval;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Optional;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class CseGlskShiftKey extends AbstractGlskShiftKey {

    private static final double EPSILON = 1e-3;

    public CseGlskShiftKey(Element glskBlockElement, String businessType, Interval pointInterval, String subjectDomainmRID) {
        initCommonMemberVariables(glskBlockElement, businessType, pointInterval, subjectDomainmRID);

        if ("ManualGSKBlock".equals(glskBlockElement.getTagName())) {
            this.businessType = "B43";
            this.psrType = "A04";
            NodeList nodesList = glskBlockElement.getElementsByTagName("Node");
            double currentFactorsSum = 0;
            for (int i = 0; i < nodesList.getLength(); i++) {
                Element nodeElement = (Element) nodesList.item(i);
                CseGlskRegisteredResource cseRegisteredResource = new CseGlskRegisteredResource(nodeElement);
                registeredResourceArrayList.add(cseRegisteredResource);
                Optional<Double> intialFactor = cseRegisteredResource.getInitialFactor();
                if (intialFactor.isPresent()) {
                    currentFactorsSum += intialFactor.get();
                }
            }

            if (Math.abs(currentFactorsSum) < EPSILON) {
                throw new GlskException("Factors sum should not be 0");
            }

            for (AbstractGlskRegisteredResource registeredResource : registeredResourceArrayList) {
                CseGlskRegisteredResource cseRegisteredResource = (CseGlskRegisteredResource) registeredResource;
                Optional<Double> intialFactor = cseRegisteredResource.getInitialFactor();
                if (intialFactor.isPresent()) {
                    cseRegisteredResource.setParticipationFactor(intialFactor.get() / currentFactorsSum);
                }
            }
        } else if ("PropGSKBlock".equals(glskBlockElement.getTagName())) {
            this.businessType = "B42";
            this.psrType = "A04";
            NodeList nodesList = glskBlockElement.getElementsByTagName("Node");
            for (int i = 0; i < nodesList.getLength(); i++) {
                Element nodeElement = (Element) nodesList.item(i);
                CseGlskRegisteredResource cseRegisteredResource = new CseGlskRegisteredResource(nodeElement);
                registeredResourceArrayList.add(cseRegisteredResource);
            }
        } else if ("PropLSKBlock".equals(glskBlockElement.getTagName())) {
            this.businessType = "B42";
            this.psrType = "A05";
            NodeList nodesList = glskBlockElement.getElementsByTagName("Node");
            for (int i = 0; i < nodesList.getLength(); i++) {
                Element nodeElement = (Element) nodesList.item(i);
                CseGlskRegisteredResource cseRegisteredResource = new CseGlskRegisteredResource(nodeElement);
                registeredResourceArrayList.add(cseRegisteredResource);
            }
        } else if ("ReserveGSKBlock".equals(glskBlockElement.getTagName())) {
            this.businessType = "B44";
            this.psrType = "A04";

            NodeList nodesList = glskBlockElement.getElementsByTagName("Node");
            for (int i = 0; i < nodesList.getLength(); i++) {
                Element nodeElement = (Element) nodesList.item(i);
                CseGlskRegisteredResource cseRegisteredResource = new CseGlskRegisteredResource(nodeElement);
                registeredResourceArrayList.add(cseRegisteredResource);
            }
        } else {
            throw new GlskException("Unknown UCTE Block type");
        }
    }

    public CseGlskShiftKey(Element glskBlockElement, String businessType, Interval pointInterval, String subjectDomainmRID, int position) {
        initCommonMemberVariables(glskBlockElement, businessType, pointInterval, subjectDomainmRID);
        this.meritOrderPosition = position;

        if ("MeritOrderGSKBlock".equals(glskBlockElement.getTagName())) {
            this.businessType = "B45";
            this.psrType = "A04";
            Element nodeElement = (Element) glskBlockElement.getElementsByTagName("Node").item(position);
            CseGlskRegisteredResource cseRegisteredResource = new CseGlskRegisteredResource(nodeElement);
            registeredResourceArrayList.add(cseRegisteredResource);
        } else {
            throw new GlskException("Unknown UCTE Block type");
        }
    }

    private void initCommonMemberVariables(Element glskBlockElement, String businessType, Interval pointInterval, String subjectDomainmRID) {
        if (businessType.equals("Z02")) {
            this.psrType = "A04";
        } else if (businessType.equals("Z05")) {
            this.psrType = "A05";
        } else {
            throw new GlskException("in GlskShiftKey UCTE constructor: unknown ucteBusinessType: " + businessType);
        }
        this.quantity = Double.valueOf(((Element) glskBlockElement.getElementsByTagName("Factor").item(0)).getAttribute("v"));
        this.glskShiftKeyInterval = pointInterval;
        this.subjectDomainmRID = subjectDomainmRID;
        this.registeredResourceArrayList = new ArrayList<>();
    }
}
