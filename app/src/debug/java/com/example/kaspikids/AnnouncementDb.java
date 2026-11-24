package com.example.kaspikids;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = AnnouncementDb.Entry.class, version = 2, exportSchema = false)
public abstract class AnnouncementDb extends RoomDatabase {
    static final Migration FROM_1_TO_2 = new Migration(1, 2) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE announcements ADD COLUMN saved_at INTEGER NOT NULL DEFAULT 0");
        }
    };

    @Entity(tableName = "announcements")
    public static final class Entry {
        @PrimaryKey public final int id;
        @NonNull public final String message;
        @ColumnInfo(name = "saved_at", defaultValue = "0") public final long savedAt;

        public Entry(int id, @NonNull String message, long savedAt) {
            this.id = id;
            this.message = message;
            this.savedAt = savedAt;
        }
    }

    @Dao
    public interface Announcements {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void save(Entry entry);

        @Query("SELECT * FROM announcements WHERE id = 1")
        Entry latest();
    }

    public abstract Announcements announcements();

    static AnnouncementDb open(Context context, String name) {
        return Room.databaseBuilder(context, AnnouncementDb.class, name)
                .addMigrations(FROM_1_TO_2).build();
    }
}
