/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.ucte;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.glsk.api.AbstractGlskPoint;
import com.farao_community.farao.data.glsk.api.AbstractGlskRegisteredResource;
import com.farao_community.farao.data.glsk.api.AbstractGlskShiftKey;
import org.threeten.extra.Interval;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class UcteGlskPoint extends AbstractGlskPoint {

    /**
     * @param element Dom element
     * @param ucteBlockType Type of block: CountryGSK, ManualGSK, AutoGSK
     * @param area country
     * @param ucteBusinessType generator or load
     * @param shareFactor shareFactor for generator or load
     */
    public UcteGlskPoint(Element element, String ucteBlockType, String area, String ucteBusinessType, Double shareFactor) {
        Objects.requireNonNull(element);
        this.position = 1;
        this.pointInterval = Interval.parse(((Element) element.getElementsByTagName("TimeInterval").item(0)).getAttribute("v"));
        this.subjectDomainmRID = area;
        this.curveType = "A03";

        glskShiftKeys = new ArrayList<>();
        switch (ucteBlockType) {
            case "CountryGSK_Block": {
                //build a country GSK B42 empty regitered resources list
                AbstractGlskShiftKey countryGlskShiftKey = new UcteGlskShiftKey("B42", ucteBusinessType, this.subjectDomainmRID, this.pointInterval, shareFactor);
                glskShiftKeys.add(countryGlskShiftKey);
                break;
            }
            case "ManualGSK_Block": {
                //build a B43 participation factor
                AbstractGlskShiftKey manuelGlskShiftKey = new UcteGlskShiftKey("B43", ucteBusinessType, this.subjectDomainmRID, this.pointInterval, shareFactor);
                //set registeredResourcesList for manuelGlskShiftKey
                List<AbstractGlskRegisteredResource> registerdResourceArrayList = new ArrayList<>();
                NodeList ucteGlskNodesList = element.getElementsByTagName("ManualNodes");

                for (int i = 0; i < ucteGlskNodesList.getLength(); ++i) {
                    AbstractGlskRegisteredResource ucteGlskNode = new UcteGlskRegisteredResource((Element) ucteGlskNodesList.item(i));
                    registerdResourceArrayList.add(ucteGlskNode);
                }
                manuelGlskShiftKey.setRegisteredResourceArrayList(registerdResourceArrayList);
                glskShiftKeys.add(manuelGlskShiftKey);
                break;
            }
            case "AutoGSK_Block": {
                /* build a B42 explicit */
                AbstractGlskShiftKey autoGlskShiftKey = new UcteGlskShiftKey("B42", ucteBusinessType, this.subjectDomainmRID, this.pointInterval, shareFactor);
                List<AbstractGlskRegisteredResource> registerdResourceArrayList = new ArrayList<>();
                NodeList ucteGlskNodesList = element.getElementsByTagName("AutoNodes");

                for (int i = 0; i < ucteGlskNodesList.getLength(); ++i) {
                    AbstractGlskRegisteredResource ucteGlskNode = new UcteGlskRegisteredResource((Element) ucteGlskNodesList.item(i));
                    registerdResourceArrayList.add(ucteGlskNode);
                }
                autoGlskShiftKey.setRegisteredResourceArrayList(registerdResourceArrayList);
                glskShiftKeys.add(autoGlskShiftKey);
                break;
            }
            default:
                throw new FaraoException("Unknown UCTE Block type");
        }
    }
}
