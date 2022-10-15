package org.pro.adaway.ui.lists;

import androidx.annotation.NonNull;

/**
 * This class represents the filter to apply to host lists.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class ListsFilter {
    public static final ListsFilter ALL = new ListsFilter(true, "");
    /**
     * Whether included hosts from sources or not.
     */
    public final boolean sourcesIncluded;
    /**
     * The query filter to apply to hosts name (wildcard based).
     */
    public final String query;
    /**
     * The query filter to apply to hosts name (sql like format).
     */
    public final String sqlQuery;

    public ListsFilter(final boolean sourcesIncluded, @NonNull final String query) {
        this.sqlQuery = "%" + query
                .replaceAll("\\*", "%")
                .replaceAll("\\?", "_")
                + "%";
        this.query = query;
        this.sourcesIncluded = sourcesIncluded;
    }
}
