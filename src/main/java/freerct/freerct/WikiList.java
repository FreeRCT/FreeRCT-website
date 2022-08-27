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

/** The page that shows all wiki articles or revisions. */
@Controller
public class WikiList {
	private static class ArticleInfo {
		public final long id;
		public final String slug;
		public final String title;
		public final Calendar updated;
		public ArticleInfo(long i, String s, String t, Calendar u) {
			id = i;
			slug = s;
			title = t;
			updated = u;
		}
	}

	private static final String kSortABC = "name-asc";
	private static final String kSortZYX = "name-desc";
	private static final String kSortNewest = "newest";
	private static final String kSortOldest = "oldest";

	private static String sortOrderName(String sort) {
		switch (sort) {
			case kSortNewest:
				return "Most recently updated";
			case kSortOldest:
				return "Least recently updated";
			case kSortZYX:
				return "Alphabetically (reversed)";
			default:
				return "Alphabetically";
		}
	}

	@GetMapping("/wiki/list")
	@ResponseBody
	public String getArticle(WebRequest request, HttpSession session, @RequestParam(value="sort", defaultValue="") String sortBy) {
		try {
			ResultSet sql = sql("select * from wiki_articles");
			List<ArticleInfo> all = new ArrayList<>();
			while (sql.next()) {
				long id = sql.getLong("id");
				ResultSet a = sql("select title,created from wiki_revisions where article=? order by id desc limit 1", id);
				if (a.next()) all.add(new ArticleInfo(id, sql.getString("slug"), a.getString("title"), getCalendar(a, "created")));
			}

			switch (sortBy) {
				case kSortNewest:
					all.sort((a, b) -> -a.updated.compareTo(b.updated));
					break;
				case kSortOldest:
					all.sort((a, b) -> a.updated.compareTo(b.updated));
					break;
				case kSortZYX:
					all.sort((a, b) -> -a.title.compareTo(b.title));
					break;
				default:
					all.sort((a, b) -> a.title.compareTo(b.title));
					break;
			}

			String body	=	"<h1>Wiki</h1>"
						+	"<div class='forum_header_grid_toplevel'>"
						+		"<div class='griditem forum_header_grid_side_column_l'>"
						+			"<div class='content_dropdown form_button'>Sort by: " + sortOrderName(sortBy)
						+				"<div class='content_dropdown_content' style='padding:0'>"
						+					"<a href='?sort=" + kSortABC    + "'>" + sortOrderName(kSortABC   ) + "</a>"
						+					"<a href='?sort=" + kSortZYX    + "'>" + sortOrderName(kSortZYX   ) + "</a>"
						+					"<a href='?sort=" + kSortNewest + "'>" + sortOrderName(kSortNewest) + "</a>"
						+					"<a href='?sort=" + kSortOldest + "'>" + sortOrderName(kSortOldest) + "</a>"
						+				"</div>"
						+			"</div>"
						+		"</div>"
						+		"<div class='griditem forum_header_grid_middle_column'>"
						+			"<p class='forum_description_stats'>"
						+				pluralForm(all.size(), "article", "articles")
						+			"</p>"
						+		"</div>"
						+		"<div class='griditem forum_header_grid_side_column_r'>"
						+			"<a class='form_button' href='/wiki/new'>New Article</a>"
						+		"</div>"
						+	"</div>"
						;

			for (ArticleInfo a : all) {
				body	+=	"<div class='forum_list_entry'>"
						+		"<div>"
						+			"<div class='forum_list_header'><a href='/wiki/w/" + a.slug + "'>" + a.title + "</a></div>"
						+		"</div>"
						+		"<div class='forum_list_right_column'>"
						+			"<div>Last updated on "
						+				datetimestring(a.updated, request.getLocale())
						+			"</div>"
						+		"</div>"
						+	"</div>"
						;
			}

			return generatePage(request, session, "Wiki", body);
		} catch (Exception e) {
			return new ErrorHandler().error(request, session, null);
		}
	}
}
