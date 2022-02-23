package freerct.freerct;

import javax.servlet.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;

import static freerct.freerct.FreeRCTApplication.generatePage;
import static freerct.freerct.FreeRCTApplication.sql;
import static freerct.freerct.FreeRCTApplication.getCalendar;
import static freerct.freerct.FreeRCTApplication.htmlEscape;
import static freerct.freerct.FreeRCTApplication.renderMarkdown;
import static freerct.freerct.FreeRCTApplication.datetimestring;
import static freerct.freerct.FreeRCTApplication.shortDatetimestring;
import static freerct.freerct.FreeRCTApplication.createLinkifiedHeader;

/** The manual page. */
@Controller
public class Manual {
	@GetMapping("/manual")
	@ResponseBody
	public String fetch(WebRequest request, HttpSession session) {
		return generatePage(request, session, "Manual", """
			<div class="manual_section">
				<div class="manual_margin hideme"">
					<div></div>  <!-- spacer -->
				</div>  <!-- end of manual_margin -->
				<div class="manual_body">
					<div>
						<h1>Game Manual</h1>
						<p>
							In FreeRCT, you are the manager of a pleasure park. You build everything that your guests need to be happy:
							most importantly, rollercoasters and other rides, but also food stalls, footpaths, flowerbeds,
							and everything else that will make your guests love your park and spend lots of money there.
						</p><p>
							Additionally, you oversee the park's administration: Set the entrance fees on rides, hire and fire staff,
							take and repay loans, and even design your very own rollercoasters!
						</p><p>
							A wise manager will lead their park to fame and fortune; those less fortunate will go under
							amid dirty paths, vandalising guests, and crashing coasters. Which one will be your fate?
						</p>
					</div>
				</div>  <!-- end of manual_body -->
			</div>  <!-- end of manual_section -->

			<div class="manual_section">
				<div class="manual_margin hideme">
					<div></div>  <!-- spacer -->
				</div>  <!-- end of manual_margin -->
				<div class="manual_body">
					<div>
			"""
			+ createLinkifiedHeader("h2", "/manual", "download-install", "Downloading and Installing")
			+ """
						<p>
							See the <a href="/download">download page</a> for information on where to get and how to install FreeRCT.
						</p>
					</div>
				</div>  <!-- end of manual_body -->
			</div>  <!-- end of manual_section -->

			<div class="manual_section">
				<div class="manual_margin">
					<div class="manual_screenshot">
						<img src="/img/screenshots/manual/mainmenu.png"></img>
						<p> The FreeRCT Main menu. </p>
					</div>
					<div></div>  <!-- spacer -->
				</div>  <!-- end of manual_margin -->
				<div class="manual_body">
					<div>
			"""
			+ createLinkifiedHeader("h2", "/manual", "mainmenu", "Main Menu")
			+ """
					<p>
						After starting FreeRCT, you should see a splash screen with the FreeRCT logo which fades slowly into the main menu.
						You can click to skip the splash screen.
					</p><p>
						The main menu has four buttons: To start a new game, to load a previously saved game, to open the options menu, and to quit the game.
					</p>

					</div><div>
			"""
			+ createLinkifiedHeader("h3", "/manual", "mm-newgame", "New Game")
			+ """
						<p>
							When you click the <em>New Game</em> button, you are immediately presented with a brand-new empty park and ready to start playing.
						</p>

					</div><div>
			"""
			+ createLinkifiedHeader("h3", "/manual", "mm-loadgame", "Load")
			+ """
						<p>
							When you click the <em>Load</em> button, a window with a list of all saved games appears.
							Select a game and press <em>Load</em> to load it, or press <em>Cancel</em> to abort.
						</p>

					</div><div>
			"""
			+ createLinkifiedHeader("h3", "/manual", "mm-settings", "Settings")
			+ """
						<p>
							When you click the <em>Settings</em> button, the Settings window opens.
							This window allows you to change the language of FreeRCT and the window resolution.
							The window resolution can also be changed by manually resizing the game window.
						</p>

					</div><div>
			"""
			+ createLinkifiedHeader("h3", "/manual", "mm-quit", "Quit")
			+ """
						<p>
							When you click the <em>Quit</em> button, FreeRCT closes immediately.
						</p>
					</div>
					<div></div>  <!-- spacer -->
				</div>  <!-- end of manual_body -->
			</div>  <!-- end of manual_section -->


			<div class="manual_section">
				<div class="manual_margin">
					<div class="manual_screenshot">
						<img src="/img/screenshots/manual/mainview.png"></img>
						<p> The main game view. </p>
					</div>
					<div></div>  <!-- spacer -->
				</div>  <!-- end of manual_margin -->
				<div class="manual_body">
					<div>
			"""
			+ createLinkifiedHeader("h2", "/manual", "game-window", "The Game Window")
			+ """
						<p>
							The game window is divided into three areas. In the center of the window is the <em>main view</em>,
							which displays your park.
							At the top of the window is the <em>toolbar</em>, which contains all the buttons and controls to manage both the park and FreeRCT.
							At the bottom of the window is the <em>info panel</em>, which shows information about your park.
						</p>

					</div><div>
			"""
			+ createLinkifiedHeader("h3", "/manual", "mainview", "The Main View")
			+ """
						<p>
							The main view displays the game world.
							You can navigate around by right-click-and-dragging.
							Use the Left and Right arrow keys to rotate the view in 90° steps.
							You can click on things (rides, guests, …) to open windows which display
							more information about the item as well as management options.
						</p>

					</div><div>
			"""
			+ createLinkifiedHeader("h3", "/manual", "toolbar", "The Toolbar")
			+ """
						<p>
							The toolbar buttons, from left to right, mean:
							<ul>
								<li>      The <em>Menu</em> dropdown contains options to save the game, enable editor mode,
										  open the Settings window, return to the main menu, and quit FreeRCT.
								</li><li> The <em>Game Speed</em> dropdown allows you to change the speed of the running game
										  as well as to pause and unpause it.
								</li><li> The <em>View</em> dropdown contains settings and windows regarding the view on the map.
								</li><li> The <em>Terraform</em> button opens a window which allows you to
										  modify the landscape in the park.
								</li><li> The <em>Paths</em> button opens a window which allows you to
										  build and remove paths and queue paths in your park.
								</li><li> The <em>Fences</em> button opens a window which allows you to
										  build and remove fences in your park.
								</li><li> The <em>Scenery</em> button opens a window which allows you to
										  build and remove scenery items such as trees and fountains in your park.
								</li><li> The <em>Path Objects</em> button opens a window which allows you to
										  build and remove benches, lamps, and litter bins in your park.
								</li><li> The <em>Build Ride</em> button opens a window which allows you to
										  select a ride type to build in your park.
								</li><li> The <em>Staff</em> button opens a window which allows you to
										  hire and dismiss park staff.
								</li><li> The <em>Inbox</em> button opens a window which allows you to
										  view all messages you received.
								</li><li> The <em>Finances</em> button opens a window which
										  provides an overview over your park's finances.
								</li>
							</ul>
						</p>

					</div><div>
			"""
			+ createLinkifiedHeader("h3", "/manual", "info-panel", "The Info Panel")
			+ """
						<p>
							In the upper left corner of the info panel you can see the amount of <em>cash</em> your park currently has.
							Below it is the number of <em>guests</em> in the park.
							To the right of these figures is an indicator of the current <em>weather</em> in the park, ranging from sunny to thunderstorm,
							with the <em>temperature</em> next to it.
							At the right edge of the info panel is the current in-game <em>date</em>.
							The <em>compass</em> to its left indicates the current rotation of your main view (the red arrow points north).
							The large empty space in the middle of the info panel is where any <em>messages</em> you receive are previewed.
						</p>
					</div>
					<div></div>  <!-- spacer -->
				</div>  <!-- end of manual_body -->
			</div>  <!-- end of manual_section -->


			<div class="manual_section">
				<div class="manual_margin">
					<div class="manual_screenshot">
						<img src="/img/screenshots/manual/paths_single.png"></img>
						<p> How to build footpaths. </p>
					</div>
					<div class="manual_screenshot">
						<img src="/img/screenshots/manual/paths_directional.png"></img>
						<p> Directional path building mode. </p>
					</div>
					<div class="manual_screenshot">
						<img src="/img/screenshots/manual/path_objects.png"></img>
						<p> How to build path objects: Benches, bins, and lamps. </p>
					</div>
					<div class="manual_screenshot">
						<img src="/img/screenshots/manual/scenery.png"></img>
						<p> How to build scenery items such as flowerbeds and fountains. </p>
					</div>
					<div class="manual_screenshot">
						<img src="/img/screenshots/manual/terraform.png"></img>
						<p> How to terraform the landscape. </p>
					</div>
					<div class="manual_screenshot">
						<img src="/img/screenshots/manual/terraform_hill.png"></img>
						<p> How to create hills and valleys quickly. </p>
					</div>
					<div class="manual_screenshot">
						<img src="/img/screenshots/manual/fences.png"></img>
						<p> How to build fences. </p>
					</div>
					<div></div>  <!-- spacer -->
				</div>  <!-- end of manual_margin -->
				<div class="manual_body">
					<div>
			"""
			+ createLinkifiedHeader("h3", "/manual", "infra", "Building Basic Park Infrastructure")
			+ """
			"""
			+ createLinkifiedHeader("h3", "/manual", "footpaths", "Footpaths")
			+ """
						<p>
							Your guests will move only on footpaths. To build footpaths, click on the 
							<em>Paths</em> button in the top toolbar and select a
							<em>normal path</em> type. Now click anywhere in your park and a footpath
							will appear. Right-click on an existing footpath to remove it.
							Building paths costs money and may only be performed if you still have
							enough cash. Deleting paths returns cash, but less than it cost to build the path.
						</p><p>
							At the bottom of the window, you can switch between <em>single</em> and
							<em>directional</em> path building mode. In single mode, a click
							corresponds to placing one tile of path. You can place paths only on flat terrain
							and suitably formed slopes. In directional mode, you can also build elevated
							paths. Click on the map to position the path building cursor there. Now use
							the four yellow arrow buttons in the path building window to choose a direction,
							and the three slope buttons to choose the angle. Finally, click the <em>Buy</em>
							button to build a path in the location indicated by the cursor. The cursor
							automatically advances one step forward so you can build a longer path quickly.
							Using the <em>Remove</em> button removes the last path segment and moves the
							cursor one step backwards. You can also move the cursor by clicking on the map
							or using the <em>Back</em> and <em>Forward</em> buttons.
						</p><p>
							Queues for rides can be built in the same manner as normal paths.
							Just select a <em>queue path</em> type instead of a <em>normal path</em>.
							You will learn more about queues in the <em>Rides</em> section below.
						</p>

					</div><div>
			"""
			+ createLinkifiedHeader("h3", "/manual", "path-objects", "Path Objects")
			+ """
						<p>
							Those paths are a bit bare, aren't they? Let's add some path objects.
							To do so, click the <em>Path Objects</em> button in the top toolbar.
						</p><p>
							There are three types of path objects available:
							<ul>
									 <li> <em>Benches</em> allow guests to sit down and rest.
										  This makes the guests happy and serves to reduce nausea and
										  tiredness. Two guests can sit on each bench. Benches can only
										  be built on flat paths.
								</li><li> <em>Litterbins</em> are used by guests to throw away wrappers
										  from food they bought. If a guest can't find a litterbin to dispose
										  of his litter, he will simply throw it down, so make sure there
										  are enough bins around! When a lot of litter has been thrown in
										  a bin, the bin needs to be emptied by a handyman before guests
										  can dispose of more litter there.
								</li><li> <em>Lamps</em> look nice. They don't have a
										  function beyond decorating your park.
								</li>
							</ul>
						</p><p>
							Like almost everything in this game, path objects cost money to build.
							To build a path object, select it in the Path Objects window and click on an
							existing footpath. <em>All</em> edges of this footpath are immediately
							furnished with the selected object. Any other path object previously present
							will be replaced.
						</p>

					</div><div>
			"""
			+ createLinkifiedHeader("h3", "/manual", "scenery", "Scenery")
			+ """
						<p>
							Most of your park is just a featureless green plain. How about adding some trees, flowers,
							and other scenery? To do so, click the <em>Scenery</em> button in the top toolbar.
						</p><p>
							The Scenery window looks and behaves much like the Path Objects window.
							However, there are more items available for purchase, so they're grouped into
							categories. Use the tab buttons at the top of the window to switch between the categories.
							Click on an item to select it, then click on the map to buy it.
							Scenery items can not be built on slopes.
							Some items occupy just one field, but some occupy several. For items that are
							not symmetrical, the two <em>Rotate</em> buttons in the top-right corner of
							the Scenery window allow you to choose the orientation of the item you're about to buy.
						</p><p>
							Right-click on an existing scenery item to remove it. Some items (e.g.
							fountains) will return cash on removal, while others (such as trees)
							actually cost more money to get rid of.
						</p>

					</div><div>
			"""
			+ createLinkifiedHeader("h3", "/manual", "terraforming", "Terraforming")
			+ """
						<p>
							The landscape is still looking a tad boring. Some hills or cliffs would be nice.
							To add some, click the <em>Landscaping</em> button in the top toolbar.
						</p><p>
							The Landscaping window has two important settings.
							On the right are two buttons to increase and decrease the size of the area you will terraform.
							Below is the option whether to level the area or move it as a whole.
						</p><p>
							Modifying the landscape is as easy as moving the mouse over the park so the cursor
							indicates the area you want to edit and then using the <em>mouse wheel</em>
							to move the area up or down.
						</p><p>
							If the size of the terraforming cursor is just a single tile,
							you can also point at the corner of a tile to modify just a quarter tile.
						</p><p>
							If the size of the terraforming cursor is just a single dot,
							it behaves like the single tile cursor with one exception: In this mode,
							surrounding tiles will be dragged along with the tile or corner you're
							editing, allowing you to create large hills or valleys without cliffs quickly.
						</p><p>
							If you hold down the <em>Shift</em> key while terraforming,
							no changes will be applied and you will instead receive an estimate
							of the cost required to actually apply the changes.
							This works for several other types of modifications as well.
						</p>

					</div><div>
			"""
			+ createLinkifiedHeader("h3", "/manual", "fences", "Fences")
			+ """
						<p>
							If you played around with the various tools, your park should have a pretty
							nice landscape by now. For the finishing touch, click the
							<em>Fences</em> button in the top toolbar.
						</p><p>
							Building fences is as easy as it can get: Select one of the three fence
							types, then click on the map to build a fence segment.
							Right-clicking on an existing fence will remove it.
						</p>
					</div>
					<div></div>  <!-- spacer -->
				</div>  <!-- end of manual_body -->
			</div>  <!-- end of manual_section -->


			<div class="manual_section">
				<div class="manual_margin">
					<div class="manual_screenshot">
						<img src="/img/screenshots/manual/rides_selection.png"></img>
						<p> The first step is to select the ride you want to build. </p>
					</div>
					<div class="manual_screenshot">
						<img src="/img/screenshots/manual/rides_build_fixed.png"></img>
						<p> To build a fixed ride, all you need to do is select a nice location. </p>
					</div>
					<div class="manual_screenshot">
						<img src="/img/screenshots/manual/rides_build_coaster1.png"></img>
						<p> Building a tracked ride requires more planning from you… </p>
					</div>
					<div class="manual_screenshot">
						<img src="/img/screenshots/manual/rides_build_coaster2.png"></img>
						<p> …since you need to spell out the entire path. </p>
					</div>
					<div class="manual_screenshot">
						<img src="/img/screenshots/manual/entrances.png"></img>
						<p> Now let's build some entrances and exits for our rides. </p>
					</div>
					<div class="manual_screenshot">
						<img src="/img/screenshots/manual/queues.png"></img>
						<p> Don't forget to connect the entrances and exits to the path network. </p>
					</div>
					<div class="manual_screenshot">
						<img src="/img/screenshots/manual/testing.png"></img>
						<p> Your coaster should be tested before you open it to the public. </p>
					</div>
					<div></div>  <!-- spacer -->
				</div>  <!-- end of manual_margin -->
				<div class="manual_body">
					<div>
			"""
			+ createLinkifiedHeader("h2", "/manual", "rides", "Rides")
			+ """
						<p>
							Rides are the most important aspect of your park. So let's see how to build and manage them.
						</p>

					</div><div>
			"""
			+ createLinkifiedHeader("h3", "/manual", "rides-selection", "Rides Selection")
			+ """
						<p>
							To build a new ride, click the <em>Rides</em> button in the top toolbar.
							The Rides Selection window displays a list of all rides you can buy,
							sorted into several categories.
						</p><p>
							Clicking on a ride in the list displays more information about the ride.
							Once you have made your choice, click the <em>Select</em> button to start
							building the ride.
						</p>

					</div><div>
			"""
			+ createLinkifiedHeader("h3", "/manual", "fixed-rides", "Building Fixed Rides")
			+ """
						<p>
							There are two types of rides: <em>Fixed</em> and <em>tracked</em> rides.
						</p><p>
							A fixed ride consists of a single building, such as a shop. After you have
							selected to build a fixed ride, a window with a preview and the placement
							cost of the ride opens. You can rotate the ride using the yellow arrow
							buttons in the Ride Build window. Click on the map to build the building
							at the location of your choice. And that's it, the ride has now been constructed.
						</p>

					</div><div>
			"""
			+ createLinkifiedHeader("h3", "/manual", "tracked-rides", "Building Tracked Rides")
			+ """
						<p>
							Some rides, such as coasters, require you to build an individual
							track of your own design. After you have selected to build a tracked
							ride, a window with a list of all available track pieces opens.
							A station tile is preselected for you. You must always begin building
							a tracked ride by placing at least one station segment.
						</p><p>
							Use the yellow arrow buttons in the Ride Build window to rotate the currently
							selected tile, then click on the map to place the first station tile.
						</p><p>
							Now you can append more station tiles by clicking the big <em>Buy Track Piece</em>
							button in the Ride Build window, or you can change the type of track piece
							you're placing. The buttons at the top of the Ride build window allow you to
							switch between straight pieces and tracks with differing curve radii,
							between flat and sloped sections with varying steepness, between regular and
							banked curves, between lifthills with and without chain lifts, between regular
							track and stations, and much more. Note that not every coaster type has
							every type of track piece available, and not every setting can be combined
							with every other one. Play around to get a feeling for what's possible and
							what's not.
						</p><p>
							The controls at the bottom of the Ride Build window allow you to
							delete the last track piece and to move the building cursor back
							and forth. This is useful to correct mistakes in the middle of the
							track without having to delete unrelated sections.
						</p><p>
							Your track must always form a closed loop. Once that is achieved, you can
							close the Ride Build window to complete the construction phase.
						</p>

					</div><div>
			"""
			+ createLinkifiedHeader("h3", "/manual", "ride-man-window", "Ride Management Window")
			+ """
						<p>
							Click on a ride to open its management window.
						</p>
						<p>
							Newly built rides are not yet ready to operate. You first need to place an
							entrance and an exit. The Ride Management Window contains two buttons
							to do this. Conveniently, you are automatically put in Entrance Placement
							mode upon opening the Management Window of a ride that lacks an entrance.
							Building the entrance is as easy as moving the mouse to a free location next
							to the ride and clicking. You are then put in Exit Placement mode,
							which works the same way to place a ride exit.
						</p><p>
							Most rides need exactly one entrance and one exit. Building another one
							moves the existing one, if any. However, there are two exceptions:
							Shops do not have entrances or exits; the buttons are not available for them.
							And tracked rides need exactly one entrance and one exit <em>per station</em>.
						</p><p>
							The Ride Management Window allows you to change the colouring of your ride
							via the <em>Recolour</em> buttons. Which parts of the ride can be recoloured
							and which colours are available depends on the ride type. You can also
							change the type and colouring of the entrances and exits.
						</p><p>
							The <em>Shop</em> Management Window shows some statistics about the items
							that are being sold here. You can not change any additional settings yet.
						</p><p>
							The Management Window for all rides except shops allows you to define a few more
							settings: The <em>entrance fee</em>, the minimal and maximal waiting time,
							and how often to call a mechanic to inspect the ride. Inspecting a ride
							increases its reliability and reduces the risk of breakdowns.
						</p><p>
							For <em>fixed rides</em>, you can additionally control the number of cycles the ride
							performs, if the ride type supports this.
						</p><p>
							For <em>coasters</em>, you can additionally decide how many trains the coaster
							has and how many cars each train consists of. Your choices are limited by
							coaster type-specific restrictions and the amount of space available in the
							coaster stations.
						</p><p>
							All rides except shops have <em>Excitement</em>, <em>Intensity</em>, and
							<em>Nausea</em> ratings.
							Coasters additionally display graphs about the speed and G forces along the
							track. All these statistics are measured when the ride is actually working, so
							the values are not available initially.
						</p><p>
							Finally, the Ride Management window contains a button to remove the ride and,
							most important of all, traffic light buttons to open and close the ride.
							Your ride is closed initally. Click the green light to open it and the red
							light to close it down.
							Coasters additionally have a button to edit the track,
							and a yellow light to test the ride.
						</p>

					</div><div>
			"""
			+ createLinkifiedHeader("h3", "/manual", "testing-rides", "Testing Rides")
			+ """
						<p>
							It is strongly recommended to test your coaster before opening it.
							Testing causes the ride to operate just as if it were open,
							but no guests will visit yet. This will create the ride statistics,
							allowing you to see how interesting or boring your track design is,
							and most importantly you will see whether your coaster actually manages
							to climb all the hills &ndash; or whether it bounces back and perhaps even crashes.
						</p>

					</div><div>
			"""
			+ createLinkifiedHeader("h3", "/manual", "ride-persons", "Interaction With Persons")
			+ """
						<p>
							Guests enter rides though <em>entrances</em>. If a ride can not be entered yet,
							guests will patiently wait in a neat queue until the ride is free.
							But queues will only form on queue paths &ndash; if the entrance is adjoined to a
							normal path, at most one guest will queue. It is therefore recommended to
							connect the ride entrance and the regular paths via a <em>queue path</em>
							several tiles in length.
						</p><p>
							Guests leave rides through the <em>exit</em>. The exit should therefore be
							directly adjoined to a normal path.
						</p><p>
							When the ride is broken down or needs to be inspected, a mechanic enters the
							ride through the ride's <em>exit</em>. For coasters with multiple stations,
							the mechanic always picks the first station's exit.
						</p><p>
							Guests base their decision on whether to enter a ride on the ride's
							Excitement, Intensity, and Nausea ratings. Ratings are measured on a scale
							from 0 (boring / dull / not nauseating) to 10 (extremely exciting /
							incredibly intense / extremely nauseating); in rare cases higher ratings
							are possible. High Excitement ratings and low Nausea ratings are desirable;
							which Intensity rating is acceptable depends on each guest's individual
							preference, though very high ratings are tolerated only by very few guests.
						</p><p>
							When guests become hungry or thirsty, they buy food or drink from shops.
							Guests will also need to visit a toilet eventually.
							The information kiosk sells park maps, which aid guests in finding their way
							around the park, and umbrellas, which guests will be glad of in bad weather.
						</p><p>
							When guests visit a ride, they become nauseous; the more so the higher the
							ride's Nausea rating is. Nauseous guests throw up on the footpaths unless
							they find a First Aid booth or sit on a bench until it subsides.
						</p>
					</div>
					<div></div>  <!-- spacer -->
				</div>  <!-- end of manual_body -->
			</div>  <!-- end of manual_section -->


			<div class="manual_section">
				<div class="manual_margin">
					<div class="manual_screenshot">
						<img src="/img/screenshots/manual/staff.png"></img>
						<p> Managing your staff properly is crucial for the park's welfare. </p>
					</div>
					<div class="manual_screenshot">
						<img src="/img/screenshots/manual/messages.png"></img>
						<p> Messages inform you about important events. </p>
					</div>
					<div class="manual_screenshot">
						<img src="/img/screenshots/manual/finances.png"></img>
						<p> The finances window provides an overview how well your park is doing. </p>
					</div>
					<div class="manual_screenshot">
						<img src="/img/screenshots/manual/person.png"></img>
						<p> Inspect any person in your park &ndash; simply by clicking on them. </p>
					</div>
					<div></div>  <!-- spacer -->
				</div>  <!-- end of manual_margin -->
				<div class="manual_body">
					<div>
			"""
			+ createLinkifiedHeader("h2", "/manual", "park-admin", "Park Administration")
			+ """
			"""
			+ createLinkifiedHeader("h3", "/manual", "staff", "Staff")
			+ """
						<p>
							Hiring staff is an important aspect of keeping your park functional.
							Click the <em>Staff</em> button in the top toolbar to open the staff window.
						</p><p>
							There are four types of staff for your park:
							<ul>
									 <li> <em>Handymen</em> take care of sweeping dirty paths, emptying full
										  litterbins, watering the flowerbeds, and mowing the lawns.
								</li><li> <em>Mechanics</em> repair broken rides, and inspect rides
										  frequently to reduce the likelihood of breakdowns.
								</li><li> <em>Security guards</em> prevent nearby guests from demolishing path objects.
								</li><li> <em>Entertainers</em> increase the happiness and patience of nearby guests.
								</li>
							</ul>
						</p><p>
							Select one of the four tabs in the staff window to see how many of this staff
							you currently have, what they're doing right now, and how high their monthly
							salary is. Use the <em>Hire</em> button to hire more staff.
							You can click on any staff member to open his personal information window.
							Next to each staff member is a black cross that allows you to dismiss him instantly.
						</p>

					</div><div>
			"""
			+ createLinkifiedHeader("h3", "/manual", "inbox", "Inbox")
			+ """
						<p>
							FreeRCT occasionally sends you a message to inform you about important events
							such as availability of new ride types, common complaints, awards won, and more.
							The newest message is previewed in the bottom toolbar. The <em>Inbox</em>
							button in the top toolbar opens a list of all messages your park ever received.
						</p>

					</div><div>
			"""
			+ createLinkifiedHeader("h3", "/manual", "finances", "Finances")
			+ """
						<p>
							The <em>Finances</em> button in the top toolbar shows you details
							about your monthly earnings and spendings. You can also take and repay loans here.
							Other controls such as marketing and research, a history,
							and further statistics such as park value are not yet implemented.
						</p>

					</div><div>
			"""
			+ createLinkifiedHeader("h3", "/manual", "person-window", "Personal Information Windows")
			+ """
						<p>
							You can click on any person &ndash; guest or staff &ndash; in the main view
							to open a window displaying personal information about them.
							Staff can also be dismissed via a button in this window.
						</p>
					</div>
					<div></div>  <!-- spacer -->
				</div>  <!-- end of manual_body -->
			</div>  <!-- end of manual_section -->

			<div class="manual_section">
				<div class="manual_margin hideme">
					<div></div>  <!-- spacer -->
				</div>  <!-- end of manual_margin -->
				<div class="manual_body">
					<div>
			"""
			+ createLinkifiedHeader("h2", "/manual", "shortcuts", "Keybindings")
			+ """
						<p>
							Many game elements are also (or, in some cases, only) accessible via the keyboard:
						</p> <p style="text-align:center"> <table>
							<tr>
								<th>Key</th>
								<th>Action</th>
							</tr>
							<tr>
								<td><em>Left</em> Arrow Key</td>
								<td>Move the main view left.</td>
							</tr>
							<tr>
								<td><em>Right</em> Arrow Key</td>
								<td>Move the main view right.</td>
							</tr>
							<tr>
								<td><em>Up</em> Arrow Key</td>
								<td>Move the main view up.</td>
							</tr>
							<tr>
								<td><em>Down</em> Arrow Key</td>
								<td>Move the main view down.</td>
							</tr>
							<tr>
								<td><em>Page Up</em></td>
								<td>Rotate the main view counter-clockwise.</td>
							</tr>
							<tr>
								<td><em>Page Down</em></td>
								<td>Rotate the main view clockwise.</td>
							</tr>
							<tr>
								<td><em>Ctrl+S</em></td>
								<td>Save the game.</td>
							</tr>
							<tr>
								<td><em>Ctrl+O</em></td>
								<td>Open the options window.</td>
							</tr>
							<tr>
								<td><em>Ctrl+W</em></td>
								<td>Return to the main menu.</td>
							</tr>
							<tr>
								<td><em>Ctrl+Q</em></td>
								<td>Quit the game.</td>
							</tr>
							<tr>
								<td><em>Alt+0</em></td>
								<td>Set the game speed to Paused.</td>
							</tr>
							<tr>
								<td><em>Alt+1</em></td>
								<td>Set the game speed to 1×.</td>
							</tr>
							<tr>
								<td><em>Alt+2</em></td>
								<td>Set the game speed to 2×.</td>
							</tr>
							<tr>
								<td><em>Alt+3</em></td>
								<td>Set the game speed to 4×.</td>
							</tr>
							<tr>
								<td><em>Alt+4</em></td>
								<td>Set the game speed to 8×.</td>
							</tr>
							<tr>
								<td><em>1</em></td>
								<td>Open the terraform window.</td>
							</tr>
							<tr>
								<td><em>2</em></td>
								<td>Open the paths window.</td>
							</tr>
							<tr>
								<td><em>3</em></td>
								<td>Open the fence window.</td>
							</tr>
							<tr>
								<td><em>4</em></td>
								<td>Open the scenery window.</td>
							</tr>
							<tr>
								<td><em>5</em></td>
								<td>Open the path objects window.</td>
							</tr>
							<tr>
								<td><em>6</em></td>
								<td>Open the rides window.</td>
							</tr>
							<tr>
								<td><em>7</em></td>
								<td>Open the staff management window.</td>
							</tr>
							<tr>
								<td><em>8</em></td>
								<td>Open the inbox window.</td>
							</tr>
							<tr>
								<td><em>9</em></td>
								<td>Open the finances window.</td>
							</tr>
							<tr>
								<td><em>M</em></td>
								<td>Open the minimap.</td>
							</tr>
							<tr>
								<td><em>U</em></td>
								<td>Toggle underground view mode.</td>
							</tr>
						</table> </p> <p>
							In the main menu, the following shortcuts are available:
						</p> <p style="text-align:center"> <table>
							<tr>
								<th>Key</th>
								<th>Action</th>
							</tr>
							<tr>
								<td>N</td>
								<td>Start a new game.</td>
							</tr>
							<tr>
								<td>L</td>
								<td>Load a saved game.</td>
							</tr>
							<tr>
								<td>O</td>
								<td>Open the options window.</td>
							</tr>
							<tr>
								<td>Q</td>
								<td>Quit the game.</td>
							</tr>
						</table> </p> <p>
							In all windows, <em>Return</em> selects the default option (if any), and the
							<em>Escape</em>, <em>Backspace</em>, and <em>Delete</em> keys close the window.
						</p>
					</div>
				</div>  <!-- end of manual_body -->
			</div>  <!-- end of manual_section -->
		""");
	}
}
