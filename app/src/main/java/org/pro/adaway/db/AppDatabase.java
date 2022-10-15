package org.pro.adaway.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import org.pro.adaway.R;
import org.pro.adaway.db.converter.ListTypeConverter;
import org.pro.adaway.db.converter.ZonedDateTimeConverter;
import org.pro.adaway.db.dao.HostEntryDao;
import org.pro.adaway.db.dao.HostListItemDao;
import org.pro.adaway.db.dao.HostsSourceDao;
import org.pro.adaway.db.entity.HostListItem;
import org.pro.adaway.db.entity.HostsSource;
import org.pro.adaway.db.entity.HostEntry;
import org.pro.adaway.helper.BuiltinHostsHelper;
import org.pro.adaway.util.AppExecutors;

import static org.pro.adaway.db.Migrations.MIGRATION_1_2;
import static org.pro.adaway.db.Migrations.MIGRATION_2_3;
import static org.pro.adaway.db.Migrations.MIGRATION_3_4;
import static org.pro.adaway.db.Migrations.MIGRATION_4_5;
import static org.pro.adaway.db.Migrations.MIGRATION_5_6;
import static org.pro.adaway.db.Migrations.MIGRATION_6_7;
import static org.pro.adaway.db.entity.HostsSource.USER_SOURCE_ID;
import static org.pro.adaway.db.entity.HostsSource.USER_SOURCE_URL;

/**
 * This class is the application database based on Room.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
@Database(entities = {HostsSource.class, HostListItem.class, HostEntry.class}, version = 7)
@TypeConverters({ListTypeConverter.class, ZonedDateTimeConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    /**
     * The database singleton instance.
     */
    private static volatile AppDatabase instance;

    /**
     * Get the database instance.
     *
     * @param context The application context.
     *
     * @return The database instance.
     */
    public static AppDatabase getInstance(final Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "app.db"
                    ).addCallback(new Callback() {
                        @Override
                        public void onCreate(@NonNull final SupportSQLiteDatabase db) {
                            AppExecutors.getInstance().diskIO().execute(
                                    () -> AppDatabase.initialize(context, instance)
                            );
                        }
                    }).addMigrations(
                            MIGRATION_1_2,
                            MIGRATION_2_3,
                            MIGRATION_3_4,
                            MIGRATION_4_5,
                            MIGRATION_5_6,
                            MIGRATION_6_7
                    ).build();
                }
            }
        }
        return instance;
    }

    /**
     * Initialize the database content.
     */
    private static void initialize(final Context context, @NonNull final AppDatabase database) {
        // Check if there is no hosts source
        final HostsSourceDao hostsSourceDao = database.hostsSourceDao();
        if (!hostsSourceDao.getAll().isEmpty()) return;
        // User list
        final HostsSource userSource = new HostsSource();
        userSource.setLabel(context.getString(R.string.hosts_user_source));
        userSource.setId(USER_SOURCE_ID);
        userSource.setUrl(USER_SOURCE_URL);
        userSource.setAllowEnabled(true);
        userSource.setRedirectEnabled(true);
        hostsSourceDao.insert(userSource);

        BuiltinHostsHelper.insertBuiltinHostsInDAO(context, hostsSourceDao);
    }

    /**
     * Get the hosts source DAO.
     *
     * @return The hosts source DAO.
     */
    public abstract HostsSourceDao hostsSourceDao();

    /**
     * Get the hosts list item DAO.
     *
     * @return The hosts list item DAO.
     */
    public abstract HostListItemDao hostsListItemDao();

    /**
     * Get the hosts entry DAO.
     *
     * @return The hosts entry DAO.
     */
    public abstract HostEntryDao hostEntryDao();
}
