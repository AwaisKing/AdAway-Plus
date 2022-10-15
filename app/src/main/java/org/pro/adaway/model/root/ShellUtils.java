package org.pro.adaway.model.root;

import static com.topjohnwu.superuser.ShellUtils.escapedString;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.topjohnwu.superuser.Shell;

import org.pro.adaway.BuildConfig;

import java.io.File;
import java.util.List;

import awaisome.compat.Optional;

/**
 * This class is an utility class to help with shell commands.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public final class ShellUtils {
    private static final String EXECUTABLE_PREFIX = "lib";
    private static final String EXECUTABLE_SUFFIX = "_exec.so";

    /**
     * Private constructor.
     */
    private ShellUtils() {}

    @NonNull
    public static String mergeAllLines(final List<String> lines) {
        return String.join("\n", lines);
    }

    public static boolean isBundledExecutableRunning(final String executable) {
        return Shell.cmd("ps -A | grep " + EXECUTABLE_PREFIX + executable + EXECUTABLE_SUFFIX).exec().isSuccess();
    }

    public static boolean runBundledExecutable(@NonNull final Context context, final String executable, final String parameters) {
        final String nativeLibraryDir = context.getApplicationInfo().nativeLibraryDir;
        final String command = "LD_LIBRARY_PATH=" + nativeLibraryDir + " " +
                nativeLibraryDir + File.separator + EXECUTABLE_PREFIX + executable + EXECUTABLE_SUFFIX + " " +
                parameters + " &";
        return Shell.cmd(command).exec().isSuccess();
    }

    public static void killBundledExecutable(final String executable) {
        Shell.cmd("killall " + EXECUTABLE_PREFIX + executable + EXECUTABLE_SUFFIX).exec();
    }

    /**
     * Check if a path is writable.
     *
     * @param file The file to check.
     *
     * @return <code>true</code> if the path is writable, <code>false</code> otherwise.
     */
    public static boolean isWritable(@NonNull final File file) {
        // Check first if file can be written without privileges
        if (file.canWrite()) return true;
        return Shell.cmd("test -w " + escapedString(file.getAbsolutePath()))
                .exec()
                .isSuccess();
    }

    public static boolean remountPartition(final File file, final MountType type) {
        final Optional<String> partitionOptional = findPartition(file);
        if (!partitionOptional.isPresent()) return false;
        final String partition = partitionOptional.get();
        final Shell.Result result = Shell.cmd("mount -o " + type.getOption() + ",remount " + partition).exec();
        final boolean success = result.isSuccess();
        if (!success && BuildConfig.DEBUG)
            Log.w("AWAISKING_APP", "Failed to remount partition " + partition + " as " + type.getOption()
                    + ": " + mergeAllLines(result.getErr()));
        return success;
    }

    private static Optional<String> findPartition(File file) {
        // Get mount points
        final Shell.Result result = Shell.cmd("cat /proc/mounts | cut -d ' ' -f2").exec();
        final List<String> out = result.getOut();
        // Check file and each parent against mount points
        while (file != null) {
            final String path = file.getAbsolutePath();
            for (final String mount : out)
                if (path.equals(mount)) return Optional.of(mount);
            file = file.getParentFile();
        }
        return Optional.empty();
    }
}
