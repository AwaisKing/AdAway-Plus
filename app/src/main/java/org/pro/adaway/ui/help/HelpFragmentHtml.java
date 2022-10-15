/*
 * Copyright (C) 2011-2012 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This file is part of AdAway.
 *
 * AdAway is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AdAway is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AdAway.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.pro.adaway.ui.help;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;

import org.pro.adaway.BuildConfig;
import org.pro.adaway.databinding.HelpFragmentBinding;

import java.io.IOException;
import java.io.InputStream;

public class HelpFragmentHtml extends Fragment {
    private int htmlFile = View.NO_ID;

    public HelpFragmentHtml() {
        super();
    }

    public HelpFragmentHtml setHtmlFile(final int htmlFile) {
        this.htmlFile = htmlFile;
        return this;
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        Spanned spanned = new SpannableString("");
        try {
            spanned = HtmlCompat.fromHtml(readHtmlRawFile(htmlFile), HtmlCompat.FROM_HTML_MODE_LEGACY);
        } catch (final IOException e) {
            if (BuildConfig.DEBUG) {
                Log.w("AWAISKING_APP", "Failed to read help file.");
            }
        }

        final HelpFragmentBinding fragmentBinding = HelpFragmentBinding.inflate(inflater, container, false);

        fragmentBinding.helpTextView.setText(spanned);
        fragmentBinding.helpTextView.setMovementMethod(LinkMovementMethod.getInstance());

        return fragmentBinding.getRoot();
    }

    @NonNull
    private String readHtmlRawFile(@RawRes final int resourceId) throws IOException {
        try (final InputStream inputStream = getResources().openRawResource(resourceId)) {
            final StringBuilder content = new StringBuilder();
            int read;
            while ((read = inputStream.read()) != -1) content.append((char) read);
            return content.toString();
        }
    }
}
