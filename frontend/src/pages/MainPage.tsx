import { useNavigate, useLocation } from "react-router-dom";
import { motion } from "framer-motion";
import { useEffect } from "react";

export default function MainPage() {
  const navigate = useNavigate();
  const location = useLocation();

  // Extract query parameter from URL (e.g., /?ownerId=123)
  const searchParams = new URLSearchParams(location.search);
  const ownerId = searchParams.get("ownerId");

  useEffect(() => {
    if (ownerId) {
      // If accessed via QR (with ownerId), redirect directly to client mode
      navigate(`/client?ownerId=${ownerId}`);
    }
  }, [ownerId, navigate]);

  const handleOwnerLogin = () => navigate("/login");
  const handleClientMode = () => navigate("/client");

  return (
    <div className="min-h-screen bg-[#121212] text-gray-100 flex flex-col">
      {/* Header */}
      <header className="flex justify-between items-center px-10 py-6 border-b border-gray-800 bg-[#181818] shadow-lg">
        <h1 className="text-2xl font-extrabold text-green-500 tracking-wide">
          Spotify Jukebox
        </h1>
        <button
          onClick={handleOwnerLogin}
          className="px-6 py-2 bg-green-500 hover:bg-green-400 text-black font-semibold rounded-full transition"
        >
          Owner Login
        </button>
      </header>

      {/* Hero Section */}
      <section className="flex flex-col items-center justify-center flex-grow text-center px-6 py-16">
        <motion.h2
          className="text-5xl font-extrabold mb-4"
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
        >
          Music. Together.
        </motion.h2>

        <motion.p
          className="text-gray-400 text-lg mb-10 max-w-xl"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
        >
          A shared jukebox experience powered by Spotify — control the vibe, or
          join the crowd. 🎧
        </motion.p>

        <motion.div
          className="flex flex-col md:flex-row gap-6"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
        >
          <button
            onClick={handleOwnerLogin}
            className="px-10 py-4 bg-green-500 hover:bg-green-400 text-black font-semibold rounded-full text-lg transition"
          >
            I’m the Owner
          </button>

          <button
            onClick={handleClientMode}
            className="px-10 py-4 bg-transparent border border-green-500 hover:bg-green-500 hover:text-black text-green-400 font-semibold rounded-full text-lg transition"
          >
            I’m a Guest
          </button>
        </motion.div>
      </section>

      {/* Info Section */}
      <section className="bg-[#181818] border-t border-gray-800 py-16 px-10 text-center">
        <h3 className="text-3xl font-bold mb-10 text-green-400">
          How It Works
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-10 max-w-6xl mx-auto text-left">
          <div className="bg-[#222] p-6 rounded-2xl border border-gray-700">
            <h4 className="text-green-400 text-lg font-semibold mb-2">
              🎧 Owners
            </h4>
            <p className="text-gray-400">
              Log in with your Spotify account. Choose your playlist and device,
              and start the session.
            </p>
          </div>

          <div className="bg-[#222] p-6 rounded-2xl border border-gray-700">
            <h4 className="text-green-400 text-lg font-semibold mb-2">
              📱 Guests
            </h4>
            <p className="text-gray-400">
              Scan the QR code displayed by the owner to join instantly. Vote
              for songs and influence what plays next.
            </p>
          </div>

          <div className="bg-[#222] p-6 rounded-2xl border border-gray-700">
            <h4 className="text-green-400 text-lg font-semibold mb-2">
              🕹️ Live Jukebox
            </h4>
            <p className="text-gray-400">
              Everyone interacts in real time — the music changes based on the
              crowd’s vibe. Perfect for cafés, bars, and events.
            </p>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-[#111] border-t border-gray-800 py-6 text-center text-gray-500 text-sm">
        <p>
          Built with ❤️ by{" "}
          <span className="text-green-400 font-medium">Bithub</span> · Powered by{" "}
          <span className="text-green-400">Spotify API</span>
        </p>
      </footer>
    </div>
  );
}
