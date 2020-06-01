package rocks.tbog.tblauncher.entry;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.io.FileNotFoundException;
import java.io.InputStream;

import rocks.tbog.tblauncher.BuildConfig;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.result.ResultHelper;
import rocks.tbog.tblauncher.result.ResultViewHelper;
import rocks.tbog.tblauncher.utils.FuzzyScore;
import rocks.tbog.tblauncher.utils.UIColors;

public final class ContactEntry extends EntryItem {
    public static final String SCHEME = "contact://";
    public final String lookupKey;

    public final String phone;
    //phone without special characters
    public final StringNormalizer.Result normalizedPhone;
    public final Uri iconUri;

    // Is this a primary phone?
    private final boolean primary;

    // How many times did we phone this contact?
    public final int timesContacted;

    // Is this contact starred ?
    public final Boolean starred;

    // Is this number a home (local) number ?
    public final Boolean homeNumber;

    public StringNormalizer.Result normalizedNickname = null;

    private String nickname = "";

    public ContactEntry(String id, String lookupKey, String phone, StringNormalizer.Result normalizedPhone,
                        Uri iconUri, Boolean primary, int timesContacted, Boolean starred,
                        Boolean homeNumber) {
        super(id);
        if (BuildConfig.DEBUG && !id.startsWith(SCHEME)) {
            throw new IllegalStateException("Invalid " + ContactEntry.class.getSimpleName() + " id `" + id + "`");
        }
        this.lookupKey = lookupKey;
        this.phone = phone;
        this.normalizedPhone = normalizedPhone;
        this.iconUri = iconUri;
        this.primary = primary;
        this.timesContacted = timesContacted;
        this.starred = starred;
        this.homeNumber = homeNumber;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        if (nickname != null) {
            // Set the actual user-friendly name
            this.nickname = nickname;
            this.normalizedNickname = StringNormalizer.normalizeWithResult(this.nickname, false);
        } else {
            this.nickname = null;
            this.normalizedNickname = null;
        }
    }

    public boolean isPrimary() {
        return primary;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Result methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public int getResultLayout() {
        return R.layout.item_contact;
    }

    @Override
    public void displayResult(@NonNull View view) {
        Context context = view.getContext();
        // Contact name
        TextView contactName = view.findViewById(R.id.item_contact_name);
        ResultViewHelper.displayHighlighted(relevanceSource, normalizedName, getName(), relevance, contactName);

        // Contact phone
        TextView contactPhone = view.findViewById(R.id.item_contact_phone);
        ResultViewHelper.displayHighlighted(relevanceSource, normalizedPhone, phone, relevance, contactPhone);

        // Contact nickname
        TextView contactNickname = view.findViewById(R.id.item_contact_nickname);
        if (getNickname().isEmpty()) {
            contactNickname.setVisibility(View.GONE);
        } else {
            ResultViewHelper.displayHighlighted(relevanceSource, normalizedNickname, getNickname(), relevance, contactNickname);
        }

        // Contact photo
        ImageView contactIcon = view.findViewById(R.id.item_contact_icon);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.getBoolean("icons-hide", false)) {
            ResultViewHelper.setIconAsync(this, contactIcon, AsyncSetEntryIcon.class);
        } else {
            contactIcon.setImageDrawable(null);
        }

//        contactIcon.assignContactUri(Uri.withAppendedPath(
//                ContactsContract.Contacts.CONTENT_LOOKUP_URI,
//                String.valueOf(contactPojo.lookupKey)));
//        contactIcon.setExtraOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                recordLaunch(v.getContext(), queryInterface);
//            }
//        });

        int primaryColor = UIColors.getPrimaryColor(context);
        // Phone action
        ImageButton phoneButton = view.findViewById(R.id.item_contact_action_phone);
        phoneButton.setColorFilter(primaryColor, PorterDuff.Mode.MULTIPLY);
        // Message action
        ImageButton messageButton = view.findViewById(R.id.item_contact_action_message);
        messageButton.setColorFilter(primaryColor, PorterDuff.Mode.MULTIPLY);

        PackageManager pm = context.getPackageManager();

        if (pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            phoneButton.setVisibility(View.VISIBLE);
            messageButton.setVisibility(View.VISIBLE);
            phoneButton.setOnClickListener(v -> {
                ResultHelper.recordLaunch(this, context);
                ResultHelper.launchCall(v.getContext(), v, phone);
            });

            messageButton.setOnClickListener(v -> {
                ResultHelper.recordLaunch(this, context);
                ResultHelper.launchMessaging(this, v);
            });

            if (homeNumber)
                messageButton.setVisibility(View.INVISIBLE);
            else
                messageButton.setVisibility(View.VISIBLE);

        } else {
            phoneButton.setVisibility(View.INVISIBLE);
            messageButton.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void doLaunch(View v) {
        Context context = v.getContext();
        SharedPreferences settingPrefs = PreferenceManager.getDefaultSharedPreferences(v.getContext());
        boolean callContactOnClick = settingPrefs.getBoolean("call-contact-on-click", false);

        if (callContactOnClick) {
            ResultHelper.launchCall(context, v, phone);
        } else {
            ResultHelper.launchContactView(this, context, v);
        }
    }

    public static class AsyncSetEntryIcon extends ResultViewHelper.AsyncSetEntryDrawable {
        public AsyncSetEntryIcon(ImageView image) {
            super(image);
        }

        protected Drawable getDrawable(EntryItem entry, Context ctx) {
            Uri iconUri = ((ContactEntry) entry).iconUri;
            Drawable drawable = null;
            if (iconUri != null)
                try {
                    InputStream inputStream = ctx.getContentResolver().openInputStream(iconUri);
                    drawable = Drawable.createFromStream(inputStream, iconUri.toString());
                } catch (FileNotFoundException ignored) {
                }
            if (drawable == null) {
                drawable = ContextCompat.getDrawable(ctx, R.drawable.ic_contact);
                if (drawable == null)
                    drawable = new ColorDrawable(UIColors.getPrimaryColor(ctx));
            }
            return TBApplication.iconsHandler(ctx).applyContactMask(ctx, drawable);
        }
    }

}
