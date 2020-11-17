/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.ucte;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * UCTE type GLSK document internal object
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public class UcteGlskSeries {

    /**
     * country code
     */
    private String area;
    /**
     * generator or load
     */
    private String ucteBusinessType;
    /**
     * id
     */
    private String timeSeriesID;
    /**
     * share factor between LSK and GSK
     */
    private double shareFactor;
    /**
     * block type: country, Manual, Auto
     */
    private String ucteGlskBlockType;
    /**
     * List of block in time series
     */
    private List<UcteGlskPoint> ucteGlskBlocks;
    /**
     * Constant business type
     */
    private static final String BUSINESS_TYPE = "BusinessType";

    /**
     * @param element element of Series
     */
    public UcteGlskSeries(Element element) {
        Node node = Objects.requireNonNull(element).getElementsByTagName("Area").item(0);
        Element nodeElement = (Element) node;
        this.area = nodeElement.getAttribute("v");
        this.ucteBusinessType = ((Element) element.getElementsByTagName(BUSINESS_TYPE).item(0)).getAttribute("v");
        this.timeSeriesID = ((Element) element.getElementsByTagName("TimeSeriesIdentification").item(0)).getAttribute("v");
        if (((Element) element.getElementsByTagName(BUSINESS_TYPE).item(0)).hasAttribute("share")) {
            this.shareFactor = Double.parseDouble(((Element) element.getElementsByTagName(BUSINESS_TYPE).item(0)).getAttribute("share"));
        } else {
            this.shareFactor = 100;
        }
        if (Objects.requireNonNull(element).getElementsByTagName("CountryGSK_Block").getLength() > 0) {
            this.ucteGlskBlockType = "CountryGSK_Block";
        } else if (Objects.requireNonNull(element).getElementsByTagName("ManualGSK_Block").getLength() > 0) {
            this.ucteGlskBlockType = "ManualGSK_Block";
        } else if (Objects.requireNonNull(element).getElementsByTagName("AutoGSK_Block").getLength() > 0) {
            this.ucteGlskBlockType = "AutoGSK_Block";
        } else {
            this.ucteGlskBlockType = "UNKNOWN_GLSKTYPE";
        }

        this.ucteGlskBlocks = new ArrayList<>();
        NodeList ucteGlskBlockNodes = Objects.requireNonNull(element).getElementsByTagName(this.ucteGlskBlockType);
        for (int i = 0; i < ucteGlskBlockNodes.getLength(); i++) {
            UcteGlskPoint glskPoint = new UcteGlskPoint((Element) ucteGlskBlockNodes.item(i), this.ucteGlskBlockType, this.area, this.ucteBusinessType, this.shareFactor);
            ucteGlskBlocks.add(glskPoint);
        }

    }

    /**
     * @return getter area
     */
    public String getArea() {
        return area;
    }

    /**
     * @param area setter
     */
    public void setArea(String area) {
        this.area = area;
    }

    /**
     * @return getter list of block in time series
     */
    public List<UcteGlskPoint> getUcteGlskBlocks() {
        return ucteGlskBlocks;
    }

    /**
     * @return get id
     */
    public String getTimeSeriesID() {
        return timeSeriesID;
    }
}
