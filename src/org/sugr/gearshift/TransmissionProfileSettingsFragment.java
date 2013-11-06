package org.sugr.gearshift;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

public class TransmissionProfileSettingsFragment extends BasePreferenceFragment {
    private TransmissionProfile mProfile;

    private boolean mNew = true;
    private boolean mSaved = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String id = null;
        Bundle args = getArguments();
        if (args.containsKey(G.ARG_PROFILE_ID)) {
            id = args.getString(G.ARG_PROFILE_ID);
            mNew = false;
        }

        if (id == null)
            mProfile = new TransmissionProfile(getActivity());
        else
            mProfile = new TransmissionProfile(id, getActivity());

        G.logD("Editing (new ? %s) profile %s",
            new Object[] {mNew, mProfile.getId()});

        String prefname = G.PREF_PREFIX + (id == null ? "temp" : id);
        mSharedPrefs = getActivity().getSharedPreferences(
                prefname, Activity.MODE_PRIVATE);

        getPreferenceManager().setSharedPreferencesName(prefname);
        if (mNew) {
            Editor e = mSharedPrefs.edit();

            e.clear();
            e.commit();
        }

        addPreferencesFromResource(R.xml.torrent_profile_preferences);
        PreferenceManager.setDefaultValues(
                getActivity(), G.PREF_PREFIX + (id == null ? "temp" : id),
                Activity.MODE_PRIVATE, R.xml.torrent_profile_preferences, true);

        mSummaryPrefs = new Object[][] {
            {G.PREF_NAME, getString(R.string.profile_summary_format), -1, -1, ""},
            {G.PREF_HOST, getString(R.string.profile_summary_format), -1, -1, ""},
            {G.PREF_PORT, getString(R.string.profile_summary_format), -1, -1, ""},
            {G.PREF_USER, getString(R.string.profile_summary_format), -1, -1, ""},
            {G.PREF_PATH, getString(R.string.profile_summary_format), -1, -1, ""},
            {G.PREF_TIMEOUT, getString(R.string.profile_summary_format), -1, -1, ""},
            /* {G.PREF_RETRIES, getString(R.string.profile_summary_format),
                R.array.pref_con_retries_values, R.array.pref_con_retries_entries, ""}, */
        };

        Bundle dirBundle = getPreferenceManager().findPreference(G.PREF_DIRECTORIES).getExtras();
        dirBundle.putString(G.ARG_PROFILE_ID, prefname);
        if (args.containsKey(G.ARG_DIRECTORIES)) {
            dirBundle.putStringArrayList(G.ARG_DIRECTORIES,
                    args.getStringArrayList(G.ARG_DIRECTORIES));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);

            View customActionBarView = inflater.inflate(R.layout.torrent_profile_settings_action_bar, null);
            View saveMenuItem = customActionBarView.findViewById(R.id.save_menu_item);
            saveMenuItem.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int errorRes = -1;
                    if (mSharedPrefs.getString(G.PREF_NAME, "").trim().equals("")) {
                        errorRes = R.string.con_name_cannot_be_empty;
                    } else if (mSharedPrefs.getString(G.PREF_HOST, "").trim().equals("")) {
                        errorRes = R.string.con_host_cannot_be_empty;
                    }

                    if (errorRes != -1) {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle(R.string.invalid_input_title);
                        builder.setMessage(errorRes);
                        builder.setPositiveButton(android.R.string.ok, null);
                        builder.show();

                        return;
                    }

                    mProfile.load(mSharedPrefs);
                    if (mNew) {
                        mProfile.save();
                    }

                    mSaved = true;

                    PreferenceActivity context = (PreferenceActivity) getActivity();
                    G.requestBackup(context);

                    if (context.onIsHidingHeaders() || !context.onIsMultiPane()) {
                        getActivity().finish();
                    } else {
                        ((PreferenceActivity) getActivity()).switchToHeader(GeneralSettingsFragment.class.getCanonicalName(), new Bundle());
                    }
                }
            });
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME |
                    ActionBar.DISPLAY_SHOW_TITLE);
            actionBar.setCustomView(customActionBarView);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();

        inflater.inflate(R.menu.torrent_profile_settings_fragment, menu);
        if (mProfile == null)
            menu.findItem(R.id.delete).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete:
                if (!mNew) {
                    /* FIXME: show undo bar https://plus.google.com/113735310430199015092/posts/RA9WEEGWYp6 */

                    mProfile.delete();

                    mSaved = true;
                }

                PreferenceActivity context = (PreferenceActivity) getActivity();

                if (context.onIsHidingHeaders() || !context.onIsMultiPane()) {
                    getActivity().finish();
                } else {
                    ((PreferenceActivity) getActivity()).switchToHeader(GeneralSettingsFragment.class.getCanonicalName(), new Bundle());
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroy() {
        if (!mSaved) {
            if (!mNew && mProfile != null) {
                mProfile.save();
            }
        }

        super.onDestroy();
    }
}
