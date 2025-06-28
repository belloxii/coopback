package com.gotryt.coop.utils;

import java.time.Year;

public class MemshipGen {

    public static String genMemship(int ledgerNumber) {
        int currentYear = Year.now().getValue();
        String paddedLedger = String.format("%04d", ledgerNumber);
        return currentYear + paddedLedger;
    }
}
