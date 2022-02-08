package freerct.freerct;

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

@Controller
public class Mainpage {
	private static class SlideshowSlide {
		public final String image, caption;
		private SlideshowSlide(String i, String c) {
			image = i;
			caption = c;
		}

		public static final SlideshowSlide[] ALL_SLIDES = new SlideshowSlide[] {
			new SlideshowSlide("mainview.png", "FreeRCT aims to be a free and open source game …"),
			new SlideshowSlide("mainmenu.png", "… which captures the look, feel and gameplay of the popular games RollerCoaster Tycoon 1 and 2."),
			new SlideshowSlide( "newpark.png", "The game is still in an early alpha state, …"),
			new SlideshowSlide( "persons.png", "… but it is already playable and offers a variety of features."),
		};
	}

	@GetMapping("/")
	@ResponseBody
	public String fetch(WebRequest request) {
		String body = """

			<script>
		"""
		+ "const NR_SLIDES = " + SlideshowSlide.ALL_SLIDES.length + ";"
		+ """
				var slideIndex = 0;
				var timeoutVar = null;

				function plusSlides(n) {
					showSlides(slideIndex += n);
				}

				function currentSlide(n) {
					showSlides(slideIndex = n);
				}

				function showSlidesAuto() {
					plusSlides(1);
				}

				function showSlides(n) {
					var i;
					var images = document.getElementsByClassName("slideshow_image");
					var texts = document.getElementsByClassName("slideshow_text");
					var dots = document.getElementsByClassName("slideshow_dot");
					if (n > NR_SLIDES) slideIndex = 1;
					if (n < 1) slideIndex = NR_SLIDES;

					for (i = 0; i < NR_SLIDES; i++) {
						images[i].style.opacity = (i + 1 == slideIndex) ? '1' : '0';
						texts[i].style.opacity = (i + 1 == slideIndex) ? '1' : '0';
					}
					for (i = 0; i < NR_SLIDES; i++) dots[i].className = dots[i].className.replace(" slideshow_dot_active", "");
					dots[slideIndex-1].className += " slideshow_dot_active";

					if (timeoutVar) clearTimeout(timeoutVar);
					timeoutVar = [setTimeout(showSlidesAuto, 5000)];
				}
			</script>

			<div class="content_flexbox">
				<div class="content_flexbox_content">

					<h1>Welcome to the FreeRCT Homepage!</h1>

					<p>
						FreeRCT aims to be a free and open source game which captures the look, feel, and gameplay of the
						popular games RollerCoaster Tycoon 1 and 2.
						The game is still in an early alpha state, but it is already playable and offers a variety of features.
					</p>

					<br><h2><a href="/news" class="linkified_header">Latest News</a></h2>
		"""
		+ News.printLatestNews(request.getLocale(), 3, 0)
		+ """

				</div>

				<div class="content_flexbox_content slideshow_main">
					<div class="slideshow_container">
		""";

		for (SlideshowSlide slide : SlideshowSlide.ALL_SLIDES) {
			body += "<div class='slideshow_slide";
			if (slide == SlideshowSlide.ALL_SLIDES[0]) body += " slideshow_first_slide";
			body += "'><img class='slideshow_image' src='/img/screenshots/mainpage_slideshow/" + slide.image + "'></img>";
			body += "<div class='slideshow_text'><p>" + renderMarkdown(slide.caption) + "</p></div>";
			body += "</div>";
		}

		body += """
					<a class="slideshow_prev" onclick="plusSlides(-1)">&#10094;</a>
					<a class="slideshow_next" onclick="plusSlides(1)">&#10095;</a>
					</div><br><div style="text-align:center; margin-top:24px">
		""";

		for (int i = 1; i <= SlideshowSlide.ALL_SLIDES.length; ++i) {
			body += "<span class='slideshow_dot' onclick='currentSlide(" + i + ")'></span>";
		}

		body += """
					</div>  <!-- slideshow_container -->
				</div>  <!-- slideshow_main -->

			</div>  <!-- content_flexbox -->

			<script>showSlidesAuto();</script>
		""";

		return generatePage(request, "Home", body);
	}
}
