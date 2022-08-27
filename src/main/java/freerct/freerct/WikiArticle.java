package freerct.freerct;

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
import static freerct.freerct.FreeRCTApplication.renderMarkdown;
import static freerct.freerct.FreeRCTApplication.datetimestring;
import static freerct.freerct.FreeRCTApplication.shortDatetimestring;
import static freerct.freerct.FreeRCTApplication.pluralForm;
import static freerct.freerct.FreeRCTApplication.createLinkifiedHeader;
import static freerct.freerct.FreeRCTApplication.generateForumPostForm;

/** The page that shows a static wiki article or one of its revisions. */
@Controller
public class WikiArticle {
	@GetMapping("/wiki/w/{wikiSlug}")
	@ResponseBody
	public String getArticle(WebRequest request, HttpSession session, @PathVariable String wikiSlug) {
		try {
			ResultSet sql = sql("select id from wiki_articles where slug=?", wikiSlug);
			sql.next();
			sql = sql("select id,title from wiki_revisions where article=? order by id desc limit 1", sql.getLong("id"));
			sql.next();
			return doGet(request, session, sql.getLong("id"), sql.getString("title"));
		} catch (Exception e) {
			return new ErrorHandler().error(request, session, null);
		}
	}

	@GetMapping("/wiki/rev/{revID}")
	@ResponseBody
	public String getRevision(WebRequest request, HttpSession session, @PathVariable long revID) {
		try {
			ResultSet sql = sql("select title from wiki_revisions where id=?", revID);
			sql.next();
			return doGet(request, session, revID, sql.getString("title"));
		} catch (Exception e) {
			return new ErrorHandler().error(request, session, null);
		}
	}

	private String doGet(WebRequest request, HttpSession session, long revID, String pageTitle) {
		try {
			ResultSet sql = sql("select article,title,content,created from wiki_revisions where id=?", revID);
			sql.next();
			final long articleID = sql.getLong("article");
			final String articleTitle = sql.getString("title");
			final String articleBody = sql.getString("content");

			final boolean isNewest = !sql("select id from wiki_revisions where article=? and id>? limit 1", articleID, revID).next();

			sql = sql("select slug from wiki_articles where id=?", articleID);
			sql.next();
			final String articleSlug = sql.getString("slug");

			String body	=	"<h1>Article: " + htmlEscape(articleTitle) + "</h1>"
						+	"<div class='forum_header_grid_toplevel'>"
						+		"<div class='griditem forum_header_grid_side_column_l'>"
						+			"<a class='form_button' href='/wiki/history/" + articleSlug + "'>History</a>"
						+		"</div>"
						;

			if (isNewest && articleTitle.equals(pageTitle)) {
				body	+=	"<div class='griditem forum_header_grid_middle_column'>"
						+		"<p class='forum_description_name'><a href='/wiki/w/" + articleSlug + "'>"
						+			"You are viewing an outdated version of this article. Click here to view the latest version.</a></p>"
						+	"</div>"
				 		+	"<div class='griditem forum_header_grid_side_column_r'>"
						+	"</div>"
						;
			} else {
				body	+=	"<div class='griditem forum_header_grid_middle_column'>"
						+	"</div>"
				 		+	"<div class='griditem forum_header_grid_side_column_r'>"
						+		"<a class='form_button' href='/wiki/edit/" + articleSlug + "'>Edit</a>"
						+	"</div>"
						;
			}

			body	+=	"</div>"
					+	renderMarkdown(articleBody)
					;

			return generatePage(request, session, "Wiki | " + pageTitle, body);
		} catch (Exception e) {
			return new ErrorHandler().error(request, session, "internal_server_error");
		}
	}
}
