/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.api;

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
    protected Double participationFactor;
    /**
     * max value for merit order
     */
    protected Double maximumCapacity;
    /**
     * min value for merit order
     */
    protected Double minimumCapacity;

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
        return participationFactor != null ? participationFactor : 0.0;
    }

    /**
     * @return getter max value
     */
    public Optional<Double> getMaximumCapacity() {
        return Optional.ofNullable(maximumCapacity);
    }

    /**
     * @return getter min value
     */
    public Optional<Double> getMinimumCapacity() {
        return Optional.ofNullable(minimumCapacity);
    }

    /**
     * @return the genrator Id according to type of Glsk File
     */
    public String getGeneratorId() {
        return mRID;
    }

    /**
     * @return the load Id according to the type of Glsk File
     */
    public String getLoadId() {
        return mRID;
    }
}
