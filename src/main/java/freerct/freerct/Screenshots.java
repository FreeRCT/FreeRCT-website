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

/** The screenshots page. */
@Controller
public class Screenshots {
	@GetMapping("/screenshots")
	@ResponseBody
	public String fetch(WebRequest request, HttpSession session) {
		return generatePage(request, session, "Screenshots", """
			<h1>Screenshots</h1>
			<script>
				const ALL_SCREENSHOT_SECTIONS = [
					{slug: '0_1', label: 'FreeRCT 0.1'},
					{slug: 'test', label: 'Test Images <emp>(to be deleted)</emp>'},  // TODO delete
				];
				const ALL_IMAGES = [
					{section: '0_1', image: 'mainpage_slideshow/mainview'},
					{section: '0_1', image: 'mainpage_slideshow/mainmenu'},
					{section: '0_1', image: 'mainpage_slideshow/newpark' },
					{section: '0_1', image: 'mainpage_slideshow/persons' },

					{section: 'test', image: 'test_TO_BE_DELETED/20150609-freerct'     },
					{section: 'test', image: 'test_TO_BE_DELETED/20121209-freerct'     },
					{section: 'test', image: 'test_TO_BE_DELETED/crowded'              },
					{section: 'test', image: 'test_TO_BE_DELETED/ice_creams_recoloured'},
					{section: 'test', image: 'test_TO_BE_DELETED/weather'              },
				];

				// Gallery code below.

				var currentGalleryPopup = null;

				function createScreenshotGallery() {
					ALL_SCREENSHOT_SECTIONS.forEach((section) => {
						document.write("<a class='anchor' id='" + section.slug + "'></a>");
						document.write("<h2><a href='/screenshots#" + section.slug + "' class='linkified_header'>");
						document.write(section.label);
						document.write("</a></h2>");

						document.write('<div class="screenshot_gallery">');

						var all_images_in_section = [];
						ALL_IMAGES.forEach((img) => { if (img.section == section.slug) all_images_in_section.push(img); });

						for (var i = 0; i < all_images_in_section.length; i++) {
							const img = all_images_in_section[i];
							document.write('<img class="screenshot_gallery_image" loading=lazy src="/img/screenshots/' + img.image +
										   '.png" height="auto" width="auto" onclick="screenshot_image_clicked(event.currentTarget)"></img>');
							document.write('<div class="screenshot_gallery_popup_outer_wrapper"><div class="screenshot_gallery_popup_inner_wrapper">');
								document.write('<div class="screenshot_gallery_popup_prev" onclick="gallery_next(-1)">&#10094;</div>');
								document.write('<div class="screenshot_gallery_popup_next" onclick="gallery_next(+1)">&#10095;</div>');
								document.write('<div class="screenshot_gallery_popup_close" onclick="hide_screenshot_images()">&#128937;</div>');
								document.write('<img class="screenshot_gallery_popup_image" src="/img/screenshots/' + img.image +
											   '.png" height="auto" width="auto" onclick="event.stopPropagation()"></img>');
							document.write('</div></div>');
						}
						document.write('</div>');
					});
					hide_screenshot_images();
				}

				function screenshot_image_clicked(element) {
					hide_screenshot_images();

					while (element.className != 'screenshot_gallery_image' && element.className != 'screenshot_gallery_popup_outer_wrapper') {
						element = element.parentNode;
						if (element == null) return;
					}
					if (element.className == 'screenshot_gallery_image') element = element.nextSibling;
					element.style.display = 'initial';
					currentGalleryPopup = element;

					event.stopPropagation();
				}

				function gallery_next(delta) {
					var element = currentGalleryPopup;
					while (element.className != 'screenshot_gallery') {
						element = element.parentNode;
						if (!element) return;
					}

					var all = element.getElementsByClassName(currentGalleryPopup.className);
					for (var i = 0; i < all.length; i++) {
						if (all[i] == currentGalleryPopup) {
							i += delta;
							while (i < 0) i += all.length;
							i %= all.length;
							screenshot_image_clicked(all[i]);
							return;
						}
					}
				}

				function gallery_key_event() {
					if (currentGalleryPopup != null) {
						if (event.key == 'Escape') {
							hide_screenshot_images();
						} else if (event.key == 'ArrowLeft') {
							gallery_next(-1);
						} else if (event.key == 'ArrowRight') {
							gallery_next(+1);
						}
					}
				}

				function hide_screenshot_images() {
					var items = document.getElementsByClassName("screenshot_gallery_popup_outer_wrapper");
					for (var i = 0; i < items.length; i++) {
						items[i].style.display = 'none';
					}
					currentGalleryPopup = null;
				}

				document.body.onclick = hide_screenshot_images;
				document.body.onkeydown = gallery_key_event;

				createScreenshotGallery()
			</script>
		""");
	}
}
