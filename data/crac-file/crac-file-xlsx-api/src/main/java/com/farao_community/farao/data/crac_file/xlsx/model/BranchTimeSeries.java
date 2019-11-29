/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file.xlsx.model;

import com.farao_community.farao.data.crac_file.xlsx.annotation.ExcelColumn;
import com.farao_community.farao.data.crac_file.xlsx.converter.DateConverter;
import com.farao_community.farao.data.crac_file.xlsx.converter.FloatConverter;
import com.farao_community.farao.data.crac_file.xlsx.converter.TypeOfTimeSeriesConverter;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Data
public class BranchTimeSeries {
    @ExcelColumn(position = 0, convertorClass = DateConverter.class)
    private final LocalDate date;
    @ExcelColumn(position = 1)
    private final String uniqueCbcoName;
    @ExcelColumn(position = 2, convertorClass = TypeOfTimeSeriesConverter.class)
    private final TypeOfTimeSeries type;
    @ExcelColumn(name = "00:30:00 AM", position = 3, convertorClass = FloatConverter.class)
    private final float limit1;
    @ExcelColumn(name = "01:30:00 AM", position = 4, convertorClass = FloatConverter.class)
    private final float limit2;
    @ExcelColumn(name = "02:30:00 AM", position = 5, convertorClass = FloatConverter.class)
    private final float limit3;
    @ExcelColumn(name = "03:30:00 AM", position = 6, convertorClass = FloatConverter.class)
    private final float limit4;
    @ExcelColumn(name = "04:30:00 AM", position = 7, convertorClass = FloatConverter.class)
    private final float limit5;
    @ExcelColumn(name = "05:30:00 AM", position = 8, convertorClass = FloatConverter.class)
    private final float limit6;
    @ExcelColumn(name = "06:30:00 AM", position = 9, convertorClass = FloatConverter.class)
    private final float limit7;
    @ExcelColumn(name = "07:30:00 AM", position = 10, convertorClass = FloatConverter.class)
    private final float limit8;
    @ExcelColumn(name = "08:30:00 AM", position = 11, convertorClass = FloatConverter.class)
    private final float limit9;
    @ExcelColumn(name = "09:30:00 AM", position = 12, convertorClass = FloatConverter.class)
    private final float limit10;
    @ExcelColumn(name = "10:30:00 AM", position = 13, convertorClass = FloatConverter.class)
    private final float limit11;
    @ExcelColumn(name = "11:30:00 AM", position = 14, convertorClass = FloatConverter.class)
    private final float limit12;
    @ExcelColumn(name = "12:30:00 PM", position = 15, convertorClass = FloatConverter.class)
    private final float limit13;
    @ExcelColumn(name = "01:30:00 PM", position = 16, convertorClass = FloatConverter.class)
    private final float limit14;
    @ExcelColumn(name = "02:30:00 PM", position = 17, convertorClass = FloatConverter.class)
    private final float limit15;
    @ExcelColumn(name = "03:30:00 PM", position = 18, convertorClass = FloatConverter.class)
    private final float limit16;
    @ExcelColumn(name = "04:30:00 PM", position = 19, convertorClass = FloatConverter.class)
    private final float limit17;
    @ExcelColumn(name = "05:30:00 PM", position = 20, convertorClass = FloatConverter.class)
    private final float limit18;
    @ExcelColumn(name = "06:30:00 PM", position = 21, convertorClass = FloatConverter.class)
    private final float limit19;
    @ExcelColumn(name = "07:30:00 PM", position = 22, convertorClass = FloatConverter.class)
    private final float limit20;
    @ExcelColumn(name = "08:30:00 PM", position = 23, convertorClass = FloatConverter.class)
    private final float limit21;
    @ExcelColumn(name = "09:30:00 PM", position = 24, convertorClass = FloatConverter.class)
    private final float limit22;
    @ExcelColumn(name = "10:30:00 PM", position = 25, convertorClass = FloatConverter.class)
    private final float limit23;
    @ExcelColumn(name = "11:30:00 PM", position = 26, convertorClass = FloatConverter.class)
    private final float limit24;

    @Getter
    @Setter
    private float currentLimit;

    public BranchTimeSeries() {
        this(null, null, null, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0);
    }

    @Builder
    public BranchTimeSeries(LocalDate date, String uniqueCbcoName, TypeOfTimeSeries type, float limit1, float limit2, float limit3, float limit4, float limit5,
                            float limit6, float limit7, float limit8, float limit9, float limit10, float limit11,
                            float limit12, float limit13, float limit14, float limit15, float limit16,
                            float limit17, float limit18, float limit19, float limit20, float limit21, float limit22, float limit23, float limit24) {
        this.date = date;
        this.uniqueCbcoName = uniqueCbcoName;
        this.type = type;
        this.limit1 = limit1;
        this.limit2 = limit2;
        this.limit3 = limit3;
        this.limit4 = limit4;
        this.limit5 = limit5;
        this.limit6 = limit6;
        this.limit7 = limit7;
        this.limit8 = limit8;
        this.limit9 = limit9;
        this.limit10 = limit10;
        this.limit11 = limit11;
        this.limit12 = limit12;
        this.limit13 = limit13;
        this.limit14 = limit14;
        this.limit15 = limit15;
        this.limit16 = limit16;
        this.limit17 = limit17;
        this.limit18 = limit18;
        this.limit19 = limit19;
        this.limit20 = limit20;
        this.limit21 = limit21;
        this.limit22 = limit22;
        this.limit23 = limit23;
        this.limit24 = limit24;
    }

    @Override
    public String toString() {
        return this.uniqueCbcoName;
    }

    public float getCurentLimit1(TimesSeries timesSeries) {
        int ts = Integer.parseInt(timesSeries.getLabel());

        switch (ts) {
            case 1:
                return limit1;
            case 2:
                return limit2;
            case 3:
                return limit3;
            case 4:
                return limit4;
            case 5:
                return limit5;
            case 6:
                return limit6;
            case 7:
                return limit7;
            case 8:
                return limit8;
            case 9:
                return limit9;
            case 10:
                return limit10;
            case 11:
                return limit11;
            case 12:
                return limit12;
            case 13:
                return limit13;
            case 14:
                return limit14;
            case 15:
                return limit15;
            case 16:
                return limit16;
            case 17:
                return limit17;
            case 18:
                return limit18;
            case 19:
                return limit19;
            case 20:
                return limit20;
            case 21:
                return limit21;
            case 22:
                return limit22;
            case 23:
                return limit23;
            case 24:
                return limit24;
            default:
                return ts;
        }
    }
}
