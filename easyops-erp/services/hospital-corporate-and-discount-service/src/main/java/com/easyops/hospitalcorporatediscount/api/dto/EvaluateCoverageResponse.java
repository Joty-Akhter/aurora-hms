package com.easyops.hospitalcorporatediscount.api.dto;

import java.math.BigDecimal;
import java.util.List;

public class EvaluateCoverageResponse {

    private List<EvaluateCoverageItemResponse> items;
    private BigDecimal totalCovered;
    private BigDecimal totalPatientShare;
    private BigDecimal totalCorporateShare;

    public List<EvaluateCoverageItemResponse> getItems() { return items; }
    public void setItems(List<EvaluateCoverageItemResponse> items) { this.items = items; }
    public BigDecimal getTotalCovered() { return totalCovered; }
    public void setTotalCovered(BigDecimal totalCovered) { this.totalCovered = totalCovered; }
    public BigDecimal getTotalPatientShare() { return totalPatientShare; }
    public void setTotalPatientShare(BigDecimal totalPatientShare) { this.totalPatientShare = totalPatientShare; }
    public BigDecimal getTotalCorporateShare() { return totalCorporateShare; }
    public void setTotalCorporateShare(BigDecimal totalCorporateShare) { this.totalCorporateShare = totalCorporateShare; }
}
