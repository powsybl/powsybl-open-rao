/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.import_.glsk_document_api;

import java.util.Optional;

/**
 * Registered Resource: a generator or a load, with its participation factor
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public abstract class AbstractGlskRegisteredResource {
    /**
     * mRID of registered resource
     */
    protected String mRID;
    /**
     * name
     */
    protected String name;
    /**
     * participation factor between generator and load. default = 1
     */
    protected Optional<Double> participationFactor;
    /**
     * max value for merit order
     */
    protected Optional<Double> maximumCapacity;
    /**
     * min value for merit order
     */
    protected Optional<Double> minimumCapacity;

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
    abstract public String getGeneratorId();

    /**
     * @return the load Id according to the type of Glsk File
     */
    abstract public String getLoadId();
}
