package ca.marklauman.dominionpicker;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import ca.marklauman.dominionpicker.cardadapters.AdapterCardsFilter;
import ca.marklauman.dominionpicker.database.LoaderId;
import ca.marklauman.dominionpicker.database.Provider;
import ca.marklauman.dominionpicker.database.TableCard;
import ca.marklauman.dominionpicker.settings.Prefs;

/** Fragment governing the card list screen.
 *  @author Mark Lauman */
public class FragmentPicker extends Fragment
                            implements LoaderCallbacks<Cursor>, Prefs.Listener {

    /** The view associated with the card list. */
    private ListView card_list;
    /** The adapter for the card list. */
    private AdapterCardsFilter adapter;
    /** The view associated with an empty list */
    private View empty;
    /** The view associated with a loading list */
    private View loading;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Prefs.addListener(this);
        adapter = new AdapterCardsFilter(getContext());
        getActivity().getSupportLoaderManager()
                     .restartLoader(LoaderId.PICKER, null, this);
    }


    @Override
    public void onDestroy() {
        Prefs.removeListener(this);
        super.onDestroy();
    }


    /** Called when a preference's value has changed */
    @Override
    public void prefChanged(String key) {
        switch(key) {
            // These preferences affect the picker
            case Prefs.FILT_SET:  case Prefs.FILT_COST: case Prefs.FILT_POTION:
            case Prefs.FILT_CURSE: case Prefs.SORT_CARD: case Prefs.FILT_LANG:
                FragmentActivity act = getActivity();
                if(act == null) return;
                act.getSupportLoaderManager()
                   .restartLoader(LoaderId.PICKER, null, this);
                break;
        }
    }


    /** Called to create this fragment's view for the first time.  */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_picker, container, false);
        card_list = (ListView) view.findViewById(R.id.card_list);
        empty = view.findViewById(android.R.id.empty);
        loading = view.findViewById(android.R.id.progress);

        if(adapter.getCursor() != null)
            card_list.setEmptyView(empty);
        else card_list.setEmptyView(loading);
        return view;
    }


    public void toggleAll() {
        adapter.toggleAll();
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Show the loading icon if we have views
        if(card_list != null) {
            empty.setVisibility(View.GONE);
            loading.setVisibility(View.GONE);
            card_list.setEmptyView(loading);
        }
        adapter.changeCursor(null);

        // Basic setup
        CursorLoader c = new CursorLoader(getActivity());
        c.setUri(Provider.URI_CARD_ALL);
        c.setProjection(AdapterCardsFilter.COLS_USED);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());

        // Selection
        String sel = getFilter(pref);
        sel += " AND "+Prefs.filt_lang;
        c.setSelection(sel);

        // Sort order
        String[] sort_col = getResources().getStringArray(R.array.sort_card_col);
        String[] sort_pref = pref.getString(Prefs.SORT_CARD, "").split(",");
        String sort = "";
        for(String s : sort_pref) {
            int i = Integer.parseInt(s);
            if(i == 1) break;
            sort += sort_col[i] + ", ";
        }
        c.setSortOrder(sort + sort_col[1]);

        return c;
    }


    /** Get the filter used by the picker to hide cards that will never be in the supply.
     *  This does not include individual deselected or required cards.
     *  @param pref The preferences used to retrieve filter values.
     *  @return The SQL selection statement that FragmentPicker uses to hide cards.
     *  This statement is never null or the empty string. There will be something in here. */
    public static String getFilter(SharedPreferences pref) {
        // Filter out sets (the set filter is always present)
        String curSel = pref.getString(Prefs.FILT_SET, "");
        String sel = (curSel.length()==0) ? TableCard._SET_ID+"=NULL"
                                          : TableCard._SET_ID+" IN ("+curSel+")";

        // Filter out potions
        if(!pref.getBoolean(Prefs.FILT_POTION, true))
            sel += " AND "+TableCard._POT+"=0";

        // Filter out coins
        curSel = pref.getString(Prefs.FILT_COST, "");
        if(0 < curSel.length())
            sel += " AND "+TableCard._COST_VAL+" NOT IN ("+curSel+")";

        // Filter out cursers
        boolean filt_curse = pref.getBoolean(Prefs.FILT_CURSE, true);
        if(!filt_curse)
            sel += " AND " + TableCard._META_CURSER + "=0";

        return sel;
    }


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.changeCursor(data);

        // display the loaded data
        if(card_list != null) {
            card_list.setAdapter(adapter);
            empty.setVisibility(View.GONE);
            loading.setVisibility(View.GONE);
            card_list.setEmptyView(empty);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if(card_list != null) {
            empty.setVisibility(View.GONE);
            loading.setVisibility(View.GONE);
            card_list.setEmptyView(loading);
        }
        adapter.changeCursor(null);
    }
}