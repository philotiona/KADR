package com.example.kaspikids;

import java.io.IOException;

final class AnnouncementService {
    interface Api {
        String fetch() throws IOException;
    }

    interface Cache {
        void save(String message);
    }

    private final Api api;
    private final Cache cache;

    AnnouncementService(Api api, Cache cache) {
        this.api = api;
        this.cache = cache;
    }

    String refresh() throws IOException {
        String message = api.fetch();
        cache.save(message);
        return message;
    }
}
