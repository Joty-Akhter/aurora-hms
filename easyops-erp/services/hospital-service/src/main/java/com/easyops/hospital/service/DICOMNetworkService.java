package com.easyops.hospital.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.DimseRSP;
import org.dcm4che3.net.FutureDimseRSP;
import org.dcm4che3.net.InputStreamDataWriter;
import org.dcm4che3.net.PDVInputStream;
import org.dcm4che3.net.Priority;
import org.dcm4che3.net.TransferCapability;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicCStoreSCP;
import org.dcm4che3.net.service.DicomServiceRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DICOM network protocols (C-ECHO, C-STORE, C-FIND, C-MOVE, C-GET).
 * <p>
 * By default the service runs in <strong>explicit stub mode</strong> ({@code dicom.network.stub=true}):
 * {@link #cEcho} and {@link #cStore} return failure without connecting, {@link #cFind} returns an empty list,
 * and {@link #cMove}/{@link #cGet} return stub results. Set {@code dicom.network.stub=false} and configure
 * {@code dicom.network.remote-host} (and enable the network) for live C-ECHO / C-STORE / C-FIND.
 * C-MOVE is an SCU operation in live mode; C-GET uses a local Storage SCP for inbound C-STORE sub-operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DICOMNetworkService {

    private static final int STATUS_SUCCESS = 0x0000;
    private static final int STATUS_PENDING = 0xFF00;
    private static final int STATUS_PENDING_WARNING = 0xFF01;
    /** DICOM Storage (and related) warning status category (e.g. 0xB000 coercion). */
    private static final int STATUS_WARNING_MASK = 0xB000;
    private final DicomCGetIngestionService dicomCGetIngestionService;

    /**
     * C-STORE completed: success (0x0000) or Storage Service warning (0xBxxx) per DICOM Part 7.
     */
    private static boolean isCStoreAcceptableStatus(int status) {
        if (status == STATUS_SUCCESS) {
            return true;
        }
        return (status & 0xF000) == STATUS_WARNING_MASK;
    }

    @Value("${dicom.network.enabled:false}")
    private boolean networkEnabled;

    /**
     * When true (default), live DIMSE operations use deterministic stub responses; no PACS connection is attempted.
     */
    @Value("${dicom.network.stub:true}")
    private boolean explicitStubMode;

    @Value("${dicom.network.ae-title:EHR-SCU}")
    private String aeTitle;

    @Value("${dicom.network.port:11112}")
    private int port;

    @Value("${dicom.network.host:localhost}")
    private String host;

    @Value("${dicom.network.remote-ae-title:PACS}")
    private String remoteAeTitle;

    @Value("${dicom.network.remote-host:}")
    private String remoteHost;

    @Value("${dicom.network.remote-port:104}")
    private int remotePort;

    @Value("${dicom.network.cget.receive-path:./storage/dicom/network-cget}")
    private String cgetReceivePath;

    private ExecutorService executorService;

    @PostConstruct
    public void initialize() {
        if (!networkEnabled) {
            log.info("DICOM network service is disabled (dicom.network.enabled=false)");
            return;
        }
        executorService = Executors.newCachedThreadPool();
        if (explicitStubMode) {
            log.info(
                "DICOM network service: explicit stub mode (dicom.network.stub=true). "
                    + "C-ECHO/C-STORE fail without connecting; C-FIND returns empty; C-MOVE/C-GET return stub responses.");
        } else {
            log.info(
                "DICOM network service: live mode (dicom.network.stub=false). "
                    + "Remote PACS: {}:{} (called AET configurable per request or {})",
                remoteHost,
                remotePort,
                remoteAeTitle);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * C-STORE: Store DICOM object to remote PACS.
     */
    public CStoreResult cStore(File dicomFile, String remoteAeTitleOverride) throws IOException {
        return cStore(dicomFile, remoteAeTitleOverride, null, null);
    }

    /**
     * C-STORE with optional per-request remote host/port override.
     */
    public CStoreResult cStore(
            File dicomFile,
            String remoteAeTitleOverride,
            String remoteHostOverride,
            Integer remotePortOverride) throws IOException {
        requireNetworkEnabled();
        if (explicitStubMode) {
            return stubCStoreFailure("Explicit stub mode (dicom.network.stub=true); C-STORE not sent to PACS.");
        }
        String targetHost = firstNonBlank(remoteHostOverride, remoteHost);
        int targetPort = remotePortOverride != null && remotePortOverride > 0 ? remotePortOverride : remotePort;
        if (isBlank(targetHost)) {
            throw new IllegalStateException(
                "dicom.network.remote-host must be set (or overridden) when dicom.network.stub=false for live C-STORE.");
        }
        String calledAe = firstNonBlank(remoteAeTitleOverride, remoteAeTitle);
        if (isBlank(calledAe)) {
            throw new IllegalStateException(
                "dicom.network.remote-ae-title must be set (or overridden) when dicom.network.stub=false for live C-STORE.");
        }
        if (dicomFile == null || !dicomFile.isFile() || !dicomFile.canRead()) {
            CStoreResult bad = new CStoreResult();
            bad.setSuccess(false);
            bad.setStatus(-1);
            bad.setMessage("DICOM file is missing or not readable");
            return bad;
        }
        String sopClass;
        String transferSyntax;
        try (DicomInputStream dis = new DicomInputStream(dicomFile)) {
            Attributes fmi = dis.readFileMetaInformation();
            if (fmi != null && !fmi.isEmpty()) {
                transferSyntax = fmi.getString(Tag.TransferSyntaxUID, UID.ImplicitVRLittleEndian);
                sopClass = fmi.getString(Tag.MediaStorageSOPClassUID);
            } else {
                transferSyntax = UID.ImplicitVRLittleEndian;
                sopClass = null;
            }
            Attributes ds = dis.readDataset();
            if (sopClass == null || sopClass.isBlank()) {
                sopClass = ds.getString(Tag.SOPClassUID);
            }
        }
        if (sopClass == null || sopClass.isBlank()) {
            CStoreResult bad = new CStoreResult();
            bad.setSuccess(false);
            bad.setStatus(-1);
            bad.setMessage("Could not determine SOP Class UID from DICOM file");
            return bad;
        }

        Device device = null;
        Association assoc = null;
        try {
            device = createScuDevice();
            ApplicationEntity ae = device.getApplicationEntity(aeTitle);
            ae.addTransferCapability(new TransferCapability(
                "C-STORE",
                sopClass,
                TransferCapability.Role.SCU,
                transferSyntax,
                UID.ImplicitVRLittleEndian,
                UID.ExplicitVRLittleEndian));

            Connection localConn = ae.getConnections().get(0);
            Connection remoteConn = new Connection();
            remoteConn.setHostname(targetHost.trim());
            remoteConn.setPort(targetPort);

            AAssociateRQ rq = new AAssociateRQ();
            rq.setCalledAET(calledAe);

            assoc = ae.connect(localConn, remoteConn, rq);
            int messageId = assoc.nextMessageID();
            try (FileInputStream fis = new FileInputStream(dicomFile)) {
                InputStreamDataWriter writer = new InputStreamDataWriter(fis);
                DimseRSP rsp = assoc.cstore(sopClass, transferSyntax, messageId, writer, transferSyntax);
                CStoreResult last = null;
                while (rsp.next()) {
                    Attributes cmd = rsp.getCommand();
                    int status = cmd != null ? cmd.getInt(Tag.Status, -1) : -1;
                    CStoreResult result = new CStoreResult();
                    result.setStatus(status);
                    boolean ok = isCStoreAcceptableStatus(status);
                    result.setSuccess(ok);
                    if (ok) {
                        result.setMessage(
                            status == STATUS_SUCCESS ? "C-STORE success" : "C-STORE warning: 0x" + Integer.toHexString(status));
                    } else {
                        result.setMessage("C-STORE status: 0x" + Integer.toHexString(status));
                    }
                    if (cmd != null) {
                        result.setSopInstanceUID(cmd.getString(Tag.AffectedSOPInstanceUID));
                    }
                    last = result;
                }
                if (last != null) {
                    return last;
                }
            }
            CStoreResult fail = new CStoreResult();
            fail.setSuccess(false);
            fail.setStatus(-1);
            fail.setMessage("No C-STORE response from remote AE");
            return fail;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("C-STORE interrupted", e);
        } catch (Exception e) {
            log.error("C-STORE failed for remote {}:{}", targetHost, targetPort, e);
            CStoreResult fail = new CStoreResult();
            fail.setSuccess(false);
            fail.setStatus(-1);
            fail.setMessage(e.getMessage());
            return fail;
        } finally {
            if (assoc != null) {
                try {
                    assoc.release();
                } catch (Exception e) {
                    log.debug("Association release: {}", e.getMessage());
                }
            }
            if (device != null) {
                try {
                    device.unbindConnections();
                } catch (Exception e) {
                    log.debug("Device unbind: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * C-ECHO: verify connectivity and DIMSE with the remote AE (Verification SOP Class).
     */
    public CEchoResult cEcho(String remoteAeTitleOverride, String remoteHostOverride, Integer remotePortOverride) throws IOException {
        requireNetworkEnabled();
        if (explicitStubMode) {
            CEchoResult stub = new CEchoResult();
            stub.setSuccess(false);
            stub.setStatus(-1);
            stub.setMessage("C-ECHO stub mode (dicom.network.stub=true): request not sent.");
            return stub;
        }
        String targetHost = firstNonBlank(remoteHostOverride, remoteHost);
        int targetPort = remotePortOverride != null && remotePortOverride > 0 ? remotePortOverride : remotePort;
        if (isBlank(targetHost)) {
            throw new IllegalStateException(
                "dicom.network.remote-host must be set (or overridden) when dicom.network.stub=false for live C-ECHO.");
        }
        String calledAe = firstNonBlank(remoteAeTitleOverride, remoteAeTitle);
        if (isBlank(calledAe)) {
            throw new IllegalStateException(
                "dicom.network.remote-ae-title must be set (or overridden) when dicom.network.stub=false for live C-ECHO.");
        }

        Device device = null;
        Association assoc = null;
        try {
            device = createScuDevice();
            ApplicationEntity ae = device.getApplicationEntity(aeTitle);

            Connection localConn = ae.getConnections().get(0);
            Connection remoteConn = new Connection();
            remoteConn.setHostname(targetHost.trim());
            remoteConn.setPort(targetPort);

            AAssociateRQ rq = new AAssociateRQ();
            rq.setCalledAET(calledAe);

            assoc = ae.connect(localConn, remoteConn, rq);
            DimseRSP rsp = assoc.cecho();
            CEchoResult last = null;
            while (rsp.next()) {
                Attributes cmd = rsp.getCommand();
                int status = cmd != null ? cmd.getInt(Tag.Status, -1) : -1;
                CEchoResult result = new CEchoResult();
                result.setStatus(status);
                result.setSuccess(status == STATUS_SUCCESS);
                result.setMessage(status == STATUS_SUCCESS ? "C-ECHO success" : "C-ECHO status: 0x" + Integer.toHexString(status));
                last = result;
            }
            if (last == null) {
                CEchoResult fail = new CEchoResult();
                fail.setSuccess(false);
                fail.setStatus(-1);
                fail.setMessage("No C-ECHO response from remote AE");
                return fail;
            }
            return last;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("C-ECHO interrupted", e);
        } catch (Exception e) {
            log.error("C-ECHO failed for remote {}:{}", targetHost, targetPort, e);
            CEchoResult fail = new CEchoResult();
            fail.setSuccess(false);
            fail.setStatus(-1);
            fail.setMessage(e.getMessage());
            return fail;
        } finally {
            if (assoc != null) {
                try {
                    assoc.release();
                } catch (Exception e) {
                    log.debug("Association release: {}", e.getMessage());
                }
            }
            if (device != null) {
                try {
                    device.unbindConnections();
                } catch (Exception e) {
                    log.debug("Device unbind: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * C-FIND: Query DICOM objects from remote PACS (Study Root, STUDY level).
     */
    public List<CFindResult> cFind(CFindQuery query) throws IOException {
        requireNetworkEnabled();
        if (explicitStubMode) {
            log.debug("C-FIND stub: returning empty list (dicom.network.stub=true)");
            return new ArrayList<>();
        }
        if (query == null) {
            throw new IllegalArgumentException("C-FIND query is required.");
        }
        boolean hasCriterion = firstNonBlank(
            query.getPatientId(),
            query.getStudyInstanceUID(),
            query.getAccessionNumber(),
            query.getStudyDate(),
            query.getModality()) != null;
        if (!hasCriterion) {
            throw new IllegalArgumentException("C-FIND requires at least one query criterion.");
        }

        String calledAe = firstNonBlank(query.getRemoteAeTitle(), remoteAeTitle);
        if (isBlank(calledAe)) {
            throw new IllegalStateException(
                "Set dicom.network.remote-ae-title or CFindQuery.remoteAeTitle when dicom.network.stub=false for live C-FIND.");
        }

        Attributes keys = new Attributes();
        keys.setString(Tag.QueryRetrieveLevel, VR.CS, "STUDY");
        keys.setNull(Tag.PatientName, VR.PN);
        keys.setNull(Tag.PatientID, VR.LO);
        keys.setNull(Tag.StudyInstanceUID, VR.UI);
        keys.setNull(Tag.StudyDate, VR.DA);
        keys.setNull(Tag.AccessionNumber, VR.SH);
        keys.setNull(Tag.StudyDescription, VR.LO);
        keys.setNull(Tag.ModalitiesInStudy, VR.CS);

        if (!isBlank(query.getPatientId())) {
            keys.setString(Tag.PatientID, VR.LO, query.getPatientId());
        }
        if (!isBlank(query.getStudyInstanceUID())) {
            keys.setString(Tag.StudyInstanceUID, VR.UI, query.getStudyInstanceUID());
        }
        if (!isBlank(query.getAccessionNumber())) {
            keys.setString(Tag.AccessionNumber, VR.SH, query.getAccessionNumber());
        }
        if (!isBlank(query.getStudyDate())) {
            keys.setString(Tag.StudyDate, VR.DA, query.getStudyDate());
        }
        if (!isBlank(query.getModality())) {
            keys.setString(Tag.ModalitiesInStudy, VR.CS, query.getModality());
        }

        String targetHost = firstNonBlank(query.getRemoteHost(), remoteHost);
        if (isBlank(targetHost)) {
            throw new IllegalStateException(
                "Set dicom.network.remote-host or CFindQuery.remoteHost when dicom.network.stub=false for live C-FIND.");
        }

        Device device = null;
        Association assoc = null;
        try {
            device = createScuDevice();
            ApplicationEntity ae = device.getApplicationEntity(aeTitle);
            ae.addTransferCapability(new TransferCapability(
                "C-FIND",
                UID.StudyRootQueryRetrieveInformationModelFind,
                TransferCapability.Role.SCU,
                UID.ImplicitVRLittleEndian,
                UID.ExplicitVRLittleEndian));

            Connection localConn = ae.getConnections().get(0);
            Connection remoteConn = new Connection();
            remoteConn.setHostname(targetHost.trim());
            int portOverride = query.getRemotePort();
            remoteConn.setPort(portOverride > 0 ? portOverride : remotePort);

            AAssociateRQ rq = new AAssociateRQ();
            rq.setCalledAET(calledAe);

            List<CFindResult> out = new ArrayList<>();
            assoc = ae.connect(localConn, remoteConn, rq);
            int messageId = assoc.nextMessageID();
            String ts = UID.ImplicitVRLittleEndian;
            DimseRSP rsp = assoc.cfind(
                UID.StudyRootQueryRetrieveInformationModelFind,
                Priority.NORMAL,
                keys,
                ts,
                messageId);

            boolean sawSuccess = false;
            while (rsp.next()) {
                Attributes cmd = rsp.getCommand();
                if (cmd == null) {
                    continue;
                }
                int status = cmd.getInt(Tag.Status, -1);
                if (status == STATUS_PENDING || status == STATUS_PENDING_WARNING) {
                    Attributes ds = rsp.getDataset();
                    if (ds != null) {
                        out.add(mapFindDataset(ds));
                    }
                } else if (status == STATUS_SUCCESS) {
                    sawSuccess = true;
                    break;
                } else {
                    throw new IOException(
                        "C-FIND failed with DIMSE status 0x" + Integer.toHexString(status));
                }
            }
            if (!sawSuccess) {
                throw new IOException("C-FIND completed without success status from remote AE");
            }
            return out;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("C-FIND interrupted", e);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            log.error("C-FIND failed for remote {}:{}", targetHost, remotePort, e);
            throw new IOException("C-FIND failed: " + e.getMessage(), e);
        } finally {
            if (assoc != null) {
                try {
                    assoc.release();
                } catch (Exception e) {
                    log.debug("Association release: {}", e.getMessage());
                }
            }
            if (device != null) {
                try {
                    device.unbindConnections();
                } catch (Exception e) {
                    log.debug("Device unbind: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * C-MOVE: Move matching instances to destination AE.
     */
    public CMoveResult cMove(CMoveRequest request) throws IOException {
        requireNetworkEnabled();
        if (explicitStubMode) {
            CMoveResult result = new CMoveResult();
            result.setSuccess(false);
            result.setStatus(-1);
            result.setMessage("C-MOVE stub mode (dicom.network.stub=true): request not sent.");
            return result;
        }
        if (request == null || isBlank(request.getStudyInstanceUID()) || isBlank(request.getDestinationAeTitle())) {
            throw new IllegalArgumentException("C-MOVE requires studyInstanceUID and destinationAeTitle.");
        }
        String targetHost = firstNonBlank(request.getRemoteHost(), remoteHost);
        if (isBlank(targetHost)) {
            throw new IllegalStateException(
                "Set dicom.network.remote-host or CMoveRequest.remoteHost when dicom.network.stub=false for live C-MOVE.");
        }
        String calledAe = firstNonBlank(request.getRemoteAeTitle(), remoteAeTitle);
        if (isBlank(calledAe)) {
            throw new IllegalStateException(
                "Set dicom.network.remote-ae-title or CMoveRequest.remoteAeTitle when dicom.network.stub=false for live C-MOVE.");
        }
        int targetPort = request.getRemotePort() > 0 ? request.getRemotePort() : remotePort;

        Attributes keys = new Attributes();
        keys.setString(Tag.QueryRetrieveLevel, VR.CS, "STUDY");
        keys.setString(Tag.StudyInstanceUID, VR.UI, request.getStudyInstanceUID().trim());

        Device device = null;
        Association assoc = null;
        CMoveResult result = new CMoveResult();
        try {
            device = createScuDevice();
            ApplicationEntity ae = device.getApplicationEntity(aeTitle);
            ae.addTransferCapability(new TransferCapability(
                "C-MOVE",
                UID.StudyRootQueryRetrieveInformationModelMove,
                TransferCapability.Role.SCU,
                UID.ImplicitVRLittleEndian,
                UID.ExplicitVRLittleEndian));

            Connection localConn = ae.getConnections().get(0);
            Connection remoteConn = new Connection();
            remoteConn.setHostname(targetHost.trim());
            remoteConn.setPort(targetPort);

            AAssociateRQ rq = new AAssociateRQ();
            rq.setCalledAET(calledAe);

            assoc = ae.connect(localConn, remoteConn, rq);
            int messageId = assoc.nextMessageID();
            FutureDimseRSP rsp = new FutureDimseRSP(messageId);
            assoc.cmove(
                UID.StudyRootQueryRetrieveInformationModelMove,
                Priority.NORMAL,
                keys,
                UID.ImplicitVRLittleEndian,
                request.getDestinationAeTitle().trim(),
                rsp);

            boolean sawSuccess = false;
            boolean sawDimseFailure = false;
            while (rsp.next()) {
                Attributes cmd = rsp.getCommand();
                if (cmd == null) {
                    continue;
                }
                int status = cmd.getInt(Tag.Status, -1);
                result.setStatus(status);
                result.setNumberOfObjects(cmd.getInt(Tag.NumberOfCompletedSuboperations, result.getNumberOfObjects()));
                result.setRemaining(cmd.getInt(Tag.NumberOfRemainingSuboperations, result.getRemaining()));
                result.setFailed(cmd.getInt(Tag.NumberOfFailedSuboperations, result.getFailed()));
                result.setWarning(cmd.getInt(Tag.NumberOfWarningSuboperations, result.getWarning()));

                if (status == STATUS_PENDING || status == STATUS_PENDING_WARNING) {
                    result.setMessage("C-MOVE in progress");
                    continue;
                }
                if (status == STATUS_SUCCESS) {
                    sawSuccess = true;
                    result.setSuccess(true);
                    result.setMessage("C-MOVE success");
                    break;
                }
                sawDimseFailure = true;
                result.setSuccess(false);
                result.setMessage("C-MOVE status: 0x" + Integer.toHexString(status));
                break;
            }
            if (!sawSuccess && !sawDimseFailure
                && (isBlank(result.getMessage()) || "C-MOVE in progress".equals(result.getMessage()))) {
                throw new IOException(
                    isBlank(result.getMessage())
                        ? "No C-MOVE response from remote AE"
                        : "C-MOVE completed without success status from remote AE");
            }
            if (isBlank(result.getMessage())) {
                result.setSuccess(false);
                result.setStatus(-1);
                result.setMessage("No C-MOVE response from remote AE");
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("C-MOVE interrupted", e);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            log.error("C-MOVE failed for remote {}:{}", targetHost, targetPort, e);
            throw new IOException("C-MOVE failed: " + e.getMessage(), e);
        } finally {
            if (assoc != null) {
                try {
                    assoc.release();
                } catch (Exception e) {
                    log.debug("Association release: {}", e.getMessage());
                }
            }
            if (device != null) {
                try {
                    device.unbindConnections();
                } catch (Exception e) {
                    log.debug("Device unbind: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * C-GET: Retrieve matching instances over the same association (includes inbound C-STORE sub-operations).
     */
    public CGetResult cGet(CGetRequest request) throws IOException {
        requireNetworkEnabled();
        if (explicitStubMode) {
            CGetResult result = new CGetResult();
            result.setSuccess(false);
            result.setStatus(-1);
            result.setMessage("C-GET stub mode (dicom.network.stub=true): request not sent.");
            return result;
        }
        if (request == null || isBlank(request.getStudyInstanceUID())) {
            throw new IllegalArgumentException("C-GET requires studyInstanceUID.");
        }
        String targetHost = firstNonBlank(request.getRemoteHost(), remoteHost);
        if (isBlank(targetHost)) {
            throw new IllegalStateException(
                "Set dicom.network.remote-host or CGetRequest.remoteHost when dicom.network.stub=false for live C-GET.");
        }
        String calledAe = firstNonBlank(request.getRemoteAeTitle(), remoteAeTitle);
        if (isBlank(calledAe)) {
            throw new IllegalStateException(
                "Set dicom.network.remote-ae-title or CGetRequest.remoteAeTitle when dicom.network.stub=false for live C-GET.");
        }
        int targetPort = request.getRemotePort() > 0 ? request.getRemotePort() : remotePort;

        Attributes keys = new Attributes();
        if (!isBlank(request.getSeriesInstanceUID())) {
            keys.setString(Tag.QueryRetrieveLevel, VR.CS, "SERIES");
            keys.setString(Tag.SeriesInstanceUID, VR.UI, request.getSeriesInstanceUID().trim());
        } else {
            keys.setString(Tag.QueryRetrieveLevel, VR.CS, "STUDY");
        }
        keys.setString(Tag.StudyInstanceUID, VR.UI, request.getStudyInstanceUID().trim());

        AtomicInteger receivedInstances = new AtomicInteger(0);
        AtomicInteger importedAttachmentCount = new AtomicInteger(0);
        AtomicInteger skippedDuplicateCount = new AtomicInteger(0);
        AtomicInteger skippedNoMatchingStudyCount = new AtomicInteger(0);
        File receiveDir = new File(cgetReceivePath);
        Device device = null;
        Association assoc = null;
        CGetResult result = new CGetResult();
        try {
            device = createCGetScuScpDevice(
                receiveDir,
                receivedInstances,
                importedAttachmentCount,
                skippedDuplicateCount,
                skippedNoMatchingStudyCount,
                request.getStudyInstanceUID().trim());
            ApplicationEntity ae = device.getApplicationEntity(aeTitle);
            ae.addTransferCapability(new TransferCapability(
                "C-GET",
                UID.StudyRootQueryRetrieveInformationModelGet,
                TransferCapability.Role.SCU,
                UID.ImplicitVRLittleEndian,
                UID.ExplicitVRLittleEndian));

            Connection localConn = ae.getConnections().get(0);
            Connection remoteConn = new Connection();
            remoteConn.setHostname(targetHost.trim());
            remoteConn.setPort(targetPort);

            AAssociateRQ rq = new AAssociateRQ();
            rq.setCalledAET(calledAe);

            assoc = ae.connect(localConn, remoteConn, rq);
            int messageId = assoc.nextMessageID();
            FutureDimseRSP rsp = new FutureDimseRSP(messageId);
            assoc.cget(
                UID.StudyRootQueryRetrieveInformationModelGet,
                Priority.NORMAL,
                keys,
                UID.ImplicitVRLittleEndian,
                rsp);

            boolean sawSuccess = false;
            boolean sawDimseFailure = false;
            while (rsp.next()) {
                Attributes cmd = rsp.getCommand();
                if (cmd == null) {
                    continue;
                }
                int status = cmd.getInt(Tag.Status, -1);
                result.setStatus(status);
                result.setNumberOfObjects(cmd.getInt(Tag.NumberOfCompletedSuboperations, receivedInstances.get()));
                result.setRemaining(cmd.getInt(Tag.NumberOfRemainingSuboperations, result.getRemaining()));
                result.setFailed(cmd.getInt(Tag.NumberOfFailedSuboperations, result.getFailed()));
                result.setWarning(cmd.getInt(Tag.NumberOfWarningSuboperations, result.getWarning()));

                if (status == STATUS_PENDING || status == STATUS_PENDING_WARNING) {
                    result.setMessage("C-GET in progress");
                    continue;
                }
                if (status == STATUS_SUCCESS) {
                    sawSuccess = true;
                    result.setSuccess(true);
                    result.setNumberOfObjects(Math.max(result.getNumberOfObjects(), receivedInstances.get()));
                    result.setReceiveDirectory(receiveDir.getAbsolutePath());
                    result.setMessage("C-GET success");
                    break;
                }
                sawDimseFailure = true;
                result.setSuccess(false);
                result.setReceiveDirectory(receiveDir.getAbsolutePath());
                result.setMessage("C-GET status: 0x" + Integer.toHexString(status));
                break;
            }
            if (!sawSuccess && !sawDimseFailure
                && (isBlank(result.getMessage()) || "C-GET in progress".equals(result.getMessage()))) {
                throw new IOException(
                    isBlank(result.getMessage())
                        ? "No C-GET response from remote AE"
                        : "C-GET completed without success status from remote AE");
            }
            if (isBlank(result.getMessage())) {
                result.setSuccess(false);
                result.setStatus(-1);
                result.setReceiveDirectory(receiveDir.getAbsolutePath());
                result.setMessage("No C-GET response from remote AE");
            } else if (isBlank(result.getReceiveDirectory())) {
                result.setReceiveDirectory(receiveDir.getAbsolutePath());
            }
            result.setImportedAttachmentCount(importedAttachmentCount.get());
            result.setSkippedDuplicateCount(skippedDuplicateCount.get());
            result.setSkippedNoMatchingStudyCount(skippedNoMatchingStudyCount.get());
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("C-GET interrupted", e);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            log.error("C-GET failed for remote {}:{}", targetHost, targetPort, e);
            throw new IOException("C-GET failed: " + e.getMessage(), e);
        } finally {
            if (assoc != null) {
                try {
                    assoc.release();
                } catch (Exception e) {
                    log.debug("Association release: {}", e.getMessage());
                }
            }
            if (device != null) {
                try {
                    device.unbindConnections();
                } catch (Exception e) {
                    log.debug("Device unbind: {}", e.getMessage());
                }
            }
        }
    }

    private void requireNetworkEnabled() {
        if (!networkEnabled) {
            throw new IllegalStateException("DICOM network service is disabled (dicom.network.enabled=false)");
        }
    }

    private static CFindResult mapFindDataset(Attributes ds) {
        CFindResult r = new CFindResult();
        r.setPatientId(ds.getString(Tag.PatientID));
        r.setPatientName(ds.getString(Tag.PatientName));
        r.setStudyInstanceUID(ds.getString(Tag.StudyInstanceUID));
        r.setStudyDate(ds.getString(Tag.StudyDate));
        r.setStudyDescription(ds.getString(Tag.StudyDescription));
        r.setAccessionNumber(ds.getString(Tag.AccessionNumber));
        r.setModality(ds.getString(Tag.ModalitiesInStudy));
        return r;
    }

    private Device createScuDevice() {
        Device device = new Device("EasyOpsHospitalDicomScu");
        device.setExecutor(executorService != null ? executorService : Executors.newSingleThreadExecutor());

        ApplicationEntity ae = new ApplicationEntity(aeTitle);
        ae.setAssociationInitiator(true);
        ae.setAssociationAcceptor(false);

        Connection localConn = new Connection();
        localConn.setHostname(host);
        localConn.setPort(0);

        device.addConnection(localConn);
        device.addApplicationEntity(ae);
        ae.addConnection(localConn);

        ae.addTransferCapability(new TransferCapability(
            "C-ECHO",
            UID.Verification,
            TransferCapability.Role.SCU,
            UID.ImplicitVRLittleEndian));

        try {
            device.bindConnections();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to bind local DICOM connection for SCU", e);
        }
        return device;
    }

    private Device createCGetScuScpDevice(
            File receiveDir,
            AtomicInteger receivedInstances,
            AtomicInteger importedAttachmentCount,
            AtomicInteger skippedDuplicateCount,
            AtomicInteger skippedNoMatchingStudyCount,
            String requestedStudyInstanceUid) {
        if (!receiveDir.exists() && !receiveDir.mkdirs()) {
            throw new IllegalStateException("Failed to create C-GET receive directory: " + receiveDir.getAbsolutePath());
        }
        Device device = new Device("EasyOpsHospitalDicomCGetScuScp");
        device.setExecutor(executorService != null ? executorService : Executors.newSingleThreadExecutor());

        ApplicationEntity ae = new ApplicationEntity(aeTitle);
        ae.setAssociationInitiator(true);
        ae.setAssociationAcceptor(true);

        Connection localConn = new Connection();
        localConn.setHostname(host);
        // Use an ephemeral local port to avoid port conflicts on repeated/concurrent C-GET operations.
        localConn.setPort(0);

        device.addConnection(localConn);
        device.addApplicationEntity(ae);
        ae.addConnection(localConn);

        // Accept inbound C-STORE sub-operations for C-GET.
        ae.addTransferCapability(new TransferCapability(
            "C-STORE-SCP",
            "*",
            TransferCapability.Role.SCP,
            UID.ImplicitVRLittleEndian,
            UID.ExplicitVRLittleEndian));

        DicomServiceRegistry serviceRegistry = new DicomServiceRegistry();
        serviceRegistry.addDicomService(new BasicCStoreSCP("*") {
            @Override
            protected void store(
                    Association as,
                    PresentationContext pc,
                    Attributes rq,
                    PDVInputStream data,
                    Attributes rsp) throws IOException {
                String iuid = rq.getString(Tag.AffectedSOPInstanceUID);
                String cuid = rq.getString(Tag.AffectedSOPClassUID);
                String tsuid = pc.getTransferSyntax();
                if (isBlank(iuid) || isBlank(cuid)) {
                    throw new IOException("Missing SOP identifiers in inbound C-STORE request.");
                }
                Path receivePath = receiveDir.toPath();
                Path outPath = receivePath.resolve(iuid.replace('.', '_') + ".dcm");
                Path tmp = Files.createTempFile(receivePath, "cget-in-", ".part");
                try {
                    try (DicomOutputStream dos = new DicomOutputStream(tmp.toFile())) {
                        Attributes fmi = as.createFileMetaInformation(iuid, cuid, tsuid);
                        dos.writeFileMetaInformation(fmi);
                        data.copyTo(dos);
                    }
                    try {
                        Files.move(tmp, outPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    } catch (AtomicMoveNotSupportedException e) {
                        Files.move(tmp, outPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } finally {
                    try {
                        Files.deleteIfExists(tmp);
                    } catch (IOException cleanup) {
                        log.debug("Could not remove temp C-GET receive file {}", tmp, cleanup);
                    }
                }
                File outFile = outPath.toFile();
                receivedInstances.incrementAndGet();
                persistReceivedCGetInstance(
                    outFile,
                    requestedStudyInstanceUid,
                    iuid,
                    importedAttachmentCount,
                    skippedDuplicateCount,
                    skippedNoMatchingStudyCount);
            }
        });
        device.setDimseRQHandler(serviceRegistry);

        ae.addTransferCapability(new TransferCapability(
            "C-ECHO",
            UID.Verification,
            TransferCapability.Role.SCU,
            UID.ImplicitVRLittleEndian));

        try {
            device.bindConnections();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to bind local DICOM connection for C-GET SCU/SCP", e);
        }
        return device;
    }

    private static CStoreResult stubCStoreFailure(String message) {
        log.info("C-STORE stub: {}", message);
        CStoreResult result = new CStoreResult();
        result.setSuccess(false);
        result.setStatus(-1);
        result.setMessage(message);
        result.setSopInstanceUID(null);
        return result;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    private void persistReceivedCGetInstance(
            File receivedFile,
            String requestedStudyInstanceUid,
            String sopInstanceUid,
            AtomicInteger importedAttachmentCount,
            AtomicInteger skippedDuplicateCount,
            AtomicInteger skippedNoMatchingStudyCount) {
        try {
            DicomCGetIngestionService.Outcome outcome =
                dicomCGetIngestionService.ingestReceivedInstance(receivedFile, requestedStudyInstanceUid, sopInstanceUid);
            switch (outcome) {
                case IMPORTED -> importedAttachmentCount.incrementAndGet();
                case SKIPPED_DUPLICATE -> skippedDuplicateCount.incrementAndGet();
                case SKIPPED_NO_MATCHING_STUDY -> skippedNoMatchingStudyCount.incrementAndGet();
            }
        } catch (Exception e) {
            log.warn("Failed to persist C-GET received instance {}", sopInstanceUid, e);
        }
    }

    @Data
    public static class CStoreResult {
        private boolean success;
        private int status;
        private String message;
        private String sopInstanceUID;
    }

    @Data
    public static class CEchoResult {
        private boolean success;
        private int status;
        private String message;
    }

    @Data
    public static class CFindQuery {
        private String patientId;
        private String studyInstanceUID;
        private String accessionNumber;
        private String studyDate;
        private String modality;
        private String remoteAeTitle;
        private String remoteHost;
        private int remotePort;
    }

    @Data
    public static class CFindResult {
        private String patientId;
        private String patientName;
        private String studyInstanceUID;
        private String studyDate;
        private String studyDescription;
        private String accessionNumber;
        private String modality;
    }

    @Data
    public static class CMoveRequest {
        private String studyInstanceUID;
        private String destinationAeTitle;
        private String remoteAeTitle;
        private String remoteHost;
        private int remotePort;
    }

    @Data
    public static class CMoveResult {
        private boolean success;
        private int status;
        private int numberOfObjects;
        private int remaining = -1;
        private int failed = -1;
        private int warning = -1;
        private String message;
    }

    @Data
    public static class CGetRequest {
        private String studyInstanceUID;
        private String seriesInstanceUID;
        private String remoteAeTitle;
        private String remoteHost;
        private int remotePort;
    }

    @Data
    public static class CGetResult {
        private boolean success;
        private int status;
        private int numberOfObjects;
        private int remaining = -1;
        private int failed = -1;
        private int warning = -1;
        private String receiveDirectory;
        private String message;
        private int importedAttachmentCount;
        private int skippedDuplicateCount;
        private int skippedNoMatchingStudyCount;
    }
}
