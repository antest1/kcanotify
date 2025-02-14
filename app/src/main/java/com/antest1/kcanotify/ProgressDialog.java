package com.antest1.kcanotify;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class ProgressDialog extends DialogFragment {
    private TextView progressMessage, progressPercent, progressDetail;
    private ProgressBar progressBar;
    private int current = 0;
    private int maxValue = 1;
    private boolean isIndeterminate = false;
    private String progressMessageData = "";
    private String progressDetailFormat = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.dialog_progressbar, container, false);
        dialogView.setBackgroundColor(Color.TRANSPARENT);

        progressMessage = dialogView.findViewById(R.id.progressMessage);
        progressMessage.setText(progressMessageData);
        progressBar = dialogView.findViewById(R.id.progressBar);
        progressBar.setIndeterminate(isIndeterminate);
        progressPercent = dialogView.findViewById(R.id.progressPercent);
        progressDetail = dialogView.findViewById(R.id.progressDetail);
        setProgress(0);
        return dialogView;
    }

    public void setIndeterminate(boolean enable) {
        isIndeterminate = enable;
        if (progressBar != null)
            progressBar.setIndeterminate(isIndeterminate);
    }

    public void setMessage(String message) {
        progressMessageData = message;
        if (progressMessage != null)
            progressMessage.setText(progressMessageData);
    }

    public void setProgressNumberFormat(String format) {
        progressDetailFormat = format;
        setProgress(current);
    }
    
    public void setMax(int value) {
        boolean resetFlag = value != maxValue;
        maxValue = value;
        if (resetFlag) setProgress(current);
    }

    public void setProgress(int value) {
        current = value;
        int percent = maxValue > 0 ? (int) Math.ceil(100 * current / (double) maxValue) : 0;
        if (progressBar != null) progressBar.setProgress(percent);
        if (progressPercent != null) {
            progressPercent.setText(KcaUtils.format("%d%%", percent));
        }
        if (progressDetail != null && progressDetailFormat != null)
            progressDetail.setText(KcaUtils.format(progressDetailFormat, current));
    }

    public void show(BaseActivity activity) {
        this.show(activity.getSupportFragmentManager(), null);
    }
}
