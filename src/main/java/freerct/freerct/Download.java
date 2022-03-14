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

/** The Download page. */
@Controller
public class Download {
	/* Notes on how to set up API calls for automatic integration.

	The full command-line call is:
		curl -u username:token URL

	- https://api.github.com/repos/freerct/freerct/actions/runs?branch=master&created=2022-02-22..*
		Returns all workflow runs on master which have been created since the given date.
		Filter for those with "name"="CI" and pick the one with the latest "created_at" timestamp.
		Note its "id" (e.g. 1882351097).
	- https://api.github.com/repos/freerct/freerct/actions/runs/{run_id}/artifacts
		Returns all artifacts. Get their properties: "id","name","size_in_bytes","archive_download_url".
	- For each new artifact, call its archive_download_url (with additional curl parameters: `-L -o FILE`) to download it.
		The result is a ZIP archive containing the ZIP file and its checksum file.
		Extract the artifact archive, compare checksums, then provide the inner archive as the final result.
	*/

	@GetMapping("/download")
	@ResponseBody
	public String fetch(WebRequest request, HttpSession session) {
		return generatePage(request, session, "Get It!", """
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
					<th>MacOS</th>
					<th>Debian/Ubuntu</th>
					<th>Source Code</th>
				</tr>
				<tr>
					<td class="table_release">0.1</td>
					<td>To be announced</td>
					<td class="invalid">N/A</td>
					<td class="invalid">N/A</td>
					<td class="invalid">N/A</td>
					<td class="invalid">N/A</td>
					<td class="invalid">N/A</td>
				</tr>
			</table> </p> <p>
				When FreeRCT 0.1 is released, links to the installers and instructions how to use them will be provided here.
			</p>

			"""
			+ createLinkifiedHeader("h2", "/download", "daily", "Daily Builds")
			+ """
			<p>
				Automated builds are provided for Windows and Debian/Ubuntu for every development version.
				The latest builds are available
				<a href='https://github.com/FreeRCT/FreeRCT/actions/workflows/ci-build.yml?query=branch%3Amaster'
					>on GitHub here</a>.
				Select the latest successful workflow run, then scroll down to the <em>Artifacts</em> section
				and download the ZIP archive for your platform.
			</p><p>
				A GitHub account is needed to download packages this way.
				In the near future, the latest packages will also be published on this website without the need to log in.
			</p><p>
				To install FreeRCT:
				<ol>
					<li>      Extract the downloaded ZIP archive. It contains two files: A binary file and a checksum file.
					</li><li> If you wish, compute the SHA256 checksum of the binary file and check that it matches the
					          checksum stated in the checksum file by running <code>md5sum FILENAME</code>.
					</li><li><ul>
						<li>      If you downloaded a <em>Debian/Ubuntu .deb package</em>,
						          simply install the package with your package manager.
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
				</li><li> You need to have CMake and Make installed in order to do compile FreeRCT.
					      CMake will check that all required libraries are installed.
					      If this fails, you need to install the libraries it complains about,
					      and retry until all dependencies are found.
				</li><li> <code>cd</code> into the downloaded <code>FreeRCT</code> directory and call:
					      <pre><code>
	<em># Construct build directory and enter it:</em>
	mkdir build
	cd build

	<em># Generate Makefile. Some build options &ndash; all of them optional &ndash; are: </em>
	<em>#   -DCMAKE_INSTALL_PREFIX=/usr/local   # Set install directory, default is '/usr'. </em>
	<em>#   -DASAN=ON                           # Link with ASan memory checker. </em>
	<em>#   -DUSERDATAPREFIX='~/.freerct'       # FreeRCT expands the '~' to the user home directory at runtime. </em>
	<em>#   -DVERSION_STRING="0.0-alpha"        # Set custom version string. </em>
	<em># Further details can be found in the README file in the root source directory. </em>
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
		""");
	}
}
