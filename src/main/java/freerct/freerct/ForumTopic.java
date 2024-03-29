package freerct.freerct;

import java.io.*;
import java.sql.*;
import java.util.*;

import javax.servlet.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;

import static freerct.freerct.FreeRCTApplication.generatePage;
import static freerct.freerct.FreeRCTApplication.sql;
import static freerct.freerct.FreeRCTApplication.sqlSync;
import static freerct.freerct.FreeRCTApplication.getCalendar;
import static freerct.freerct.FreeRCTApplication.htmlEscape;
import static freerct.freerct.FreeRCTApplication.htmlEscapeNonASCII;
import static freerct.freerct.FreeRCTApplication.renderMarkdown;
import static freerct.freerct.FreeRCTApplication.datetimestring;
import static freerct.freerct.FreeRCTApplication.shortDatetimestring;
import static freerct.freerct.FreeRCTApplication.pluralForm;
import static freerct.freerct.FreeRCTApplication.createLinkifiedHeader;
import static freerct.freerct.FreeRCTApplication.generateForumPostForm;

/** The page that displays a single forum topic. */
@Controller
public class ForumTopic {
	@PostMapping("/render_markdown")
	@ResponseBody
	public String getMarkdownPreview(@RequestBody String content) {
		return renderMarkdown(content);
	}

	private static class Post {
		public final long id, authorID, editorID;
		public final String author, editor, body;
		public final Calendar created, edited;
		public Post(long i, long a, String auth, long e, String edit, Calendar timeFirst, Calendar timeLast, String b) {
			id = i;
			authorID = a;
			editorID = e;
			author = auth;
			editor = edit;
			created = timeFirst;
			edited = timeLast;
			body = b;
		}
	}

	@GetMapping("/forum/post/{postID}")
	public String permalink(WebRequest request, @PathVariable long postID) {
		try {
			ResultSet sql = sql("select topic from posts where id=?", postID);
			sql.next();
			return "redirect:/forum/topic/" + sql.getLong("topic") + "#post_" + postID;
		} catch (Exception e) {
			return "redirect:/error?reason=internal_server_error";
		}
	}

