package com.easyops.hospitalclinicalorders.api.dto;

import java.util.ArrayList;
import java.util.List;

public class ClinicalOrderDetailResponse extends ClinicalOrderResponse {
    private List<WorklistItemResponse> worklistItems = new ArrayList<>();
    private List<ResultLinkResponse> resultLinks = new ArrayList<>();

    public List<WorklistItemResponse> getWorklistItems() { return worklistItems; }
    public void setWorklistItems(List<WorklistItemResponse> worklistItems) { this.worklistItems = worklistItems != null ? worklistItems : new ArrayList<>(); }
    public List<ResultLinkResponse> getResultLinks() { return resultLinks; }
    public void setResultLinks(List<ResultLinkResponse> resultLinks) { this.resultLinks = resultLinks != null ? resultLinks : new ArrayList<>(); }
}
