package org.dachte.donjon;

import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.util.Log;

public class DonjonParser extends Object
{ // This catches a command sent to DonjonService and does appropriate things.
  // It doesn't directly talk to the database. Functions that do belong in DonjonDB.
  // Primarily we're sending descriptive textstreams up through the Service
  // to the GUI through here.
  // However, it is appropriate to send other kinds of textstreams upwards, like
  // the directions it's valid to move in, the score, or the current room name.
  // We should design the API with this in mind.
  //
  // Our caller:
  // 10 parser() - Submits a command to us, gets response
  // 20 get_score() - Asks for score, gets response
  // 30 get_roomname() - Asks for current room, gets response
  // 40 INTERNAL - Displays whatever's appropriate to user, GOTO 10
  //
  // (If the caller is not yet in a game, it'll just call parser())
  //
  // Something to think about: should this be responsible for the rare
  // multi-line input Zorklikes sometimes have, if we choose to implement that?

	Context contextholder = null;
	private DonjonDB _ddbhandler = null;
	
	public DonjonParser(Context thiscontext)
		{ // We need this so we can access resources.
		contextholder = thiscontext;
		}
	
	public DonjonDB getDDB()
		{ // Utility function to get the current DonjonDB. It's ok to destroy that object if need be.
		if(_ddbhandler == null)
			{_ddbhandler = new DonjonDB(contextholder);}
		
		return _ddbhandler;
		}

	public Integer get_score()
		{
		return getDDB().getPlayerScore();
		}
	
	public String get_roomname()
		{ // Not sure what I'm going to do with this.
		return getDDB().currentLocation() + "\n";
		}
	
	public Integer get_gamestate()
		{	// Returns:
			// 0 - Not in a game, not even any tables
			// 1 - Not in a game, but there are tables
			// 2 - Game is selected, player is not yet named
			// 3 - Game is selected, player is named, intro text displayed
		Log.i("Donjon", "I was asked for the gameState");
		return getDDB().gameState();
		}

	public String[] get_highscores()
		{ // Return a set of tuples(TEXT playername, TEXT gamename, INTEGER nummoves, INTEGER ponts)
		return null; // TODO: Remember to fix this before ever calling this.
		}
	
	public String parser(String command)
		{ // Where all the stuff happens
		  // XXX Do we want to lowercase the command?
		String[] words = command.split(" ");
		if(words.length <= 0)
			{return "";} // User just hit enter

		String cmd = words[0];
		// The parser itself. We basically look at the first word and
		// use that to dispatch to an appropriate handler.
		if(cmd.equals("load")) // When a future version of the ADK supports J7+
			{return handle_load(words);} // then replace this w/ a switch
		else if(cmd.equals("worlds"))
			{return handle_worlds();}
		else if(cmd.equals("setname"))
			{return handle_setname(words);}
		else if(cmd.equals("look"))
			{return handle_look(words);}
		else if(cmd.equals("take"))
			{return handle_take(words);}
		else if(cmd.equals("drop"))
			{return handle_drop(words);}
		else if(cmd.equals("north")
		||	cmd.equals("south")
		||	cmd.equals("east")
		||	cmd.equals("west")
		||	cmd.equals("up")
		||	cmd.equals("down")
		||	cmd.equals("northeast")
		||	cmd.equals("northwest")
		||	cmd.equals("southeast")
		||	cmd.equals("southwest")
		||	cmd.equals("n")
		||	cmd.equals("s")
		||	cmd.equals("e")
		||	cmd.equals("w")
		||	cmd.equals("u")
		||	cmd.equals("d")
		||	cmd.equals("ne")
		||	cmd.equals("nw")
		||	cmd.equals("se")
		||	cmd.equals("sw"))
			{return handle_move(words);}
		else if(cmd.equals("quit"))
			{ // Clean out databases
			return handle_quit();
			}
//		else if(cmd.equals("quitquit"))
//			{ // Literally delete the databases on the device.
//			}
		else
			{
			return try_unhandled_cmd(words);
			}
		}
/* ********************************************************** */
// Handlers
//
// They are responsible for returning appropriate responses for the user.
// Accordingly, all of them must either have a signature of:
//	public String (String[])
// OR
//	public String ()
	public final String NOTYETSUPPORTED = "Command is not yet supported\n";
	