	@GetMapping("/forum/topic/{topicID}")
	@ResponseBody
	public String fetch(WebRequest request, HttpSession session, @PathVariable long topicID, @RequestParam(value="error", required=false) String error) {
		try {
			sql("update topics set views=views+1 where id=?", topicID);
			ResultSet sql = sql("select name,forum,views from topics where id=?", topicID);
			sql.next();
			final String topicName = sql.getString("name");
			final long forumID = sql.getLong("forum");
			final long nrViews = sql.getLong("views");

			sql = sql("select name from forums where id=?", forumID);
			sql.next();
			final String forumName = sql.getString("name");

			Boolean subscribed = null;
			if (request.getUserPrincipal() != null) {
				sql = sql("select id from users where username=?", request.getRemoteUser());
				sql.next();
				sql = sql("select * from subscriptions where topic=? and user=?", topicID, sql.getLong("id"));
				subscribed = sql.next();
			}

			List<Post> allPosts = new ArrayList<>();
			sql = sql("select id,user,editor,created,edited,body from posts where topic=? order by id asc", topicID);
			while (sql.next()) {
				ResultSet author = sql("select username from users where id=?", sql.getLong("user"));
				author.next();
				String authorName = author.getString("username");

				author = sql("select username from users where id=?", sql.getLong("editor"));
				String editorName = author.next() ? author.getString("username") : null;

				Calendar calendarFirst = getCalendar(sql, "created");
				Calendar calendarLast = getCalendar(sql, "edited");
				allPosts.add(new Post(sql.getLong("id"), sql.getLong("user"), htmlEscape(authorName), sql.getLong("editor"), htmlEscape(editorName),
						calendarFirst, calendarLast, sql.getString("body")));
			}

			String body = """
							<script>
								function quotePost(content) {
									var textarea = document.getElementById('content');
									const selStart = textarea.selectionStart;
									const selEnd = textarea.selectionEnd;
									const text = textarea.value;

									var newText;
									if (selStart > 0) {
										newText = text.substring(0, selStart);
										newText += "\\n\\n";
									} else {
										newText = "";
									}

									newText += content;
									newText += "\\n\\n";
									newText += text.substring(selEnd);

									textarea.value = newText;

									location.href = "#content";  // Jump down to textarea
								}
							</script>
						"""
						+	"<h1>Topic: " + htmlEscape(topicName) + "</h1>"
						+	"<div class='forum_header_grid_toplevel'>"
						+		"<div class='griditem forum_header_grid_side_column_l'>"
						+			"<a class='form_button' href='/forum/" + forumID + "'>Back</a>"
						+		"</div>"
						+		"<div class='griditem forum_header_grid_middle_column'>"
						+			"<p class='forum_description_name'>Forum: <a href='/forum/" + forumID + "'>" + htmlEscape(forumName) + "</a></p>"
						+			"<p class='forum_description_stats'>" + pluralForm(allPosts.size(), "post", "posts")
						+					" · " + pluralForm(nrViews, "view", "views") + "</p>"
						+		"</div>"
						+		"<div class='griditem forum_header_grid_side_column_r'>"
						+			(SecurityManager.isModerator(request) ? (
										"<a class='form_button' href='/forum/topic/rename/" + topicID + "'>Rename</a>"
						+				"<a class='form_button' href='/forum/topic/delete/" + topicID + "'>Delete</a>"
									) : "")
						+			(subscribed == null ? "" : subscribed ?
										("<a class='form_button' href='/forum/topic/unsubscribe/" + topicID + "'>Unsubscribe</a>") :
										("<a class='form_button' href='/forum/topic/subscribe/"   + topicID + "'>Subscribe</a>"))
						+		"</div>"
						+	"</div>"
						;

			for (Post p : allPosts) {
				ResultSet postCounter = sql("select count(user) as nr from posts where user=?", p.authorID);
				postCounter.next();
				ResultSet userDetails = sql("select joined,state from users where id=?", p.authorID);
				userDetails.next();

				body	+=	"<a class='anchor' id='post_" + p.id + "'></a><div class='forum_list_entry'>"
						+		"<div class='forum_post_usercolumn'>"
						+			"<div><a href='/user/" + p.author + "'>" + p.author + "</a></div>"
						;

				if (new File(Resources.RESOURCES_DIR, "img/users/" + p.author + ".png").isFile()) {
					body	+=	"<div><a class='linkified_user_profile_image' href='/user/" + p.author
							+	"' style='background-image: url(\"/img/users/" + p.author + ".png\")'></a></div>";
				}

				switch (userDetails.getInt("state")) {
					case SecurityManager.USER_STATE_ADMIN:
						body += "<div class='forum_post_userdetails'><b>Administrator</b></div>";
						break;
					case SecurityManager.USER_STATE_MODERATOR:
						body += "<div class='forum_post_userdetails'><b>Moderator</b></div>";
						break;
					default:
						break;
				}

				body	+=			"<div class='forum_post_userdetails'>Posts: <b>" + postCounter.getLong("nr") + "</b></div>"
						+			"<div class='forum_post_userdetails'>Joined: "
						+				shortDatetimestring(getCalendar(userDetails, "joined"), request.getLocale())
						+			"</div>"
						+		"</div>"
						+		"<div class='forum_post_wrapper'>"
						+			"<div class='forum_post_meta'>"
						+				"<div>"
						+					datetimestring(p.created, request.getLocale())
						+				"</div>"
						;

				if (p.edited != null) {
					body += "<div>";
					if (p.authorID == p.editorID) {
						body += "Edited on ";
					} else {
						body += "Edited by <a href='/user/" + p.editor + "'>" + p.editor + "</a> on ";
					}
					body += datetimestring(p.edited, request.getLocale()) + "</div>";
				}

				body += "</div><div class='forum_post_body'>" + renderMarkdown(p.body) + "</div>";

				final boolean mayQuote = request.getUserPrincipal() != null;
				final boolean mayEdit = SecurityManager.mayEditPost(request, p.id);
				final boolean mayDelete = SecurityManager.mayDeletePost(request, p.id);
				if (mayQuote || mayEdit || mayDelete) {
					body += "<div class='forum_header_grid_side_column_l forum_post_buttons_wrapper'>";

					if (mayQuote) {
						/* Those are Java regexes, so "\\\\" represents a single backslash.
						 * Conveniently, we can escape problematic sequences by replacing them
						 * with a backslash followed by 'x' and the ASCII code, and JavaScript
						 * will take care of replacing them back to the correct characters.
						 */
						String quotingFunctionCall =
								("> *" + p.author + " wrote:*\n\n---\n" + htmlEscapeNonASCII(p.body.trim()))
								.replaceAll("\\\\", "\\\\x5c")
								.replaceAll("'"   , "\\\\x27")
								.replaceAll("\""  , "\\\\x22")
								.replaceAll("\r"  , "\\\\x0d")
								.replaceAll("\n"  , "\\\\x0a> ")  // Here we add the "> " that indicates the block quote.
								;

						body	+=	"<div class='form_button' onclick=\"quotePost('" + quotingFunctionCall
								+		"')\" id='quote_button_" + p.id + "'><abbr title='JavaScript needs to be enabled to use this feature.' style='color:darkgrey'>"
								+	"Quote</abbr></div>"
								+	"<script>document.getElementById('quote_button_" + p.id + "').innerHTML = 'Quote'</script>"
								;
					}

					if (mayEdit  ) body += "<a class='form_button' href='/forum/post/edit/" + p.id + "'>Edit</a>";
					if (mayDelete) body += "<a class='form_button' href='/forum/post/delete/" + p.id + "'>Delete</a>";

					body += "</div>";
				}

				body += "</div></div>";
			}

			if (request.getUserPrincipal() != null) {
				body += generateForumPostForm(false, null, "New Post", "", "/forum/topic/" + topicID + "/submit_new", error, false);
			}

			return generatePage(request, session, "Forum | " + forumName + " | " + topicName, body);
		} catch (Exception e) {
			return new ErrorHandler().error(request, session, "internal_server_error");
		}
	}

