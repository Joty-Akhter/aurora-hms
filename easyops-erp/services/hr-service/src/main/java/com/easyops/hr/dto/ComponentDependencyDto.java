package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SC-49: Component dependency report – component references another (base component or formula ref).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComponentDependencyDto {
    /** Component that has the dependency (e.g. HRA). */
    private String componentCode;
    private String componentName;
    /** BASE_COMPONENT or FORMULA_REF. */
    private String dependencyType;
    /** Referenced component code (e.g. BASIC). */
    private String referencedCode;
    /** Referenced component name for display. */
    private String referencedName;
}
