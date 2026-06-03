package com.easyops.hr.entity;

/**
 * ES-07, ES-08–ES-10: How the employee salary component value is determined.
 * AMOUNT = fixed amount per pay period; PERCENTAGE = override percentage (base from master);
 * USE_MASTER_DEFAULT = use component master default amount/percentage/formula.
 */
public enum ComponentValueType {
    AMOUNT,
    PERCENTAGE,
    USE_MASTER_DEFAULT
}
