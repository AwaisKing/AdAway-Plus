package awaisome.compat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CreateDocument extends ActivityResultContract<String, Uri> {
    @NonNull
    private final String mimeType;

    public CreateDocument(@NonNull final String mimeType) {
        super();
        this.mimeType = mimeType;
    }

    @CallSuper
    @NonNull
    @Override
    public Intent createIntent(@NonNull final Context context, @NonNull final String input) {
        return new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .setType(mimeType)
                .putExtra(Intent.EXTRA_TITLE, input);
    }

    @Nullable
    @Override
    public final SynchronousResult<Uri> getSynchronousResult(@NonNull final Context context,
                                                             @NonNull final String input) {
        return null;
    }

    @Nullable
    @Override
    public final Uri parseResult(final int resultCode, @Nullable final Intent intent) {
        if (intent == null || resultCode != Activity.RESULT_OK) return null;
        return intent.getData();
    }
}
