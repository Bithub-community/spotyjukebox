import React, { useContext, useEffect, useState } from 'react';
import { LogOut } from 'lucide-react';
import { AuthContext } from '@/context/AuthProvider';
import PlaylistService from '@/services/PlaylistService';
import { Playlist } from '@/models/PlayslistModels';

const PostLoginPage = () => {
    const { user, signoutCallback } = useContext(AuthContext);
    // Initialize the client with your Spotify API access token
    const playlistService = user?.access_token ? PlaylistService(user.access_token) : null;
    const [playlists, setPlaylists] = useState<Playlist[]>([]);

    useEffect(() => {
        if (!playlistService) return;
        playlistService.getUserPlaylists().then((data) => setPlaylists(data.items));
    }, [user]);
    return (
        <div className="min-h-screen bg-gradient-to-b from-gray-50 to-gray-100 py-8 px-4">
            <div className="max-w-4xl mx-auto">
                {/* Header Section */}
                <div className="bg-white rounded-lg shadow-lg p-6 mb-8">
                    <div className="flex justify-between items-center">
                        <div>
                            <h1 className="text-4xl font-bold text-gray-800">Protected Page</h1>
                            <p className="mt-2 text-gray-600">Welcome, {user?.profile.family_name || 'User'}</p>
                        </div>
                        <button
                            onClick={() => {
                                console.log('Clicked Logout Button')
                                signoutCallback()}}
                            className="flex items-center gap-2 px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-lg transition-colors"
                        >
                            <LogOut size={20} />
                            Logout
                        </button>
                    </div>

                    <div className="mt-4 p-4 bg-gray-50 rounded-md">
                        <p className="text-sm text-gray-500 font-mono break-all">
                            Access Token: {user?.access_token}
                            <br />
                            Refresh Token: {user?.refresh_token}
                            <br />
                            Expires In: {user?.expires_in}
                            <br />
                            Scopes: {user?.scope}
                            <br />
                            id token : {user?.id_token}
                        </p>
                    </div>
                </div>

                {/* Playlists Grid */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {playlists?.map((playlist) => (
                        <div
                            key={playlist.id}
                            className="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow p-6"
                        >
                            <h2 className="text-xl font-semibold text-gray-800 mb-2">
                                {playlist?.name}
                            </h2>
                            {playlist.owner.display_name}
                            {playlist.description && (
                                <p className="text-gray-600">
                                    {playlist?.description}
                                </p>
                            )}
                        </div>
                    ))}
                </div>

                {/* Empty State */}
                {(!playlists || playlists.length === 0) && (
                    <div className="text-center py-12">
                        <p className="text-gray-500">No playlists found</p>
                    </div>
                )}
            </div>
        </div>
    );
};

export default PostLoginPage;