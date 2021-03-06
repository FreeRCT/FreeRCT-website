package freerct.freerct;

import java.io.*;
import java.text.*;
import java.util.*;
import org.json.simple.parser.*;

import javax.servlet.http.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;

import static freerct.freerct.FreeRCTApplication.generatePage;
import static freerct.freerct.FreeRCTApplication.bash;
import static freerct.freerct.FreeRCTApplication.config;
import static freerct.freerct.FreeRCTApplication.sql;
import static freerct.freerct.FreeRCTApplication.getCalendar;
import static freerct.freerct.FreeRCTApplication.htmlEscape;
import static freerct.freerct.FreeRCTApplication.renderMarkdown;
import static freerct.freerct.FreeRCTApplication.datestring;
import static freerct.freerct.FreeRCTApplication.datetimestring;
import static freerct.freerct.FreeRCTApplication.shortDatetimestring;
import static freerct.freerct.FreeRCTApplication.createLinkifiedHeader;
import static freerct.freerct.FreeRCTApplication.doDelete;

/** The Download page. */
@Controller
public class Download {
	public static final File DAILY_BUILD_DIR = new File(Resources.RESOURCES_DIR, "public/daily_builds");

	@Scheduled(cron = "0 0 3 * * *")
	public void updateDailyBuilds() {
		try {
			ContainerFactory cf = new ContainerFactory() {
				public List creatArrayContainer() { return new LinkedList(); }
				public Map createObjectContainer() { return new LinkedHashMap(); }
			};

			JSONParser parser = new JSONParser();
			Map json = (Map)parser.parse(
				bash("curl", "-u", config("githublogin"),
					"https://api.github.com/repos/freerct/freerct/actions/runs?branch=master"
				), cf);

			Map newestWorkflow = null;
			Date newestDate = null;
			for (Object key : (List)json.get("workflow_runs")) if (key instanceof Map m) {
				if (!m.get("name").toString().equals("CI") ||
					!m.get("event").toString().equals("push") ||
					!m.get("status").toString().equals("completed") ||
					!m.get("conclusion").toString().equals("success") ||
					!m.get("head_branch").toString().equals("master")) continue;
				Date d = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(m.get("created_at").toString());
				if (newestDate == null || newestDate.before(d)) {
					newestDate = d;
					newestWorkflow = m;
				}
			}

			json = (Map)parser.parse(bash("curl", "-u", config("githublogin"), newestWorkflow.get("artifacts_url").toString()), cf);

			for (Object key : (List)json.get("artifacts")) if (key instanceof Map m) {
				String name = m.get("name").toString().trim().replaceAll("[\\s()]+", "_");
				File dir = new File(DAILY_BUILD_DIR, newestWorkflow.get("id").toString());
				dir = new File(dir, name);
				dir.mkdirs();
				File zipfile = new File(dir, name + ".zip");
				bash("curl", "-L", "-o", zipfile.getAbsolutePath(), "-u", config("githublogin"), m.get("archive_download_url").toString());
				bash("unzip", zipfile.getAbsolutePath(), "-d", dir.getAbsolutePath());
				zipfile.delete();
				bash("bash", "-c", "cd \"" + dir.getAbsolutePath() + "\" && sha256sum -c *.sha256");
			}

			File[] oldBuilds = DAILY_BUILD_DIR.listFiles();
			Arrays.sort(oldBuilds, (a, b) -> {
				Long la = Long.valueOf(a.getName());
				Long lb = Long.valueOf(b.getName());
				return la == lb ? 0 : la < lb ? 1 : -1;
			});
			for (int i = 2; i < oldBuilds.length; ++i) doDelete(oldBuilds[i]);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String createAssetDownloadLink(WebRequest request, boolean linebreak, File file, String text) {
		String path = file.getAbsolutePath().replaceFirst(Resources.RESOURCES_DIR.getAbsolutePath(), "");
		String filesizeString = "This link seems to be broken.";
		long l = file.length();
		if (l > 1000 * 1000 * 1000) {
			filesizeString = String.format(request.getLocale(), "%.2f GB", (l / (1000f * 1000f * 1000f)));
		} else if (l > 1000 * 1000) {
			filesizeString = String.format(request.getLocale(), "%.2f MB", (l / (1000f * 1000f)));
		} else if (l > 1000) {
			filesizeString = String.format(request.getLocale(), "%.2f kB", (l / 1000f));
		} else {
			filesizeString = String.format(request.getLocale(), "%d bytes", l);
		}
		return "<strong><a href='" + path + "'>" + text + (linebreak ? "<br>" : " ") + "(" + filesizeString + ")</a></strong>";
	}

	private String createChecksumDownloadLink(File file) {
		String path = file.getAbsolutePath().replaceFirst(Resources.RESOURCES_DIR.getAbsolutePath(), "");
		String shaString = "This file seems to be corrupted.";
		try {
			BufferedReader r = new BufferedReader(new FileReader(file));
			shaString = r.readLine().trim().split("\\s")[0];
		} catch (Exception e) {}
		return "<abbr title='" + shaString + "'><a href='" + path + "'>SHA256</a></abbr>";
	}

	@GetMapping("/download")
	@ResponseBody
	public String fetch(WebRequest request, HttpSession session) {
		File latestDaily = null;
		File[] oldBuilds = DAILY_BUILD_DIR.listFiles();
		if (oldBuilds != null) {
			for (File f : oldBuilds) {
				if (latestDaily == null || Long.valueOf(f.getName()) > Long.valueOf(latestDaily.getName())) {
					latestDaily = f;
				}
			}
		}

		String body = """
			<h1>Get It!</h1>

			"""
			+ createLinkifiedHeader("h2", "/download", "releases", "Releases")
			+ """
			<p style="text-align:center"> <table>
				<tr>
					<th>Version</th>
					<th>Release Date</th>
					<th>Windows (64 bit)</th>
					<th>Windows (32 bit)</th>
					<th>Debian/Ubuntu</th>
					<th>Linux (Flatpak)</th>
					<th>Source Code</th>
				</tr>
				<tr>
					<td class="table_release">0.1</td>
			"""
			+		"<td>" + datestring(new Calendar.Builder().setFields(Calendar.YEAR, 2022, Calendar.MONTH, 2, Calendar.DAY_OF_MONTH, 19).build(),
							request.getLocale()) + "</td>"
			+		"<td class='center'>" + createAssetDownloadLink(request, true,
						new File(Resources.RESOURCES_DIR, "/public/release/0.1/freerct-0.1-windows-win64.exe"), "Download Installer")
			+			"<br>" + createChecksumDownloadLink(
						new File(Resources.RESOURCES_DIR, "/public/release/0.1/freerct-0.1-windows-win64.exe.sha256")) + "</td>"
			+		"<td class='center'>" + createAssetDownloadLink(request, true,
						new File(Resources.RESOURCES_DIR, "/public/release/0.1/freerct-0.1-windows-win32.exe"), "Download Installer")
			+			"<br>" + createChecksumDownloadLink(
						new File(Resources.RESOURCES_DIR, "/public/release/0.1/freerct-0.1-windows-win32.exe.sha256")) + "</td>"
			+		"<td class='center'>" + createAssetDownloadLink(request, true,
						new File(Resources.RESOURCES_DIR, "/public/release/0.1/freerct-0.1-linux-amd64.deb"), "Download Package")
			+			"<br>" + createChecksumDownloadLink(
						new File(Resources.RESOURCES_DIR, "/public/release/0.1/freerct-0.1-linux-amd64.deb.sha256")) + "</td>"
			+		"<td class='center'>" + createAssetDownloadLink(request, true,
						new File(Resources.RESOURCES_DIR, "/public/release/0.1/freerct-0.1-linux-x86_64.flatpak"), "Flatpak")
			+			"<br>" + createChecksumDownloadLink(
						new File(Resources.RESOURCES_DIR, "/public/release/0.1/freerct-0.1-linux-x86_64.flatpak.sha256")) + "</td>"
			+		"<td class='center'>" + createAssetDownloadLink(request, true,
						new File(Resources.RESOURCES_DIR, "/public/release/0.1/0.1.zip"), "Source ZIP")
			+			"<br>" + createChecksumDownloadLink(
						new File(Resources.RESOURCES_DIR, "/public/release/0.1/0.1.zip.sha256")) + "</td>"
			+ """
				</tr>
			</table></p>
			<a href="https://repology.org/project/freerct/versions">
				<img src="https://repology.org/badge/vertical-allrepos/freerct.svg" alt="Packaging status" style="float: right; margin-left: 16px">
			</a>
			<p>
				To install FreeRCT:
				<ol>
					<li>      Download the installer for your desired platform and configuration from the list above.
					</li><li> If you wish, compute the SHA256 checksum of the downloaded file and check that it matches the
					          checksum stated in the checksum file by running <code>sha256sum FILENAME</code>.
					</li><li> Doubleclick on the downloaded file to start installation.
					</li><li> If you downloaded the source code archive, extract it and continue <a href='#compile'>below</a> for instructions how to compile.
					</li>
				</ol>
				FreeRCT is also available in the package repositories for several distributions. See the column on the right for details.
			</p>

			"""
			+ createLinkifiedHeader("h2", "/download", "daily", "Daily Builds")
			+ """
			<p>
				Automated builds are provided for Windows, MacOS, and Debian/Ubuntu for every development version.
			</p>
		""";

		if (latestDaily == null) {
			body += "<p><em>Due to an error, the list of builds is temporarily unavailable.</em></p>";
		} else {
			body	+=	"<p style='text-align:center'><table><tr>"
					+	"<th>Build Configuration</th>"
					+	"<th class='center'>Installer / Package</th>"
					+	"<th class='center'>Checksum</th>"
					+	"</tr>";

			File[] builds = latestDaily.listFiles();
			Arrays.sort(builds, (a, b) -> a.getName().compareTo(b.getName()));
			for (File build : builds) {
				String assetLink = "<em>N/A</em>";
				String checksumLink = "<em>N/A</em>";
				for (File f : build.listFiles()) {
					if (f.getName().endsWith("sha256")) {
						checksumLink = createChecksumDownloadLink(f);
					} else {
						assetLink = createAssetDownloadLink(request, false, f, "Download");
					}
				}
				body	+=	"<tr><th>" + htmlEscape(build.getName().replaceAll("_", " ").trim()) + "</th>"
						+	"<td class='center'>" + assetLink + "</td>"
						+	"<td class='center'>" + checksumLink + "</td></tr>"
						;
			}
			body += "</table></p>";
		}

		body += """
			<p>
				To install FreeRCT:
				<ol>
					<li>      Download the installer for your desired platform and configuration from the list above.
					</li><li> If you wish, compute the SHA256 checksum of the downloaded file and check that it matches the
					          checksum stated in the checksum file by running <code>sha256sum FILENAME</code>.
					</li><li><ul>
						<li>      If you downloaded a <em>Windows .exe installer</em> or <em>Debian/Ubuntu .deb package</em>,
						          simply doubleclick on the downloaded file to start installation.
						</li><li> With other distributions, extract the archive to some place on your hard disk.
						          No further installation is needed &ndash; you can directly run the executable file
						          which is located in the <em>bin</em> directory of the extracted directory.
						</li>
					</ul></li>
				</ol>
			</p><p>
				The most recent development version can also be played <a href='/play/play'>in the browser</a>.
			</p>

			"""
			+ createLinkifiedHeader("h2", "/download", "compile", "Compiling From Source")
			+ """
			<ol>
				<li>      Check out the commit you wish to build in Git,
					      or download and extract a ZIP/tarball of the latest development version from
					      <a target="_blank" href="https://github.com/FreeRCT/FreeRCT">GitHub</a>.
				</li><li> You need to have CMake and Make installed in order to compile FreeRCT.
					      CMake will check that all required libraries are installed.
					      If this fails, you need to install the libraries it complains about,
					      and retry until all dependencies are found.
				</li><li> <code>cd</code> into the downloaded <code>FreeRCT</code> directory and call:
					      <pre><code>
	<em># Construct build directory and enter it:</em>
	mkdir build
	cd build

	<em># Generate Makefile. Some of the more common build options &ndash; all of them optional &ndash; are: </em>
	<em>#   -DCMAKE_INSTALL_PREFIX=/usr/local   # Set install directory, default is '/usr'. </em>
	<em>#   -DRELEASE=ON                        # Build a release build. </em>
	<em>#   -DASAN=ON                           # Link with ASan memory checker. </em>
	<em>#   -DUSERDATAPREFIX="$HOME/.freerct"   # Where user data such as savegames is stored. </em>
	<em># Further details and more specialised options can be found in the README file in the root source directory. </em>
	cmake ..

	<em># Compile and install: </em>
	make
	make install  <em># Can be skipped. </em>
					      </code></pre>
				</li>
			</ol>

			"""
			+ createLinkifiedHeader("h2", "/download", "play", "How To Play")
			+ """
			<p>
				Just start the installed binary or type <code>freerct</code>.
				If you compiled FreeRCT yourself and skipped the <code>make install</code> step,
				use <code>./build/bin/freerct</code> or <code>make run</code>.
			</p><p>
				After installation, see the <a href="/manual">online manual</a> for information how to use FreeRCT.
			</p><p>
				Available command-line options are:
			</p><p style="text-align:center"> <table>
				<tr>
					<th>Short Option</th>
					<th>Long Option</th>
					<th>Explanation</th>
				</tr>
				<tr>
					<td class="codefield">-h</td>
					<td class="codefield">--help</td>
					<td>Display all available arguments.</td>
				</tr>
				<tr>
					<td class="codefield">-v</td>
					<td class="codefield">--version</td>
					<td>Display the build version and configuration.</td>
				</tr>
				<tr>
					<td class="codefield">-l FILE</td>
					<td class="codefield">--load FILE</td>
					<td>Directly load the specified savegame file.</td>
				</tr>
				<tr>
					<td class="codefield">-r</td>
					<td class="codefield">--resave</td>
					<td>Immediately save every savegame file again after loading.</td>
				</tr>
				<tr>
					<td class="codefield">-a LANG</td>
					<td class="codefield">--language LANG</td>
					<td>Start FreeRCT in the specified language. See below for supported languages.</td>
				</tr>
				<tr>
					<td class="codefield">-i DIR</td>
					<td class="codefield">--installdir DIR</td>
					<td>Use the specified installation directory.</td>
				</tr>
				<tr>
					<td class="codefield">-u DIR</td>
					<td class="codefield">--userdatadir DIR</td>
					<td>Use the specified user data directory.</td>
				</tr>
			</table></p>
			<p>
				FreeRCT is currently available in the following languages:
			</p><p style="text-align:center"> <table>
				<tr>
					<th>Language Code</th>
					<th>Name</th>
				</tr>
				<tr>
					<td class="codefield">da_DK</td>
					<td>Danish (Denmark)</td>
				</tr>
				<tr>
					<td class="codefield">de_DE</td>
					<td>German (Germany)</td>
				</tr>
				<tr>
					<td class="codefield">en_GB</td>
					<td>English (United Kingdom)</td>
				</tr>
				<tr>
					<td class="codefield">en_US</td>
					<td>English (United States)</td>
				</tr>
				<tr>
					<td class="codefield">es_ES</td>
					<td>Spanish (Spain)</td>
				</tr>
				<tr>
					<td class="codefield">fr_FR</td>
					<td>French (France)</td>
				</tr>
				<tr>
					<td class="codefield">nds_DE</td>
					<td>Low German (Germany)</td>
				</tr>
				<tr>
					<td class="codefield">nl_NL</td>
					<td>Dutch (Netherlands)</td>
				</tr>
				<tr>
					<td class="codefield">sv_SE</td>
					<td>Swedish (Sweden)</td>
				</tr>
			</table></p>
		""";
		return generatePage(request, session, "Get It!", body);
	}
}
