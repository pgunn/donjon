package org.dachte.donjon;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;


// DonjonDB
//
// Holds the code that sits right above the DB. Please don't expose any table
// details outside this component

public class DonjonDB extends Object
{
	Context savedcontext = null;
	public DonjonDB(Context mycontext)
		{ // Need this so we can access resources
		savedcontext = mycontext;
		}
	
	public Integer gameState()
		{
		if(! tablesExist())
			{return 0;}
		if(! inWorld() ) // tablesExist())
			{return 1;}
		else if(! playerNamed())
			{return 2;}
		else
			{return 3;}
		}

	public Boolean tablesExist()
		{
		return tables_exist();
		}
	
	public Boolean makeTables()
		{
		return create_database_tables();
		}

	public String[] listWorlds()
		{ // A "world" is a SQLFile that defines an adventure.
		  // This queries the filesystem for a list of such defined adventures
		  // Remember: Such files belong in the app manifest.
		String[] filenames = get_available_game_files();
		String[] ret = Arrays.copyOf(filenames,filenames.length);
		for(int i=0; i < ret.length; i++)
			{
			ret[i] = ret[i].replaceAll("\\.sql$", ""); // Remove extension
			}
		return ret;
		}
	
	public Boolean loadWorld(String worldid)
		{ // Provided that world exists, this clears all database tables
		  // and loads the specified world
		if(clear_database_tables())
			{return load_game_tables(worldid);}
		else {return false;}
		}
	
	public Boolean inWorld()
		{ // Do we have a world loaded?
		return(get_game_intro() != null);
		}

	public Boolean playerNamed()
		{ // Query the database for a player name.
		return (getPlayerName() != null);
		}
	
	public String getPlayerName()
		{
		return player_name();
		}
	
	public Integer getPlayerScore()
		{
		return player_score();
		}
	
	public Boolean playerInitialised()
		{
		if(! inWorld())
			{return false;}
		if(! playerNamed())
			{return false;}
		return true;
		}
	
	public void acceptName(String player_name)
		{ 	// Right after a database is loaded, the player needs to
			// enter their player name before they can do anything else.
			// The game doesn't need to use this in-universe, but it should
			// use it for score, or at least as a marker that the player has
			// been given intro text for the game they're playing.
		set_player_name(player_name);
		}
	
	public String gameIntro()
		{ // Show initial text for game after player is named
		return get_game_intro();
		}
	
	public void clearDB()
		{
		clear_database_tables();
		}
	
	public void saveScore()
		{
		do_save_score();
		}
	
	// ************************************
	// Stuff that's used in a running game
	public String currentLocation()
		{
		return get_location();
		}
	
	public String getLocDescription()
		{
		return get_location_description();
		}
	
	public String[] movableDirections()
		{ // Simple list of directions we can move. For machine parsing.
		return get_movable_directions();
		}
	
	public String describe_movable_directions()
		{ // Return text string describing those directions. For room describing.
		String[] dirs = get_movable_directions();
		if(dirs == null)
			{return "";}
		String ret = "";
		for(String dir:dirs)
			{ // TODO and also this kind of implies we should use long directional names internally so we can describe prettily
			ret = ret + describe_movable_direction(dir);
			}
		return ret;
		}
	
	public String try_move_direction(String direction)
		{ // We're allowing for the possibility of a failure to move
		Log.i("Donjon", "Attempting move in direction " + direction);
		if(! direction_possible(direction))
			{ // report the movement fail message to the player
			String descriptivefail = move_fail_msg(direction);
			if(descriptivefail==null)
				{
				return "You can't move in that direction!";
				}
			return move_fail_msg(direction);
			}
		// XXX If we ever have triggers to activate on a move, they'll go here
		do_move_player(direction);
		return "Moved";
		}
	
	public String[] inventory()
		{ // List what we have
		// Note that this returns strings, and there may be items that have the same name that are not
		// distinguished by this list; don't use it for internal structure that may need to tell things apart.
		return list_player_inventory();
		}
	
