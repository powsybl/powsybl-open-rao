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
        this.initialFactor = element.getElementsByTagName("Factor").getLength() == 0 ? null :
                Double.parseDouble(((Element) element.getElementsByTagName("Factor").item(0)).getAttribute("v"));
        this.maximumCapacity = element.getElementsByTagName("Pmax").getLength() == 0 ? Optional.empty() :
                Optional.of(-Double.parseDouble(((Element) element.getElementsByTagName("Pmax").item(0)).getAttribute("v")));
        this.minimumCapacity = element.getElementsByTagName("Pmin").getLength() == 0 ? Optional.empty() :
                Optional.of(-Double.parseDouble(((Element) element.getElementsByTagName("Pmin").item(0)).getAttribute("v")));
    }

    void setParticipationFactor(double participationFactor) {
        this.participationFactor = Optional.of(participationFactor);
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
