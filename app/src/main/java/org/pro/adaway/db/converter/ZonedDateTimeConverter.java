package org.pro.adaway.db.converter;

import static org.threeten.bp.ZoneOffset.UTC;

import androidx.room.TypeConverter;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

/**
 * This class is a type converter for Room to support {@link ZonedDateTime} type.
 * It is stored as a Unix epoc timestamp.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public final class ZonedDateTimeConverter {
    private ZonedDateTimeConverter() {
        // Prevent instantiation
    }

    @TypeConverter
    public static ZonedDateTime fromTimestamp(Long value) {
        return value == null ? null : ZonedDateTime.of(LocalDateTime.ofEpochSecond(value, 0,
                UTC), UTC);
    }

    @TypeConverter
    public static Long toTimestamp(ZonedDateTime zonedDateTime) {
        return zonedDateTime == null ? null : zonedDateTime.toEpochSecond();
    }
}