	public String describe_inventory_item(String itemname)
		{ // For everything with the given name in the user's inventory, show its description.
		// Needs to handle different items with the same name sensibly.
		Integer[] inv = get_player_inventory();
		ArrayList<String> descs = new ArrayList<String>();
		for(Integer iter:inv)
			{
			String iname = get_item_name(iter);
			if(iname.equals(itemname))
				{
				String desc = get_item_description(iter);
				descs.add(desc);
				}
			}
		if(descs.size() == 0)
			{
			return null;
			}
		else if(descs.size() == 1)
			{ // Probably the usual case.
			return descs.get(0);
			}
		else
			{
			StringBuffer ret = new StringBuffer();
			ret.append("There are " + (descs.size()) + " items with that name:\n");
			for(String iter:descs)
				{
				ret.append("* " + iter + "\n");
				}
			return ret.toString();
			}
		}
	
	public String pick_up(String thingname)
		{ // How to handle multiple items with same name? Indices?
		if(! player_can_pick_up(thingname))
			{return "You could not carry " + thingname;}
		
		player_pick_one_up(thingname);
		return "Picked up";
		}
	
	public String drop(String thing) // TODO
		{ // XXX Do we want to drop-by-index or by name? Maybe allow either? If we allow indices, we should
		  // figure out a sensible way to provide for them.
		return null;
		}
	
	public String check_triggers()
		{ // Check the game state for quest triggers. Should only trigger once per "turn" (develop this concept further).
		// Foreach quest that has all its triggers met, fire the quest and add any text response to bundle of text that's
		// returned to our caller.
		String ret = "";
		Integer[] firableQuests = get_firable_quests();
		for(Integer questID:firableQuests)
			{
			ret += fire_quest(questID);
			}
		return ret;
		}


	// ************************************
	// Lower level database calls
	// These rely on "raw" resources that live in res/raw/
	//
	// InputStream openRawResource(R.raw.<filename>)
	
	private Boolean create_database_tables()
		{ // emptyschema.sql
		String emptys_data = slurpfile(R.raw.emptyschema);
		Log.i("Donjon", "Asked to make empty database");
		Boolean ok = run_sql_batch(emptys_data);
		if(! ok)
			Log.i("Donjon", "Failed to make empty database");
		return ok;
		}
	
	private Boolean clear_database_tables()
		{ // schemapurge.sql
		if(! tables_exist())
			{
			create_database_tables();
			}
		String purge_data = slurpfile(R.raw.schemapurge);
		return run_sql_batch(purge_data);
		}
	
	private final String GAMEASSETS = "sqlgames";
	private Boolean load_game_tables(String gameid)
		{ // fills in data for a specific game
		String loadgame_data = slurp_asset(GAMEASSETS + "/" + gameid + ".sql");
		if(run_sql_batch(loadgame_data)) // FIXME: Probably should back out if data fails to load
			{return true;}
		else {return false;}
		}

	private String[] get_available_game_files()
		{
		return read_assetdir(GAMEASSETS);
		}
	
	private Boolean run_sql_batch(String commands)
		{
//		Log.i("Donjon", "SQLBatch:" + commands);
		try
			{
			String[] queries = commands.split(";");
			for(String query : queries)
				{
				String fixedquery = query.replaceAll("--.*?\n", ""); // Remove SQL comments
				fixedquery = fixedquery.replace("\n", " ");
				if(fixedquery.matches("^\\s*$"))
					{continue;}
				Log.i("Donjon", "DBBatch[" + fixedquery + "]");
				get_dbh().execSQL(fixedquery);
				}
			}
		catch (Exception e) {return false;}
		return true;
		}
	
	private Boolean tables_exist()
		{
		Cursor sth = get_dbh().rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='game'", null);
		int gamestable = sth.getCount();
		Log.i("Donjon", "Verifying database has the games table - I see " + gamestable );
		return(gamestable > 0);
		}
	
