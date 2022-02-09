package freerct.freerct;

import java.io.*;
import java.nio.file.Files;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import com.github.rjeschke.txtmark.*;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.web.context.request.*;

/** The main class. It provided the main loop and various important utility functions. */
@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
public class FreeRCTApplication {
	public static final int MENU_BAR_BAR_HEIGHT            =                      50;  ///< Height of the menu bar in pixels.
	public static final int DESIRED_PADDING_BELOW_MENU_BAR = MENU_BAR_BAR_HEIGHT + 8;  ///< Spacing above the topmost content element in pixels.

	private static final int LATEST_POSTS_DEFAULT_COUNT = 5;  ///< How many latest posts we normally show by default.

	public static final long POST_EDIT_TIMEOUT = 1000 * 60 * 60 * 24;  ///< How long after posting a user may edit their post, in milliseconds.

	private static final Map<String, Object> _config = new HashMap<>();
	private static Connection _database = null;

	/** The main loop. */
	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(FreeRCTApplication.class);
		Map<String, Object> properties = new HashMap<>();

		try {
			File file = new File("config");
			if (file.isFile()) {
				Map<String, Object> curMap = properties;
				for (String line : Files.readAllLines(file.toPath())) {
					line = line.trim();
					if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) continue;

					if (line.matches("~+")) {
						if (curMap == properties) {
							curMap = _config;
							continue;
						}
						throw new Exception("More than two sections in config file");
					}

					String[] str = line.split("=");
					for (int i = 0; i < str.length; ++i) str[i] = str[i].trim();
					if (str.length < 2) {
						if (str.length == 1) curMap.put(str[0], "");
						continue;
					}

					String arg = str[1];
					for (int i = 2; i < str.length; ++i) arg += "=" + str[i];
					if (arg.startsWith("\"")) {
						arg = arg.substring(1);
						if (arg.endsWith("\"")) arg = arg.substring(0, arg.length() - 1);
					}
					curMap.put(str[0], arg);
				}
			}
		} catch (Exception e) {
			System.out.println("Cannot load config file: " + e);
			e.printStackTrace();
			System.exit(1);
		}

		app.setDefaultProperties(properties);

		try {
			_database = DriverManager.getConnection("jdbc:mysql://" + config("databasehost") + ":" +
					                                config("databaseport") + "/" + config("databasename"),
					                                config("databaseuser"), config("databasepassword"));
		} catch (Exception e) {
			System.out.println("Cannot connect to database: " + e);
			e.printStackTrace();
			System.exit(2);
		}

		app.run(args);  // Main loop.
	}

	/**
	 * Get a config file entry.
	 * @param key Config key to look up.
	 * @return The value, or null if the key does not exist.
	 */
	public static String config(String key) {
		Object o = _config.get(key);
		return o == null ? null : o.toString();
	}

	/**
	 * Execute a safe SQL statement or query.
	 * @param query SQL statement to execute.
	 * @param values Parameters to insert into the query's placeholders. Never concatenate arbitrary values to a query!!!
	 * @return The ResultSet returned by the query, or null if none.
	 */
	public static ResultSet sql(String query, Object... values) throws SQLException {
		synchronized (sqlSync()) {
			try {
				PreparedStatement s = _database.prepareStatement(query);
				for (int i = 0; i < values.length; i++) s.setObject(i + 1, values[i]);
				return s.execute() ? s.getResultSet() : null;
			} catch (SQLException e) {
				System.out.println("SQL Error: " + e);
				e.printStackTrace();
				throw e;
			}
		}
	}

	/**
	 * Get the correct singular or plural form for a string.
	 * @param n Number for which to get the form.
	 * @param singular The singular form.
	 * @param plural The plural form.
	 * @return The singular or plural string as appropriate, prepended with n.
	 */
	public static String pluralForm(long n, String singular, String plural) {
		return n + " " + (n == 1 ? singular : plural);
	}

	/**
	 * Get the object on whose monitor all SQL queries are synchronized,
	 * allowing you to perform multiple queries in a row without any
	 * other database access happening concurrently.
	 * @return The object to use in your 'synchronized()' clause.
	 */
	public static Object sqlSync() {
		return _database;
	}

	/**
	 * Get a Calendar object from a ResultSet.
	 * @param sql ResultSet to query.
	 * @param field Name of the column.
	 * @return The Calendar instance.
	 */
	public static Calendar getCalendar(ResultSet sql, String field) throws SQLException {
		Timestamp t = sql.getTimestamp(field);
		if (t == null) return null;
		Calendar c = Calendar.getInstance();
		c.setTime(t);
		return c;
	}

	/**
	 * Get the URI of the current request.
	 * @param request The current web request.
	 * @return The URI of the current webpage.
	 */
	public static String uri(WebRequest request) {
		return ((ServletWebRequest)request).getRequest().getRequestURI().toString();
	}

	/**
	 * Interpret an arbitrary string as a Markdown-styled text. Also takes care of HTML escaping.
	 * Note that the result is always the concatenation of one or more <p> tags
	 * and can therefore not be used as an inline element.
	 * @param input Text to render (may be HTML-unsafe).
	 * @return Rendered and HTML-safe string.
	 */
	public static String renderMarkdown(String input) {
		if (input == null) return null;

		/* Escaping the '>' character means we cannot use Markdown's quote syntax.
		 * So we first need to define a custom MD symbol for quotes (we use "§§§");
		 * convert '>' to this symbol; then escape HTML; then change it back;
		 * and only then run Markdown. Afterwards, convert double-escaped
		 * characters back to single-escaped.
		 * This means that there may be unescaped '>' symbols in the resulting
		 * text, but since we don't allow any unescaped '<' symbols this alone
		 * should not enable HTML injection.
		 */
		input = input.replaceAll(">", _markdown_quote_symbol);
		input = htmlEscape(input);
		input = input.replaceAll(_markdown_quote_symbol, ">");
		input = Processor.process(input, _markdown_cfg).trim();
		input = input.replaceAll("&amp;([a-z]+);", "&$1;");
		return input;
	}

	private static final String _markdown_quote_symbol = "§§§";
	private static final Configuration _markdown_cfg = Configuration.builder().enableSafeMode().forceExtentedProfile().build();

	/**
	 * Escape HTML characters in arbitrary text.
	 * @param input (Possibly unsafe) text to escape.
	 * @return Escaped text.
	 */
	public static String htmlEscape(String input) {
		return input == null ? null : (input
			.replaceAll("&", "&amp;")  // This rule must come first!
			.replaceAll("\"", "&quot;")
			.replaceAll("\'", "&apos;")
			.replaceAll("<", "&lt;")
			.replaceAll(">", "&gt;")
			);
	}

	/**
	 * Format a date and time string with the user's locale in a long format.
	 * @param c Date and time to format.
	 * @param locale Locale to use.
	 * @return Formatted and HTML-escaped string.
	 */
	public static String datetimestring(Calendar c, Locale locale) {
		return htmlEscape(DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.LONG, locale).format(c.getTime()));
	}

	/**
	 * Format a date and time string with the user's locale in a short format.
	 * @param c Date and time to format.
	 * @param locale Locale to use.
	 * @return Formatted and HTML-escaped string.
	 */
	public static String shortDatetimestring(Calendar c, Locale locale) {
		return htmlEscape(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale).format(c.getTime()));
	}

	/**
	 * Get a string representation of the difference of a given date and time to the present.
	 * @param timestamp Date and time to compare.
	 * @return The time string.
	 */
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
		public final String link, label;
		public final boolean newTab;
		public DropdownEntry(String l, String d, boolean t) {
			link = l;
			label = d;
			newTab = t;
		}
		public DropdownEntry(String l, String d) {
			this(l, d, false);
		}
	}

	public static String createLinkifiedHeader(String tag, String doc, String slug, String text) {
		return	"<a class='anchor' id='" + slug + "'></a><" + tag
			+	"><a href='" + doc + "#" + slug + "' class='linkified_header'>"
			+	text
			+	"</a></" + tag + ">";
	}


	private static String createMenuBarEntry(String uri, DropdownEntry e) {
		String str	=	"<li class='menubar_li'><a ";
		if (Objects.equals(uri, e.link)) str += "class='menubar_active' ";
		str			+=	"href='" + e.link + "'>" + e.label + "<span class='tooltip_bottom'>" + e.label + "</span></a>"
					+	"</li>"
					;
		return str;
	}

	private static String createMenuBarDropdown(String uri, DropdownEntry e, DropdownEntry ... content) {
		String str	=	"<li class='menubar_li menubar_dropdown' onmouseover='dropdownMouse(this, true)' onmouseout='dropdownMouse(this, false)'><a ";
		if (Objects.equals(uri, e.link)) str += "class='menubar_active' ";
		str			+=	"href='" + e.link + "'>" + e.label + "<span class='tooltip_corner'>" + e.label + "</span></a>"
					+	"<div class='menubar_dropdown_content'>"
					;
		for (DropdownEntry d : content) {
			str += "<a ";
			if (Objects.equals(uri, d.link)) str += "class='menubar_active' ";
			if (d.newTab) str += "target='_blank' ";
			str += "href='" + d.link + "'>" + d.label + "<span class='tooltip_left'>" + d.label + "</span></a>";
		}
		str += "</div></li>";
		return str;
	}

	private static String createLatestPost(long postID, String forum, String topic, String user, Calendar timestamp) {
		return	"<div class='latest_post_entry'>"
			+		"<div>[" + forum + "]</div>"
			+		"<div><a href='/forum/post/" + postID + "'>" + topic + "</a></div>"
			+		"<div>by <a href='/user/" + user + "'>" + user + "</a></div>"
			+		"<div>" + timestringSince(timestamp) + "</div>"
			+ 	"</div>"
			;
	}

	private static String createLatestPosts(int howMany) {
		if (howMany <= 0) return "";
		try {
			ResultSet allPosts = sql("select id,topic,user,created from posts order by id desc limit ?", howMany * howMany);
			Set<Long> topicIDs = new HashSet<>();
			String result = "";

			while (allPosts.next() && howMany > 0) {
				long topic = allPosts.getLong("topic");
				if (topicIDs.contains(topic)) continue;

				topicIDs.add(topic);
				--howMany;

				ResultSet sql = sql("select name,forum from topics where id=?", topic);
				sql.next();
				String topicName = sql.getString("name");

				sql = sql("select name from forums where id=?", sql.getLong("forum"));
				sql.next();
				String forumName = sql.getString("name");

				sql = sql("select username from users where id=?", allPosts.getLong("user"));
				sql.next();
				String userName = sql.getString("username");
				result += createLatestPost(allPosts.getLong("id"), htmlEscape(forumName), htmlEscape(topicName),
						htmlEscape(userName), getCalendar(allPosts, "created"));
			}

			return result;
		} catch (SQLException e) {
			return "";
		}
	}

	/**
	 * Generate the form in which a user can create or edit a post.
	 * @param subjectLine Whether to include a "Subject" form field.
	 * @param subjectTitle Title to show above the form's subject field, or null to omit this field.
	 * @param textareaTitle Title to show above the form's text area field, or null to omit this field.
	 * @param content Initial content of the textarea.
	 * @param formaction URL to navigate to when clicking Submit.
	 * @param error Error message to show from a previous failed attempt (may be null).
	 * @return The HTML string.
	 */
	public static String generateForumPostForm(boolean reset, String subjectTitle, String textareaTitle, String content, String formaction, String error) {
		String body = "<a class='anchor' id='post_form'></a><form class='grid new_post_form' method='post' enctype='multipart/form-data'>"
			+	"""
					<script>
						function updatePreview() {
							const input = document.getElementById('content').value;
							var preview = document.getElementById('preview');
							var label = document.getElementById('preview_label');
							if (!input || !input.trim()) {
								preview.style.display = 'none';
								label.style.display = 'none';
								return;
							}

							(async () => {
								const response = await fetch("/render_markdown", {
									method: 'POST',
									body: input,
								});
								const result = await response.text();
								preview.style.display = 'initial';
								label.style.display = 'initial';
								preview.innerHTML = result;
							})()
						}
					</script>
				""";

		int rowOff = 0;

		if (subjectTitle != null) {
			body	+=	"<label class='griditem' style='grid-column:2/span 1; grid-row:" + (1 + rowOff) + "/span 1' for='subject'>"
					+	subjectTitle + "</label><input class='griditem' style='grid-column:1/span 3; grid-row:" + (2 + rowOff)
					+	"/span 1' type='text' id='subject' required name='subject' autofocus"
					;

			if (textareaTitle == null) body += " value='" + htmlEscape(content) + "'";

			body += ">";

			rowOff += 2;
		}

		if (textareaTitle != null) {
			body	+=	"<label class='griditem' style='grid-column:2/span 1; grid-row:" + (1 + rowOff)
					+	"/span 1' for='content'>" + textareaTitle + "</label>"
					+	"<textarea class='griditem' style='grid-column:1/span 3; grid-row:" + (2 + rowOff) + "/span 1; resize:vertical'"
					+			"id='content' rows=8 required name='content'>" + content + "</textarea>"
					+	"<input class='griditem form_button' style='grid-column:1/span 1; grid-row:" + (3 + rowOff) + "/span 1'"
					+			"type='button' onclick='updatePreview()' value='Preview'>"
					;
		}

		if (reset) {
			body	+=	"<input class='griditem form_button' style='grid-column:2/span 1; grid-row:" + (3 + rowOff) + "/span 1'"
					+		"type='reset' value='Reset'>";
		}

		body	+=	"<input class='griditem form_button form_default_action' style='grid-column:3/span 1; grid-row:" + (3 + rowOff) + "/span 1'"
				+		"type='submit' value='Submit' formaction='" + formaction + "'>"
				;

		if (textareaTitle != null) rowOff += 2;

		if (error != null) {
			body += "<label class='griditem form_error form_error_caption' style='grid-column:1/span 3; grid-row:" + (4 + rowOff) + "/span 1'>";
			switch (error.toLowerCase()) {
				case "empty_post":
					body += "This post is empty.";
					break;
				case "empty_title":
					body += "Please add a topic title.";
					break;
				case "restricted":
					body += "You may edit your posts only within 24 hours after posting and not if a moderator has previously edited your post.";
					break;
				default:
					body += "An unknown error has occurred.";
					break;
			}
			body += "</label>";

			++rowOff;
		}

		if (textareaTitle != null) {
			body	+=	"<label class='griditem' id='preview_label' style='display:none;grid-column:2/span 1; grid-row:"
					+		(3 + rowOff) + "/span 1' for='preview'>Preview</label>"
					+	"<div class='griditem forum_post_body forum_list_entry' style='display:none;grid-column:1/span 3; grid-row:"
					+		(4 + rowOff) + "/span 1' id='preview'></div>"
					+	"</form>"
					;
		}

		return body;
	}

	/**
	 * Generate the complete HTML webpage for a request.
	 * @param request Current web request.
	 * @param pagename Title for the page.
	 * @param body Main content of the page.
	 * @return Complete HTML-formatted webpage.
	 */
	public static String generatePage(WebRequest request, String pagename, String body) {
		final String uri = uri(request);
		String result =
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

			+		createMenuBarEntry   (uri, new DropdownEntry("/"           , "FreeRCT Home"))
			+		createMenuBarEntry   (uri, new DropdownEntry("/screenshots", "Screenshots" ))
			+		createMenuBarEntry   (uri, new DropdownEntry("/download"   , "Get It!"     ))
			+		createMenuBarEntry   (uri, new DropdownEntry("/manual"     , "Manual"      ))
			;

		List<DropdownEntry> allForums = new ArrayList<>();
		try {
			ResultSet sql = sql("select id,name from forums");
			while (sql.next()) {
				allForums.add(new DropdownEntry("/forum/" + sql.getLong("id"), sql.getString("name")));
			}
		} catch (SQLException e) {
			allForums.clear();
		}
		result += createMenuBarDropdown(uri, new DropdownEntry("/forum", "Forums"), allForums.toArray(new DropdownEntry[0]));

		result
			+=		createMenuBarDropdown(uri, new DropdownEntry("/contribute"                              , "Contribute"          ),
					                           new DropdownEntry("https://github.com/FreeRCT/FreeRCT"       , "Git Repository", true),
					                           new DropdownEntry("https://github.com/FreeRCT/FreeRCT/issues", "Issue Tracker" , true))
			+		createMenuBarEntry   (uri, new DropdownEntry("/news"                                    , "News Archive"        ))
			;

		result
			+=		"</ul>"
			+		"<p id='menubar_spacer_bottom'></p>"

			+		"<div class='toplevel_content_flexbox'>"
			+			"<div class='content_flexbox_content'>" + body + "</div>"
			+			"<div class='content_flexbox_content main_right_column'>"
			;

		if (request.getUserPrincipal() != null) {
			result
				+=	"<div class='right_column_box'>"
				+		"<div class='right_column_login'>"
				+			"Logged in as <a href='/user/" + request.getRemoteUser() + "'>" + request.getRemoteUser()
				+		"</a></div>"
				+		"<div class='right_column_login'><a href='/logout'>Log Out</a></div>"
				+	"</div>"
				;
		} else {
			result += "<div class='right_column_box right_column_login'><a href='/login?next=" + uri + "'>Log In</a> / <a href='/signup'>Register</a></div>";
		}

		SortedSet<String> allLoggedInUsers = SecurityManager.getLoggedInUsers();
		if (!allLoggedInUsers.isEmpty()) {
			result += "<div class='right_column_box logged_in_users'>";
			result += "<h2>Currently Online</h2>";
			for (String user : allLoggedInUsers) {
				result += "<div class='right_column_login'><a href='/user/" + user + "'>" + user + "</a></div>";
			}
			result += "</div>";
		}

		result
			+=				"<div class='right_column_box'>"
			+					"<h1>Latest Posts</h1>" + createLatestPosts(LATEST_POSTS_DEFAULT_COUNT)
			+				"</div>"
			+			"</div>"
			+		"</div>"

			+		"<p id='footer_spacer'></p><footer>"
			+			"<div>© 2021-" + Calendar.getInstance().get(Calendar.YEAR) + " by the FreeRCT Development Team</div>"
			+			"<div><a href='/contact'>Legal Notice / Contact</a></div>"
			+		"</footer>"
			+		"<script>readjustMenuBarY();</script>"

			+	"</body>"
			+ "</html>"
			;
		return result;
	}
}
