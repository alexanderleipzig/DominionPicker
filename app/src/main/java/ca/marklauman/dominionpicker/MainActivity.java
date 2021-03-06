package ca.marklauman.dominionpicker;

import ca.marklauman.dominionpicker.history.FragmentHistory;
import ca.marklauman.dominionpicker.rules.FragmentRules;
import ca.marklauman.dominionpicker.settings.ActivityOptions;
import ca.marklauman.dominionpicker.settings.Prefs;
import ca.marklauman.tools.ExpandedArrayAdapter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

/** Execution starts here. This activity is the hub you first see upon entering
 *  the app. Individual screens are covered by their own Fragments.
 *  @author Mark Lauman */
public class MainActivity extends AppCompatActivity
                          implements ListView.OnItemClickListener {

    /** The name of the app */
    private String app_name;
    /** The names of the navigation drawer entries */
    private String[] navNames;

    /** Layout for the navigation drawer */
    private DrawerLayout navLayout;
    /** Listens to and toggles the navigation drawer */
    private NavToggle navToggle;
    /** ListView for the navigation drawer */
    private View navView;
    /** The adapter for the navigation drawer's ListView */
    private ExpandedArrayAdapter<String> navAdapt;

    /** The current active fragment. */
    private Fragment active;
    /** Handler used to manage the shuffler */
    private ShuffleManager shuffler;

    /** Called when the activity is first created */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Prefs.setup(this);

        shuffler = new ShuffleManager();
		setContentView(R.layout.activity_main);
        navLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.left_drawer);

        // Get the strings for the nav drawer
        app_name = getString(R.string.app_name);
        navNames = getResources().getStringArray(R.array.navNames);

        // Display nav drawer icon on the action bar
        ActionBar bar = getSupportActionBar();
        if(bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setHomeAsUpIndicator(R.drawable.ic_core_menu);
            bar.setHomeButtonEnabled(true);
        }

        // Setup the navigation drawer
        navLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        navToggle = new NavToggle(this, navLayout);
        navLayout.setDrawerListener(navToggle);
        navView.findViewById(R.id.options)
               .setOnClickListener(new OptionLauncher());
        String[] headers = getResources().getStringArray(R.array.navNames);
        navAdapt = new ExpandedArrayAdapter<>(this, R.layout.nav_drawer_item, headers);
        navAdapt.setIcons(R.drawable.ic_core_checkbox, R.drawable.ic_card,
                          R.drawable.ic_cards, R.drawable.ic_action_market);
        navAdapt.setSelBack(R.color.list_item_sel);
        ListView navList = (ListView) navView.findViewById(R.id.drawer_list);
        navList.setAdapter(navAdapt);
        navList.setOnItemClickListener(this);


        // setup the active fragment
        FragmentManager fm = getSupportFragmentManager();
        int sel = PreferenceManager.getDefaultSharedPreferences(this)
                                   .getInt(Prefs.ACTIVE_TAB, getResources().getInteger(R.integer.def_tab));
        active = fm.findFragmentById(R.id.content_frame);
        if(active == null) {
            onItemClick(null, null, sel, 0);
            navToggle.onDrawerClosed(null);
        } else navAdapt.setSelection(sel);
	}

    @Override
    public void onDestroy() {
        shuffler.unregister();
        super.onDestroy();
    }

    /** Setup the ActionBar's menu. Only called once at application start */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        // Needs to be dynamic for multiple tabs
        getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

    /** Prepare the ActionBar's menu. Called every time the menu
     *  is displayed - including after each invalidation.
     *  Useful to hide/display menu items.             */
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean navHidden = navLayout != null && !navLayout.isDrawerOpen(navView);
        int sel = (navAdapt == null) ? 0 : navAdapt.getSelection();
        // show the toggle all button on the picker screen
        menu.findItem(R.id.action_toggle_all)
            .setVisible(navHidden && sel == 1);
        // show the submit button on the picker and rules screens.
        menu.findItem(R.id.action_submit)
            .setVisible(navHidden && sel < 2);
        return super.onPrepareOptionsMenu(menu);
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        // Handle navigation bar requests first
        if(navToggle.onOptionsItemSelected(item))
            return true;

        // Then handle our menu items
		switch(item.getItemId()) {
            case R.id.action_toggle_all:
                ((FragmentPicker)active).toggleAll();
                return true;
            case R.id.action_submit:
                shuffler.startShuffle();
                return true;
		}

        // Not an item we created
		return super.onOptionsItemSelected(item);
	}


    /** Used by subclasses to get this activity */
    private MainActivity getActivity() {
        return this;
    }


    /** Called when an item in the navigation bar's list is selected */
    @Override
    public void onItemClick(AdapterView parent, View view, int position, long id) {
        // The case where you selected the same thing again.
        if(position == navAdapt.getSelection()) {
            navLayout.closeDrawer(navView);
            return;
        }

        navAdapt.setSelection(position);
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                         .putInt(Prefs.ACTIVE_TAB, position)
                         .commit();

        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        switch(position) {
            case 0: active = new FragmentRules();
                    t.replace(R.id.content_frame, active);
                    break;
            case 1: active = new FragmentPicker();
                    t.replace(R.id.content_frame, active);
                    break;
            case 2: active = new FragmentHistory();
                    t.replace(R.id.content_frame, active);
                    break;
            case 3: active = new FragmentMarket();
                    t.replace(R.id.content_frame, active);
                    break;
        }
        t.commit();

        navLayout.closeDrawer(navView);
    }


    /** Opens the options menu when Options is clicked. */
    private class OptionLauncher implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            Intent options = new Intent(getActivity(), ActivityOptions.class);
            startActivity(options);
            navLayout.closeDrawer(navView);
        }
    }


    /** Handles the navigation bar's opening and closing. */
    private class NavToggle extends ActionBarDrawerToggle {
        public NavToggle(Activity activity, DrawerLayout drawerLayout) {
            super(activity, drawerLayout,
                  R.string.menu_open, R.string.menu_close);
        }

        /** Called when a drawer has settled in a completely closed state. */
        public void onDrawerClosed(View view) {
            // make the current selection the active title
            ActionBar ab = getSupportActionBar();
            if(ab != null) ab.setTitle(navNames[navAdapt.getSelection()]);
            invalidateOptionsMenu();
            super.onDrawerClosed(view);
        }

        /** Called when a drawer has settled in a completely open state. */
        public void onDrawerOpened(View view) {
            // make the application name the active title
            ActionBar ab = getSupportActionBar();
            if(ab != null) ab.setTitle(app_name);
            invalidateOptionsMenu();
            super.onDrawerOpened(view);
        }
    }


    /** Allows this activity to request shuffles and get the results */
    private class ShuffleManager extends BroadcastReceiver {
        /** The shuffler */
        private SupplyShuffler shuffler;

        public ShuffleManager() {
            super();
            shuffler = null;
            LocalBroadcastManager.getInstance(getActivity())
                    .registerReceiver(this, new IntentFilter(SupplyShuffler.MSG_INTENT));
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            shuffler = null;
            int res = intent.getIntExtra(SupplyShuffler.MSG_RES, -100);
            String msg;
            switch(res) {
                case SupplyShuffler.RES_OK:
                    Intent showSupply = new Intent(getActivity(), ActivitySupply.class);
                    showSupply.putExtra(ActivitySupply.PARAM_HISTORY_ID,
                                        intent.getLongExtra(SupplyShuffler.MSG_SUPPLY_ID, -1));
                    startActivity(showSupply);
                    return;
                case SupplyShuffler.RES_MORE:
                    msg = String.format(getString(R.string.more_k),
                                        intent.getStringExtra(SupplyShuffler.MSG_SHORT));
                    Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG)
                         .show();
                    return;
                case SupplyShuffler.RES_NO_YW:
                    Toast.makeText(getActivity(), R.string.yw_no_bane, Toast.LENGTH_LONG)
                         .show();
                    return;
                default: // Do nothing
            }
        }

        /** Start a shuffle and register this receiver.
         *  Also cancels any shuffles in progress. */
        public void startShuffle() {
            cancelShuffle();
            shuffler = new SupplyShuffler();
            shuffler.execute();
        }

        /** Stop a shuffle if it is in session. */
        public void cancelShuffle() {
            if(shuffler != null) shuffler.cancel(true);
            shuffler = null;
        }

        /** Unregister this manager and shut down open shuffles */
        public void unregister() {
            LocalBroadcastManager.getInstance(getActivity())
                                 .unregisterReceiver(this);
            cancelShuffle();
        }
    }
}