	private String player_name()
		{ // TODO Refactor this thing. Will it do the right thing if the cursor is empty?
		Cursor statement = get_dbh().rawQuery("SELECT name FROM player", null);
		if(! statement.moveToFirst())
			{
			Log.i("Donjon", "player_name found no first tuple to visit");
			return null;
			}
		if((statement.getColumnCount() == 0) || statement.isNull(0))
			{
			Log.i("Donjon", "player_name didn't find a value in the name field of the player table");
			return null;
			}
		String pname = statement.getString(0);
		Log.i("Donjon", "player_name() retrieved playername of " + pname);
		return statement.getString(0);
		}
	
	private void set_player_name(String name)
		{
		if(player_name() != null)
			{ // Update it
			Log.i("Donjon", "Updating player tuple to include name:" + name);
			get_dbh().execSQL("UPDATE player SET name=?", new String[]{name});
			}
		else
			{ // Insert it
			Log.i("Donjon", "Inserting new player tuple with name:" + name);
			get_dbh().execSQL("INSERT INTO player(name, doko) VALUES(?,?)", new String[]{name, "1"}); 
			}
		}
	
	private Integer player_score()
		{
		Cursor dbq = get_dbh().rawQuery("SELECT score FROM gameflag", null);
		Integer ret = new Integer(0);
		if(! dbq.moveToFirst())
			{
			Log.i("Donjon", "player_score found no first tuple to visit");
			return ret;
			}
		if((dbq.getColumnCount() == 0) || dbq.isNull(0))
			{
			return ret;
			}
		dbq.moveToFirst();
		for(int i=0; i < dbq.getColumnCount();i++)
			{
			ret += dbq.getInt(0);
			dbq.moveToNext();
			}
		dbq.close();
		Log.i("Donjon", "player_name() retrieved playername of " + ret.toString());
		return ret;
		}
	
	private String get_game_intro()
		{
		Cursor statement = get_dbh().rawQuery("SELECT gameintro FROM game", null);
		if(! statement.moveToFirst())
			{return null;}
		if((statement.getColumnCount() == 0)|| statement.isNull(0))
			{return null;}
		return statement.getString(0);
		}
	
	private String get_location()
		{
		Cursor statement = get_dbh().rawQuery("SELECT name FROM places WHERE placeid IN (SELECT doko FROM player)", null);
		if(! statement.moveToFirst())
			{return null;}
		if((statement.getColumnCount() == 0)|| statement.isNull(0))
			{return null;}
		return statement.getString(0);		
		}
	
	private String get_location_description()
		{
		Cursor statement = get_dbh().rawQuery("SELECT descrip FROM places WHERE placeid IN (SELECT doko FROM player)", null);
		if(! statement.moveToFirst())
			{return null;}
		if((statement.getColumnCount() == 0)|| statement.isNull(0))
			{return null;}
		return statement.getString(0);
		}
	
	private String[] get_movable_directions()
		{
		Cursor statement = get_dbh().rawQuery("SELECT dir FROM place_connections WHERE wo IN (SELECT placeid FROM places WHERE placeid IN (SELECT doko FROM player))", null);
		if(! statement.moveToFirst())
			{return null;}
		if(statement.getColumnCount() == 0)
			{return null;}
		// Iterate over the list, return a String[] of all values
		String[] ret = new String[statement.getColumnCount()];
		if(! statement.moveToFirst())
			{return ret;}
		for(int i=0;i<statement.getColumnCount();i++)
			{
			ret[i] = statement.getString(0);
			statement.moveToNext();
			}
		return ret;			
		}
	
	private String describe_movable_direction(String dir)
		{
		Cursor statement = get_dbh().rawQuery("SELECT descrip FROM place_connections WHERE wo IN (SELECT placeid FROM places WHERE placeid IN (SELECT doko FROM player)) AND dir=?", new String[]{dir});
		if(! statement.moveToFirst())
			{return null;}
		return statement.getString(0);
		}
	
