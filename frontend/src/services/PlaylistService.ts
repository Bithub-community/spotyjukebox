import { Playlist, SpotifyApiResponse } from "@/models/PlayslistModels";
import { createSpotifyClient } from "./SpotifyClientBase";

/**
 * Service wrapper around Spotify Web API endpoints related to playlists and artists.
 * All requests are automatically authenticated via `baseClient(token)`.
 */
const PlaylistService = (token: string) => {
  const baseClient = createSpotifyClient(token); // Preconfigured fetch client with auth header

  return {
    /**
     * Search for Spotify content (tracks, artists, albums, etc.)
     * @param query - Search keywords.
     * @param type - Item type(s) to search for (e.g., "track", "artist").
     * @param limit - Maximum number of items to return.
     */
    search: (query: string, type: string, limit = 20) => {
      const params = new URLSearchParams({ q: query, type, limit: limit.toString() });
      baseClient(`/search?${params.toString()}`);
    },

    /**
     * Get playlists belonging to the authenticated user.
     * @param limit - Number of playlists to fetch.
     * @param offset - Pagination start index.
     */
    getUserPlaylists: (
      limit = 20,
      offset = 0
    ): Promise<SpotifyApiResponse<Playlist>> => {
      const params = new URLSearchParams({
        limit: limit.toString(),
        offset: offset.toString(),
      });
      return baseClient(`/me/playlists?${params.toString()}`);
    },

    /** Retrieve detailed info for a specific playlist by ID. */
    getPlaylist: (playlistId: string) => baseClient(`/playlists/${playlistId}`),

    /**
     * Add one or more tracks to a playlist.
     * @param playlistId - Target playlist ID.
     * @param uris - Array of Spotify track URIs.
     */
    addTracksToPlaylist: (playlistId: string, uris: string[]) => {
      return baseClient(`/playlists/${playlistId}/tracks`, {
        method: "POST",
        body: { uris },
      });
    },

    /** Fetch details for a specific artist by ID. */
    getArtist: (artistId: string) => baseClient(`/artists/${artistId}`),

    /**
     * Retrieve an artist's top tracks for a given market.
     * @param artistId - Spotify artist ID.
     * @param market - Market code (e.g., "US", "BE").
     */
    getArtistTopTracks: (artistId: string, market = "US") => {
      return baseClient(`/artists/${artistId}/top-tracks?market=${market}`);
    },
  };
};

export default PlaylistService;
