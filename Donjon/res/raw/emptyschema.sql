CREATE TABLE places (
	placeid INTEGER PRIMARY KEY,
	name TEXT NOT NULL,
	descrip TEXT NOT NULL,
	explored BOOLEAN NOT NULL DEFAULT false );

CREATE TABLE place_connections (
	plconid INTEGER PRIMARY KEY,
	wo INTEGER NOT NULL REFERENCES places(placeid),
	wo_to INTEGER NOT NULL REFERENCES places(placeid),
	dir TEXT NOT NULL, -- "north"
	descrip TEXT NOT NULL, -- "To the north is a winding passageway. "
	fail_msg TEXT,
	possible INTEGER NOT NULL DEFAULT 1);

CREATE TABLE player ( -- A singleton table
	playerid INTEGER PRIMARY KEY, -- Not useful
	name TEXT, -- Please don't fill this in in a "game".
	doko INTEGER REFERENCES places(placeid),
	moves INTEGER NOT NULL DEFAULT 0);

CREATE TABLE game ( -- Another singleton
	gameid INTEGER PRIMARY KEY, -- Not useful
	gametitle TEXT NOT NULL,
	gameintro TEXT NOT NULL);

CREATE TABLE actors (
	actorid INTEGER PRIMARY KEY,
	name TEXT NOT NULL,
	descrip TEXT NOT NULL,
	doko INTEGER REFERENCES places(placeid));

CREATE TABLE items (
	itemid INTEGER PRIMARY KEY,
	name TEXT NOT NULL,
	descrip TEXT NOT NULL,
	held_by INTEGER,
	on_floor INTEGER,
	inside INTEGER,
	weight INTEGER NOT NULL DEFAULT 0,
	liftable BOOLEAN DEFAULT true);

CREATE TABLE scores (
	scoreid INTEGER PRIMARY KEY,
	playername TEXT NOT NULL,
	gamename TEXT NOT NULL,
	moves INTEGER NOT NULL DEFAULT 0,
	punkt INTEGER NOT NULL DEFAULT 0
	);

CREATE TABLE quest (
	questid INTEGER PRIMARY KEY,
	done INTEGER NOT NULL DEFAULT 0,
	once INTEGER NOT NULL DEFAULT 1 -- Is it only doable once?
	);

CREATE TABLE qtrigger (
	qtriggerid INTEGER PRIMARY KEY,
	questid INTEGER REFERENCES quest(questid),
	type TEXT NOT NULL,
	data TEXT,
	targtype TEXT,
	target TEXT
	);

CREATE TABLE qaction (
	qactionid INTEGER PRIMARY KEY,
	questid INTEGER REFERENCES quest(questid),
	actiontype TEXT NOT NULL,
	target TEXT,
	data TEXT	
	);

CREATE TABLE gameflag (
	gfid INTEGER PRIMARY KEY,
	flagname TEXT NOT NULL,
	val TEXT NOT NULL,
	score INTEGER NOT NULL DEFAULT 0
	);
	