	private String move_fail_msg(String dir)
		{
		Cursor statement = get_dbh().rawQuery("SELECT fail_msg FROM place_connections WHERE wo IN (SELECT placeid FROM places WHERE placeid IN (SELECT doko FROM player)) AND dir=?", new String[]{dir});
		if(! statement.moveToFirst())
			{return null;}
		return statement.getString(0);
		}
	
	
	private Boolean direction_possible(String dir)
		{
		Cursor statement = get_dbh().rawQuery("SELECT possible FROM place_connections WHERE wo IN (SELECT placeid FROM places WHERE placeid IN (SELECT doko FROM player)) AND dir=?", new String[]{dir});
		if(! statement.moveToFirst())
			{return false;}
		if(statement.getInt(0) != 0)
			{return true;}
		else	{return false;}
		}
	
	private void do_move_player(String dir)
		{
		get_dbh().execSQL("UPDATE player SET doko=(SELECT wo_to FROM place_connections WHERE wo IN (SELECT doko FROM player) AND dir=?)", new String[]{dir});
		}

	private void do_save_score()
		{
		Integer score = player_score();
		get_dbh().execSQL("INSERT INTO scores(playername,gamename,moves,punkt) VALUES ( (SELECT name FROM player), (SELECT gametitle FROM game), (SELECT moves FROM player), ?)", new String[]{score.toString()});
		}

	// *************************************
	// SQLite way of dropping all tables
	
	public void drop_all_tables()
		{
		Cursor dbq = get_dbh().rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
		String[] all_tables = new String[dbq.getColumnCount()];
		dbq.moveToFirst();
		for(int i=0; i < dbq.getColumnCount();i++)
			{
			all_tables[i] = dbq.getString(0);
			dbq.moveToNext();
			}
		dbq.close(); // Just to be safe
		for(String tablename:all_tables)
			get_dbh().execSQL("DROP TABLE " + tablename, null); // Placeholders are not normally allowed for DDL. Assuming that's true of SQLite
		}
	
	// *********************************
	// Inventory
	// Objects have:
	// *an id (which we don't expose)
	// *a name
	// *a description
	// *a weight (many games won't use)
	// *all sorts of "where is it" type information (expose indirectly)

	private Integer[] get_player_inventory()
		{
		return get_inventory(0); // XXX Convention: Playerid is actor 0
		}
	
	private Integer[] get_inventory(Integer actorid)
		{
		Cursor dbq = get_dbh().rawQuery("SELECT itemid FROM items WHERE held_by=?", new String[]{actorid.toString()});
		Integer[] ret = new Integer[dbq.getColumnCount()];
		dbq.moveToFirst();
		for(int i=0; i < dbq.getColumnCount(); i++)
			{
			ret[i] = dbq.getInt(0);
			dbq.moveToNext();
			}
		dbq.close();
		return ret;
		}

	private String[] list_player_inventory()
		{ // Gives name of everything in inventory
		return list_inventory(0);
		}
	
	private String[] list_inventory(Integer actorid)
		{
		Integer[] itemids = get_inventory(actorid);
		String[] ret = new String[itemids.length];
		for(Integer itemid:itemids)
			{
			ret[itemid] = get_item_name(itemid);
			}
		return ret;
		}
	
	private String get_item_description(Integer itemid)
		{
		Cursor dbq = get_dbh().rawQuery("SELECT descrip FROM items WHERE itemid=?", new String[]{itemid.toString()});
		dbq.moveToFirst();
		String ret = dbq.getString(0);
		return ret;
		}
	
	private String get_item_name(Integer itemid)
		{
		Cursor dbq = get_dbh().rawQuery("SELECT name FROM items WHERE itemid=?", new String[]{itemid.toString()});
		dbq.moveToFirst();
		String ret = dbq.getString(0);
		return ret;
		}
	
