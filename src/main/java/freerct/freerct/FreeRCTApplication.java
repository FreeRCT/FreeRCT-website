package freerct.freerct;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.context.request.*;

@SpringBootApplication
public class FreeRCTApplication {
	public static void main(String[] args) {
		SpringApplication.run(FreeRCTApplication.class, args);
	}

	public static String uri(WebRequest request) {
		return ((ServletWebRequest)request).getRequest().getRequestURI().toString();
	}

	public static String timestringSince(Calendar timestamp) {
		long delta = Calendar.getInstance().getTimeInMillis() - timestamp.getTimeInMillis();
		boolean future = false;
		if (delta < 0) {
			future = true;
			delta *= -1;
		}
		delta /= 1000 * 60;  // Now it's in minutes.

		if (delta < 1) {
			return "now";
		}
		if (delta <= 1) {
			return future ? ("in a minute") : ("a minute ago");
		}
		if (delta < 60) {
			return future ? ("in " + delta + " minutes") : (delta + " minutes ago");
		}

		delta /= 60;  // Now it's in hours.
		if (delta <= 1) {
			return future ? ("in 1 hour") : ("1 hour ago");
		}
		if (delta < 24) {
			return future ? ("in " + delta + " hours") : (delta + " hours ago");
		}

		delta /= 24;  // Now it's in days.
		if (delta <= 1) {
			return future ? ("tomorrow") : ("yesterday");
		}
		if (delta < 7) {
			return future ? ("in " + delta + " days") : (delta + " days ago");
		}

		if (delta <= 365) {
			delta /= 7;  // Now it's in weeks.
			return delta <= 1 ? future ? ("next week") : ("last week") : future ? ("in " + delta + " weeks") : (delta + " weeks ago");
		}

		delta /= 365;  // Now it's in years (approximately, neglecting leap years).
		return delta <= 1 ? future ? ("next year") : ("last year") : future ? ("in " + delta + " years") : (delta + " years ago");
	}

	private static class DropdownEntry {
		public final String link, slug, label;
		public final boolean newTab;
		public DropdownEntry(String l, String u, String d, boolean t) {
			link = l;
			slug = u;
			label = d;
			newTab = t;
		}
		public DropdownEntry(String l, String u, String d) {
			this(l, u, d, false);
		}
	}

	public static final int MENU_BAR_BAR_HEIGHT = 50;
	public static final int DESIRED_PADDING_BELOW_MENU_BAR = MENU_BAR_BAR_HEIGHT + 8;

	public static String createLinkifiedHeader(String tag, String doc, String slug, String text) {
		return	"<" + tag + " id='" + slug + "' style='padding-top:" + DESIRED_PADDING_BELOW_MENU_BAR + "px'>"
			+	"<a href='" + doc + "#" + slug + "' class='linkified_header'>"
			+	text
			+	"</a></" + tag + ">";
	}


	private static String createMenuBarEntry(String uri, DropdownEntry e) {
		String str	=	"<li class='menubar_li'><a ";
		if (Objects.equals(uri, e.link)) str += "class='menubar_active' ";
		str			+=	"id='menubar_entry_" + e.slug + "' href='" + e.link + "'>" + e.label + "<span class='tooltip_bottom'>" + e.label + "</span></a>"
					+	"</li>"
					;
		return str;
	}

	private static String createMenuBarDropdown(String uri, DropdownEntry e, DropdownEntry ... content) {
		String str	=	"<li class='menubar_li menubar_dropdown' onmouseover='dropdownMouse(this, true)' onmouseout='dropdownMouse(this, false)'><a ";
		if (Objects.equals(uri, e.link)) str += "class='menubar_active' ";
		str			+=	"id='menubar_entry_" + e.slug + "' href='" + e.link + "'>" + e.label + "<span class='tooltip_corner'>" + e.label + "</span></a>"
					+	"<div class='menubar_dropdown_content'>"
					;
		for (DropdownEntry d : content) {
			str += "<a ";
			if (Objects.equals(uri, d.link)) str += "class='menubar_active' ";
			if (d.newTab) str += "target='_blank' ";
			str += "id='menubar_entry_" + d.slug + "' href='" + d.link + "'>" + d.label + "<span class='tooltip_left'>" + d.label + "</span></a>";
		}
		str += "</div></li>";
		return str;
	}

