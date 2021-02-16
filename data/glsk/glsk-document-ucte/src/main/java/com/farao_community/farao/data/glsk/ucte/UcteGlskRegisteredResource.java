/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.ucte;

import com.farao_community.farao.data.glsk.api.AbstractGlskRegisteredResource;
import org.w3c.dom.Element;

import java.util.Objects;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class UcteGlskRegisteredResource extends AbstractGlskRegisteredResource {

    public UcteGlskRegisteredResource(Element element) {
        Objects.requireNonNull(element);
        this.name = ((Element) element.getElementsByTagName("NodeName").item(0)).getAttribute("v");
        this.mRID = this.name;
        this.participationFactor = getContentAsDoubleOrNull(element, "Factor");
    }

    private Double getContentAsDoubleOrNull(Element baseElement, String tag) {
        return baseElement.getElementsByTagName(tag).getLength() == 0 ? null :
                Double.parseDouble(((Element) baseElement.getElementsByTagName(tag).item(0)).getAttribute("v"));
    }

    @Override
    public String getGeneratorId() {
        return mRID + "_generator";
    }

    @Override
    public String getLoadId() {
        return mRID + "_load";
    }
}