	private Boolean player_can_pick_up(String itemname)
		{ // Iterate over all items with itemname here, call Integer version, return true if any return true
		return null;
		}

	private Boolean player_can_pick_up(Integer indexed)
		{ // Indexed. Figure out how indexing should work
		  // Is it here?
		  // Is it liftable?
		  // Is it too heavy to lift (if applicable)?
		  // Are inventory slots full?
		if(	(item_atplayer(indexed))
		&& 	(item_liftable(indexed))
		&&	(item_addweight_ok(indexed))
		&&	(player_has_slot_free(indexed)))
			{return true;}
		else {return false;}
		}
	
	private void player_pick_one_up(String itemname)
		{ // Iterate over all items with itemname here, call Integer version

		}
	
	private void player_pick_one_up(Integer indexed)
		{ // Already kosher to pick up. Just do it.

		}
	
	private Boolean item_atplayer(Integer indexed)
		{
		return null;
		}
	
	private Boolean item_liftable(Integer indexed)
		{
		return null;
		}
	private Boolean item_addweight_ok(Integer indexed)
		{
		return null;
		}
	private Boolean player_has_slot_free(Integer indexed)
		{
		return null;
		}
	
	
	// *********************************
	// Quests and triggers
	
	private Integer[] get_firable_quests()
		{ 	// Strictly speaking, the questids don't need to be returned, just something which is iterable and
			// a valid token to pass to fire_quest to identify a specific quest.
		Integer[] ret = new Integer[0];
		// So, design decision time here. Do we try to find a way to handle all kinds of triggers in the SQL, or
		// do we handle all that in code? Let's code up the in-Java approach for now and maybe write some of the queries
		// that can handle some trigger types in SQL. Maybe that'll show us that we want to hybridise, or maybe that'll
		// make which approach we want to use more clear. At the very least it'll make changing our mind easier later
		
		Integer[] allquests = get_all_quests(0);
		for(Integer qselector:allquests)
			{
			boolean IsFirable = true;
			for(Integer tselector:get_triggers_for_quest(qselector))
				{
				if(! trigger_met(tselector))
					{
					IsFirable = false;
					}
				}
			if(IsFirable)
				{fire_quest(qselector);}
			}
		return ret;
		}

	private String fire_quest(Integer questid) // TODO
		{
		return null;
		}

	private Boolean trigger_met(Integer trigger)
		{ // Returns true if a trigger describes the current state-of-the-world, false otherwise
		HashMap<String,String> triginfo = get_full_triggerinfo(trigger); // type, data, targtype, target
		String trigger_type = triginfo.get("type");
		if(trigger_type.equals("player_has")) // My kingdom for a switch statement!
			{
			return false; // TODO
			}
		else if(trigger_type.equals("player_at"))
			{
			return false; // TODO
			}
		else
			{return false;}
		}
	
	private Integer[] get_all_quests(Integer include_done)
		{
		Cursor dbq;
		if(include_done==1)
			{dbq = get_dbh().rawQuery("SELECT questid FROM quest", null);}
		else
			{dbq = get_dbh().rawQuery("SELECT questid FROM quest WHERE done=0", null);}
		
		Integer[] ret = new Integer[dbq.getColumnCount()];
		if(! dbq.moveToFirst())
			{return null;}
		for(int i=0; i < dbq.getColumnCount();i++)
			{
			ret[i] = dbq.getInt(0);
			dbq.moveToNext();
			}
		dbq.close(); // Just to be safe
		return ret;
		}
	
	private Integer[] get_triggers_for_quest(Integer questid)
		{
		Cursor dbq = get_dbh().rawQuery("SELECT qtriggerid FROM qtrigger WHERE questid=?", new String[]{questid.toString()});
												// My kingdom for a good map{} function
		Integer[] ret = new Integer[dbq.getColumnCount()];
		if(! dbq.moveToFirst())
			{return null;}
		for(int i=0; i < dbq.getColumnCount(); i++)
			{
			ret[i] = dbq.getInt(0);
			dbq.moveToNext();
			}
		dbq.close();
		return ret;
		}