	private String handle_load(String[] cmd) // TODO: Later, refuse to do this until someone has quit their current game.
		{ // load WorldName
		if(get_gamestate() > 2)
			{return "You must quit your current game before loading a new one\n";}
		String[] worlds = getDDB().listWorlds();
		if(cmd.length != 2) // Should be 2 words
			{return "Usage: load WorldName\n";}
		List <String> world_list = Arrays.asList(worlds);
		if(world_list.contains(cmd[1]))
			{ // It's a valid world. Let's try to load it.
			Log.i("Donjon", "Trying to load world " + cmd[1]);
			Boolean ok = getDDB().loadWorld(cmd[1]);
			if(! ok)
				{return "Internal error: Failed to load world: " + cmd[1] + "\n";}
			else	{return "World loaded.\nPlease submit your name with setname NAME to get started\n";}
			}
		else	{return "I don't recognise that world.\n";}
		}

	private String handle_worlds()
		{
		String[] worlds = getDDB().listWorlds();
		StringBuilder ret = new StringBuilder();
		ret.append("Worlds:\n");
		for(String world:worlds)
			{ret.append("\t" + world);}
		ret.append("\n");
		return ret.toString();
		}

	private String handle_setname(String[] cmd)
		{ // Can fail if user provides an invalid name or if the database is not yet loaded.
		if(cmd.length != 2) // No spaces accepted, for now
			{return "Usage: setname NAME\n";}
		if(get_gamestate() != 2)
			{return "You must have loaded a game but not set your name yet to use this command\n";}
		getDDB().acceptName(cmd[1]);
		return getDDB().gameIntro();
		}

	private String handle_look(String[] cmd)
		{ // Either "look" or "look at $item"
		if(get_gamestate() != 3)
			{return "You're not ready to execute this command\n";}
		
		if(cmd.length == 1) // "look"
			{
			String locname = getDDB().currentLocation();
			String locdesc = getDDB().getLocDescription();
			String exitdesc = getDDB().describe_movable_directions();
			return "You are in " + locname + "\n" + locdesc + "\n" + exitdesc + "\n";
			}
		else if((cmd.length == 3) && (cmd[1].equals("at")) ) // "look at SOMETHING"
			{
			return NOTYETSUPPORTED; // FIXME
			}
		else
			{return "Usage: look\nOR\n\tlook at SOMETHING\n";}
		
		}
	
	private String handle_take(String[] cmd)
		{ // "all" is a special keyword handled by iterating over all visible objs
		return NOTYETSUPPORTED;
		}

	private String handle_drop(String[] cmd)
		{ // "all" is a special keyword handled by iterating over inventory
		return NOTYETSUPPORTED;
		}
	
	private String handle_move(String[] cmd)
		{ 	// Try to move. Might not succeed!
			// Get list of connections for where we are
			// If we're not in the "to" part of that, than generically fail
			// If we're in there but "possible" is false, than fail w/ msg
			// Otherwise succeed
		if(get_gamestate() != 3)
			{return "You're not ready to execute this command\n";}
		String movedir = cmd[0]; 
		if(movedir.equals("n"))		{movedir = "north"	;}
		if(movedir.equals("e"))		{movedir = "east"	;}
		if(movedir.equals("s"))		{movedir = "south"	;}
		if(movedir.equals("w"))		{movedir = "west"	;}
		if(movedir.equals("ne"))	{movedir = "northeast"	;}
		if(movedir.equals("se"))	{movedir = "southeast"	;}
		if(movedir.equals("nw"))	{movedir = "northwest"	;}
		if(movedir.equals("sw"))	{movedir = "southwest"	;}
		if(movedir.equals("u"))		{movedir = "up"		;}
		if(movedir.equals("d"))		{movedir = "down"	;}
				
		return getDDB().try_move_direction(movedir) + "\n";
		}
	
	private String handle_quit()
		{
		if(get_gamestate() < 2)
			{return "You're not in a game right now; no point quitting it\n";}
		getDDB().saveScore();
		getDDB().clearDB();
		return "OK\n";
		}
	
	private String try_unhandled_cmd(String[] words)
		{ // Later, check flags for commands that are only situationally appropriate
		return "You can't do that!\n";
		}
	
// *****************************************
// Utility functions for handlers


}
