/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.import_;

import com.farao_community.farao.commons.FaraoException;
import org.threeten.extra.Interval;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


/**
 * Shift Key
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public class GlskShiftKey {

    /**
     * business type of shift key. B42, B43, B45
     */
    private String businessType;
    /**
     * load or generator
     */
    private String psrType;
    /**
     * explicit shift key factor
     */
    private Double quantity;
    /**
     * list of registered resources
     */
    private List<GlskRegisteredResource> registeredResourceArrayList;

    /**
     * time interval of shift key
     */
    private Interval glskShiftKeyInterval;
    /**
     * country mrid
     */
    private String subjectDomainmRID;
    /**
     * merit order position
     */
    private int meritOrderPosition;
    /**
     * merit order direction
     */
    private String flowDirection;

    /**
     * @param element Dom element of CIM Glsk
     * @param pointInterval interval of point
     * @param subjectDomainmRID country mrid
     */
    public GlskShiftKey(Element element, Interval pointInterval, String subjectDomainmRID) {
        Objects.requireNonNull(element);
        this.businessType = element.getElementsByTagName("businessType").item(0).getTextContent();
        List<String> supportedBusinessType = Arrays.asList("B42", "B43", "B45");
        if (!supportedBusinessType.contains(businessType)) {
            throw new FaraoException("BusinessType not supported: " + businessType);
        }
        this.psrType = element.getElementsByTagName("mktPSRType.psrType").item(0).getTextContent();
        if (element.getElementsByTagName("quantity.quantity").getLength() > 0) {
            this.quantity = Double.valueOf(element.getElementsByTagName("quantity.quantity").item(0).getTextContent());
        } else {
            this.quantity = 1.0;
        }
        this.subjectDomainmRID = subjectDomainmRID;
        this.meritOrderPosition = element.getElementsByTagName("attributeInstanceComponent.position").getLength() == 0 ? 0 :
                Integer.valueOf(element.getElementsByTagName("attributeInstanceComponent.position").item(0).getTextContent());
        this.flowDirection = element.getElementsByTagName("flowDirection.direction").getLength() == 0 ? "" :
                element.getElementsByTagName("flowDirection.direction").item(0).getTextContent();
        //registeredResources
        this.registeredResourceArrayList = new ArrayList<>();
        NodeList glskRegisteredResourcesElements = element.getElementsByTagName("RegisteredResource");
        for (int i = 0; i < glskRegisteredResourcesElements.getLength(); i++) {
            registeredResourceArrayList.add(new GlskRegisteredResource((Element) glskRegisteredResourcesElements.item(i)));
        }

        this.glskShiftKeyInterval = pointInterval;
    }


    /**
     * @param businessType business type for UCTE Glsk shift key constructor
     * @param ucteBusinessType ucte businesstype for load and generator
     * @param subjectDomainmRID country code
     * @param pointInterval interval
     * @param shareFactor shareFactor between load and generator
     */
    public GlskShiftKey(String businessType, String ucteBusinessType, String subjectDomainmRID, Interval pointInterval, Double shareFactor) {
        //for ucte format country gsk
        this.businessType = businessType;
        if (ucteBusinessType.equals("Z02")) {
            this.psrType = "A04";
        } else if (ucteBusinessType.equals("Z05")) {
            this.psrType = "A05";
        } else {
            throw new FaraoException("in GlskShiftKey UCTE constructor: unknown ucteBusinessType: " + ucteBusinessType);
        }

        this.quantity = shareFactor / 100;
        this.registeredResourceArrayList = new ArrayList<>();
        this.glskShiftKeyInterval = pointInterval;
        this.subjectDomainmRID = subjectDomainmRID;
    }


    /**
     * @return debug to string
     */
    public String glskShiftKeyToString() {
        return "\t==== GSK Shift Key ====\n" +
                "\tBusinessType = " + businessType + "\n" +
                "\tPsrType = " + psrType + "\n" +
                "\tQuantity = " + quantity + "\n" +
                "\tGlskShiftKeyInterval = " + glskShiftKeyInterval + "\n" +
                "\tRegisteredResource size = " + registeredResourceArrayList.size() + "\n";
    }

    /**
     * @return getter businesstype
     */
    public String getBusinessType() {
        return businessType;
    }

    /**
     * @param businessType setter business type
     */
    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    /**
     * @return getter psrType
     */
    public String getPsrType() {
        return psrType;
    }

    /**
     * @param psrType setter psrType
     */
    public void setPsrType(String psrType) {
        this.psrType = psrType;
    }

    /**
     * @return getter quantity
     */
    public Double getQuantity() {
        return quantity;
    }

    /**
     * @return get list of registered resources
     */
    public List<GlskRegisteredResource> getRegisteredResourceArrayList() {
        return registeredResourceArrayList;
    }

    /**
     * @param registeredResourceArrayList setter registered resources
     */
    public void setRegisteredResourceArrayList(List<GlskRegisteredResource> registeredResourceArrayList) {
        this.registeredResourceArrayList = registeredResourceArrayList;
    }

    /**
     * @return getter country mrid
     */
    public String getSubjectDomainmRID() {
        return subjectDomainmRID;
    }

    /**
     * @return getter merit order position
     */
    public int getMeritOrderPosition() {
        return meritOrderPosition;
    }

    /**
     * @return getter merit order direction
     */
    public String getFlowDirection() {
        return flowDirection;
    }

}
