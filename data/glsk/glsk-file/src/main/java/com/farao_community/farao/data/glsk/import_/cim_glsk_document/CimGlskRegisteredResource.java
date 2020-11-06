/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.glsk.import_.cim_glsk_document;

import com.farao_community.farao.data.glsk.import_.glsk_document_api.AbstractGlskRegisteredResource;
import org.w3c.dom.Element;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CimGlskRegisteredResource extends AbstractGlskRegisteredResource {

    public CimGlskRegisteredResource(Element element) {
        Objects.requireNonNull(element);
        this.mRID = element.getElementsByTagName("mRID").item(0).getTextContent();
        this.name = element.getElementsByTagName("name").item(0).getTextContent();
        this.participationFactor = element.getElementsByTagName("sK_ResourceCapacity.defaultCapacity").getLength() == 0 ? Optional.empty() :
            Optional.of(Double.parseDouble(element.getElementsByTagName("sK_ResourceCapacity.defaultCapacity").item(0).getTextContent()));
        this.maximumCapacity = element.getElementsByTagName("resourceCapacity.maximumCapacity").getLength() == 0 ? Optional.empty() :
            Optional.of(Double.parseDouble(element.getElementsByTagName("resourceCapacity.maximumCapacity").item(0).getTextContent()));
        this.minimumCapacity = element.getElementsByTagName("resourceCapacity.minimumCapacity").getLength() == 0 ? Optional.empty() :
            Optional.of(Double.parseDouble(element.getElementsByTagName("resourceCapacity.minimumCapacity").item(0).getTextContent()));
    }

    @Override
    public String getGeneratorId() {
        return mRID;
    }

    @Override
    public String getLoadId() {
        return mRID;
    }
}
