package net.ion.rosetta.query;

import java.util.HashMap;

public enum Constants {

	daap_browsealbumlisting("abal", 12), daap_browseartistlisting("abar", 12), daap_browsecomposerlisting("abcp", 12), daap_browsegenrelisting("abgn", 12), daap_baseplaylist("abpl", 1), daap_databasebrowse("abro", 12), daap_databasesongs("adbs", 12), daap_albumgrouping("agal", 12), // guess
	daap_songgrouping("agrp", 9), daap_databaseplaylists("aply", 12), daap_playlistrepeatmode("aprm", 1), daap_protocolversion("apro", 11), daap_playlistshufflemode("apsm", 1), daap_playlistsongs("apso", 12), daap_resolveinfo("arif", 12), daap_resolve("arsv", 12), daap_songalbumartist("asaa", 9), daap_songalbumid(
			"asai", 7), daap_songalbum("asal", 9), daap_songartist("asar", 9), daap_bookmarkable("asbk", 1), daap_songbookmark("asbo", 5), daap_songbitrate("asbr", 3), daap_songbeatsperminute("asbt", 3), daap_songcodectype("ascd", 5), daap_songcomment("ascm", 9), daap_songcontentdescription("ascn",
			9), daap_songcompilation("asco", 1), daap_songcomposer("ascp", 9), daap_songcontentrating("ascr", 1), daap_songcodecsubtype("ascs", 5), daap_songcategory("asct", 9), daap_songdateadded("asda", 10), daap_songdisabled("asdb", 1), daap_songdisccount("asdc", 3), daap_songdatakind("asdk", 1), daap_songdatemodified(
			"asdm", 10), daap_songdiscnumber("asdn", 3), daap_songdatepurchased("asdp", 10), daap_songdatereleased("asdr", 10), daap_songdescription("asdt", 9), daap_songextradata("ased", 3), daap_songeqpreset("aseq", 9), daap_songformat("asfm", 9), daap_songgenre("asgn", 9), daap_songgapless(
			"asgp", 1), daap_songhasbeenplayed("ashp", 1), daap_songkeywords("asky", 9), daap_songlongcontentdescription("aslc", 9), daap_songpodcasturl("aspu", 9), daap_songrelativevolume("asrv", 2), daap_sortartist("assa", 9), daap_sortcomposer("assc", 9), daap_sortalbumartist("assl", 9), daap_sortname(
			"assn", 9), daap_songstoptime("assp", 5), daap_songsamplerate("assr", 5), daap_sortseriesname("asss", 9), daap_songstarttime("asst", 5), daap_sortalbum("assu", 9), daap_songsize("assz", 5), daap_songtrackcount("astc", 3), daap_songtime("astm", 5), daap_songtracknumber("astn", 3), daap_songdataurl(
			"asul", 9), daap_songuserrating("asur", 1), daap_songyear("asyr", 3), daap_supportsextradata("ated", 3), daap_serverdatabases("avdb", 12),

	com_apple_itunes_itmsArtistid("aeAI", 5), com_apple_itunes_itmsComposerid("aeCI", 5), com_apple_itunes_contentRating("aeCR", 9), com_apple_itunes_episodeNumStr("aeEN", 9), com_apple_itunes_episodeSort("aeES", 5), com_apple_itunes_aeFP("aeFP", 1), // unknown (dmap.serverinforesponse)
	com_apple_itunes_gaplessEncDr("aeGD", 5), com_apple_itunes_gaplessEncDel("aeGE", 5), com_apple_itunes_gaplessHeur("aeGH", 5), com_apple_itunes_itmsGenreid("aeGI", 5), com_apple_itunes_gaplessResy("aeGR", 7), com_apple_itunes_gaplessDur("aeGU", 7), com_apple_itunes_hasVideo("aeHV", 1), com_apple_itunes_mediakind(
			"aeMK", 1), com_apple_itunes_networkName("aeNN", 9), com_apple_itunes_normVolume("aeNV", 5), com_apple_itunes_isPodcast("aePC", 1), com_apple_itunes_itmsPlaylistid("aePI", 5), com_apple_itunes_isPodcastPlaylist("aePP", 1), com_apple_itunes_specialPlaylist("aePS", 1), com_apple_itunes_itmsStorefrontid(
			"aeSF", 5), com_apple_itunes_itmsSongid("aeSI", 5), com_apple_itunes_seriesName("aeSN", 9), com_apple_itunes_smartPlaylist("aeSP", 1), com_apple_itunes_seasonNum("aeSU", 5), com_apple_itunes_musicSharingVersion("aeSV", 5),

