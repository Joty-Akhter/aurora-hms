package com.easyops.hospitalscheduling.api.dto;

import java.time.LocalDate;
import java.util.List;

public class ExpectedAdmissionsResponse {

    private List<ExpectedAdmissionsItem> items;

    public List<ExpectedAdmissionsItem> getItems() { return items; }
    public void setItems(List<ExpectedAdmissionsItem> items) { this.items = items; }

    public static class ExpectedAdmissionsItem {
        private LocalDate date;
        private long count;
        private String wardOrBedClass;

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
        public String getWardOrBedClass() { return wardOrBedClass; }
        public void setWardOrBedClass(String wardOrBedClass) { this.wardOrBedClass = wardOrBedClass; }
    }
}
