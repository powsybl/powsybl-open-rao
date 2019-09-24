package com.farao_community.farao.flowbased_computation.csv_exporter;

public enum Direction {
    DIRECT("DIRECT", 1),
    OPPOSITE("OPPOSITE", -1);

    private String name;
    private int sign;

    public String getName() {
        return name;
    }

    public int getSign() {
        return  sign;
    }

    private Direction(String name, int sign) {
        this.name = name;
        this.sign = sign;
    }
}
