package com.example.music1;

import android.app.Dialog;
import android.content.Context;
import android.media.audiofx.Equalizer;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class EqualizerDialog extends Dialog {
    private static final String TAG = "EqualizerDialog";
    private Equalizer equalizer;
    private LinearLayout bandsContainer;
    private Spinner presetSpinner;
    private Button btnClose, btnReset;
    private short numberOfBands;
    private short[] bandLevelRange;

    public EqualizerDialog(@NonNull Context context, Equalizer equalizer) {
        super(context);
        this.equalizer = equalizer;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_equalizer);

        if (equalizer == null) {
            dismiss();
            return;
        }

        initViews();
        setupEqualizer();
        loadPresets();
    }

    private void initViews() {
        bandsContainer = findViewById(R.id.bandsContainer);
        presetSpinner = findViewById(R.id.presetSpinner);
        btnClose = findViewById(R.id.btnClose);
        btnReset = findViewById(R.id.btnReset);

        btnClose.setOnClickListener(v -> dismiss());
        btnReset.setOnClickListener(v -> resetToFlat());
    }

    private void setupEqualizer() {
        numberOfBands = equalizer.getNumberOfBands();
        bandLevelRange = equalizer.getBandLevelRange();

        for (short i = 0; i < numberOfBands; i++) {
            addBandSlider(i);
        }
    }

    private void addBandSlider(short band) {
        int centerFreq = equalizer.getCenterFreq(band) / 1000;

        LinearLayout bandLayout = new LinearLayout(getContext());
        bandLayout.setOrientation(LinearLayout.VERTICAL);
        bandLayout.setPadding(0, 16, 0, 16);

        TextView freqText = new TextView(getContext());
        freqText.setTextColor(ContextCompat.getColor(getContext(), R.color.texto_primario));
        freqText.setText(String.format("%d kHz", centerFreq));
        bandLayout.addView(freqText);

        SeekBar seekBar = new SeekBar(getContext());
        seekBar.setMax(bandLevelRange[1] - bandLevelRange[0]);
        seekBar.setProgress(equalizer.getBandLevel(band) - bandLevelRange[0]);
        seekBar.setTag(band);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    short band = (short) seekBar.getTag();
                    short level = (short) (progress + bandLevelRange[0]);
                    equalizer.setBandLevel(band, level);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        TextView levelText = new TextView(getContext());
        levelText.setTextColor(ContextCompat.getColor(getContext(), R.color.texto_secundario));
        levelText.setTextSize(12);
        updateLevelText(levelText, equalizer.getBandLevel(band));
        levelText.setTag("level_" + band);

        bandLayout.addView(seekBar);
        bandLayout.addView(levelText);
        bandsContainer.addView(bandLayout);
    }

    private void updateLevelText(TextView textView, short level) {
        float dbLevel = level / 100f;
        textView.setText(String.format("%+.1f dB", dbLevel));
    }

    private void loadPresets() {
        short numberOfPresets = equalizer.getNumberOfPresets();
        ArrayList<String> presetNames = new ArrayList<>();

        for (short i = 0; i < numberOfPresets; i++) {
            presetNames.add(equalizer.getPresetName(i));
        }
        presetNames.add("Personalizado");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                presetNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        presetSpinner.setAdapter(adapter);

        presetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < numberOfPresets) {
                    equalizer.usePreset((short) position);
                    updateSlidersFromPreset();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateSlidersFromPreset() {
        for (int i = 0; i < bandsContainer.getChildCount(); i++) {
            LinearLayout bandLayout = (LinearLayout) bandsContainer.getChildAt(i);
            SeekBar seekBar = (SeekBar) bandLayout.getChildAt(1);
            TextView levelText = (TextView) bandLayout.getChildAt(2);

            short band = (short) seekBar.getTag();
            short level = equalizer.getBandLevel(band);

            seekBar.setProgress(level - bandLevelRange[0]);
            updateLevelText(levelText, level);
        }
    }

    private void resetToFlat() {
        for (int i = 0; i < bandsContainer.getChildCount(); i++) {
            LinearLayout bandLayout = (LinearLayout) bandsContainer.getChildAt(i);
            SeekBar seekBar = (SeekBar) bandLayout.getChildAt(1);
            TextView levelText = (TextView) bandLayout.getChildAt(2);

            short band = (short) seekBar.getTag();
            short level = 0;

            seekBar.setProgress(level - bandLevelRange[0]);
            equalizer.setBandLevel(band, level);
            updateLevelText(levelText, level);
        }
    }
}