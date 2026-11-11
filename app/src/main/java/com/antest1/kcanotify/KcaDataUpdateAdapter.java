package com.antest1.kcanotify;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.BaseProgressIndicator;

import java.util.ArrayList;
import java.util.List;

// List of the data update screen: a summary card, section headers and update entries.
// The screen builds the rows from the current state and replaces them as a whole on every change.
public class KcaDataUpdateAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int PROGRESS_HIDDEN = -2;
    public static final int PROGRESS_INDETERMINATE = -1;

    private static final int TYPE_SUMMARY = 0;
    private static final int TYPE_HEADER = 1;
    private static final int TYPE_ENTRY = 2;

    public enum CardStyle { NORMAL, PROGRESS, ERROR }

    public enum Trailing { NONE, UPDATE, LATEST, QUEUED, DOWNLOADING, DONE, FAILED, NOT_READY }

    public interface SummaryActions {
        void onCancel();
        void onRetry();
        void onUpdateAll();
    }

    public static class Summary {
        public CardStyle style = CardStyle.NORMAL;
        public int icon = 0;
        public @AttrRes int iconTint = 0;
        public boolean iconSuccess = false;
        public String title = "";
        public String body = "";
        public int progress = PROGRESS_HIDDEN;
        public boolean cancel, retry, updateAll;
    }

    public static class Row {
        final int type;
        final String key;
        Summary summary;
        String title;
        String supporting;
        int icon;
        boolean sub;
        boolean expandable, expanded;
        // keeps the space of the expand icon, so that the states line up with expandable rows
        boolean expandSpace = true;
        int progress = PROGRESS_HIDDEN;
        Trailing trailing = Trailing.NONE;
        int trailingProgress = PROGRESS_INDETERMINATE;
        Runnable onClick, onTrailingClick;

        private Row(int type, String key) {
            this.type = type;
            this.key = key;
        }

        public static Row summary(Summary summary) {
            Row row = new Row(TYPE_SUMMARY, "summary");
            row.summary = summary;
            return row;
        }

        public static Row header(String key, String title) {
            Row row = new Row(TYPE_HEADER, "header:" + key);
            row.title = title;
            return row;
        }

        public static Row entry(String key, int icon, String title, String supporting) {
            Row row = new Row(TYPE_ENTRY, key);
            row.icon = icon;
            row.title = title;
            row.supporting = supporting;
            return row;
        }

        // a file row under an expanded category
        public static Row file(String key, String title, String supporting) {
            Row row = entry(key, 0, title, supporting);
            row.sub = true;
            return row;
        }

        public Row expandable(boolean expanded, Runnable onClick) {
            this.expandable = true;
            this.expanded = expanded;
            this.onClick = onClick;
            return this;
        }

        public Row noExpandSpace() {
            this.expandSpace = false;
            return this;
        }

        public Row progress(int progress) {
            this.progress = progress;
            return this;
        }

        public Row trailing(Trailing trailing, int progress, Runnable onClick) {
            this.trailing = trailing;
            this.trailingProgress = progress;
            this.onTrailingClick = onClick;
            return this;
        }
    }

    private final SummaryActions actions;
    private List<Row> rows = new ArrayList<>();

    public KcaDataUpdateAdapter(SummaryActions actions) {
        this.actions = actions;
        setHasStableIds(true);
    }

    public void setRows(List<Row> rows) {
        this.rows = rows;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @Override
    public long getItemId(int position) {
        return rows.get(position).key.hashCode();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_SUMMARY:
                return new SummaryHolder(inflater.inflate(R.layout.item_data_update_summary, parent, false));
            case TYPE_HEADER:
                return new HeaderHolder(inflater.inflate(R.layout.item_data_update_header, parent, false));
            default:
                return new EntryHolder(inflater.inflate(R.layout.item_data_update_entry, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof SummaryHolder) {
            ((SummaryHolder) holder).bind(row.summary, actions);
        } else if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).title.setText(row.title);
        } else {
            ((EntryHolder) holder).bind(row);
        }
    }

    static int getThemeColor(Context context, @AttrRes int attr) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attr, value, true);
        if (value.resourceId != 0) return ContextCompat.getColor(context, value.resourceId);
        return value.data;
    }

    // progress: PROGRESS_HIDDEN, PROGRESS_INDETERMINATE or 0-100
    static void setProgress(BaseProgressIndicator<?> indicator, int progress) {
        if (progress == PROGRESS_HIDDEN) {
            indicator.setVisibility(View.GONE);
            return;
        }
        boolean indeterminate = progress == PROGRESS_INDETERMINATE;
        if (indicator.isIndeterminate() != indeterminate) {
            // the mode can only be switched while the indicator is not shown
            indicator.setVisibility(View.INVISIBLE);
            indicator.setIndeterminate(indeterminate);
        }
        if (!indeterminate) indicator.setProgressCompat(progress, false);
        indicator.setVisibility(View.VISIBLE);
    }

    static class SummaryHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final ImageView icon;
        final TextView title, body;
        final BaseProgressIndicator<?> progress;
        final View actionsView;
        final Button cancel, retry, updateAll;

        SummaryHolder(View view) {
            super(view);
            card = view.findViewById(R.id.summary_card);
            icon = view.findViewById(R.id.summary_icon);
            title = view.findViewById(R.id.summary_title);
            body = view.findViewById(R.id.summary_body);
            progress = view.findViewById(R.id.summary_progress);
            actionsView = view.findViewById(R.id.summary_actions);
            cancel = view.findViewById(R.id.summary_cancel);
            retry = view.findViewById(R.id.summary_retry);
            updateAll = view.findViewById(R.id.summary_update_all);
        }

        void bind(Summary summary, SummaryActions actions) {
            Context context = itemView.getContext();
            int background, text, subText;
            switch (summary.style) {
                case PROGRESS:
                    background = getThemeColor(context, com.google.android.material.R.attr.colorPrimaryContainer);
                    text = subText = getThemeColor(context, com.google.android.material.R.attr.colorOnPrimaryContainer);
                    break;
                case ERROR:
                    background = getThemeColor(context, com.google.android.material.R.attr.colorErrorContainer);
                    text = subText = getThemeColor(context, com.google.android.material.R.attr.colorOnErrorContainer);
                    break;
                default:
                    background = getThemeColor(context, com.google.android.material.R.attr.colorSurfaceContainerHighest);
                    text = getThemeColor(context, com.google.android.material.R.attr.colorOnSurface);
                    subText = getThemeColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant);
                    break;
            }
            card.setCardBackgroundColor(background);
            title.setTextColor(text);
            body.setTextColor(subText);

            if (summary.icon != 0) {
                icon.setImageResource(summary.icon);
                int tint = summary.iconSuccess
                        ? ContextCompat.getColor(context, R.color.m3_dataupdate_success)
                        : getThemeColor(context, summary.iconTint);
                icon.setImageTintList(ColorStateList.valueOf(tint));
                icon.setVisibility(View.VISIBLE);
            } else {
                icon.setVisibility(View.GONE);
            }
            title.setText(summary.title);
            body.setText(summary.body);
            body.setVisibility(summary.body == null || summary.body.isEmpty() ? View.GONE : View.VISIBLE);
            setProgress(progress, summary.progress);

            cancel.setVisibility(summary.cancel ? View.VISIBLE : View.GONE);
            retry.setVisibility(summary.retry ? View.VISIBLE : View.GONE);
            updateAll.setVisibility(summary.updateAll ? View.VISIBLE : View.GONE);
            actionsView.setVisibility(summary.cancel || summary.retry || summary.updateAll ? View.VISIBLE : View.GONE);
            cancel.setOnClickListener(v -> actions.onCancel());
            retry.setOnClickListener(v -> actions.onRetry());
            updateAll.setOnClickListener(v -> actions.onUpdateAll());
        }
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView title;

        HeaderHolder(View view) {
            super(view);
            title = view.findViewById(R.id.header_title);
        }
    }

    static class EntryHolder extends RecyclerView.ViewHolder {
        final View lead;
        final ImageView icon, trailingIcon, expand;
        final TextView title, supporting, trailingText;
        final BaseProgressIndicator<?> progress, trailingProgress;
        final View trailingView;

        EntryHolder(View view) {
            super(view);
            lead = view.findViewById(R.id.entry_lead);
            icon = view.findViewById(R.id.entry_icon);
            title = view.findViewById(R.id.entry_title);
            supporting = view.findViewById(R.id.entry_supporting);
            progress = view.findViewById(R.id.entry_progress);
            trailingView = view.findViewById(R.id.entry_trailing);
            trailingProgress = view.findViewById(R.id.entry_trailing_progress);
            trailingIcon = view.findViewById(R.id.entry_trailing_icon);
            trailingText = view.findViewById(R.id.entry_trailing_text);
            expand = view.findViewById(R.id.entry_expand);
        }

        void bind(Row row) {
            Context context = itemView.getContext();
            float density = context.getResources().getDisplayMetrics().density;

            // file rows are indented under their category, without an icon
            lead.setVisibility(row.sub ? View.INVISIBLE : View.VISIBLE);
            if (!row.sub) icon.setImageResource(row.icon);
            itemView.setMinimumHeight((int) ((row.sub ? 52 : 64) * density));
            itemView.setBackgroundColor(row.sub
                    ? getThemeColor(context, com.google.android.material.R.attr.colorSurfaceContainerLow)
                    : 0);
            title.setText(row.title);
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, row.sub ? 14 : 16);
            supporting.setText(row.supporting);
            supporting.setVisibility(row.supporting == null || row.supporting.isEmpty() ? View.GONE : View.VISIBLE);
            setProgress(progress, row.progress);

            if (row.expandable) {
                expand.setImageResource(row.expanded ? R.drawable.ic_du_expand_less : R.drawable.ic_du_expand_more);
                expand.setVisibility(View.VISIBLE);
            } else {
                expand.setVisibility(row.expandSpace ? View.INVISIBLE : View.GONE);
            }
            if (row.onClick != null) {
                itemView.setOnClickListener(v -> row.onClick.run());
                itemView.setClickable(true);
            } else {
                itemView.setOnClickListener(null);
                itemView.setClickable(false);
            }
            bindTrailing(context, row);
        }

        private void bindTrailing(Context context, Row row) {
            if (row.trailing == Trailing.NONE) {
                trailingView.setVisibility(View.GONE);
                return;
            }
            trailingView.setVisibility(View.VISIBLE);
            int color;
            int iconRes = 0;
            String text;
            switch (row.trailing) {
                case UPDATE:
                    color = getThemeColor(context, com.google.android.material.R.attr.colorPrimary);
                    iconRes = R.drawable.ic_download;
                    text = context.getString(R.string.data_update_state_update);
                    break;
                case QUEUED:
                    color = getThemeColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant);
                    text = context.getString(R.string.data_update_state_queued);
                    break;
                case DOWNLOADING:
                    color = getThemeColor(context, com.google.android.material.R.attr.colorTertiary);
                    text = row.trailingProgress >= 0 ? KcaUtils.format("%d%%", row.trailingProgress) : "";
                    break;
                case DONE:
                    color = ContextCompat.getColor(context, R.color.m3_dataupdate_success);
                    iconRes = R.drawable.ic_du_check_circle;
                    text = context.getString(R.string.data_update_state_done);
                    break;
                case FAILED:
                    color = getThemeColor(context, androidx.appcompat.R.attr.colorError);
                    iconRes = R.drawable.ic_refresh;
                    text = context.getString(R.string.data_update_state_failed);
                    break;
                case NOT_READY:
                    color = getThemeColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant);
                    iconRes = R.drawable.ic_du_timer;
                    text = context.getString(R.string.data_update_state_not_ready);
                    break;
                default: // LATEST
                    color = getThemeColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant);
                    iconRes = R.drawable.ic_du_check;
                    text = context.getString(R.string.data_update_state_latest);
                    break;
            }
            setProgress(trailingProgress, row.trailing == Trailing.DOWNLOADING ? row.trailingProgress : PROGRESS_HIDDEN);
            if (iconRes != 0) {
                trailingIcon.setImageResource(iconRes);
                trailingIcon.setImageTintList(ColorStateList.valueOf(color));
                trailingIcon.setVisibility(View.VISIBLE);
            } else {
                trailingIcon.setVisibility(View.GONE);
            }
            trailingText.setText(text);
            trailingText.setTextColor(color);
            trailingText.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);

            if (row.onTrailingClick != null) {
                trailingView.setOnClickListener(v -> row.onTrailingClick.run());
                trailingView.setClickable(true);
                trailingView.setContentDescription(text);
            } else {
                trailingView.setOnClickListener(null);
                trailingView.setClickable(false);
                trailingView.setContentDescription(null);
            }
        }
    }
}
