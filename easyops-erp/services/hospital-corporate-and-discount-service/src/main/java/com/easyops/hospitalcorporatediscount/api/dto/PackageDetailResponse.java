package com.easyops.hospitalcorporatediscount.api.dto;

import java.util.List;

public class PackageDetailResponse extends PackageResponse {

    private List<PackageItemResponse> items;

    public List<PackageItemResponse> getItems() { return items; }
    public void setItems(List<PackageItemResponse> items) { this.items = items; }
}
