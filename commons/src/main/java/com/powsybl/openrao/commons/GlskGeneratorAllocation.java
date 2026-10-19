package com.powsybl.openrao.commons;

public record GlskGeneratorAllocation(String generatorId, double weight, double availableCapacity, double saturationRatio, double cumulativeAllocation) { }
