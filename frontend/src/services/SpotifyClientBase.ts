const BASE_URL = "https://api.spotify.com/v1";

type FetchOptions = Omit<RequestInit, "body"> & {
  body?: Record<string, any> | null;
};

/**
 * Creates a lightweight, token-aware Spotify Web API client.
 * Handles rate limiting, JSON parsing, and error wrapping.
 */
export const createSpotifyClient = (accessToken: string) => {
  const fetchSpotify = async (endpoint: string, options: FetchOptions = {}) => {
    const { body, ...restOptions } = options;

    // 🔹 Include access token in every request
    const headers: HeadersInit = {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    };

    const response = await fetch(`${BASE_URL}${endpoint}`, {
      ...restOptions,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    });

    // ⚠️ Handle Spotify rate limits (HTTP 429)
    if (response.status === 429) {
      const retryAfter = parseInt(response.headers.get("Retry-After") || "1", 10);
      console.warn(`Spotify API rate limit hit. Retrying after ${retryAfter} seconds...`);
      await new Promise((res) => setTimeout(res, retryAfter * 1000));
      return fetchSpotify(endpoint, options); // retry once recursively
    }

    // Always read response text (Spotify may send non-JSON errors)
    const raw = await response.text();

    // ❌ Throw detailed error if request failed
    if (!response.ok) {
      let errorMessage: string;
      try {
        const errJson = JSON.parse(raw);
        errorMessage = errJson.error?.message || JSON.stringify(errJson);
      } catch {
        errorMessage = raw || response.statusText;
      }
      throw new Error(`Spotify API error (${response.status}): ${errorMessage}`);
    }

    // ✅ Return parsed JSON if successful, or null for empty (204)
    if (response.status === 204 || !raw) return null;

    try {
      return JSON.parse(raw);
    } catch {
      // Return raw text if response isn’t valid JSON
      return raw;
    }
  };

  return fetchSpotify;
};