	@PostMapping("/forum/topic/{topicID}/submit_new")
	public String createPost(WebRequest request, HttpSession session,
			@PathVariable long topicID,
			@RequestPart("content") String content) {
		try {
			session.setAttribute("freerct-new-post-content", content);

			content = content.trim();
			if (content.isEmpty()) return "redirect:/forum/topic/" + topicID + "?error=empty_post#post_form";

			ResultSet sql = sql("select id from users where username=?", request.getRemoteUser());
			sql.next();
			final long userID = sql.getLong("id");

			synchronized (sqlSync()) {
				sql("insert into posts (topic,user,body) value(?,?,?)", topicID, userID, content);
				sql = sql("select last_insert_id() as new_id");
			}
			sql.next();
			final long postID = sql.getLong("new_id");

			sql = sql("select name from topics where id=?", topicID);
			sql.next();
			final String topicName = sql.getString("name");

			Subscriptions.sendMailsToTopicSubscribers(userID, topicID, "forum_new_post", "Forum New Post",
					"A new post was added to the topic \"" + topicName + "\" by " + request.getRemoteUser() + ":\n\n"
							+ content
			    			+ "\n\n-------------------------\n"
			    			+ "Link to post: https://freerct.net/forum/post/" + postID
	    			);

			session.removeAttribute("freerct-new-post-content");
			return "redirect:/forum/post/" + postID;
		} catch (Exception e) {
			return "redirect:/error?reason=internal_server_error";
		}
	}
}
