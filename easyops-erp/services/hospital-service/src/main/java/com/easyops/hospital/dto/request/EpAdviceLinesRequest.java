package com.easyops.hospital.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EpAdviceLinesRequest {

    /** Raw count includes duplicates/chips; service caps distinct normalized lines separately (see EpAdviceCatalogService). */
    @Size(max = 128)
    private List<@Size(max = 1000) String> lines = new ArrayList<>();

    public List<String> getLines() {
        return lines == null ? List.of() : lines;
    }
}
