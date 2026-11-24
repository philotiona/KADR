package com.example.kaspikids;

import static org.junit.Assert.*;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AnnouncementDatabaseTest {
    private final Context context = ApplicationProvider.getApplicationContext();

    @Test public void inMemoryDaoUpsertsAndReadsLatestMessage() {
        AnnouncementDb db = Room.inMemoryDatabaseBuilder(context, AnnouncementDb.class).build();
        try {
            assertNull(db.announcements().latest());
            db.announcements().save(new AnnouncementDb.Entry(1, "First message", 100L));
            db.announcements().save(new AnnouncementDb.Entry(1, "Latest message", 200L));
            assertEquals("Latest message", db.announcements().latest().message);
            assertEquals(200L, db.announcements().latest().savedAt);
        } finally {
            db.close();
        }
    }

    @Test public void migrationFromVersionOnePreservesTheMessageAndAddsTheTimestamp() {
        String name = "lab7-migration-" + UUID.randomUUID() + ".db";
        try {
            try (SQLiteDatabase old = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)) {
                old.execSQL("CREATE TABLE announcements (id INTEGER NOT NULL PRIMARY KEY, message TEXT NOT NULL)");
                old.execSQL("INSERT INTO announcements (id, message) VALUES (1, 'Version one message')");
                old.setVersion(1);
            }

            AnnouncementDb db = AnnouncementDb.open(context, name);
            try {
                AnnouncementDb.Entry migrated = db.announcements().latest();
                assertEquals("Version one message", migrated.message);
                assertEquals(0L, migrated.savedAt);
                assertEquals(2, db.getOpenHelper().getReadableDatabase().getVersion());
            } finally {
                db.close();
            }
        } finally {
            context.deleteDatabase(name);
        }
    }
}
