package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.threshold.AbstractThreshold;
import com.farao_community.farao.data.crac_impl.threshold.ThresholdAdderImpl;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class SimpleCnecAdder implements CnecAdder {

    private SimpleCrac parent;
    private String id;
    private String name;
    private State state;
    private NetworkElement networkElement;
    private Set<AbstractThreshold> thresholds;

    public SimpleCnecAdder(SimpleCrac parent) {
        Objects.requireNonNull(parent);
        this.parent = parent;
        thresholds = new HashSet<>();
    }

    public void addThreshold(AbstractThreshold threshold) {
        thresholds.add(threshold);
    }

    @Override
    public CnecAdder setId(String id) {
        Objects.requireNonNull(id);
        this.id = id;
        return this;
    }

    @Override
    public CnecAdder setName(String name) {
        Objects.requireNonNull(name);
        this.name = name;
        return this;
    }

    @Override
    public CnecAdder setState(State state) {
        Objects.requireNonNull(state);
        this.state = state;
        return this;
    }

    @Override
    public NetworkElementAdder newNetworkElement() {
        if (networkElement == null) {
            return new NetworkElementAdderImpl<CnecAdder>(this);
        } else {
            throw new FaraoException("Only one network element can be added to cnec.");
        }
    }

    @Override
    public ThresholdAdder newThreshold() {
        return new ThresholdAdderImpl(this);
    }

    @Override
    public Cnec add() {
        if (this.id == null) {
            throw new FaraoException("Cannot add a cnec without an id. Please use setId.");
        }
        if (this.state == null) {
            throw new FaraoException("Cannot add a cnec without a state. Please use setState.");
        }
        if (this.networkElement == null) {
            throw new FaraoException("Cannot add a cnec without a network element. Please use newNetworkElement.");
        }
        if (this.thresholds.isEmpty()) {
            throw new FaraoException("Cannot add a cnec without a threshold. Please use newThreshold.");
        }
        if (this.name == null) {
            this.name = this.id;
        }
        SimpleCnec cnec = new SimpleCnec(this.id, this.name, networkElement, thresholds, state);
        parent.addCnec(cnec);
        return cnec;
    }

    @Override
    public NetworkElement addNetworkElement(NetworkElement networkElement) {
        this.networkElement = networkElement;
        return networkElement;
    }
}