	private Integer[] get_questactions_for_quest(Integer questid)
		{
		Cursor dbq = get_dbh().rawQuery("SELECT qactionid FROM qaction WHERE questid=?", new String[]{questid.toString()});
		// My kingdom for a good map{} function
		Integer[] ret = new Integer[dbq.getColumnCount()];
		if(! dbq.moveToFirst())
			{return null;}
		for(int i=0; i < dbq.getColumnCount();i++)
			{
			ret[i] = dbq.getInt(0);
			dbq.moveToNext();
			}
		dbq.close();
		return ret;
		}
	
	private HashMap<String,String> get_full_triggerinfo(Integer triggerid)
		{
		Cursor dbq = get_dbh().rawQuery("SELECT type,data,targtype,target FROM qtrigger where qtriggerid=?", new String[]{triggerid.toString()});
		HashMap<String,String> ret = new HashMap<String,String>();
		if(! dbq.moveToFirst())
			{return null;}
		
		ret.put("type", 	getPossiblyNullStringField(dbq, 0, null));
		ret.put("data", 	getPossiblyNullStringField(dbq, 1, null));
		ret.put("targtype", 	getPossiblyNullStringField(dbq, 2, null));
		ret.put("target", 	getPossiblyNullStringField(dbq, 3, null));
		return ret;
		}
	
	// *************************************
	// Now, the connection itself
	private SQLiteDatabase _inner_dbh = null;
	private SQLiteDatabase get_dbh()
		{
		if((_inner_dbh == null) || (! _inner_dbh.isOpen()))
			{ // We don't have a functional database handle, so let's make one.
			String db_path = savedcontext.getDir("builtdata", 0) + "/donjon.db";
			Log.i("Donjon", "Connecting to database: " + db_path);
			_inner_dbh = SQLiteDatabase.openOrCreateDatabase(db_path, null);
			}
		return _inner_dbh;
		}

	private String getPossiblyNullStringField(Cursor dbq, Integer pos, String fillin)
		{ // TODO Learn how to use generics so we can handle more than just Strings
		String ret = fillin;
		if(! dbq.isNull(pos))
			{ret = dbq.getString(pos);}
		return ret;
		}

	// *************************************
	// Generic helpers. Pull these out into generic libraries later.
	
	private String[] read_assetdir(String fullpath)
		{
		AssetManager base = savedcontext.getAssets();
		try 
			{
			return base.list(fullpath);
			}
		catch (Exception myexception)
			{return null;}
		}
	
	private String slurp_asset(String fullpath) // path under assets
		{
		String ret = null;
		AssetManager base = savedcontext.getAssets();
		try
			{
			InputStream ins = base.open(fullpath);
			int size = ins.available();
			byte[] buffer = new byte[size];
			ins.read(buffer);
			ins.close();
			Charset utf8cs = Charset.forName("UTF-8");
			ret = utf8cs.decode(ByteBuffer.wrap(buffer)).toString();
			}
		catch (Exception myerror)
			{
			return null;
			}
		return ret;
		}
	
	private String slurpfile(int resname, Boolean harderr)
		{
		String ret = null;
		InputStream ins = savedcontext.getResources().openRawResource(resname);
		try
			{
			int size = ins.available();

			// Read the entire resource into a local byte buffer.
			byte[] buffer = new byte[size];
			ins.read(buffer);
			ins.close();
			Charset utf8cs = Charset.forName("UTF-8");
			ret = utf8cs.decode(ByteBuffer.wrap(buffer)).toString();
//			Log.i("Donjon", "Apologies:" + ret);
			}
		catch (Exception err)
			{
			Log.i("Donjon", "Caught exception in slurpfile()");
			ret = null;
			}

		if((! harderr) && (ret == null) )
			{return "";}
		return ret;
		}
	private String slurpfile(int resname)
		{
		return slurpfile(resname, true);
		}
}
