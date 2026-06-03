package com.easyops.hospitalpharmacy.service;

import com.easyops.hospitalpharmacy.entity.DispenseLine;
import com.easyops.hospitalpharmacy.entity.DispenseOrder;
import com.easyops.hospitalpharmacy.entity.PharmacyReceiptReprintAudit;
import com.easyops.hospitalpharmacy.repository.DispenseLineRepository;
import com.easyops.hospitalpharmacy.repository.DispenseOrderRepository;
import com.easyops.hospitalpharmacy.repository.PharmacyReceiptReprintAuditRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Phase P3 WS-F — printable dispense receipt (PDF).
 */
@Service
@RequiredArgsConstructor
public class DispenseReceiptService {

    private final DispenseOrderRepository dispenseOrderRepository;
    private final DispenseLineRepository dispenseLineRepository;
    private final PharmacyReceiptReprintAuditRepository pharmacyReceiptReprintAuditRepository;

    @Transactional(readOnly = true)
    public byte[] buildReceiptPdf(UUID orderId) {
        DispenseOrder order = dispenseOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Dispense order not found: " + orderId));
        List<DispenseLine> lines = dispenseLineRepository.findByDispenseOrder(order);

        try {
            Document document = new Document();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 11);
            document.add(new Paragraph("Dispense receipt", titleFont));
            document.add(new Paragraph(" ", body));
            document.add(new Paragraph("Order: " + order.getId(), body));
            document.add(new Paragraph("Pharmacy: " + order.getPharmacyLocation().getName(), body));
            if (order.getPatientId() != null) {
                document.add(new Paragraph("Patient id: " + order.getPatientId(), body));
            }
            if (order.getPrescriptionId() != null) {
                document.add(new Paragraph("Prescription id: " + order.getPrescriptionId(), body));
            }
            if (order.getPaperPrescriptionRef() != null && !order.getPaperPrescriptionRef().isBlank()) {
                document.add(new Paragraph("Paper Rx ref: " + order.getPaperPrescriptionRef(), body));
            }
            if (order.getPrescriptionImageAttachmentId() != null) {
                document.add(new Paragraph("Prescription image (attachment id): " + order.getPrescriptionImageAttachmentId(), body));
            }
            if (order.getExternalValidationStatus() != null) {
                document.add(new Paragraph("External validation: " + order.getExternalValidationStatus().name(), body));
            }
            document.add(new Paragraph("Printed: " + OffsetDateTime.now(), body));
            document.add(new Paragraph(
                    "Amounts: per billing integration when charges are posted (see billable-items / billing).",
                    body));
            document.add(new Paragraph(" ", body));

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.addCell("Drug");
            table.addCell("Batch");
            table.addCell("Qty");
            table.addCell("Status");
            table.addCell("Note");
            for (DispenseLine line : lines) {
                table.addCell(safe(line.getDrug().getGenericName()));
                table.addCell(safe(line.getBatchNumber()));
                table.addCell(qty(line.getQuantityDispensed()));
                table.addCell(line.getStatus() != null ? line.getStatus().name() : "");
                String note = line.getOverrideReasonCode() != null ? line.getOverrideReasonCode()
                        : (line.getFormularyOverrideReason() != null ? line.getFormularyOverrideReason() : "");
                table.addCell(safe(note));
            }
            document.add(table);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build receipt PDF", e);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String qty(BigDecimal q) {
        return q == null ? "" : q.toPlainString();
    }

    @Transactional
    public void recordReceiptPrinted(UUID orderId, UUID userId) {
        DispenseOrder order = dispenseOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Dispense order not found: " + orderId));
        OffsetDateTime since = OffsetDateTime.now().minusMinutes(1);
        boolean duplicate = pharmacyReceiptReprintAuditRepository.existsByDispenseOrder_IdAndUserIdAndPrintedAtAfter(
                orderId, userId, since);
        PharmacyReceiptReprintAudit row = PharmacyReceiptReprintAudit.builder()
                .id(UUID.randomUUID())
                .dispenseOrder(order)
                .userId(userId)
                .duplicateOfPrevious(duplicate)
                .build();
        pharmacyReceiptReprintAuditRepository.save(row);
    }
}
