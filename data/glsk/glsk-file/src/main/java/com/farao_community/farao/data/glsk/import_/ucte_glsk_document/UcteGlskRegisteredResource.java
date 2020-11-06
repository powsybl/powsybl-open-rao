/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.import_.ucte_glsk_document;

import com.farao_community.farao.data.glsk.import_.glsk_document_api.AbstractGlskRegisteredResource;
import org.w3c.dom.Element;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class UcteGlskRegisteredResource extends AbstractGlskRegisteredResource {

    public UcteGlskRegisteredResource(Element element) {
        Objects.requireNonNull(element);
        this.name = ((Element) element.getElementsByTagName("NodeName").item(0)).getAttribute("v");
        this.mRID = this.name;
        this.participationFactor = element.getElementsByTagName("Factor").getLength() == 0 ? Optional.empty() :
            Optional.of(Double.parseDouble(((Element) element.getElementsByTagName("Factor").item(0)).getAttribute("v")));
        this.maximumCapacity = Optional.empty();
        this.minimumCapacity = Optional.empty();
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
