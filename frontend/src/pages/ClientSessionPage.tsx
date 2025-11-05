import { useEffect, useState, useRef } from "react";
import { useSearchParams } from "react-router-dom";
import axios from "axios";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

interface Track {
  id: string;
  name: string;
  artist: string;
  albumArt: string;
  votes: number;
  inCooldown?: boolean;
  cooldownRemaining?: number;
}

export default function ClientSessionPage() {
  const [params] = useSearchParams();
  const ownerId = params.get("ownerId");

  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;
  const WS_URL = import.meta.env.VITE_WS_URL;

  const [nowPlaying, setNowPlaying] = useState<Track | null>(null);
  const nowPlayingRef = useRef<Track | null>(null);
  const [upNext, setUpNext] = useState<Track[]>([]);
  const [votes, setVotes] = useState<Record<string, number>>({});
  const [voted, setVoted] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [voteError, setVoteError] = useState<string | null>(null);
  const [viewMode, setViewMode] = useState<"grid" | "compact">("compact");

  // persistent client ID
  const [clientId] = useState(() => {
    let existing = localStorage.getItem("clientId");
    if (!existing) {
      existing = "guest-" + Math.random().toString(36).substring(2, 10);
      localStorage.setItem("clientId", existing);
    }
    return existing;
  });

  // 📡 WebSocket connection
  useEffect(() => {
    if (!ownerId || !WS_URL) return;

    const socket = new SockJS(WS_URL);
    const client = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/votes/${ownerId}`, (message) => {
          const updated = JSON.parse(message.body);
          setVotes(updated);
        });
      },
    });

    client.activate();
    return () => void client.deactivate();
  }, [ownerId, WS_URL]);

  // 🎵 Fetch now-playing + queue
  useEffect(() => {
    if (!ownerId || !API_BASE_URL) {
      setError("No session found. Please scan a valid QR code.");
      return;
    }

    const fetchNowPlaying = async () => {
      try {
        const res = await axios.get(`${API_BASE_URL}/api/spotify/now-playing/${ownerId}`);
        const item = res.data.item;
        if (!item) return;

        const currentTrackId = nowPlayingRef.current?.id;

        // Detect track change
        if (currentTrackId && currentTrackId !== item.id) {
          try {
            await axios.post(`${API_BASE_URL}/api/jukebox/played`, {
              ownerId,
              trackId: currentTrackId,
            });
          } catch (err) {
            console.warn("⚠️ Failed to reset votes:", err);
          }

          setVotes((prev) => {
            const newVotes = { ...prev };
            delete newVotes[currentTrackId];
            return newVotes;
          });

          setUpNext((prev) => prev.filter((t) => t.id !== currentTrackId));
        }

        const newTrack = {
          id: item.id,
          name: item.name,
          artist: item.artists.map((a: any) => a.name).join(", "),
          albumArt: item.album.images[0]?.url || "",
          votes: 0,
        };

        setNowPlaying(newTrack);
        nowPlayingRef.current = newTrack;
      } catch (err) {
        console.error("Failed to fetch now playing:", err);
        setError("Failed to connect to Spotify session.");
      }
    };

    const fetchQueue = async () => {
      try {
        const res = await axios.get(`${API_BASE_URL}/api/spotify/upcoming-tracks/${ownerId}`);
        const queue = res.data.queue || [];
        setUpNext(
          queue.map((track: any) => ({
            id: track.id,
            name: track.name,
            artist: track.artists?.map((a: any) => a.name).join(", ") || "Unknown",
            albumArt: track.album?.images?.[0]?.url || "",
            votes: track.votes || 0,
            inCooldown: track.inCooldown || false,
            cooldownRemaining: track.cooldownRemaining || 0,
          }))
        );
      } catch (err) {
        console.error("Failed to fetch queue:", err);
      }
    };

    fetchNowPlaying();
    fetchQueue();
    const interval = setInterval(() => {
      fetchNowPlaying();
      fetchQueue();
    }, 10000);
    return () => clearInterval(interval);
  }, [ownerId, API_BASE_URL]);

  // 🗳 Voting
  const handleVote = async (trackId: string) => {
    if (!ownerId || !API_BASE_URL) return;
    setVoted(trackId);

    try {
      await axios.post(`${API_BASE_URL}/api/jukebox/vote`, {
        ownerId,
        trackId,
        clientId,
      });
    } catch (err: any) {
      const message =
        err.response?.data?.error ||
        err.response?.data?.message ||
        "Vote failed";
      if (message.includes("already voted")) {
        setVoteError("⚠️ You already voted for this song.");
      } else {
        setVoteError("❌ Vote failed. Try again.");
      }
    } finally {
      setTimeout(() => setVoted(null), 2000);
    }
  };

  // Auto-clear error
  useEffect(() => {
    if (voteError) {
      const timer = setTimeout(() => setVoteError(null), 3000);
      return () => clearTimeout(timer);
    }
  }, [voteError]);

  if (!nowPlaying)
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#121212] text-gray-400">
        <p>Connecting to Jukebox...</p>
      </div>
    );

  return (
    <div className="min-h-screen bg-[#121212] text-gray-200 flex flex-col items-center px-3 sm:px-4 py-6 sm:py-8">
      {/* ⚠️ Error Banner */}
      {voteError && (
        <div className="fixed top-4 left-4 right-4 sm:left-1/2 sm:right-auto sm:transform sm:-translate-x-1/2 sm:w-auto bg-yellow-500/20 border border-yellow-400 text-yellow-300 text-sm px-4 py-2 rounded-lg text-center shadow-lg backdrop-blur-md animate-pulse z-50">
          {voteError}
        </div>
      )}

      {/* Now Playing */}
      <div className="text-center mb-8 sm:mb-10 w-full">
        <h1 className="text-2xl sm:text-3xl font-bold text-green-400 mb-4">Now Playing 🎵</h1>
        <div className="bg-[#181818] rounded-2xl shadow-lg p-4 sm:p-6 border border-gray-800 max-w-md mx-auto">
          <img
            src={nowPlaying.albumArt}
            alt={nowPlaying.name}
            className="w-48 h-48 sm:w-64 sm:h-64 mx-auto rounded-xl mb-4 shadow-md"
          />
          <h2 className="text-xl sm:text-2xl font-semibold px-2">{nowPlaying.name}</h2>
          <p className="text-gray-400 px-2">{nowPlaying.artist}</p>
        </div>
      </div>

      {/* Toggle View */}
      <div className="flex justify-center gap-2 sm:gap-3 mb-6 w-full">
        <button
          onClick={() => setViewMode("grid")}
          className={`px-4 sm:px-3 py-1.5 sm:py-1 rounded-md text-sm ${
            viewMode === "grid"
              ? "bg-green-500 text-black"
              : "bg-[#222] text-gray-400 hover:text-white"
          }`}
        >
          Grid View
        </button>
        <button
          onClick={() => setViewMode("compact")}
          className={`px-4 sm:px-3 py-1.5 sm:py-1 rounded-md text-sm ${
            viewMode === "compact"
              ? "bg-green-500 text-black"
              : "bg-[#222] text-gray-400 hover:text-white"
          }`}
        >
          Compact View
        </button>
      </div>

      {/* Up Next */}
      <div className="w-full max-w-3xl">
        <h3 className="text-lg sm:text-xl font-semibold text-green-400 mb-4 text-center">
          Up Next
        </h3>

        {upNext.length === 0 ? (
          <p className="text-gray-500 text-center">No upcoming tracks.</p>
        ) : viewMode === "compact" ? (
          // 🎧 COMPACT TABLE - Mobil optimize
          <div className="overflow-x-auto -mx-3 sm:mx-0">
            <div className="inline-block min-w-full align-middle">
              <table className="w-full text-left text-sm border border-gray-800 rounded-xl overflow-hidden">
                <thead className="bg-[#181818] text-gray-400 uppercase text-xs">
                  <tr>
                    <th className="py-3 px-2 sm:px-4 w-8 text-center">#</th>
                    <th className="py-3 px-2 sm:px-4">Track</th>
                    <th className="py-3 px-2 sm:px-4 text-right">Votes</th>
                  </tr>
                </thead>
                <tbody>
                  {upNext.map((track, index) => (
                    <tr
                      key={track.id}
                      className={`hover:bg-[#282828]/40 transition ${
                        track.inCooldown ? "opacity-60" : ""
                      }`}
                    >
                      <td className="py-3 px-2 sm:px-4 text-center text-gray-500">
                        {index + 1}
                      </td>
                      <td className="py-3 px-2 sm:px-4">
                        <div className="flex items-center gap-2 sm:gap-3">
                          <img
                            src={track.albumArt}
                            alt={track.name}
                            className="w-10 h-10 rounded object-cover flex-shrink-0"
                          />
                          <div className="flex flex-col min-w-0">
                            <span className="text-white truncate text-sm sm:text-base">
                              {track.name}
                            </span>
                            <span className="text-xs text-gray-400 truncate">
                              {track.artist}
                            </span>
                            {track.inCooldown && (
                              <span className="text-[10px] sm:text-[11px] text-orange-400 font-medium mt-1">
                                🕐 Cooldown — {track.cooldownRemaining} song
                                {track.cooldownRemaining !== 1 ? "s" : ""}
                              </span>
                            )}
                          </div>
                        </div>
                      </td>
                      <td className="py-3 px-2 sm:px-4 text-right">
                        <button
                          onClick={() => handleVote(track.id)}
                          className={`px-2 py-1 text-xs rounded-full transition whitespace-nowrap ${
                            track.inCooldown
                              ? "bg-gray-700 cursor-not-allowed"
                              : voted === track.id
                              ? "bg-green-500 text-black"
                              : "bg-gray-700 hover:bg-green-500 hover:text-black"
                          }`}
                          disabled={track.inCooldown || voted !== null}
                        >
                          {track.inCooldown
                            ? "Cooldown"
                            : voted === track.id
                            ? "✅"
                            : "Vote"}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        ) : (
          // GRID VIEW - Mobil optimize
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 sm:gap-6">
            {upNext.map((track) => (
              <div
                key={track.id}
                className={`bg-[#181818] p-4 rounded-xl border transition-all duration-300 ${
                  track.inCooldown
                    ? "opacity-60 border-gray-700"
                    : voted === track.id
                    ? "border-green-500 ring-1 ring-green-400 hover:scale-105"
                    : "border-gray-800 hover:scale-105"
                }`}
              >
                <img
                  src={track.albumArt}
                  alt={track.name}
                  className={`w-full h-32 sm:h-40 object-cover rounded-lg mb-3 ${
                    track.inCooldown ? "grayscale" : ""
                  }`}
                />
                <h4 className="font-semibold text-sm sm:text-base truncate">{track.name}</h4>
                <p className="text-gray-400 text-xs sm:text-sm mb-3 truncate">{track.artist}</p>

                {track.inCooldown ? (
                  <div className="text-center py-2 bg-gray-800/50 rounded-lg">
                    <p className="text-xs text-orange-400 font-medium">
                      🕐 Cooldown — {track.cooldownRemaining} song
                      {track.cooldownRemaining !== 1 ? "s" : ""}
                    </p>
                  </div>
                ) : (
                  <div className="flex justify-between items-center">
                    <span className="text-sm text-gray-500">
                      {votes[track.id] ?? track.votes ?? 0} votes
                    </span>
                    <button
                      onClick={() => handleVote(track.id)}
                      className={`px-3 py-1 rounded-full text-sm font-medium transition ${
                        voted === track.id
                          ? "bg-green-500 text-black cursor-not-allowed"
                          : "bg-gray-700 hover:bg-green-500 hover:text-black"
                      }`}
                      disabled={voted !== null}
                    >
                      {voted === track.id ? "Voted ✅" : "Vote"}
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Footer */}
      <div className="mt-8 sm:mt-10 text-gray-500 text-xs sm:text-sm text-center px-4">
        Connected to session:{" "}
        <span className="text-green-400 font-mono break-all">{ownerId}</span>
      </div>
    </div>
  );
}