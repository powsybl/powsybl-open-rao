package com.farao_community.farao.commons.data.glsk_file.actors;

public enum TypeGlskFile {
    UCTE("UCTE"),
    CIM("CIM");
    private String value;

    public String getValue() {
        return value;
    }

    TypeGlskFile(String value) {
        this.value = value;
    }
}