	private static String createLatestPost(String postID, String forum, String topic, String user, Calendar timestamp) {
		return	"<div class='latest_post_entry'>"
			+		"<div>[" + forum + "]</div>"
			+		"<div><a href='/post/" + postID + "'>" + topic + "</a></div>"
			+		"<div>by <a href='/user/" + user + "'>" + user + "</a></div>"
			+		"<div>" + timestringSince(timestamp) + "</div>"
			+ 	"</div>"
			;
	}

	private static AtomicLong globalCounter = new AtomicLong(0);
	public static String generatePage(WebRequest request, String pagename, String body) {
		long counter = globalCounter.incrementAndGet();
		final String uri = uri(request);
		return
			"<!DOCTYPE HTML>"
			+"<html>"
			+	"<head>"
			+		"<head>"
			+			"<link rel='stylesheet' href='/css/style.css'>"
			+			"<title>FreeRCT | " + pagename + "</title>"
			+		"</head>"
			+		"<meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />"
			+	"</head>"
			+	"<body>"

			+		"<script>"
			+			"const MENU_BAR_BAR_HEIGHT            = " + MENU_BAR_BAR_HEIGHT            + ";"
			+			"const DESIRED_PADDING_BELOW_MENU_BAR = " + DESIRED_PADDING_BELOW_MENU_BAR + ";"
			+ """
						function readjustMenuBarY() {
							var ul = document.getElementById('menubar_ul');
							var logo = document.getElementById('menubar_logo');
							var canvas = document.getElementById('menubar_top_canvas');
							var menuitems = document.getElementsByClassName('menubar_li');
							var toc = document.getElementsByClassName('toc');

							var totalMenuH            =   510;
							var logoMaxH              =   320;
							var menuSpacer            =   372.5;
							var bottomSpacer          =   350;
							var fontSize              =    16;
							var alwaysCollapse        = false;
							var replaceTextWithImages = false;
							// Keep the `max-width` constants in sync with the constants in menubar.css!!!
							if (window.matchMedia("(max-width: 1150px)").matches) {
								totalMenuH     =   50;
								logoMaxH       =   50;
								menuSpacer     =   70;
								bottomSpacer   =   60;
								fontSize       =   14;
								alwaysCollapse = true;
								if (window.matchMedia("(max-width: 990px)").matches) {
									replaceTextWithImages = true;
								}
							} else if (window.matchMedia("(max-width: 1480px)").matches) {
								totalMenuH   = 306;
								logoMaxH     = 192;
								menuSpacer   = 223.5;
								bottomSpacer = 210;
								fontSize     =  15;
							}

							const menuBarMaxY = alwaysCollapse ? 0 : (totalMenuH - MENU_BAR_BAR_HEIGHT) / 2;

							const scroll = /* document.body.scrollTop */ window.scrollY / 1.2;
							const newBarY = Math.min(menuBarMaxY, Math.max(0, scroll));

							const newLogoH = logoMaxH - (alwaysCollapse ? 0 : ((logoMaxH - MENU_BAR_BAR_HEIGHT) * newBarY / menuBarMaxY));
							const newLogoHalfspace = (logoMaxH - newLogoH) / 2;

							canvas.style.height = (menuBarMaxY - newBarY) + 'px';

							const topPos = (totalMenuH - MENU_BAR_BAR_HEIGHT) / 2 - newBarY;
							ul.style.top = topPos + 'px';
							for (i = 0; i < toc.length; i++) {
								toc[i].style.top = (topPos + MENU_BAR_BAR_HEIGHT) + 'px';
							}

							document.getElementById('menubar_spacer_menu').style.marginRight = menuSpacer + 'px';
							document.getElementById('menubar_spacer_bottom').style.marginBottom = bottomSpacer + 'px';
							logo.style.height = newLogoH + 'px';
							logo.style.marginLeft = newLogoHalfspace + 'px';
							logo.style.marginRight = newLogoHalfspace + 'px';

							for (i = 0; i < menuitems.length; i++) {
								var element = menuitems[i];
								element.style.backgroundImage = /* replaceTextWithImages ? 'url(img/menu/' + id.link + '.png)' : */ 'none';
								element.style.color = replaceTextWithImages ? 'transparent' : 'var(--text-light)';
								element.style.fontSize = replaceTextWithImages ? '0' : (fontSize + 'px');
								element.style.minHeight = replaceTextWithImages ? '50px' : '0';
							}
						}

						document.body.onscroll = readjustMenuBarY;
						document.body.onresize = readjustMenuBarY;
						document.body.onload   = readjustMenuBarY;

						function dropdownMouse(dd, inside) {
							dd.style = inside ? 'background-color: var(--green-dark)' : '';
						}

					</script>


					<link rel='icon' href='/img/logo.png'>
					<div id='menubar_top_canvas'></div>
					<a class='pictorial_link' href=#top>
						<img id='menubar_logo' src='/img/logo.png' height=auto width=auto></img>
					</a>

			"""
			+		"<ul id='menubar_ul'>"
			+		"<p id='menubar_spacer_menu'></p>"


			+		createMenuBarEntry   (uri, new DropdownEntry("/"                                        , "home"       , "FreeRCT Home"        ))
			+		createMenuBarEntry   (uri, new DropdownEntry("/screenshots"                             , "screenshots", "Screenshots"         ))
			+		createMenuBarEntry   (uri, new DropdownEntry("/download"                                , "download"   , "Get It!"             ))
			+		createMenuBarEntry   (uri, new DropdownEntry("/manual"                                  , "manual"     , "Manual"              ))
			+		createMenuBarDropdown(uri, new DropdownEntry("/contribute"                              , "contribute" , "Contribute"          ),
					                           new DropdownEntry("https://github.com/FreeRCT/FreeRCT"       , "github"     , "Git Repository", true),
					                           new DropdownEntry("https://github.com/FreeRCT/FreeRCT/issues", "issues"     , "Issue Tracker" , true))
			+		createMenuBarEntry   (uri, new DropdownEntry("/news"                                    , "news"       , "News Archive"        ))
			+		"</ul>"
			+		"<p id='menubar_spacer_bottom'></p>"

			+		"<div class='toplevel_content_flexbox'>"
			+			"<div class='content_flexbox_content'>" + body + "</div>"
			+			"<div class='content_flexbox_content' id='latest_posts'>"
			+				"<h1>Latest Posts</h1>"
			+				createLatestPost("8", "Graphics Development", "I want to contribute", "Tester",
									new Calendar.Builder().setDate(2032, 11, 30).setTimeOfDay(15, 24, 01).build())
			+				createLatestPost("7", "Website", counter + " posts already!", "Nordfriese", Calendar.getInstance())
			+				createLatestPost("6", "English Players’ Forum", "Hello World", "Tester",
									new Calendar.Builder().setDate(2022, 1, 4).setTimeOfDay(16, 00, 00).build())
			+				createLatestPost("5", "Deutsches Spielerforum", "Hallo Welt :)", "Nordfriese",
									new Calendar.Builder().setDate(2022, 1, 4).setTimeOfDay(14, 00, 00).build())
			+				createLatestPost("4", "Playing FreeRCT", "this game is awesome but how do i play please help me i have no idea and i need help anyone please", "Tester",
									new Calendar.Builder().setDate(2022, 1, 3).setTimeOfDay(18, 00, 00).build())
			+				createLatestPost("3", "General", "Another Topic", "Nordfriese",
									new Calendar.Builder().setDate(2022, 1, 3).setTimeOfDay(10, 00, 01).build())
			+				createLatestPost("2", "Technical Help", "Help!", "Tester",
									new Calendar.Builder().setDate(2022, 0, 0).setTimeOfDay(15, 24, 01).build())
			+				createLatestPost("1", "Website", "New website", "Nordfriese",
									new Calendar.Builder().setDate(2010, 0, 0).setTimeOfDay(15, 24, 01).build())
			+			"</div>"
			+		"</div>"

			+		"<p id='footer_spacer'></p><footer id='menubar_footer'>© 2021-"
							+ Calendar.getInstance().get(Calendar.YEAR)
			+		" by the FreeRCT Development Team</footer>"
			+		"<script>readjustMenuBarY();</script>"

			+	"</body>"
			+"</html>"
			;
	}
}
