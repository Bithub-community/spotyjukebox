import { useState } from "react";
import QrScanner from "react-qr-scanner";
import { useNavigate } from "react-router-dom";

export default function ClientPage() {
  const navigate = useNavigate();
  const [cameraAllowed, setCameraAllowed] = useState(true);
  const [scannedId, setScannedId] = useState<string | null>(null);

  const handleScan = (data: string | null) => {
    if (data) {
      console.log("✅ QR detected:", data);
      setScannedId(data);

      // Check if scanned QR content is a full URL or just an ID
      if (data.startsWith("http")) {
        // Parse the URL to extract the "ownerId" query parameter
        const url = new URL(data);
        const ownerId = url.searchParams.get("ownerId");
        if (ownerId) navigate(`/client/session?ownerId=${ownerId}`);
      } else {
        // If it's a plain ID, navigate directly
        navigate(`/client/session?ownerId=${data}`);
      }
    }
  };

  const handleError = (err: any) => {
    console.error("QR Error:", err);
    // Disable camera UI when access is denied or device is unavailable
    setCameraAllowed(false);
  };

  return (
    <div className="min-h-screen bg-[#121212] text-gray-200 flex flex-col items-center justify-center px-4">
      {/* Page title */}
      <h1 className="text-3xl font-extrabold text-green-400 mb-3">
        Join the Jukebox
      </h1>
      <p className="text-gray-400 text-center mb-6">
        Point your camera at the QR code to join the live session 🎶
      </p>

      {/* QR camera view */}
      {cameraAllowed ? (
        <div className="bg-gray-900 p-3 rounded-2xl border border-gray-700 shadow-lg">
          <QrScanner
            delay={300} // scan interval in ms
            style={{ width: 300, height: 300 }}
            onError={handleError}
            onScan={handleScan}
            constraints={{ facingMode: "environment" }} // use back camera if available
          />
        </div>
      ) : (
        <div className="text-center text-red-400 mt-8">
          <p className="font-semibold text-lg mb-2">Camera access denied</p>
          <p className="text-gray-400">
            Please allow camera permissions and reload the page.
          </p>
        </div>
      )}

      {/* Display info while connecting after scan */}
      {scannedId && (
        <div className="mt-6 text-center">
          <h2 className="text-green-400 text-xl font-semibold">
            Connecting to session...
          </h2>
          <p className="text-gray-500 text-sm mt-1">{scannedId}</p>
        </div>
      )}

      {/* Footer */}
      <div className="mt-12 text-gray-500 text-sm text-center">
        <p>Powered by Spotify API · Bithub Jukebox</p>
      </div>
    </div>
  );
}
