INSERT INTO game(gametitle, gameintro) VALUES('ParkSlope', 'A game not unlike what is found in some parts of Brooklyn');

INSERT INTO places(placeid, name,descrip) VALUES(1, 'Tea Chapel', 'A very large teahouse with bizarre fans waving on the wall, various couches and chairs, and a counter serving everything from mixed drinks to cake. A loud band is playing here');
INSERT INTO places(placeid, name, descrip) VALUES(2, 'Union Street w/o 7th Ave', 'A funky Park Slope street with a variety of stores, people walking their dogs, and the occasional baby stroller or co-op member parting the normal flow of human and motor traffic');
INSERT INTO place_connections(wo, wo_to, dir, descrip) VALUES(1,2, 'south', 'To the south is an exit sitting beneath a stained glass window with the title of the shop prominent, leading out onto the street');
INSERT INTO place_connections(wo, wo_to, dir, descrip) VALUES(2,1, 'north', 'To the north is a set of benches sitting around the entrance to a teahouse, labelled Tea Chapel');
INSERT INTO places(placeid, name, descrip) VALUES(3, 'Park Slope Food Conflict!', 'A bizarre arena full of benches where shoppers are arm-wrestling in competitions for the obligation to pay for each others food. The food looks tasty, but the competition looks fierce, and you feel many eyes considering pairing with you for such a challenge. Fortunately, you are not hungry.');
INSERT INTO place_connections(wo, wo_to, dir, descrip) VALUES(2,3, 'south', 'To the south is a busy entrance to a store with an image of two arms gripping each other over a bowl of salad. Militant-looking granoliers wander in and out, some in strange outfits.');
INSERT INTO place_connections(wo, wo_to, dir, descrip) VALUES(3,2, 'north', 'To the north is what looks like an escape to the street.');