	dacp_controlint("caci", 12), dacp_state("caps", 1), dacp_shuffle("cash", 1), dacp_repeat("carp", 1), dacp_albumshuffle("caas", 5), // guess, only seen '2'
	dacp_albumrepeat("caar", 5), // guess, only seen '6'
	dacp_isavailable("caia", 1), // something to do with speakers (true)
	dacp_nowplaying("canp", 0), // 4 ids: dbid, plid, playlistItem, itemid
	dacp_nowplayingname("cann", 9), dacp_nowplayingartist("cana", 9), dacp_nowplayingalbum("canl", 9), dacp_nowplayinggenre("cang", 9), dacp_remainingtime("cant", 5), dacp_speakers("casp", 12), // guess
	dacp_ss("cass", 1), // no idea
	dacp_songtime("cast", 5), dacp_su("casu", 1), // no idea
	dacp_sg("ceSG", 1), // no idea

	dmcp_controlprompt("cmcp", 12), // used when control state changes (eg server exit)
	dmcp_getpropertyresponse("cmgt", 12), // guess
	dmcp_ik("cmik", 1), // unknown (1) is...?
	dmcp_sp("cmsp", 1), // unknown (1) speakers? supports...?
	dmcp_status("cmst", 12), dmcp_sv("cmsv", 1), // unknown (1)
	dmcp_mediarevision("cmsr", 5), dmcp_mediakind("cmmk", 5), dmcp_volume("cmvo", 5),

	dmap_bag("mbcl", 12), dmap_contentcodesresponse("mccr", 12), dmap_contentcodesname("mcna", 9), dmap_contentcodesnumber("mcnm", 5), dmap_container("mcon", 12), dmap_containercount("mctc", 5), dmap_containeritemid("mcti", 5), dmap_contentcodestype("mcty", 3), dmap_dictionary("mdcl", 12), dmap_editdictionary(
			"medc", 12), dmap_editstatus("meds", 5), dmap_itemid("miid", 5), dmap_itemkind("mikd", 1), dmap_itemcount("mimc", 5), dmap_itemname("minm", 9), dmap_listing("mlcl", 12), dmap_sessionid("mlid", 5), dmap_listingitem("mlit", 12), dmap_loginresponse("mlog", 12), dmap_parentcontainerid(
			"mpco", 5), dmap_persistentid("mper", 7), dmap_protocolversion("mpro", 11), dmap_returnedcount("mrco", 5), dmap_supportsautologout("msal", 1), dmap_authenticationschemes("msas", 1), dmap_authenticationmethod("msau", 1), dmap_supportsbrowse("msbr", 1), dmap_databasescount("msdc", 5), dmap_supportsedit(
			"msed", 1), dmap_supportsextensions("msex", 1), dmap_supportsindex("msix", 1), dmap_loginrequired("mslr", 1), dmap_speakermachineaddress("msma", 7), // not idea! only seen '0'
	dmap_speakermachinelist("msml", 12), dmap_supportspersistentids("mspi", 1), dmap_supportsquery("msqy", 1), dmap_supportsresolve("msrs", 1), dmap_serverinforesponse("msrv", 12), dmap_utctime("mstc", 10), dmap_timeoutinterval("mstm", 5), dmap_utcoffset("msto", 6), dmap_statusstring("msts", 9), dmap_status(
			"mstt", 5), dmap_supportsupdate("msup", 1), dmap_specifiedtotalcount("mtco", 5), dmap_deletedidlisting("mudl", 12), dmap_updateresponse("mupd", 12), dmap_serverrevision("musr", 5), dmap_updatetype("muty", 1);

	public static final int BYTE = 1;
	public static final int SIGNED_BYTE = 2;
	public static final int SHORT = 3;
	public static final int SIGNED_SHORT = 4;
	public static final int INTEGER = 5;
	public static final int SIGNED_INTEGER = 6;
	public static final int LONG = 7;
	public static final int SIGNED_LONG = 8;
	public static final int STRING = 9;
	public static final int DATE = 10;
	public static final int VERSION = 11;
	public static final int COMPOSITE = 12;
	public static final int LONG_LONG = 0;

	public final int code;
	public final String shortName;
	public final String longName;
	public final int type;

	private Constants(String code, int type) {
		this.code = stringToCode(code);
		this.shortName = code;
		this.type = type;

		String name = "";
		for (char c : this.name().toCharArray()) {
			if (c == '_') {
				name += ".";
			} else if (Character.isUpperCase(c)) {
				name += "-" + Character.toLowerCase(c);
			} else {
				name += c;
			}
		}
		this.longName = name;
	}

	public String toString() {
		return longName;
	}

	private static int stringToCode(String code) {
		char[] b = code.toCharArray();
		int v = 0;
		for (int i = 0; i < 4; i++) {
			v <<= 8;
			v += b[i] & 255;
		}
		return v;
	}

	public static Constants get(int code) {
		return lookup.get(code);
	}

	public static Constants get(String name) {
		return names.get(name);
	}

	private static HashMap<Integer, Constants> lookup = new HashMap<Integer, Constants>();
	private static HashMap<String, Constants> names = new HashMap<String, Constants>();

	static {
		for (Constants c : Constants.values()) {
			lookup.put(c.code, c);
			names.put(c.longName, c);
		}
	}
}
