import { useContext, useEffect, useState } from 'react';
import { LogOut } from 'lucide-react';
import { AuthContext } from '@/context/AuthProvider';
import axios from 'axios';
import { Playlist } from '@/models/PlayslistModels';

const PostLoginPage = () => {

  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

  const { user, logout } = useContext(AuthContext);

  const [playlists, setPlaylists] = useState<Playlist[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [spotifyProfile, setSpotifyProfile] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  // 🔹 Fetch Spotify profile info from backend after login
  useEffect(() => {
    const loadUserProfile = async () => {
      try {
        if (!user?.id) {
          setError('User not found');
          setLoading(false);
          return;
        }

        // Retrieve user’s Spotify profile from backend
        const profileRes = await axios.get(
          `${API_BASE_URL}/api/auth/spotify/me/${user.id}`
        );

        console.log('✅ Backend profile:', profileRes.data);
        setSpotifyProfile(profileRes.data);
      } catch (err: any) {
        console.error('❌ Failed to fetch profile:', err);
        setError('Failed to load user profile');
      } finally {
        setLoading(false);
      }
    };

    loadUserProfile();
  }, [user?.id, API_BASE_URL]);

  // 🔹 Fetch user playlists from backend
  useEffect(() => {
    const loadPlaylists = async () => {
      if (!user?.id) return;

      try {
        const res = await axios.get(
          `${API_BASE_URL}/api/playlists/${user.id}`
        );
        console.log('🎵 Playlists from backend:', res.data);
        setPlaylists(res.data.items || []);
      } catch (err: any) {
        console.error('❌ Playlist fetch error:', err);
        setError('Failed to load playlists');
      }
    };

    loadPlaylists();
  }, [user?.id, API_BASE_URL]);

  // 🎧 Redirect user to Spotify OAuth authorization flow
  const handleSpotifyConnect = () => {
    const clientId = import.meta.env.VITE_SPOTIFY_CLIENT_ID;
    const redirectUri = import.meta.env.VITE_SPOTIFY_REDIRECT_URI;
    const scopes =import.meta.env.VITE_SPOTIFY_SCOPES;

    const SPOTIFY_AUTH_URL = "https://accounts.spotify.com/authorize";

    const url = `${SPOTIFY_AUTH_URL}?client_id=${clientId}&response_type=code&redirect_uri=${encodeURIComponent(
      redirectUri
    )}&scope=${encodeURIComponent(scopes)}`;

    window.location.href = url;
  };

  // Loading state while fetching user data
  if (loading) {
    return (
      <div className="min-h-screen flex justify-center items-center">
        <p className="text-gray-600">Loading user data...</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-50 to-gray-100 py-8 px-4">
      <div className="max-w-4xl mx-auto">
        {/* Profile Header */}
        <div className="bg-white rounded-lg shadow-lg p-6 mb-8">
          <div className="flex justify-between items-center">
            <div>
              <h1 className="text-4xl font-bold text-gray-800">
                Spotify Jukebox
              </h1>
              <h2 className="mt-2 text-gray-600">
                Welcome, {spotifyProfile?.displayName || 'User'}
              </h2>

              {/* Button for connecting to Spotify if not linked */}
              {!spotifyProfile && (
                <button
                  onClick={handleSpotifyConnect}
                  className="bg-green-600 hover:bg-green-700 text-white px-6 py-3 rounded-lg font-semibold mt-4"
                >
                  Connect Spotify
                </button>
              )}
            </div>

            {/* Logout */}
            <button
              onClick={logout}
              className="flex items-center gap-2 px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-lg transition-colors"
            >
              <LogOut size={20} />
              Logout
            </button>
          </div>

          {/* Error message */}
          {error && (
            <div className="bg-red-100 text-red-700 p-4 rounded mt-4">
              {error}
            </div>
          )}

          {/* Display Spotify profile info if available */}
          {spotifyProfile && (
            <div className="mt-4 p-4 bg-gray-50 rounded-md">
              <p className="text-sm text-gray-600 font-mono break-all">
                Spotify ID: {spotifyProfile.userId} <br />
                Email: {spotifyProfile.email} <br />
                Display Name: {spotifyProfile.displayName}
              </p>
            </div>
          )}
        </div>

        {/* Playlist Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {playlists.map((playlist) => (
            <div
              key={playlist.id}
              className="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow p-6 cursor-pointer"
            >
              {playlist.images?.[0] ? (
                <img
                  src={playlist.images[0].url}
                  alt={playlist.name}
                  className="rounded-lg mb-3"
                />
              ) : (
                <div className="w-full h-32 bg-gray-200 rounded-lg mb-3 flex items-center justify-center text-gray-400">
                  No image
                </div>
              )}
              <h2 className="text-xl font-semibold text-gray-800 mb-1">
                {playlist.name}
              </h2>
              <p className="text-sm text-gray-500">
                {playlist.tracks?.total || 0} tracks
              </p>
            </div>
          ))}
        </div>

        {/* Empty state */}
        {(!playlists || playlists.length === 0) && !error && (
          <div className="text-center py-12 text-gray-500">
            No playlists found
          </div>
        )}
      </div>
    </div>
  );
};

export default PostLoginPage;
