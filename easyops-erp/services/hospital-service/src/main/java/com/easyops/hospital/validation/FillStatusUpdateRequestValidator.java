package com.easyops.hospital.validation;

import com.easyops.hospital.dto.request.FillStatusUpdateRequest;
import com.easyops.hospital.entity.PrescriptionTransmission.FillStatus;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FillStatusUpdateRequestValidator
        implements ConstraintValidator<ValidFillStatusRequest, FillStatusUpdateRequest> {

    @Override
    public boolean isValid(FillStatusUpdateRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true; // @NotNull on the controller parameter handles null body
        }

        // fillStatusDate null is handled by @NotNull on the field; no duplicate check here.
        // fillStatus null is handled by @NotNull on the field; skip conditional checks when absent.

        FillStatus status = request.getFillStatus();
        if (status == null) {
            return true;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        if ((status == FillStatus.FILLED || status == FillStatus.PARTIALLY_FILLED)
                && request.getFilledDate() == null) {
            context.buildConstraintViolationWithTemplate(
                            "filledDate is required when fillStatus is " + status)
                    .addPropertyNode("filledDate")
                    .addConstraintViolation();
            valid = false;
        }

        if (status == FillStatus.PICKED_UP && request.getPickedUpDate() == null) {
            context.buildConstraintViolationWithTemplate(
                            "pickedUpDate is required when fillStatus is PICKED_UP")
                    .addPropertyNode("pickedUpDate")
                    .addConstraintViolation();
            valid = false;
        }

        if ((status == FillStatus.CANCELLED || status == FillStatus.REJECTED)
                && (request.getCancellationReason() == null
                        || request.getCancellationReason().isBlank())) {
            context.buildConstraintViolationWithTemplate(
                            "cancellationReason is required when fillStatus is " + status)
                    .addPropertyNode("cancellationReason")
                    .addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}
