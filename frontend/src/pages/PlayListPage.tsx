import { useContext, useEffect, useState } from "react";
import { AuthContext } from "@/context/AuthProvider";
import axios from "axios";
import { Playlist } from "@/models/PlayslistModels";
import { useNavigate } from "react-router-dom";
import { Music, User, PlayCircle, Info } from "lucide-react";

export default function PlaylistPage() {

  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

  const { user } = useContext(AuthContext);
  const [playlists, setPlaylists] = useState<Playlist[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  // 🎧 Fetch user playlists from backend once the user is loaded
  useEffect(() => {
    if (!user?.id) return;

    axios
      .get(`${API_BASE_URL}/api/spotify/playlists/${user.id}`)
      .then((res) => {
        console.log("🎵 Spotify playlists:", res.data);
        setPlaylists(res.data.items || []);
      })
      .catch((err) => {
        console.error("❌ Playlist fetch error:", err);
        setError("Could not load playlists");
      })
      .finally(() => setLoading(false));
  }, [user?.id, API_BASE_URL]);

  // Move to the next page after selecting a playlist
  const handleNext = () => {
    if (selected) {
      const chosen = playlists.find((p) => p.id === selected);
      navigate("/devices", { state: { selectedPlaylist: chosen } });
    }
  };

  // Open detailed playlist view without triggering parent click
  const handleViewDetails = (playlistId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    navigate(`/playlist/${playlistId}`);
  };

  // Loading state
  if (loading) {
    return (
      <div className="flex flex-col justify-center items-center h-screen bg-[#121212]">
        <div className="animate-spin rounded-full h-16 w-16 border-t-2 border-b-2 border-[#1DB954] mb-4"></div>
        <p className="text-gray-400 text-lg">Loading your playlists...</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#121212] text-[#B3B3B3] px-4 py-8">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-8 text-center">
          <div className="flex items-center justify-center gap-3 mb-3">
            <Music className="w-10 h-10 text-[#1DB954]" />
            <h1 className="text-4xl font-bold text-white">Your Playlists</h1>
          </div>
          <p className="text-gray-400 text-lg">
            Select a playlist to continue or view details
          </p>
        </div>

        {/* Error Message */}
        {error && (
          <div className="bg-red-900/40 border border-red-700 text-red-200 px-6 py-4 rounded-xl mb-6 text-center">
            <p className="font-medium">{error}</p>
          </div>
        )}

        {/* Playlist Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 mb-8">
          {playlists.map((playlist) => (
            <div
              key={playlist.id}
              className={`group relative rounded-2xl overflow-hidden transition-all duration-300 transform hover:scale-105 ${
                selected === playlist.id
                  ? "ring-4 ring-[#1DB954] shadow-2xl shadow-[#1DB954]/30"
                  : "hover:shadow-lg hover:shadow-black/50"
              }`}
            >
              <div
                onClick={() => setSelected(playlist.id)}
                className="cursor-pointer bg-[#181818] hover:bg-[#282828] border border-transparent rounded-2xl overflow-hidden h-full flex flex-col transition-all duration-200"
              >
                {/* Playlist Image */}
                <div className="relative aspect-square overflow-hidden">
                  {playlist.images?.[0] ? (
                    <>
                      <img
                        src={playlist.images[0].url}
                        alt={playlist.name}
                        className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-110"
                      />
                      <div className="absolute inset-0 bg-gradient-to-t from-black/70 to-transparent"></div>
                    </>
                  ) : (
                    <div className="w-full h-full bg-[#282828] flex items-center justify-center">
                      <Music className="w-20 h-20 text-gray-600" />
                    </div>
                  )}

                  {/* Selection indicator when playlist is chosen */}
                  {selected === playlist.id && (
                    <div className="absolute top-4 right-4 bg-[#1DB954] rounded-full p-2 shadow-lg">
                      <PlayCircle className="w-6 h-6 text-black" />
                    </div>
                  )}
                </div>

                {/* Playlist Info */}
                <div className="p-5 flex-1 flex flex-col">
                  <h2 className="font-bold text-lg text-white mb-2 line-clamp-2 group-hover:text-[#1DB954] transition-colors">
                    {playlist.name}
                  </h2>

                  <div className="flex items-center gap-2 text-sm text-gray-400 mb-3">
                    <Music className="w-4 h-4" />
                    <span>{playlist.tracks?.total ?? 0} tracks</span>
                  </div>

                  <div className="flex items-center gap-2 text-xs text-gray-500 mb-4">
                    <User className="w-3 h-3" />
                    <span>{playlist.owner?.display_name || "Unknown"}</span>
                  </div>

                  <button
                    onClick={(e) => handleViewDetails(playlist.id, e)}
                    className="mt-auto w-full flex items-center justify-center gap-2 px-4 py-2.5 bg-[#1DB954]/10 hover:bg-[#1DB954]/20 border border-[#1DB954]/30 rounded-lg transition-all duration-200 text-sm font-medium text-[#1DB954]"
                  >
                    <Info className="w-4 h-4" />
                    <span>View Details</span>
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Empty State */}
        {(!playlists || playlists.length === 0) && !error && (
          <div className="text-center py-20">
            <Music className="w-24 h-24 text-gray-700 mx-auto mb-6" />
            <h3 className="text-2xl font-semibold text-gray-400 mb-2">
              No playlists found
            </h3>
            <p className="text-gray-500">
              Create some playlists on Spotify to get started!
            </p>
          </div>
        )}

        {/* Footer Navigation */}
        <div className="sticky bottom-0 bg-[#181818]/80 backdrop-blur-lg border-t border-[#282828] rounded-2xl p-6 mt-8">
          <div className="flex items-center justify-between max-w-7xl mx-auto">
            <button
              className="flex items-center gap-2 px-6 py-3 bg-[#282828] hover:bg-[#333333] rounded-xl text-white transition-all duration-200"
              onClick={() => navigate(-1)} // Go back to previous page
            >
              ← Back
            </button>

            <button
              className={`flex items-center gap-2 px-8 py-3 rounded-xl font-semibold transition-all duration-200 ${
                selected
                  ? "bg-[#1DB954] hover:bg-[#1ED760] text-black"
                  : "bg-[#282828] text-gray-500 cursor-not-allowed"
              }`}
              onClick={handleNext}
              disabled={!selected}
            >
              Continue →
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
