import { createSpotifyClient } from "./SpotifyClientBase";

const UserService = (token: string) => {

    const baseClient = createSpotifyClient(token);
    return {
        /**
        * Search for items in Spotify.
        * @param query The search query string.
        * @param type The type of items to search for (e.g., "track", "artist").
        * @param limit The number of items to return.
        * @returns The search results.
        */
        registerUser: () => {
            const body = ({name: "x"})
            baseClient(`/userRegiser`);
        },
    }
}