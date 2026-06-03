import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  IconButton,
  Box,
  ImageList,
  ImageListItem,
  ImageListItemBar,
  CircularProgress,
  Typography,
  Button,
  Toolbar,
  Dialog as MetadataDialog,
  DialogTitle as MetadataDialogTitle,
  DialogContent as MetadataDialogContent,
  DialogActions as MetadataDialogActions,
  Grid,
  Chip,
} from '@mui/material';
import {
  Close as CloseIcon,
  ZoomIn as ZoomInIcon,
  ZoomOut as ZoomOutIcon,
  RotateRight as RotateIcon,
  Download as DownloadIcon,
  Info as InfoIcon,
  NavigateBefore as PrevIcon,
  NavigateNext as NextIcon,
  Refresh as ResetIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import hospitalService, { DICOMImage, DICOMMetadata } from '../../services/hospitalService';

import cornerstone from 'cornerstone-core';
import cornerstoneTools from 'cornerstone-tools';
import cornerstoneMath from 'cornerstone-math';
import cornerstoneWADOImageLoader from 'cornerstone-wado-image-loader';
import dicomParser from 'dicom-parser';
import Hammer from 'hammerjs';

type CornerstoneViewport = {
  scale?: number;
  rotation?: number;
  voi?: {
    windowWidth?: number;
    windowCenter?: number;
  };
};

let csInitialized = false;

const initCornerstone = () => {
  if (csInitialized) return;

  (cornerstoneWADOImageLoader as any).external.cornerstone = cornerstone;
  (cornerstoneWADOImageLoader as any).external.dicomParser = dicomParser;

  (cornerstoneTools as any).external.cornerstone = cornerstone;
  (cornerstoneTools as any).external.cornerstoneMath = cornerstoneMath;
  (cornerstoneTools as any).external.Hammer = Hammer;

  (cornerstoneWADOImageLoader as any).webWorkerManager?.initialize({
    maxWebWorkers: Math.min(4, navigator.hardwareConcurrency || 1),
    startWebWorkersOnDemand: true,
    taskConfiguration: {
      decodeTask: {
        initializeCodecsOnStartup: false,
        usePDFJS: false,
        strict: false,
      },
    },
  });

  cornerstoneTools.init({
    showSVGCursors: true,
    globalToolSyncEnabled: false,
  });

  cornerstoneTools.addTool(cornerstoneTools.WwwcTool);
  cornerstoneTools.addTool(cornerstoneTools.PanTool);
  cornerstoneTools.addTool(cornerstoneTools.ZoomTool, {
    configuration: {
      invert: false,
      preventZoomOutsideImage: false,
      minScale: 0.1,
      maxScale: 20,
    },
  });
  cornerstoneTools.addTool(cornerstoneTools.StackScrollMouseWheelTool);

  csInitialized = true;
};

interface DICOMImageViewerProps {
  studyId: string;
  open: boolean;
  onClose: () => void;
}

const DICOMImageViewer: React.FC<DICOMImageViewerProps> = ({ studyId, open, onClose }) => {
  const { enqueueSnackbar } = useSnackbar();
  const viewportRef = useRef<HTMLDivElement | null>(null);

  const [images, setImages] = useState<DICOMImage[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedImage, setSelectedImage] = useState<DICOMImage | null>(null);
  const [metadata, setMetadata] = useState<DICOMMetadata | null>(null);
  const [metadataOpen, setMetadataOpen] = useState(false);
  const [loadingImage, setLoadingImage] = useState(false);
  const [frameIndex, setFrameIndex] = useState(1);
  const [frameCount, setFrameCount] = useState(1);
  const [renderInfo, setRenderInfo] = useState({ zoom: 1, rotation: 0, wc: '-', ww: '-' });

  const objectUrlMapRef = useRef<Record<string, string>>({});
  const imageRenderedHandlerRef = useRef<EventListener | null>(null);
  const stackScrollHandlerRef = useRef<EventListener | null>(null);

  useEffect(() => {
    if (open && studyId) {
      setSelectedImage(null);
      setMetadata(null);
      loadImages();
    }
  }, [open, studyId]);

  useEffect(() => {
    return () => {
      const urls = Object.values(objectUrlMapRef.current);
      urls.forEach((u) => URL.revokeObjectURL(u));
      objectUrlMapRef.current = {};
      if (viewportRef.current) {
        try {
          if (imageRenderedHandlerRef.current) {
            viewportRef.current.removeEventListener('cornerstoneimagerendered', imageRenderedHandlerRef.current);
          }
          if (stackScrollHandlerRef.current) {
            viewportRef.current.removeEventListener('cornerstonetoolsstackscroll', stackScrollHandlerRef.current);
          }
          cornerstone.disable(viewportRef.current);
        } catch {
          // no-op
        }
      }
    };
  }, []);

  useEffect(() => {
    if (selectedImage || !viewportRef.current) return;
    try {
      if (imageRenderedHandlerRef.current) {
        viewportRef.current.removeEventListener('cornerstoneimagerendered', imageRenderedHandlerRef.current);
      }
      if (stackScrollHandlerRef.current) {
        viewportRef.current.removeEventListener('cornerstonetoolsstackscroll', stackScrollHandlerRef.current);
      }
      cornerstone.disable(viewportRef.current);
      (viewportRef.current as any).__cornerstoneEnabled = false;
    } catch {
      // no-op
    }
  }, [selectedImage]);

  const loadImages = async () => {
    if (!studyId) return;
    try {
      setLoading(true);
      const response = await hospitalService.getDicomImagesByStudy(studyId);
      setImages(response.data || []);
    } catch (err) {
      console.error('Failed to load DICOM images:', err);
      enqueueSnackbar('Failed to load DICOM images', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const ensureImageBlobUrl = async (image: DICOMImage): Promise<string> => {
    if (objectUrlMapRef.current[image.attachmentId]) {
      return objectUrlMapRef.current[image.attachmentId];
    }
    const response = await hospitalService.downloadDicomImage(image.attachmentId);
    const blob = new Blob([response.data as BlobPart], { type: 'application/dicom' });
    const url = URL.createObjectURL(blob);
    objectUrlMapRef.current[image.attachmentId] = url;
    return url;
  };

  const buildStackImageIds = (blobUrl: string, count: number): string[] => {
    if (count <= 1) {
      return [`wadouri:${blobUrl}`];
    }
    const imageIds: string[] = [];
    for (let i = 0; i < count; i += 1) {
      imageIds.push(`wadouri:${blobUrl}?frame=${i}`);
    }
    return imageIds;
  };

  const applyToolBindings = () => {
    cornerstoneTools.setToolActive('Wwwc', { mouseButtonMask: 1 });
    cornerstoneTools.setToolActive('Pan', { mouseButtonMask: 2 });
    cornerstoneTools.setToolActive('Zoom', { mouseButtonMask: 4 });
    cornerstoneTools.setToolActive('StackScrollMouseWheel', {});
  };

  const updateRenderInfo = (element: HTMLDivElement) => {
    const viewport = cornerstone.getViewport(element) as CornerstoneViewport;
    const wc = viewport?.voi?.windowCenter;
    const ww = viewport?.voi?.windowWidth;
    setRenderInfo({
      zoom: viewport?.scale || 1,
      rotation: viewport?.rotation || 0,
      wc: typeof wc === 'number' ? wc.toFixed(1) : '-',
      ww: typeof ww === 'number' ? ww.toFixed(1) : '-',
    });
  };

  const handleImageClick = async (image: DICOMImage) => {
    setSelectedImage(image);
    setMetadata(null);
    setLoadingImage(true);
    setFrameIndex(1);

    try {
      initCornerstone();

      const [blobUrl, metadataResponse] = await Promise.all([
        ensureImageBlobUrl(image),
        hospitalService.getDicomMetadata(image.attachmentId),
      ]);

      const md = metadataResponse.data as DICOMMetadata;
      setMetadata(md);

      const detectedFrameCount = Math.max(1, Number(md.numberOfFrames || 1));
      setFrameCount(detectedFrameCount);

      const element = viewportRef.current;
      if (!element) {
        setLoadingImage(false);
        return;
      }

      if (!(element as any).__cornerstoneEnabled) {
        cornerstone.enable(element);
        (element as any).__cornerstoneEnabled = true;
      }

      const stackImageIds = buildStackImageIds(blobUrl, detectedFrameCount);
      const imageId = stackImageIds[0];
      const csImage = await cornerstone.loadAndCacheImage(imageId);
      cornerstone.displayImage(element, csImage);

      const initialViewport = cornerstone.getViewport(element);
      if (md.windowCenter !== undefined && md.windowCenter !== null
        && md.windowWidth !== undefined && md.windowWidth !== null) {
        initialViewport.voi.windowCenter = Number(md.windowCenter);
        initialViewport.voi.windowWidth = Number(md.windowWidth);
        cornerstone.setViewport(element, initialViewport);
      }

      cornerstoneTools.addStackStateManager(element, ['stack']);
      const existingStackState = cornerstoneTools.getToolState(element, 'stack');
      if (existingStackState?.data?.length) {
        existingStackState.data.length = 0;
      }
      cornerstoneTools.addToolState(element, 'stack', {
        currentImageIdIndex: 0,
        imageIds: stackImageIds,
      });

      applyToolBindings();
      updateRenderInfo(element);

      if (imageRenderedHandlerRef.current) {
        element.removeEventListener('cornerstoneimagerendered', imageRenderedHandlerRef.current);
      }
      if (stackScrollHandlerRef.current) {
        element.removeEventListener('cornerstonetoolsstackscroll', stackScrollHandlerRef.current);
      }

      const onImageRendered: EventListener = () => updateRenderInfo(element);
      const onStackScroll = () => {
        const stackData = cornerstoneTools.getToolState(element, 'stack');
        const current = stackData?.data?.[0]?.currentImageIdIndex ?? 0;
        setFrameIndex(current + 1);
      };

      imageRenderedHandlerRef.current = onImageRendered;
      stackScrollHandlerRef.current = onStackScroll as EventListener;
      element.addEventListener('cornerstoneimagerendered', imageRenderedHandlerRef.current);
      element.addEventListener('cornerstonetoolsstackscroll', stackScrollHandlerRef.current);
    } catch (err) {
      console.error('Failed to initialize DICOM renderer:', err);
      enqueueSnackbar('Failed to render DICOM image', { variant: 'error' });
    } finally {
      setLoadingImage(false);
    }
  };

  const handleClose = () => {
    const urls = Object.values(objectUrlMapRef.current);
    urls.forEach((u) => URL.revokeObjectURL(u));
    objectUrlMapRef.current = {};
    closePreview();
    onClose();
  };

  const getThumbnailUrl = (image: DICOMImage): string => {
    return `/api/hospital/dicom/images/${image.attachmentId}/thumbnail`;
  };

  const closePreview = () => {
    setSelectedImage(null);
    setMetadata(null);
    setFrameIndex(1);
    setFrameCount(1);
    setRenderInfo({ zoom: 1, rotation: 0, wc: '-', ww: '-' });
  };

  const rotate90 = () => {
    if (!viewportRef.current) return;
    const vp = cornerstone.getViewport(viewportRef.current);
    vp.rotation = ((vp.rotation || 0) + 90) % 360;
    cornerstone.setViewport(viewportRef.current, vp);
    updateRenderInfo(viewportRef.current);
  };

  const zoomBy = (delta: number) => {
    if (!viewportRef.current) return;
    const vp = cornerstone.getViewport(viewportRef.current);
    const next = Math.min(20, Math.max(0.1, (vp.scale || 1) + delta));
    vp.scale = next;
    cornerstone.setViewport(viewportRef.current, vp);
    updateRenderInfo(viewportRef.current);
  };

  const resetViewport = () => {
    if (!viewportRef.current) return;
    cornerstone.reset(viewportRef.current);
    updateRenderInfo(viewportRef.current);
  };

  const goFrame = async (targetFrame: number) => {
    if (!selectedImage || !viewportRef.current) return;
    if (targetFrame < 1 || targetFrame > frameCount) return;

    const blobUrl = objectUrlMapRef.current[selectedImage.attachmentId];
    if (!blobUrl) return;

    try {
      const imageId = frameCount > 1
        ? `wadouri:${blobUrl}?frame=${targetFrame - 1}`
        : `wadouri:${blobUrl}`;
      const csImage = await cornerstone.loadAndCacheImage(imageId);
      cornerstone.displayImage(viewportRef.current, csImage);
      const stackData = cornerstoneTools.getToolState(viewportRef.current, 'stack');
      if (stackData?.data?.[0]) {
        stackData.data[0].currentImageIdIndex = targetFrame - 1;
      }
      setFrameIndex(targetFrame);
      updateRenderInfo(viewportRef.current);
    } catch (err) {
      console.error('Failed to switch frame:', err);
      enqueueSnackbar('Failed to load frame', { variant: 'warning' });
    }
  };

  const handleDownload = async () => {
    if (!selectedImage) return;
    try {
      const response = await hospitalService.downloadDicomImage(selectedImage.attachmentId);
      const blob = new Blob([response.data as BlobPart], { type: 'application/dicom' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = selectedImage.fileName || `dicom-image-${selectedImage.attachmentId}.dcm`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
      enqueueSnackbar('Image downloaded successfully', { variant: 'success' });
    } catch (err) {
      console.error('Failed to download image:', err);
      enqueueSnackbar('Failed to download image', { variant: 'error' });
    }
  };

  const metadataRows = useMemo(() => {
    if (!metadata) return [];
    return [
      ['Patient Name', metadata.patientName],
      ['Patient ID', metadata.patientId],
      ['Modality', metadata.modality],
      ['Study Date', metadata.studyDate],
      ['Study Time', metadata.studyTime],
      ['Study Description', metadata.studyDescription],
      ['Accession Number', metadata.accessionNumber],
      ['Series Number', metadata.seriesNumber],
      ['Instance Number', metadata.instanceNumber],
      ['Rows', metadata.rows],
      ['Columns', metadata.columns],
      ['Bits Allocated', metadata.bitsAllocated],
      ['Window Center', metadata.windowCenter],
      ['Window Width', metadata.windowWidth],
      ['Number Of Frames', metadata.numberOfFrames],
      ['Study UID', metadata.studyInstanceUID],
      ['Series UID', metadata.seriesInstanceUID],
      ['SOP UID', metadata.sopInstanceUID],
    ].filter(([, v]) => v !== undefined && v !== null && v !== '');
  }, [metadata]);

  return (
    <>
      <Dialog open={open} onClose={handleClose} maxWidth="lg" fullWidth>
        <DialogTitle>
          <Box display="flex" justifyContent="space-between" alignItems="center">
            <Typography variant="h6">DICOM Images</Typography>
            <IconButton onClick={handleClose}>
              <CloseIcon />
            </IconButton>
          </Box>
        </DialogTitle>
        <DialogContent>
          {loading ? (
            <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
              <CircularProgress />
            </Box>
          ) : images.length === 0 ? (
            <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
              <Typography variant="body1" color="text.secondary">
                No DICOM images available for this study
              </Typography>
            </Box>
          ) : (
            <ImageList cols={4} gap={8} sx={{ mt: 1 }}>
              {images.map((image) => (
                <ImageListItem
                  key={image.attachmentId}
                  onClick={() => handleImageClick(image)}
                  sx={{
                    cursor: 'pointer',
                    border: selectedImage?.attachmentId === image.attachmentId ? '2px solid #1976d2' : 'none',
                    '&:hover': { opacity: 0.85 },
                  }}
                >
                  <img
                    src={getThumbnailUrl(image)}
                    alt={image.fileName}
                    loading="lazy"
                    style={{
                      width: '100%',
                      height: '200px',
                      objectFit: 'contain',
                      backgroundColor: '#000',
                    }}
                    onError={(e) => {
                      const target = e.target as HTMLImageElement;
                      target.style.display = 'none';
                    }}
                  />
                  <ImageListItemBar
                    title={image.fileName}
                    subtitle={image.fileSize ? `${(image.fileSize / 1024).toFixed(2)} KB` : ''}
                  />
                </ImageListItem>
              ))}
            </ImageList>
          )}
        </DialogContent>
      </Dialog>

      {selectedImage && (
        <Dialog
          open={!!selectedImage}
          onClose={closePreview}
          maxWidth={false}
          PaperProps={{
            sx: {
              width: '95vw',
              height: '95vh',
              maxWidth: 'none',
              maxHeight: 'none',
              m: 0,
              bgcolor: 'black',
            },
          }}
        >
          <Toolbar
            sx={{
              bgcolor: 'rgba(0, 0, 0, 0.82)',
              color: 'white',
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              zIndex: 2,
              gap: 0.5,
            }}
          >
            <Typography variant="h6" sx={{ flexGrow: 1, color: 'white' }}>
              {selectedImage.fileName}
            </Typography>
            <Chip size="small" label={`Frame ${frameIndex}/${frameCount}`} sx={{ color: 'white', borderColor: 'white' }} variant="outlined" />
            <Chip size="small" label={`WW ${renderInfo.ww}`} sx={{ color: 'white', borderColor: 'white' }} variant="outlined" />
            <Chip size="small" label={`WL ${renderInfo.wc}`} sx={{ color: 'white', borderColor: 'white' }} variant="outlined" />
            <Chip size="small" label={`Zoom ${Math.round(renderInfo.zoom * 100)}%`} sx={{ color: 'white', borderColor: 'white' }} variant="outlined" />
            <Chip size="small" label={`Rot ${renderInfo.rotation}°`} sx={{ color: 'white', borderColor: 'white' }} variant="outlined" />
            <IconButton onClick={() => goFrame(frameIndex - 1)} sx={{ color: 'white' }} disabled={frameIndex <= 1}>
              <PrevIcon />
            </IconButton>
            <IconButton onClick={() => goFrame(frameIndex + 1)} sx={{ color: 'white' }} disabled={frameIndex >= frameCount}>
              <NextIcon />
            </IconButton>
            <IconButton onClick={() => zoomBy(-0.15)} sx={{ color: 'white' }}>
              <ZoomOutIcon />
            </IconButton>
            <IconButton onClick={() => zoomBy(0.15)} sx={{ color: 'white' }}>
              <ZoomInIcon />
            </IconButton>
            <IconButton onClick={rotate90} sx={{ color: 'white' }}>
              <RotateIcon />
            </IconButton>
            <IconButton onClick={resetViewport} sx={{ color: 'white' }}>
              <ResetIcon />
            </IconButton>
            <IconButton onClick={handleDownload} sx={{ color: 'white' }}>
              <DownloadIcon />
            </IconButton>
            <IconButton onClick={() => setMetadataOpen(true)} sx={{ color: 'white' }}>
              <InfoIcon />
            </IconButton>
            <IconButton onClick={closePreview} sx={{ color: 'white' }}>
              <CloseIcon />
            </IconButton>
          </Toolbar>

          <Box
            sx={{
              width: '100%',
              height: '100%',
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              pt: 8,
              position: 'relative',
              backgroundColor: 'black',
            }}
          >
            {loadingImage ? (
              <CircularProgress sx={{ color: 'white' }} />
            ) : (
              <>
                <Box
                  ref={viewportRef}
                  sx={{
                    width: '96%',
                    height: '92%',
                    border: '1px solid #333',
                    backgroundColor: 'black',
                  }}
                />
                <Typography
                  variant="caption"
                  sx={{
                    position: 'absolute',
                    left: 16,
                    bottom: 10,
                    color: '#B0BEC5',
                    bgcolor: 'rgba(0,0,0,0.55)',
                    px: 1,
                    py: 0.5,
                    borderRadius: 0.5,
                  }}
                >
                  Left drag: window/level | Wheel: scroll frames | Middle drag: pan | Right drag: zoom
                </Typography>
                {metadata && (
                  <>
                    <Typography
                      variant="caption"
                      sx={{
                        position: 'absolute',
                        left: 16,
                        top: 76,
                        color: '#E0E0E0',
                        bgcolor: 'rgba(0,0,0,0.55)',
                        px: 1,
                        py: 0.5,
                        borderRadius: 0.5,
                      }}
                    >
                      {metadata.patientName || 'Unknown Patient'} | {metadata.patientId || '-'}
                    </Typography>
                    <Typography
                      variant="caption"
                      sx={{
                        position: 'absolute',
                        right: 16,
                        top: 76,
                        color: '#E0E0E0',
                        bgcolor: 'rgba(0,0,0,0.55)',
                        px: 1,
                        py: 0.5,
                        borderRadius: 0.5,
                        textAlign: 'right',
                      }}
                    >
                      {metadata.modality || '-'} | {metadata.studyDate || '-'} {metadata.studyTime || ''}
                    </Typography>
                  </>
                )}
              </>
            )}
          </Box>
        </Dialog>
      )}

      <MetadataDialog open={metadataOpen} onClose={() => setMetadataOpen(false)} maxWidth="md" fullWidth>
        <MetadataDialogTitle>DICOM Metadata</MetadataDialogTitle>
        <MetadataDialogContent>
          {metadataRows.length ? (
            <Grid container spacing={2} sx={{ mt: 1 }}>
              {metadataRows.map(([label, value]) => (
                <Grid item xs={12} md={6} key={String(label)}>
                  <Typography variant="subtitle2" color="text.secondary">{label}</Typography>
                  <Typography variant="body1" sx={{ wordBreak: 'break-all' }}>{String(value)}</Typography>
                </Grid>
              ))}
            </Grid>
          ) : (
            <Typography variant="body2" color="text.secondary">
              No metadata available
            </Typography>
          )}
        </MetadataDialogContent>
        <MetadataDialogActions>
          <Button onClick={() => setMetadataOpen(false)}>Close</Button>
        </MetadataDialogActions>
      </MetadataDialog>
    </>
  );
};

export default DICOMImageViewer;
