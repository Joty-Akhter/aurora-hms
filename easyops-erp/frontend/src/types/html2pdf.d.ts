declare module 'html2pdf.js' {
  interface Html2PdfOptions {
    margin?: number | [number, number, number, number];
    filename?: string;
    image?: { type?: string; quality?: number };
    html2canvas?: Record<string, unknown>;
    jsPDF?: {
      unit?: string;
      format?: string | number[];
      orientation?: 'portrait' | 'landscape';
    };
    pagebreak?: { mode?: string | string[] };
  }
  interface Html2PdfChain {
    set(opt: Html2PdfOptions): Html2PdfChain;
    from(element: HTMLElement): Html2PdfChain;
    save(): Promise<void>;
  }
  function html2pdf(): Html2PdfChain;
  export default html2pdf;
}
