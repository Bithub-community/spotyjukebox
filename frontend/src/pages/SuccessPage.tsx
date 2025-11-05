import { useLocation, useNavigate } from "react-router-dom";
import { QRCodeCanvas } from "qrcode.react";

export default function SuccessPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { selectedPlaylist, selectedDevice } = location.state || {};
  const storedUser = JSON.parse(localStorage.getItem("user") || "{}");
  const ownerId = storedUser?.id;

  // 🔹 QR code value that guests will scan to join the session
  const qrValue = `${window.location.origin}/client/session?ownerId=${ownerId}`;

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-[#121212] text-gray-200 px-4">
      {/* Main Card */}
      <div className="bg-[#181818] p-10 rounded-2xl border border-gray-800 text-center max-w-md shadow-xl">
        <h2 className="text-3xl font-extrabold mb-4 text-green-400 tracking-wide">
          Session Ready 🎉
        </h2>
        <p className="text-gray-400 mb-6 leading-relaxed">
          Your playlist{" "}
          <span className="text-green-400 font-semibold">
            {selectedPlaylist?.name || "your playlist"}
          </span>{" "}
          is now playing on{" "}
          <span className="text-green-400 font-semibold">
            {selectedDevice?.name || "your device"}
          </span>
          .
        </p>

        {/* QR Code Display */}
        <div className="bg-[#0f0f0f] p-5 rounded-2xl border border-gray-700 inline-block mb-4">
          <QRCodeCanvas
            value={qrValue}
            size={200}
            bgColor="#0f0f0f"
            fgColor="#1DB954"
            includeMargin={true}
          />
        </div>

        <p className="text-gray-400 text-sm mb-8">
          Let your guests scan this QR code to join your Jukebox session.
        </p>

        {/* Navigation & Link Buttons */}
        <div className="flex flex-col gap-3">
          <button
            onClick={() => navigate("/playlists")}
            className="px-6 py-2 bg-green-600 hover:bg-green-500 rounded-full font-semibold transition"
          >
            Back to Playlists
          </button>

          <button
            onClick={() => {
              const clientUrl = `${window.location.origin}/client/session?ownerId=${ownerId}`;
              console.log("🔗 Opening client URL:", clientUrl);

              // 🔹 Handle opening logic depending on environment
              if (
                window.location.hostname === "localhost" ||
                window.location.hostname === "127.0.0.1"
              ) {
                // In dev: open local tab directly
                window.open(clientUrl, "_blank");
              } else {
                // In production: open safe link (no referrer)
                window.open(clientUrl, "_blank", "noopener,noreferrer");
              }
            }}
            className="px-6 py-2 bg-transparent border border-green-500 hover:bg-green-500 hover:text-black rounded-full font-semibold transition"
          >
            Open Client Link
          </button>
        </div>
      </div>

      {/* Footer */}
      <div className="mt-10 text-gray-500 text-sm text-center">
        <p>
          Built with ❤️ by{" "}
          <span className="text-green-400 font-medium">Bithub</span> · Powered by{" "}
          <span className="text-green-400 font-medium">Spotify API</span>
        </p>
      </div>
    </div>
  );
}
