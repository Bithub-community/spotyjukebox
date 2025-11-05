import { useEffect, useState } from "react";
import axios from "axios";
import { useNavigate, useLocation } from "react-router-dom";

interface Device {
  id: string;
  name: string;
  type: string;
  active: boolean;
}

const SelectDevicePage = () => {

  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

  const [devices, setDevices] = useState<Device[]>([]);
  const [selectedDevice, setSelectedDevice] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const location = useLocation();
  const { selectedPlaylist } = location.state || {};

  // Retrieve user info from local storage (set during login)
  const storedUser = JSON.parse(localStorage.getItem("user") || "{}");
  const userId = storedUser?.id;

  // 🔹 Fetch user’s available Spotify devices from backend
  useEffect(() => {
    const fetchDevices = async () => {
      try {
        if (!userId) return;
        const res = await axios.get(
          `${API_BASE_URL}/api/spotify/devices/${userId}`
        );
        setDevices(res.data);
      } catch (err) {
        console.error("❌ Failed to load devices:", err);
      } finally {
        setLoading(false);
      }
    };
    fetchDevices();
  }, [userId, API_BASE_URL]);

  // Proceed to confirmation page with selected device + playlist
  const handleNext = () => {
    if (selectedDevice) {
      const selected = devices.find((d) => d.id === selectedDevice);
      navigate("/confirm", {
        state: { selectedPlaylist, selectedDevice: selected },
      });
    }
  };

  // Loading state
  if (loading) {
    return (
      <div className="flex justify-center items-center h-screen text-gray-300 text-xl">
        Loading devices...
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gray-950 text-gray-200 px-4">
      <div className="w-full max-w-lg bg-gray-900 rounded-2xl shadow-lg p-6 border border-gray-800">
        <h2 className="text-2xl font-semibold mb-4 text-center">
          Select a Spotify Device
        </h2>

        {/* If no devices are found, show user a hint */}
        {devices.length === 0 ? (
          <p className="text-center text-gray-400">
            🎧 No active devices found. Make sure Spotify is open and playing
            music on one of your devices.
          </p>
        ) : (
          <ul className="space-y-3">
            {devices.map((d) => (
              <li
                key={d.id}
                onClick={() => setSelectedDevice(d.id)}
                className={`p-4 rounded-xl border transition-colors cursor-pointer ${
                  selectedDevice === d.id
                    ? "bg-green-600 border-green-500 text-white"
                    : "bg-gray-800 hover:bg-gray-700 border-gray-700"
                }`}
              >
                <div className="flex justify-between items-center">
                  <div>
                    <p className="font-semibold">{d.name}</p>
                    <p className="text-sm text-gray-400">{d.type}</p>
                  </div>
                  {d.active && (
                    <span className="px-2 py-1 bg-green-500 text-xs font-medium rounded-md">
                      Active
                    </span>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}

        {/* Navigation buttons */}
        <div className="flex justify-between mt-8">
          <button
            className="px-4 py-2 bg-gray-700 hover:bg-gray-600 rounded-lg"
            onClick={() => navigate(-1)} // Go back to previous step
          >
            ← Back
          </button>
          <button
            className={`px-5 py-2 rounded-lg font-medium ${
              selectedDevice
                ? "bg-green-600 hover:bg-green-500"
                : "bg-gray-600 cursor-not-allowed"
            }`}
            onClick={handleNext}
            disabled={!selectedDevice}
          >
            Next →
          </button>
        </div>

        {/* Step indicator */}
        <div className="mt-6 text-center text-gray-500 text-sm">
          Step 2 of 3 — <span className="text-green-400">Select Device</span>
        </div>
      </div>
    </div>
  );
};

export default SelectDevicePage;
