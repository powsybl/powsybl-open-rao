/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.cse;

import com.farao_community.farao.data.glsk.api.AbstractGlskRegisteredResource;
import org.w3c.dom.Element;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class CseGlskRegisteredResource extends AbstractGlskRegisteredResource {
    private final Double initialFactor;

    public CseGlskRegisteredResource(Element element) {
        Objects.requireNonNull(element);
        this.name = ((Element) element.getElementsByTagName("Name").item(0)).getAttribute("v");
        this.mRID = this.name;
        this.initialFactor = getContentAsDoubleOrNull(element, "Factor");
        this.maximumCapacity = negativeIfNotNull(getContentAsDoubleOrNull(element, "Pmax"));
        this.minimumCapacity = negativeIfNotNull(getContentAsDoubleOrNull(element, "Pmin"));
    }

    void setParticipationFactor(double participationFactor) {
        this.participationFactor = participationFactor;
    }

    private Double getContentAsDoubleOrNull(Element baseElement, String tag) {
        return baseElement.getElementsByTagName(tag).getLength() == 0 ? null :
                Double.parseDouble(((Element) baseElement.getElementsByTagName(tag).item(0)).getAttribute("v"));
    }

    private Double negativeIfNotNull(Double value) {
        return value == null ? null : -value;
    }

    @Override
    public String getGeneratorId() {
        return mRID + "_generator";
    }

    @Override
    public String getLoadId() {
        return mRID + "_load";
    }

    Optional<Double> getInitialFactor() {
        return Optional.ofNullable(initialFactor);
    }
}
