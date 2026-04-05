package com.example.backend.controllers.dto;

import com.opencsv.bean.CsvBindByName;

@lombok.Data
public class PoiCsvRecord {

    @CsvBindByName(column = "name")
    private String name;

    @CsvBindByName(column = "address")
    private String address;

    @CsvBindByName(column = "latitude")
    private double latitude;

    @CsvBindByName(column = "longitude")
    private double longitude;

    @CsvBindByName(column = "category")
    private String category;
}
