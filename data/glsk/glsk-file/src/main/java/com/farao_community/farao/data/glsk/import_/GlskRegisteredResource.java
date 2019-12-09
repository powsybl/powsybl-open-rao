/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.import_;

import com.farao_community.farao.data.glsk.import_.actors.TypeGlskFile;
import org.w3c.dom.Element;

import java.util.Objects;
import java.util.Optional;

/**
 * Registered Resource: a generator or a load, with its participation factor
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public class GlskRegisteredResource {
    /**
     * mRID of registered resource
     */
    private String mRID;
    /**
     * name
     */
    private String name;
    /**
     * participation factor between generator and load. default = 1
     */
    private Optional<Double> participationFactor;
    /**
     * max value for merit order
     */
    private Optional<Double> maximumCapacity;
    /**
     * min value for merit order
     */
    private Optional<Double> minimumCapacity;

    /**
     * @param element Dom element
     */
    public GlskRegisteredResource(Element element) {
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

    /**
     * @param ucteformat UCTE format constructor
     * @param element Dom element
     */
    public GlskRegisteredResource(boolean ucteformat, Element element) {
        if (ucteformat) {
            Objects.requireNonNull(element);
            this.name = ((Element) element.getElementsByTagName("NodeName").item(0)).getAttribute("v");
            this.mRID = this.name;
            this.participationFactor = element.getElementsByTagName("Factor").getLength() == 0 ? Optional.empty() :
                    Optional.of(Double.parseDouble(((Element) element.getElementsByTagName("Factor").item(0)).getAttribute("v")));
            this.maximumCapacity = Optional.empty();
            this.minimumCapacity = Optional.empty();
        }
    }

    /**
     * @return getter country mrid
     */
    public String getmRID() {
        return mRID;
    }

    /**
     * @param mRID setter mrid
     */
    public void setmRID(String mRID) {
        this.mRID = mRID;
    }

    /**
     * @return get name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name set name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return get participation factor
     */
    public double getParticipationFactor() {
        return participationFactor.orElse(0.0);
    }

    /**
     * @return getter max value
     */
    public Optional<Double> getMaximumCapacity() {
        return maximumCapacity;
    }

    /**
     * @return getter min value
     */
    public Optional<Double> getMinimumCapacity() {
        return minimumCapacity;
    }

    /**
     * @return the genrator Id according to type of Glsk File
     */
    public String getGeneratorId(TypeGlskFile typeGlskFile) {
        if (typeGlskFile.equals(TypeGlskFile.UCTE)) {
            return mRID + "_generator";
        } else {
            return mRID;
        }
    }

    /**
     * @return the load Id according to the type of Glsk File
     */
    public String getLoadId(TypeGlskFile typeGlskFile) {
        if (typeGlskFile.equals(TypeGlskFile.UCTE)) {
            return mRID + "_load";
        } else {
            return mRID;
        }
    }
}
