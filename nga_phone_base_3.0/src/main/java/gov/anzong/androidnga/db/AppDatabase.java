package gov.anzong.androidnga.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import gov.anzong.androidnga.db.note.NoteDao;
import gov.anzong.androidnga.db.topic.TopicHistoryDao;
import gov.anzong.androidnga.db.user.UserDao;
import sp.phone.common.NoteInfo;
import sp.phone.common.TopicHistoryInfo;
import sp.phone.common.User;

/**
 * @author yangyihang
 */
@Database(entities = {User.class, NoteInfo.class, TopicHistoryInfo.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String MAIN_DB_NAME = "app_database.db";

    private static AppDatabase sInstance;

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `topic_history` (`tid` INTEGER NOT NULL, `author` TEXT, `fid` INTEGER NOT NULL, `author_id` INTEGER NOT NULL, `last_poster` TEXT, `replies` INTEGER NOT NULL, `subject` TEXT, `title_font` TEXT, `type` INTEGER NOT NULL, `topic_misc` TEXT, `page` INTEGER NOT NULL, `post_date` INTEGER NOT NULL, `board` TEXT, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`tid`))");
        }
    };

    public static void init(Context context) {
        sInstance = Room.databaseBuilder(context, AppDatabase.class, MAIN_DB_NAME)
                .allowMainThreadQueries()
                .addMigrations(MIGRATION_3_4)
                .build();
    }

    public static AppDatabase getInstance() {
        return sInstance;
    }

    public abstract UserDao userDao();

    public abstract NoteDao noteDao();

    public abstract TopicHistoryDao topicHistoryDao();

}
