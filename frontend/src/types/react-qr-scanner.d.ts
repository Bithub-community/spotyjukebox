// Type declaration for 'react-qr-scanner' — allows proper TypeScript usage
declare module "react-qr-scanner" {
  import * as React from "react";

  export interface QrScannerProps {
    /** Callback triggered when a QR code is successfully scanned */
    onScan?: (data: string | null) => void;

    /** Callback triggered when a camera or scanning error occurs */
    onError?: (err: any) => void;

    /** Scan interval delay in milliseconds */
    delay?: number;

    /** Optional style object applied to the video container */
    style?: React.CSSProperties;

    /** Camera constraints (e.g., facingMode: 'environment') */
    constraints?: MediaTrackConstraints;
  }

  /** React component for scanning QR codes using a webcam */
  export default class QrScanner extends React.Component<QrScannerProps> {}
}
