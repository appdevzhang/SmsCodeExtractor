package com.github.tianma8023.smscode.app;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceGroup;
import android.text.TextUtils;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.tianma8023.smscode.R;
import com.github.tianma8023.smscode.service.accessibility.SmsCodeAutoInputService;
import com.github.tianma8023.smscode.utils.AccessibilityUtils;
import com.github.tianma8023.smscode.utils.ShellUtils;

import static com.github.tianma8023.smscode.constant.PrefConst.AUTO_INPUT_MODE_ACCESSIBILITY;
import static com.github.tianma8023.smscode.constant.PrefConst.AUTO_INPUT_MODE_ROOT;
import static com.github.tianma8023.smscode.constant.PrefConst.FOCUS_MODE_AUTO;
import static com.github.tianma8023.smscode.constant.PrefConst.KEY_AUTO_INPUT_MODE;
import static com.github.tianma8023.smscode.constant.PrefConst.KEY_ENABLE_AUTO_INPUT_CODE;
import static com.github.tianma8023.smscode.constant.PrefConst.KEY_ENTRY_AUTO_INPUT_CODE;
import static com.github.tianma8023.smscode.constant.PrefConst.KEY_FOCUS_MODE;
import static com.github.tianma8023.smscode.constant.PrefConst.KEY_MANUAL_FOCUS_IF_FAILED;

public class AutoInputSettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

    private Context mContext;

    private ListPreference mAutoInputModePref;
    private String mAutoInputMode;

    private SwitchPreference mManualFocusIfFailedPref;
    private String mFocusMode;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.settings_auto_input_code);

        SwitchPreference autoInputEnablePref = (SwitchPreference) findPreference(KEY_ENABLE_AUTO_INPUT_CODE);
        autoInputEnablePref.setOnPreferenceChangeListener(this);

        mAutoInputModePref = (ListPreference) findPreference(KEY_AUTO_INPUT_MODE);
        mAutoInputModePref.setOnPreferenceChangeListener(this);
        mAutoInputMode = mAutoInputModePref.getValue();

        ListPreference focusModePref = (ListPreference) findPreference(KEY_FOCUS_MODE);
        focusModePref.setOnPreferenceChangeListener(this);
        mFocusMode = focusModePref.getValue();

        mManualFocusIfFailedPref = (SwitchPreference) findPreference(KEY_MANUAL_FOCUS_IF_FAILED);

        refreshEnableAutoInputPreference(autoInputEnablePref.isChecked());
        refreshAutoInputModePreference(mAutoInputMode);
        refreshFocusModePreference(focusModePref, focusModePref.getValue());
        refreshManualFocusIfFailedPreference();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        switch (key) {
            case KEY_ENABLE_AUTO_INPUT_CODE:
                refreshEnableAutoInputPreference((Boolean) newValue);
                break;
            case KEY_AUTO_INPUT_MODE: {
                if (!newValue.equals(mAutoInputMode)) {
                    mAutoInputMode = (String) newValue;
                    refreshAutoInputModePreference(mAutoInputMode);
                    if (AUTO_INPUT_MODE_ROOT.equals(mAutoInputMode)) {
                        showRootModePrompt();
                    } else if (AUTO_INPUT_MODE_ACCESSIBILITY.equals(mAutoInputMode)) {
                        showAccessibilityModePrompt();
                    }
                }
                break;
            }
            case KEY_FOCUS_MODE: {
                if (!newValue.equals(mFocusMode)) {
                    mFocusMode = (String) newValue;
                    refreshFocusModePreference((ListPreference) preference, mFocusMode);
                    refreshManualFocusIfFailedPreference();
                    break;
                }
            }
            default:
                return false;
        }
        return true;
    }

    private void showAccessibilityModePrompt() {
        String serviceId = AccessibilityUtils.getServiceId(SmsCodeAutoInputService.class);
        boolean accessibilityEnabled = AccessibilityUtils.checkAccessibilityEnabled(getActivity(), serviceId);

        if (!accessibilityEnabled) {
            new MaterialDialog.Builder(mContext)
                    .title(R.string.open_auto_input_accessibility)
                    .content(R.string.open_auto_input_accessibility_prompt)
                    .positiveText(R.string.go_to_open)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            AccessibilityUtils.gotoAccessibility(mContext);
                        }
                    })
                    .show();
        }
    }

    private void showRootModePrompt() {
        new MaterialDialog.Builder(mContext)
                .title(R.string.acquire_root_permission)
                .content(R.string.acquire_root_permission_prompt)
                .positiveText(R.string.okay)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        ShellUtils.checkRootPermission();
                    }
                })
                .show();
    }

    private void refreshEnableAutoInputPreference(boolean autoInputEnabled) {
        if (autoInputEnabled && TextUtils.isEmpty(mAutoInputModePref.getValue())) {
            Toast.makeText(getActivity(), R.string.pref_auto_input_mode_summary_default, Toast.LENGTH_SHORT).show();
            onDisplayPreferenceDialog(mAutoInputModePref);
        }
    }

    private void refreshAutoInputModePreference(String newValue) {
        if (TextUtils.isEmpty(newValue)) {
            mAutoInputModePref.setSummary(R.string.pref_auto_input_mode_summary_default);
            return;
        }
        CharSequence[] entries = mAutoInputModePref.getEntries();
        int index = mAutoInputModePref.findIndexOfValue(newValue);
        try {
            mAutoInputModePref.setSummary(entries[index]);
        } catch (Exception e) {
            // ignore
        }
    }

    private void refreshFocusModePreference(ListPreference focusModePref, String newValue) {
        if (TextUtils.isEmpty(newValue))
            return;
        CharSequence[] entries = focusModePref.getEntries();
        int index = focusModePref.findIndexOfValue(newValue);
        try {
            focusModePref.setSummary(entries[index]);
        } catch (Exception e) {
            //ignore
        }
    }

    private void refreshManualFocusIfFailedPreference() {
        PreferenceGroup autoInputGroup = (PreferenceGroup) findPreference(KEY_ENTRY_AUTO_INPUT_CODE);
        if (FOCUS_MODE_AUTO.equals(mFocusMode)) {
            // auto-focus
            // show manual focus if failed preference
            autoInputGroup.addPreference(mManualFocusIfFailedPref);
        } else {
            // manual focus
            // hide manual focus if failed preference
            autoInputGroup.removePreference(mManualFocusIfFailedPref);
        }
    }

}
