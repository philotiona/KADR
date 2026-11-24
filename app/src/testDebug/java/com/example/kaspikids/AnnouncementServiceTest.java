package com.example.kaspikids;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

import java.io.IOException;
import org.junit.Test;

public class AnnouncementServiceTest {
    private final AnnouncementService.Api api = mock(AnnouncementService.Api.class);
    private final AnnouncementService.Cache cache = mock(AnnouncementService.Cache.class);
    private final AnnouncementService service = new AnnouncementService(api, cache);

    @Test public void refreshStoresTheAnnouncementReturnedByTheApi() throws IOException {
        when(api.fetch()).thenReturn("Вечерний сеанс");

        String message = service.refresh();

        assertEquals("Вечерний сеанс", message);
        verify(api).fetch();
        verify(cache).save(message);
    }

    @Test public void failedRefreshLeavesTheCacheAlone() throws IOException {
        when(api.fetch()).thenThrow(new IOException("HTTP 500"));

        assertThrows(IOException.class, service::refresh);
        verify(api).fetch();
        verifyNoInteractions(cache);
    }
}
