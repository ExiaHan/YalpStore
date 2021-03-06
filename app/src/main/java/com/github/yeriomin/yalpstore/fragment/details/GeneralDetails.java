package com.github.yeriomin.yalpstore.fragment.details;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.yeriomin.yalpstore.CategoryAppsActivity;
import com.github.yeriomin.yalpstore.CategoryManager;
import com.github.yeriomin.yalpstore.DetailsActivity;
import com.github.yeriomin.yalpstore.R;
import com.github.yeriomin.yalpstore.Util;
import com.github.yeriomin.yalpstore.model.App;
import com.github.yeriomin.yalpstore.model.ImageSource;
import com.github.yeriomin.yalpstore.task.LoadImageTask;
import com.github.yeriomin.yalpstore.widget.Badge;
import com.github.yeriomin.yalpstore.widget.ExpansionPanel;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GeneralDetails extends Abstract {

    public GeneralDetails(DetailsActivity activity, App app) {
        super(activity, app);
    }

    @Override
    public void draw() {
        drawAppBadge(app);
        if (app.isInPlayStore()) {
            drawGeneralDetails(app);
            drawDescription(app);
            new GoogleDependency((DetailsActivity) activity, app).draw();
        }
    }

    private void drawAppBadge(App app) {
        new LoadImageTask((ImageView) activity.findViewById(R.id.icon)).setPlaceholder(false).execute(app.getIconInfo());

        setText(R.id.displayName, app.getDisplayName());
        setText(R.id.packageName, app.getPackageName());
        drawVersion((TextView) activity.findViewById(R.id.versionString), app);
    }

    private void drawGeneralDetails(App app) {
        activity.findViewById(R.id.general_details).setVisibility(View.VISIBLE);
        setText(R.id.updated, R.string.details_updated, app.getUpdated());
        setText(R.id.developer, R.string.details_developer, app.getDeveloperName());
        setText(R.id.price_and_ads, app.getPrice() + ", " + activity.getString(app.containsAds() ? R.string.details_contains_ads : R.string.details_no_ads));
        drawOfferDetails(app);
        drawChanges(app);
        if (app.getVersionCode() == 0) {
            activity.findViewById(R.id.updated).setVisibility(View.GONE);
        }
        drawBadges();
    }

    private void drawChanges(App app) {
        String changes = app.getChanges();
        if (TextUtils.isEmpty(changes)) {
            activity.findViewById(R.id.changes_in_details).setVisibility(View.GONE);
            activity.findViewById(R.id.changes_title).setVisibility(View.GONE);
            activity.findViewById(R.id.changes_upper).setVisibility(View.GONE);
            return;
        }
        if (app.getInstalledVersionCode() == 0) {
            setText(R.id.changes_in_details, Html.fromHtml(changes).toString());
            activity.findViewById(R.id.changes_in_details).setVisibility(View.VISIBLE);
            activity.findViewById(R.id.changes_title).setVisibility(View.VISIBLE);
        } else {
            setText(R.id.changes_upper, Html.fromHtml(changes).toString());
            ExpansionPanel changesPanel = activity.findViewById(R.id.changes_panel);
            changesPanel.setVisibility(View.VISIBLE);
            changesPanel.toggle();
        }
    }

    private void drawOfferDetails(App app) {
        List<String> keyList = new ArrayList<>(app.getOfferDetails().keySet());
        Collections.reverse(keyList);
        for (String key: keyList) {
            addOfferItem(key, app.getOfferDetails().get(key));
        }
    }

    private void addOfferItem(String key, String value) {
        if (null == value) {
            return;
        }
        TextView itemView = new TextView(activity);
        try {
            itemView.setAutoLinkMask(Linkify.ALL);
            itemView.setText(activity.getString(R.string.two_items, key, Html.fromHtml(value)));
        } catch (RuntimeException e) {
            Log.w(getClass().getSimpleName(), "System WebView missing: " + e.getMessage());
            itemView.setAutoLinkMask(0);
            itemView.setText(activity.getString(R.string.two_items, key, Html.fromHtml(value)));
        }
        ((LinearLayout) activity.findViewById(R.id.offer_details)).addView(itemView);
    }

    private void drawVersion(TextView textView, App app) {
        String versionName = app.getVersionName();
        if (TextUtils.isEmpty(versionName)) {
            return;
        }
        textView.setText(activity.getString(R.string.details_versionName, versionName));
        textView.setVisibility(View.VISIBLE);
        if (!app.isInstalled()) {
            return;
        }
        try {
            PackageInfo info = activity.getPackageManager().getPackageInfo(app.getPackageName(), 0);
            String currentVersion = info.versionName;
            if (info.versionCode == app.getVersionCode() || null == currentVersion) {
                return;
            }
            String newVersion = versionName;
            if (currentVersion.equals(newVersion)) {
                currentVersion += " (" + info.versionCode;
                newVersion = app.getVersionCode() + ")";
            }
            textView.setText(activity.getString(R.string.details_versionName_updatable, currentVersion, newVersion));
        } catch (PackageManager.NameNotFoundException e) {
            // We've checked for that already
        }
    }

    private void drawDescription(App app) {
        ExpansionPanel descriptionPanel = activity.findViewById(R.id.description_panel);
        if (TextUtils.isEmpty(app.getDescription())) {
            descriptionPanel.setVisibility(View.GONE);
        } else {
            descriptionPanel.setVisibility(View.VISIBLE);
            setText(R.id.description, Html.fromHtml(app.getDescription()).toString());
            if (app.getInstalledVersionCode() == 0 || TextUtils.isEmpty(app.getChanges())) {
                descriptionPanel.toggle();
            }
        }
    }

    private void drawBadges() {
        ((Badge) activity.findViewById(R.id.downloads_badge)).setLabel(Util.addSiPrefix(app.getInstalls()));
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(1);
        ((Badge) activity.findViewById(R.id.rating_badge)).setLabel(app.isEarlyAccess() ? activity.getString(R.string.early_access) : df.format(app.getRating().getAverage()));
        ((Badge) activity.findViewById(R.id.size_badge)).setLabel(Formatter.formatShortFileSize(activity, app.getSize()));
        Badge categoryBadge = activity.findViewById(R.id.category_badge);
        categoryBadge.setLabel(new CategoryManager(activity).getCategoryName(app.getCategoryId()));
        categoryBadge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CategoryAppsActivity.start(activity, app.getCategoryId());
            }
        });
        new LoadImageTask(categoryBadge.getIconView()).setPlaceholder(false).execute(new ImageSource(app.getCategoryIconUrl()));
        ViewGroup.LayoutParams iconParams = categoryBadge.getIconView().getLayoutParams();
        int categoryIconSize = Util.getPx(activity, 64);
        iconParams.width = categoryIconSize;
        iconParams.height = categoryIconSize;
    }
}
