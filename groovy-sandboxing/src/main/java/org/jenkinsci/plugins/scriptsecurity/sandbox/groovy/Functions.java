package org.jenkinsci.plugins.scriptsecurity.sandbox.groovy;

/*
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
 */

public class Functions {


//	private static final AtomicLong iota = new AtomicLong();
//	private static Logger LOGGER = Logger.getLogger(Functions.class.getName());
//	@Restricted({NoExternalUse.class})
//	@SuppressFBWarnings(
//			value = {"MS_SHOULD_BE_FINAL"},
//			justification = "for script console"
//	)
//	public static boolean UI_REFRESH = SystemProperties.getBoolean("jenkins.ui.refresh");
//	private static final Pattern ICON_SIZE = Pattern.compile("\\d+x\\d+");
//	@SuppressFBWarnings(
//			value = {"MS_SHOULD_BE_FINAL"},
//			justification = "for script console"
//	)
//	public static boolean DEBUG_YUI = SystemProperties.getBoolean("debug.YUI");
//	private static final SimpleFormatter formatter = new SimpleFormatter();
//	private static String footerURL = null;
//	private static final Pattern LINE_END = Pattern.compile("\r?\n");
//
//	public Functions() {
//	}
//
//	public String generateId() {
//		return "id" + iota.getAndIncrement();
//	}
//
//	public static boolean isModel(Object o) {
//		return o instanceof ModelObject;
//	}
//
//	public static boolean isModelWithContextMenu(Object o) {
//		return o instanceof ModelObjectWithContextMenu;
//	}
//
//	public static boolean isModelWithChildren(Object o) {
//		return o instanceof ModelObjectWithChildren;
//	}
//
//	/** @deprecated */
//	@Deprecated
//	public static boolean isMatrixProject(Object o) {
//		return o != null && o.getClass().getName().equals("hudson.matrix.MatrixProject");
//	}
//
//	public static String xsDate(Calendar cal) {
//		return Util.XS_DATETIME_FORMATTER.format(cal.getTime());
//	}
//
//	@Restricted({NoExternalUse.class})
//	public static String iso8601DateTime(Date date) {
//		return Util.XS_DATETIME_FORMATTER.format(date);
//	}
//
//	@Restricted({NoExternalUse.class})
//	public static String localDate(Date date) {
//		return DateFormat.getDateInstance(3).format(date);
//	}
//
//	public static String rfc822Date(Calendar cal) {
//		return Util.RFC822_DATETIME_FORMATTER.format(cal.getTime());
//	}
//
//	@Restricted({NoExternalUse.class})
//	public static String getTimeSpanString(Date date) {
//		return Util.getTimeSpanString(Math.abs(date.getTime() - (new Date()).getTime()));
//	}
//
//	public static boolean isExtensionsAvailable() {
//		Jenkins jenkins = Jenkins.getInstanceOrNull();
//		return jenkins != null && jenkins.getInitLevel().compareTo(InitMilestone.EXTENSIONS_AUGMENTED) >= 0 && !jenkins.isTerminating();
//	}
//
//	public static void initPageVariables(JellyContext context) {
//		StaplerRequest currentRequest = Stapler.getCurrentRequest();
//		currentRequest.getWebApp().getDispatchValidator().allowDispatch(currentRequest, Stapler.getCurrentResponse());
//		String rootURL = currentRequest.getContextPath();
//		Functions h = new Functions();
//		context.setVariable("h", h);
//		context.setVariable("rootURL", rootURL);
//		context.setVariable("resURL", rootURL + getResourcePath());
//		context.setVariable("imagesURL", rootURL + getResourcePath() + "/images");
//		context.setVariable("divBasedFormLayout", true);
//		context.setVariable("userAgent", currentRequest.getHeader("User-Agent"));
//		IconSet.initPageVariables(context);
//	}
//
//	public static <B> Class getTypeParameter(Class<? extends B> c, Class<B> base, int n) {
//		Type parameterization = Types.getBaseClass(c, base);
//		if (parameterization instanceof ParameterizedType) {
//			ParameterizedType pt = (ParameterizedType)parameterization;
//			return Types.erasure(Types.getTypeArgument(pt, n));
//		} else {
//			throw new AssertionError(c + " doesn't properly parameterize " + base);
//		}
//	}
//
//	public JDK.DescriptorImpl getJDKDescriptor() {
//		return (JDK.DescriptorImpl)Jenkins.get().getDescriptorByType(JDK.DescriptorImpl.class);
//	}
//
//	public static String getDiffString(int i) {
//		if (i == 0) {
//			return "±0";
//		} else {
//			String s = Integer.toString(i);
//			return i > 0 ? "+" + s : s;
//		}
//	}
//
//	public static String getDiffString2(int i) {
//		if (i == 0) {
//			return "";
//		} else {
//			String s = Integer.toString(i);
//			return i > 0 ? "+" + s : s;
//		}
//	}
//
//	public static String getDiffString2(String prefix, int i, String suffix) {
//		if (i == 0) {
//			return "";
//		} else {
//			String s = Integer.toString(i);
//			return i > 0 ? prefix + "+" + s + suffix : prefix + s + suffix;
//		}
//	}
//
//	public static String addSuffix(int n, String singular, String plural) {
//		StringBuilder buf = new StringBuilder();
//		buf.append(n).append(' ');
//		if (n == 1) {
//			buf.append(singular);
//		} else {
//			buf.append(plural);
//		}
//
//		return buf.toString();
//	}
//
//	public static RunUrl decompose(StaplerRequest req) {
//		List<Ancestor> ancestors = req.getAncestors();
//		Ancestor f = null;
//		Ancestor l = null;
//		Iterator var4 = ancestors.iterator();
//
//		while(var4.hasNext()) {
//			Ancestor anc = (Ancestor)var4.next();
//			if (anc.getObject() instanceof Run) {
//				if (f == null) {
//					f = anc;
//				}
//
//				l = anc;
//			}
//		}
//
//		if (l == null) {
//			return null;
//		} else {
//			String head = f.getPrev().getUrl() + '/';
//			String base = l.getUrl();
//			String reqUri = req.getOriginalRequestURI();
//			String furl = f.getUrl();
//			int slashCount = 0;
//
//			for(int i = furl.indexOf(47); i >= 0; i = furl.indexOf(47, i + 1)) {
//				++slashCount;
//			}
//
//			String rest = reqUri.replaceFirst("(?:/+[^/]*){" + slashCount + "}", "");
//			return new RunUrl((Run)f.getObject(), head, base, rest);
//		}
//	}
//
//	public static Area getScreenResolution() {
//		Cookie res = getCookie(Stapler.getCurrentRequest(), "screenResolution");
//		return res != null ? Area.parse(res.getValue()) : null;
//	}
//
//	@Restricted({NoExternalUse.class})
//	public static boolean useHidingPasswordFields() {
//		return SystemProperties.getBoolean(Functions.class.getName() + ".hidingPasswordFields", true);
//	}
//
//	public static Node.Mode[] getNodeModes() {
//		return Mode.values();
//	}
//
//	public static String getProjectListString(List<AbstractProject> projects) {
//		return Items.toNameList(projects);
//	}
//
//	/** @deprecated */
//	@Deprecated
//	public static Object ifThenElse(boolean cond, Object thenValue, Object elseValue) {
//		return cond ? thenValue : elseValue;
//	}
//
//	public static String appendIfNotNull(String text, String suffix, String nullText) {
//		return text == null ? nullText : text + suffix;
//	}
//
//	public static Map getSystemProperties() {
//		return new TreeMap(System.getProperties());
//	}
//
//	@Restricted({DoNotUse.class})
//	public static String getSystemProperty(String key) {
//		return SystemProperties.getString(key);
//	}
//
//	@Restricted({DoNotUse.class})
//	public static boolean isUiRefreshEnabled() {
//		return UI_REFRESH;
//	}
//
//	public static Map getEnvVars() {
//		return new TreeMap(EnvVars.masterEnvVars);
//	}
//
//	public static boolean isWindows() {
//		return File.pathSeparatorChar == ';';
//	}
//
//	public static boolean isGlibcSupported() {
//		try {
//			GNUCLibrary.LIBC.getpid();
//			return true;
//		} catch (Throwable var1) {
//			return false;
//		}
//	}
//
//	public static List<LogRecord> getLogRecords() {
//		return Jenkins.logRecords;
//	}
//
//	public static String printLogRecord(LogRecord r) {
//		return formatter.format(r);
//	}
//
//	@Restricted({NoExternalUse.class})
//	public static String[] printLogRecordHtml(LogRecord r, LogRecord prior) {
//		String[] oldParts = prior == null ? new String[4] : logRecordPreformat(prior);
//		String[] newParts = logRecordPreformat(r);
//
//		for(int i = 0; i < 3; ++i) {
//			newParts[i] = "<span class='" + (newParts[i].equals(oldParts[i]) ? "logrecord-metadata-old" : "logrecord-metadata-new") + "'>" + newParts[i] + "</span>";
//		}
//
//		newParts[3] = Util.xmlEscape(newParts[3]);
//		return newParts;
//	}
//
//	private static String[] logRecordPreformat(LogRecord r) {
//		String source;
//		if (r.getSourceClassName() == null) {
//			source = r.getLoggerName();
//		} else if (r.getSourceMethodName() == null) {
//			source = r.getSourceClassName();
//		} else {
//			source = r.getSourceClassName() + " " + r.getSourceMethodName();
//		}
//
//		String message = (new SimpleFormatter()).formatMessage(r) + "\n";
//		Throwable x = r.getThrown();
//		return new String[]{String.format("%1$tb %1$td, %1$tY %1$tl:%1$tM:%1$tS %1$Tp", new Date(r.getMillis())), source, r.getLevel().getLocalizedName(), x == null ? message : message + printThrowable(x) + "\n"};
//	}
//
//	public static <T> Iterable<T> reverse(Collection<T> collection) {
//		List<T> list = new ArrayList(collection);
//		Collections.reverse(list);
//		return list;
//	}
//
//	public static Cookie getCookie(HttpServletRequest req, String name) {
//		Cookie[] cookies = req.getCookies();
//		if (cookies != null) {
//			Cookie[] var3 = cookies;
//			int var4 = cookies.length;
//
//			for(int var5 = 0; var5 < var4; ++var5) {
//				Cookie cookie = var3[var5];
//				if (cookie.getName().equals(name)) {
//					return cookie;
//				}
//			}
//		}
//
//		return null;
//	}
//
//	public static String getCookie(HttpServletRequest req, String name, String defaultValue) {
//		Cookie c = getCookie(req, name);
//		return c != null && c.getValue() != null ? c.getValue() : defaultValue;
//	}
//
//	@Restricted({NoExternalUse.class})
//	public static String validateIconSize(String iconSize) throws SecurityException {
//		if (!ICON_SIZE.matcher(iconSize).matches()) {
//			throw new SecurityException("invalid iconSize");
//		} else {
//			return iconSize;
//		}
//	}
//
//	public static String getYuiSuffix() {
//		return DEBUG_YUI ? "debug" : "min";
//	}
//
//	public static <V> SortedMap<Integer, V> filter(SortedMap<Integer, V> map, String from, String to) {
//		if (from == null && to == null) {
//			return map;
//		} else if (to == null) {
//			return map.headMap(Integer.parseInt(from) - 1);
//		} else {
//			return from == null ? map.tailMap(Integer.parseInt(to)) : map.subMap(Integer.parseInt(to), Integer.parseInt(from) - 1);
//		}
//	}
//
//	@Restricted({NoExternalUse.class})
//	public static <V> SortedMap<Integer, V> filterExcludingFrom(SortedMap<Integer, V> map, String from, String to) {
//		if (from == null && to == null) {
//			return map;
//		} else if (to == null) {
//			return map.headMap(Integer.parseInt(from));
//		} else {
//			return from == null ? map.tailMap(Integer.parseInt(to)) : map.subMap(Integer.parseInt(to), Integer.parseInt(from));
//		}
//	}
//
//	/** @deprecated */
//	@Deprecated
//	public static void configureAutoRefresh(HttpServletRequest request, HttpServletResponse response, boolean noAutoRefresh) {
//	}
//
//	/** @deprecated */
//	@Deprecated
//	public static boolean isAutoRefresh(HttpServletRequest request) {
//		return false;
//	}
//
//	public static boolean isCollapsed(String paneId) {
//		return PaneStatusProperties.forCurrentUser().isCollapsed(paneId);
//	}
//
//	@Restricted({NoExternalUse.class})
//	public static boolean isUserTimeZoneOverride() {
//		return TimeZoneProperty.forCurrentUser() != null;
//	}
//
//	@Restricted({NoExternalUse.class})
//	@Nullable
//	public static String getUserTimeZone() {
//		return TimeZoneProperty.forCurrentUser();
//	}
//
//	@Restricted({NoExternalUse.class})
//	public static String getUserTimeZonePostfix() {
//		if (!isUserTimeZoneOverride()) {
//			return "";
//		} else {
//			TimeZone tz = TimeZone.getTimeZone(getUserTimeZone());
//			return tz.getDisplayName(tz.observesDaylightTime(), 0);
//		}
//	}
//
//	public static String getNearestAncestorUrl(StaplerRequest req, Object it) {
//		List list = req.getAncestors();
//
//		for(int i = list.size() - 1; i >= 0; --i) {
//			Ancestor anc = (Ancestor)list.get(i);
//			if (anc.getObject() == it) {
//				return anc.getUrl();
//			}
//		}
//
//		return null;
//	}
//
//	public static String getSearchURL() {
//		List list = Stapler.getCurrentRequest().getAncestors();
//
//		for(int i = list.size() - 1; i >= 0; --i) {
//			Ancestor anc = (Ancestor)list.get(i);
//			if (anc.getObject() instanceof SearchableModelObject) {
//				return anc.getUrl() + "/search/";
//			}
//		}
//
//		return null;
//	}
//
//	public static String appendSpaceIfNotNull(String n) {
//		return n == null ? null : n + ' ';
//	}
//
//	public static String nbspIndent(String size) {
//		int i = size.indexOf(120);
//		i = Integer.parseInt(i > 0 ? size.substring(0, i) : size) / 10;
//		StringBuilder buf = new StringBuilder(30);
//
//		for(int j = 2; j <= i; ++j) {
//			buf.append("&nbsp;");
//		}
//
//		return buf.toString();
//	}
//
//	public static String getWin32ErrorMessage(IOException e) {
//		return Util.getWin32ErrorMessage(e);
//	}
//
//	public static boolean isMultiline(String s) {
//		if (s == null) {
//			return false;
//		} else {
//			return s.indexOf(13) >= 0 || s.indexOf(10) >= 0;
//		}
//	}
//
//	public static String encode(String s) {
//		return Util.encode(s);
//	}
//
//	public static String urlEncode(String s) {
//		if (s == null) {
//			return "";
//		} else {
//			try {
//				return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
//			} catch (UnsupportedEncodingException var2) {
//				throw new Error(var2);
//			}
//		}
//	}
//
//	public static String escape(String s) {
//		return Util.escape(s);
//	}
//
//	public static String xmlEscape(String s) {
//		return Util.xmlEscape(s);
//	}
//
//	public static String xmlUnescape(String s) {
//		return s.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
//	}
//
//	public static String htmlAttributeEscape(String text) {
//		StringBuilder buf = new StringBuilder(text.length() + 64);
//
//		for(int i = 0; i < text.length(); ++i) {
//			char ch = text.charAt(i);
//			if (ch == '<') {
//				buf.append("&lt;");
//			} else if (ch == '>') {
//				buf.append("&gt;");
//			} else if (ch == '&') {
//				buf.append("&amp;");
//			} else if (ch == '"') {
//				buf.append("&quot;");
//			} else if (ch == '\'') {
//				buf.append("&#39;");
//			} else {
//				buf.append(ch);
//			}
//		}
//
//		return buf.toString();
//	}
//
//	public static void checkPermission(Permission permission) throws IOException, ServletException {
//		checkPermission((AccessControlled)Jenkins.get(), permission);
//	}
//
//	public static void checkPermission(AccessControlled object, Permission permission) throws IOException, ServletException {
//		if (permission != null) {
//			object.checkPermission(permission);
//		}
//
//	}
//
//	public static void checkPermission(Object object, Permission permission) throws IOException, ServletException {
//		if (permission != null) {
//			if (object instanceof AccessControlled) {
//				checkPermission((AccessControlled)object, permission);
//			} else {
//				List<Ancestor> ancs = Stapler.getCurrentRequest().getAncestors();
//				Iterator var3 = Iterators.reverse(ancs).iterator();
//
//				while(var3.hasNext()) {
//					Ancestor anc = (Ancestor)var3.next();
//					Object o = anc.getObject();
//					if (o instanceof AccessControlled) {
//						checkPermission((AccessControlled)o, permission);
//						return;
//					}
//				}
//
//				checkPermission((AccessControlled)Jenkins.get(), permission);
//			}
//
//		}
//	}
//
//	public static boolean hasPermission(Permission permission) throws IOException, ServletException {
//		return hasPermission(Jenkins.get(), permission);
//	}
//
//	public static boolean hasPermission(Object object, Permission permission) throws IOException, ServletException {
//		if (permission == null) {
//			return true;
//		} else if (object instanceof AccessControlled) {
//			return ((AccessControlled)object).hasPermission(permission);
//		} else {
//			List<Ancestor> ancs = Stapler.getCurrentRequest().getAncestors();
//			Iterator var3 = Iterators.reverse(ancs).iterator();
//
//			Object o;
//			do {
//				if (!var3.hasNext()) {
//					return Jenkins.get().hasPermission(permission);
//				}
//
//				Ancestor anc = (Ancestor)var3.next();
//				o = anc.getObject();
//			} while(!(o instanceof AccessControlled));
//
//			return ((AccessControlled)o).hasPermission(permission);
//		}
//	}
//
//	public static void adminCheck(StaplerRequest req, StaplerResponse rsp, Object required, Permission permission) throws IOException, ServletException {
//		if (required != null && !Hudson.adminCheck(req, rsp)) {
//			rsp.setStatus(403);
//			rsp.getOutputStream().close();
//			throw new ServletException("Unauthorized access");
//		} else {
//			if (permission != null) {
//				checkPermission(permission);
//			}
//
//		}
//	}
//
//	public static String inferHudsonURL(StaplerRequest req) {
//		String rootUrl = Jenkins.get().getRootUrl();
//		if (rootUrl != null) {
//			return rootUrl;
//		} else {
//			StringBuilder buf = new StringBuilder();
//			buf.append(req.getScheme()).append("://");
//			buf.append(req.getServerName());
//			if ((!req.getScheme().equals("http") || req.getLocalPort() != 80) && (!req.getScheme().equals("https") || req.getLocalPort() != 443)) {
//				buf.append(':').append(req.getLocalPort());
//			}
//
//			buf.append(req.getContextPath()).append('/');
//			return buf.toString();
//		}
//	}
//
//	public static String getFooterURL() {
//		if (footerURL == null) {
//			footerURL = SystemProperties.getString("hudson.footerURL");
//			if (StringUtils.isBlank(footerURL)) {
//				footerURL = "https://www.jenkins.io/";
//			}
//		}
//
//		return footerURL;
//	}
//
//	public static List<JobPropertyDescriptor> getJobPropertyDescriptors(Class<? extends Job> clazz) {
//		return JobPropertyDescriptor.getPropertyDescriptors(clazz);
//	}
//
//	public static List<JobPropertyDescriptor> getJobPropertyDescriptors(Job job) {
//		return DescriptorVisibilityFilter.apply(job, JobPropertyDescriptor.getPropertyDescriptors(job.getClass()));
//	}
//
//	public static List<Descriptor<BuildWrapper>> getBuildWrapperDescriptors(AbstractProject<?, ?> project) {
//		return BuildWrappers.getFor(project);
//	}
//
//	public static List<Descriptor<SecurityRealm>> getSecurityRealmDescriptors() {
//		return SecurityRealm.all();
//	}
//
//	public static List<Descriptor<AuthorizationStrategy>> getAuthorizationStrategyDescriptors() {
//		return AuthorizationStrategy.all();
//	}
//
//	public static List<Descriptor<Builder>> getBuilderDescriptors(AbstractProject<?, ?> project) {
//		return BuildStepDescriptor.filter(Builder.all(), project.getClass());
//	}
//
//	public static List<Descriptor<Publisher>> getPublisherDescriptors(AbstractProject<?, ?> project) {
//		return BuildStepDescriptor.filter(Publisher.all(), project.getClass());
//	}
//
//	public static List<SCMDescriptor<?>> getSCMDescriptors(AbstractProject<?, ?> project) {
//		return SCM._for(project);
//	}
//
//	/** @deprecated */
//	@Deprecated
//	@Restricted({DoNotUse.class})
//	@RestrictedSince("2.12")
//	public static List<Descriptor<ComputerLauncher>> getComputerLauncherDescriptors() {
//		return Jenkins.get().getDescriptorList(ComputerLauncher.class);
//	}
//
//	/** @deprecated */
//	@Deprecated
//	@Restricted({DoNotUse.class})
//	@RestrictedSince("2.12")
//	public static List<Descriptor<RetentionStrategy<?>>> getRetentionStrategyDescriptors() {
//		return RetentionStrategy.all();
//	}
//
//	public static List<ParameterDefinition.ParameterDescriptor> getParameterDescriptors() {
//		return ParameterDefinition.all();
//	}
//
//	public static List<Descriptor<CaptchaSupport>> getCaptchaSupportDescriptors() {
//		return CaptchaSupport.all();
//	}
//
//	public static List<Descriptor<ViewsTabBar>> getViewsTabBarDescriptors() {
//		return ViewsTabBar.all();
//	}
//
//	public static List<Descriptor<MyViewsTabBar>> getMyViewsTabBarDescriptors() {
//		return MyViewsTabBar.all();
//	}
//
//	/** @deprecated */
//	@Deprecated
//	@Restricted({DoNotUse.class})
//	@RestrictedSince("2.12")
//	public static List<NodePropertyDescriptor> getNodePropertyDescriptors(Class<? extends Node> clazz) {
//		List<NodePropertyDescriptor> result = new ArrayList();
//		Collection<NodePropertyDescriptor> list = Jenkins.get().getDescriptorList(NodeProperty.class);
//		Iterator var3 = list.iterator();
//
//		while(var3.hasNext()) {
//			NodePropertyDescriptor npd = (NodePropertyDescriptor)var3.next();
//			if (npd.isApplicable(clazz)) {
//				result.add(npd);
//			}
//		}
//
//		return result;
//	}
//
//	public static List<NodePropertyDescriptor> getGlobalNodePropertyDescriptors() {
//		List<NodePropertyDescriptor> result = new ArrayList();
//		Collection<NodePropertyDescriptor> list = Jenkins.get().getDescriptorList(NodeProperty.class);
//		Iterator var2 = list.iterator();
//
//		while(var2.hasNext()) {
//			NodePropertyDescriptor npd = (NodePropertyDescriptor)var2.next();
//			if (npd.isApplicableAsGlobal()) {
//				result.add(npd);
//			}
//		}
//
//		return result;
//	}
//
//	/** @deprecated */
//	@Deprecated
//	@Restricted({NoExternalUse.class})
//	public static Collection<Descriptor> getSortedDescriptorsForGlobalConfig(Predicate<GlobalConfigurationCategory> predicate) {
//		ExtensionList<Descriptor> exts = ExtensionList.lookup(Descriptor.class);
//		List<Tag> r = new ArrayList(exts.size());
//		Iterator var3 = exts.getComponents().iterator();
//
//		while(var3.hasNext()) {
//			ExtensionComponent<Descriptor> c = (ExtensionComponent)var3.next();
//			Descriptor d = (Descriptor)c.getInstance();
//			if (d.getGlobalConfigPage() != null && Jenkins.get().hasPermission(d.getRequiredGlobalConfigPagePermission()) && predicate.apply(d.getCategory())) {
//				r.add(new Tag(c.ordinal(), d));
//			}
//		}
//
//		Collections.sort(r);
//		List<Descriptor> answer = new ArrayList(r.size());
//		Iterator var7 = r.iterator();
//
//		while(var7.hasNext()) {
//			Tag d = (Tag)var7.next();
//			answer.add(d.d);
//		}
//
//		return DescriptorVisibilityFilter.apply(Jenkins.get(), answer);
//	}
//
//	public static Collection<Descriptor> getSortedDescriptorsForGlobalConfigByDescriptor(java.util.function.Predicate<Descriptor> predicate) {
//		ExtensionList<Descriptor> exts = ExtensionList.lookup(Descriptor.class);
//		List<Tag> r = new ArrayList(exts.size());
//		Iterator var3 = exts.getComponents().iterator();
//
//		while(var3.hasNext()) {
//			ExtensionComponent<Descriptor> c = (ExtensionComponent)var3.next();
//			Descriptor d = (Descriptor)c.getInstance();
//			if (d.getGlobalConfigPage() != null && predicate.test(d)) {
//				r.add(new Tag(c.ordinal(), d));
//			}
//		}
//
//		Collections.sort(r);
//		List<Descriptor> answer = new ArrayList(r.size());
//		Iterator var8 = r.iterator();
//
//		while(var8.hasNext()) {
//			Tag d = (Tag)var8.next();
//			answer.add(d.d);
//		}
//
//		return DescriptorVisibilityFilter.apply(Jenkins.get(), answer);
//	}
//
//	public static Collection<Descriptor> getSortedDescriptorsForGlobalConfigByDescriptor() {
//		return getSortedDescriptorsForGlobalConfigByDescriptor((descriptor) -> {
//			return true;
//		});
//	}
//
//	/** @deprecated */
//	@Deprecated
//	public static Collection<Descriptor> getSortedDescriptorsForGlobalConfigNoSecurity() {
//		return getSortedDescriptorsForGlobalConfigByDescriptor((d) -> {
//			return GlobalSecurityConfiguration.FILTER.negate().test(d);
//		});
//	}
//
//	public static Collection<Descriptor> getSortedDescriptorsForGlobalConfigUnclassified() {
//		return getSortedDescriptorsForGlobalConfigByDescriptor((d) -> {
//			return d.getCategory() instanceof GlobalConfigurationCategory.Unclassified && Jenkins.get().hasPermission(d.getRequiredGlobalConfigPagePermission());
//		});
//	}
//
//	@Restricted({NoExternalUse.class})
//	public static Collection<Descriptor> getSortedDescriptorsForGlobalConfigUnclassifiedReadable() {
//		return getSortedDescriptorsForGlobalConfigByDescriptor((d) -> {
//			return d.getCategory() instanceof GlobalConfigurationCategory.Unclassified && (Jenkins.get().hasPermission(d.getRequiredGlobalConfigPagePermission()) || Jenkins.get().hasPermission(Jenkins.SYSTEM_READ));
//		});
//	}
//
//	public static boolean hasAnyPermission(AccessControlled ac, Permission[] permissions) {
//		return permissions != null && permissions.length != 0 ? ac.hasAnyPermission(permissions) : true;
//	}
//
//	public static boolean hasAnyPermission(Object object, Permission[] permissions) throws IOException, ServletException {
//		if (permissions != null && permissions.length != 0) {
//			if (object instanceof AccessControlled) {
//				return hasAnyPermission((AccessControlled)object, permissions);
//			} else {
//				AccessControlled ac = (AccessControlled)Stapler.getCurrentRequest().findAncestorObject(AccessControlled.class);
//				return ac != null ? hasAnyPermission(ac, permissions) : hasAnyPermission((AccessControlled)Jenkins.get(), permissions);
//			}
//		} else {
//			return true;
//		}
//	}
//
//	public static void checkAnyPermission(AccessControlled ac, Permission[] permissions) {
//		if (permissions != null && permissions.length != 0) {
//			ac.checkAnyPermission(permissions);
//		}
//	}
//
//	public static void checkAnyPermission(Object object, Permission[] permissions) throws IOException, ServletException {
//		if (permissions != null && permissions.length != 0) {
//			if (object instanceof AccessControlled) {
//				checkAnyPermission((AccessControlled)object, permissions);
//			} else {
//				List<Ancestor> ancs = Stapler.getCurrentRequest().getAncestors();
//				Iterator var3 = Iterators.reverse(ancs).iterator();
//
//				while(var3.hasNext()) {
//					Ancestor anc = (Ancestor)var3.next();
//					Object o = anc.getObject();
//					if (o instanceof AccessControlled) {
//						checkAnyPermission((AccessControlled)o, permissions);
//						return;
//					}
//				}
//
//				checkAnyPermission((AccessControlled)Jenkins.get(), permissions);
//			}
//
//		}
//	}
//
//	public static String getIconFilePath(Action a) {
//		String name = a.getIconFileName();
//		if (name == null) {
//			return null;
//		} else {
//			return name.startsWith("/") ? name.substring(1) : "images/24x24/" + name;
//		}
//	}
//
//	public static int size2(Object o) throws Exception {
//		return o == null ? 0 : ASTSizeFunction.sizeOf(o, Introspector.getUberspect());
//	}
//
//	public static String getRelativeLinkTo(Item p) {
//		Map<Object, String> ancestors = new HashMap();
//		View view = null;
//		StaplerRequest request = Stapler.getCurrentRequest();
//		Iterator var4 = request.getAncestors().iterator();
//
//		while(var4.hasNext()) {
//			Ancestor a = (Ancestor)var4.next();
//			ancestors.put(a.getObject(), a.getRelativePath());
//			if (a.getObject() instanceof View) {
//				view = (View)a.getObject();
//			}
//		}
//
//		String path = (String)ancestors.get(p);
//		if (path != null) {
//			return normalizeURI(path + '/');
//		} else {
//			Item i = p;
//			String url = "";
//
//			while(true) {
//				ItemGroup ig = i.getParent();
//				url = i.getShortUrl() + url;
//				if (ig == Jenkins.get() || view != null && ig == view.getOwner().getItemGroup()) {
//					assert i instanceof TopLevelItem;
//
//					return view != null ? normalizeURI((String)ancestors.get(view) + '/' + url) : normalizeURI(request.getContextPath() + '/' + p.getUrl());
//				}
//
//				path = (String)ancestors.get(ig);
//				if (path != null) {
//					return normalizeURI(path + '/' + url);
//				}
//
//				assert ig instanceof Item;
//
//				i = (Item)ig;
//			}
//		}
//	}
//
//	private static String normalizeURI(String uri) {
//		return URI.create(uri).normalize().toString();
//	}
//
//	public static List<TopLevelItem> getAllTopLevelItems(ItemGroup root) {
//		return root.getAllItems(TopLevelItem.class);
//	}
//
//	@Nullable
//	public static String getRelativeNameFrom(@Nullable Item p, @Nullable ItemGroup g, boolean useDisplayName) {
//		if (p == null) {
//			return null;
//		} else if (g == null) {
//			return useDisplayName ? p.getFullDisplayName() : p.getFullName();
//		} else {
//			String separationString = useDisplayName ? " » " : "/";
//			Map<ItemGroup, Integer> parents = new HashMap();
//			int depth = 0;
//
//			while(g != null) {
//				parents.put(g, depth++);
//				if (g instanceof Item) {
//					g = ((Item)g).getParent();
//				} else {
//					g = null;
//				}
//			}
//
//			StringBuilder buf = new StringBuilder();
//			Item i = p;
//
//			while(true) {
//				if (buf.length() > 0) {
//					buf.insert(0, separationString);
//				}
//
//				buf.insert(0, useDisplayName ? i.getDisplayName() : i.getName());
//				ItemGroup gr = i.getParent();
//				Integer d = (Integer)parents.get(gr);
//				if (d != null) {
//					for(int j = d; j > 0; --j) {
//						buf.insert(0, separationString);
//						buf.insert(0, "..");
//					}
//
//					return buf.toString();
//				}
//
//				if (!(gr instanceof Item)) {
//					return null;
//				}
//
//				i = (Item)gr;
//			}
//		}
//	}
//
//	@Nullable
//	public static String getRelativeNameFrom(@Nullable Item p, @Nullable ItemGroup g) {
//		return getRelativeNameFrom(p, g, false);
//	}
//
//	@Nullable
//	public static String getRelativeDisplayNameFrom(@Nullable Item p, @Nullable ItemGroup g) {
//		return getRelativeNameFrom(p, g, true);
//	}
//
//	public static Map<Thread, StackTraceElement[]> dumpAllThreads() {
//		Map<Thread, StackTraceElement[]> sorted = new TreeMap(new ThreadSorter((SyntheticClass_1)null));
//		sorted.putAll(Thread.getAllStackTraces());
//		return sorted;
//	}
//
//	public static ThreadInfo[] getThreadInfos() {
//		ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
//		return mbean.dumpAllThreads(mbean.isObjectMonitorUsageSupported(), mbean.isSynchronizerUsageSupported());
//	}
//
//	public static ThreadGroupMap sortThreadsAndGetGroupMap(ThreadInfo[] list) {
//		ThreadGroupMap sorter = new ThreadGroupMap();
//		Arrays.sort(list, sorter);
//		return sorter;
//	}
//
//	/** @deprecated */
//	@Deprecated
//	public static boolean isMustangOrAbove() {
//		return true;
//	}
//
//	public static String dumpThreadInfo(ThreadInfo ti, ThreadGroupMap map) {
//		String grp = map.getThreadGroup(ti);
//		StringBuilder sb = new StringBuilder("\"" + ti.getThreadName() + "\" Id=" + ti.getThreadId() + " Group=" + (grp != null ? grp : "?") + " " + ti.getThreadState());
//		if (ti.getLockName() != null) {
//			sb.append(" on " + ti.getLockName());
//		}
//
//		if (ti.getLockOwnerName() != null) {
//			sb.append(" owned by \"" + ti.getLockOwnerName() + "\" Id=" + ti.getLockOwnerId());
//		}
//
//		if (ti.isSuspended()) {
//			sb.append(" (suspended)");
//		}
//
//		if (ti.isInNative()) {
//			sb.append(" (in native)");
//		}
//
//		sb.append('\n');
//		StackTraceElement[] stackTrace = ti.getStackTrace();
//
//		int var8;
//		for(int i = 0; i < stackTrace.length; ++i) {
//			StackTraceElement ste = stackTrace[i];
//			sb.append("\tat ").append(ste);
//			sb.append('\n');
//			if (i == 0 && ti.getLockInfo() != null) {
//				Thread.State ts = ti.getThreadState();
//				switch (ts) {
//					case BLOCKED:
//						sb.append("\t-  blocked on ").append(ti.getLockInfo());
//						sb.append('\n');
//						break;
//					case WAITING:
//					case TIMED_WAITING:
//						sb.append("\t-  waiting on ").append(ti.getLockInfo());
//						sb.append('\n');
//				}
//			}
//
//			MonitorInfo[] var13 = ti.getLockedMonitors();
//			var8 = var13.length;
//
//			for(int var9 = 0; var9 < var8; ++var9) {
//				MonitorInfo mi = var13[var9];
//				if (mi.getLockedStackDepth() == i) {
//					sb.append("\t-  locked ").append(mi);
//					sb.append('\n');
//				}
//			}
//		}
//
//		LockInfo[] locks = ti.getLockedSynchronizers();
//		if (locks.length > 0) {
//			sb.append("\n\tNumber of locked synchronizers = " + locks.length);
//			sb.append('\n');
//			LockInfo[] var12 = locks;
//			int var14 = locks.length;
//
//			for(var8 = 0; var8 < var14; ++var8) {
//				LockInfo li = var12[var8];
//				sb.append("\t- ").append(li);
//				sb.append('\n');
//			}
//		}
//
//		sb.append('\n');
//		return sb.toString();
//	}
//
//	public static <T> Collection<T> emptyList() {
//		return Collections.emptyList();
//	}
//
//	public static String jsStringEscape(String s) {
//		if (s == null) {
//			return null;
//		} else {
//			StringBuilder buf = new StringBuilder();
//
//			for(int i = 0; i < s.length(); ++i) {
//				char ch = s.charAt(i);
//				switch (ch) {
//					case '"':
//						buf.append("\\\"");
//						break;
//					case '\'':
//						buf.append("\\'");
//						break;
//					case '\\':
//						buf.append("\\\\");
//						break;
//					default:
//						buf.append(ch);
//				}
//			}
//
//			return buf.toString();
//		}
//	}
//
	public static String capitalize(String s) {
		return s != null && s.length() != 0 ? Character.toUpperCase(s.charAt(0)) + s.substring(1) : s;
	}
//
//	public static String getVersion() {
//		return Jenkins.VERSION;
//	}
//
//	public static String getResourcePath() {
//		return Jenkins.RESOURCE_PATH;
//	}
//
//	public static String getViewResource(Object it, String path) {
//		Class clazz = it.getClass();
//		if (it instanceof Class) {
//			clazz = (Class)it;
//		}
//
//		if (it instanceof Descriptor) {
//			clazz = ((Descriptor)it).clazz;
//		}
//
//		String buf = Stapler.getCurrentRequest().getContextPath() + Jenkins.VIEW_RESOURCE_PATH + '/' + clazz.getName().replace('.', '/').replace('$', '/') + '/' + path;
//		return buf;
//	}
//
//	public static boolean hasView(Object it, String path) throws IOException {
//		if (it == null) {
//			return false;
//		} else {
//			return Stapler.getCurrentRequest().getView(it, path) != null;
//		}
//	}
//
//	public static boolean defaultToTrue(Boolean b) {
//		return b == null ? true : b;
//	}
//
//	public static <T> T defaulted(T value, T defaultValue) {
//		return value != null ? value : defaultValue;
//	}
//
//	@NotNull
//	public static String printThrowable(@Nullable Throwable t) {
//		if (t == null) {
//			return Messages.Functions_NoExceptionDetails();
//		} else {
//			StringBuilder s = new StringBuilder();
//			doPrintStackTrace(s, t, (Throwable)null, "", new HashSet());
//			return s.toString();
//		}
//	}
//
//	private static void doPrintStackTrace(@NotNull StringBuilder s, @NotNull Throwable t, @Nullable Throwable higher, @NotNull String prefix, @NotNull Set<Throwable> encountered) {
//		if (!encountered.add(t)) {
//			s.append("<cycle to ").append(t).append(">\n");
//		} else if (Util.isOverridden(Throwable.class, t.getClass(), "printStackTrace", new Class[]{PrintWriter.class})) {
//			StringWriter sw = new StringWriter();
//			t.printStackTrace(new PrintWriter(sw));
//			s.append(sw);
//		} else {
//			Throwable lower = t.getCause();
//			if (lower != null) {
//				doPrintStackTrace(s, lower, t, prefix, encountered);
//			}
//
//			Throwable[] var6 = t.getSuppressed();
//			int var7 = var6.length;
//
//			int end;
//			for(end = 0; end < var7; ++end) {
//				Throwable suppressed = var6[end];
//				s.append(prefix).append("Also:   ");
//				doPrintStackTrace(s, suppressed, t, prefix + "\t", encountered);
//			}
//
//			if (lower != null) {
//				s.append(prefix).append("Caused: ");
//			}
//
//			String summary = t.toString();
//			if (lower != null) {
//				String suffix = ": " + lower;
//				if (summary.endsWith(suffix)) {
//					summary = summary.substring(0, summary.length() - suffix.length());
//				}
//			}
//
//			s.append(summary).append(System.lineSeparator());
//			StackTraceElement[] trace = t.getStackTrace();
//			end = trace.length;
//			if (higher != null) {
//				for(StackTraceElement[] higherTrace = higher.getStackTrace(); end > 0; --end) {
//					int higherEnd = end + higherTrace.length - trace.length;
//					if (higherEnd <= 0 || !higherTrace[higherEnd - 1].equals(trace[end - 1])) {
//						break;
//					}
//				}
//			}
//
//			for(int i = 0; i < end; ++i) {
//				s.append(prefix).append("\tat ").append(trace[i]).append(System.lineSeparator());
//			}
//
//		}
//	}
//
//	public static void printStackTrace(@Nullable Throwable t, @NotNull PrintWriter pw) {
//		pw.println(printThrowable(t).trim());
//	}
//
//	public static void printStackTrace(@Nullable Throwable t, @NotNull PrintStream ps) {
//		ps.println(printThrowable(t).trim());
//	}
//
//	public static int determineRows(String s) {
//		return s == null ? 5 : Math.max(5, LINE_END.split(s).length);
//	}
//
//	/** @deprecated */
//	@Deprecated
//	@Restricted({DoNotUse.class})
//	@RestrictedSince("2.173")
//	public static String toCCStatus(Item i) {
//		return "Unknown";
//	}
//
//	public static boolean isAnonymous() {
//		return ACL.isAnonymous2(Jenkins.getAuthentication2());
//	}
//
//	public static JellyContext getCurrentJellyContext() {
//		JellyContext context = (JellyContext)ExpressionFactory2.CURRENT_CONTEXT.get();
//
//		assert context != null;
//
//		return context;
//	}
//
//	public static String runScript(Script script) throws JellyTagException {
//		StringWriter out = new StringWriter();
//		script.run(getCurrentJellyContext(), XMLOutput.createXMLOutput(out));
//		return out.toString();
//	}
//
//	public static <T> List<T> subList(List<T> base, int maxSize) {
//		return maxSize < base.size() ? base.subList(0, maxSize) : base;
//	}
//
//	public static String joinPath(String... components) {
//		StringBuilder buf = new StringBuilder();
//		String[] var2 = components;
//		int var3 = components.length;
//
//		for(int var4 = 0; var4 < var3; ++var4) {
//			String s = var2[var4];
//			if (s.length() != 0) {
//				if (buf.length() > 0) {
//					if (buf.charAt(buf.length() - 1) != '/') {
//						buf.append('/');
//					}
//
//					if (s.charAt(0) == '/') {
//						s = s.substring(1);
//					}
//				}
//
//				buf.append(s);
//			}
//		}
//
//		return buf.toString();
//	}
//
//	@Nullable
//	public static String getActionUrl(String itUrl, Action action) {
//		String urlName = action.getUrlName();
//		if (urlName == null) {
//			return null;
//		} else {
//			try {
//				if ((new URI(urlName)).isAbsolute()) {
//					return urlName;
//				}
//			} catch (URISyntaxException var4) {
//				Logger.getLogger(Functions.class.getName()).log(Level.WARNING, "Failed to parse URL for {0}: {1}", new Object[]{action, var4});
//				return null;
//			}
//
//			return urlName.startsWith("/") ? joinPath(Stapler.getCurrentRequest().getContextPath(), urlName) : joinPath(Stapler.getCurrentRequest().getContextPath() + '/' + itUrl, urlName);
//		}
//	}
//
//	public static String toEmailSafeString(String projectName) {
//		StringBuilder buf = new StringBuilder(projectName.length());
//
//		for(int i = 0; i < projectName.length(); ++i) {
//			char ch = projectName.charAt(i);
//			if (('a' > ch || ch > 'z') && ('A' > ch || ch > 'Z') && ('0' > ch || ch > '9') && "-_.".indexOf(ch) < 0) {
//				buf.append('_');
//			} else {
//				buf.append(ch);
//			}
//		}
//
//		return String.valueOf(buf);
//	}
//
//	public String getServerName() {
//		String url = Jenkins.get().getRootUrl();
//
//		try {
//			if (url != null) {
//				String host = (new URL(url)).getHost();
//				if (host != null) {
//					return host;
//				}
//			}
//		} catch (MalformedURLException var3) {
//		}
//
//		return Stapler.getCurrentRequest().getServerName();
//	}
//
//	/** @deprecated */
//	@Deprecated
//	public String getCheckUrl(String userDefined, Object descriptor, String field) {
//		if (userDefined == null && field != null) {
//			if (descriptor instanceof Descriptor) {
//				Descriptor d = (Descriptor)descriptor;
//				return d.getCheckUrl(field);
//			} else {
//				return null;
//			}
//		} else {
//			return userDefined;
//		}
//	}
//
//	public void calcCheckUrl(Map attributes, String userDefined, Object descriptor, String field) {
//		if (userDefined == null && field != null) {
//			if (descriptor instanceof Descriptor) {
//				Descriptor d = (Descriptor)descriptor;
//				FormValidation.CheckMethod m = d.getCheckMethod(field);
//				attributes.put("checkUrl", m.toStemUrl());
//				attributes.put("checkDependsOn", m.getDependsOn());
//			}
//
//		}
//	}
//
//	public boolean hyperlinkMatchesCurrentPage(String href) throws UnsupportedEncodingException {
//		String url = Stapler.getCurrentRequest().getRequestURL().toString();
//		if (href != null && href.length() > 1) {
//			url = URLDecoder.decode(url, "UTF-8");
//			href = URLDecoder.decode(href, "UTF-8");
//			if (url.endsWith("/")) {
//				url = url.substring(0, url.length() - 1);
//			}
//
//			if (href.endsWith("/")) {
//				href = href.substring(0, href.length() - 1);
//			}
//
//			return url.endsWith(href);
//		} else {
//			return ".".equals(href) && url.endsWith("/");
//		}
//	}
//
//	public <T> List<T> singletonList(T t) {
//		return Collections.singletonList(t);
//	}
//
//	public static List<PageDecorator> getPageDecorators() {
//		return (List)(Jenkins.getInstanceOrNull() == null ? Collections.emptyList() : PageDecorator.all());
//	}
//
//	public static SimplePageDecorator getSimplePageDecorator() {
//		return SimplePageDecorator.first();
//	}
//
//	public static List<SimplePageDecorator> getSimplePageDecorators() {
//		return SimplePageDecorator.all();
//	}
//
//	public static List<Descriptor<Cloud>> getCloudDescriptors() {
//		return Cloud.all();
//	}
//
//	public String prepend(String prefix, String body) {
//		return body != null && body.length() > 0 ? prefix + body : body;
//	}
//
//	public static List<Descriptor<CrumbIssuer>> getCrumbIssuerDescriptors() {
//		return CrumbIssuer.all();
//	}
//
//	public static String getCrumb(StaplerRequest req) {
//		Jenkins h = Jenkins.getInstanceOrNull();
//		CrumbIssuer issuer = h != null ? h.getCrumbIssuer() : null;
//		return issuer != null ? issuer.getCrumb(req) : "";
//	}
//
//	public static String getCrumbRequestField() {
//		Jenkins h = Jenkins.getInstanceOrNull();
//		CrumbIssuer issuer = h != null ? h.getCrumbIssuer() : null;
//		return issuer != null ? issuer.getDescriptor().getCrumbRequestField() : "";
//	}
//
//	public static Date getCurrentTime() {
//		return new Date();
//	}
//
//	public static Locale getCurrentLocale() {
//		Locale locale = null;
//		StaplerRequest req = Stapler.getCurrentRequest();
//		if (req != null) {
//			locale = req.getLocale();
//		}
//
//		if (locale == null) {
//			locale = Locale.getDefault();
//		}
//
//		return locale;
//	}
//
//	public static String generateConsoleAnnotationScriptAndStylesheet() {
//		String cp = Stapler.getCurrentRequest().getContextPath() + Jenkins.RESOURCE_PATH;
//		StringBuilder buf = new StringBuilder();
//		Iterator var2 = ConsoleAnnotatorFactory.all().iterator();
//
//		String path;
//		while(var2.hasNext()) {
//			ConsoleAnnotatorFactory f = (ConsoleAnnotatorFactory)var2.next();
//			path = cp + "/extensionList/" + ConsoleAnnotatorFactory.class.getName() + "/" + f.getClass().getName();
//			if (f.hasScript()) {
//				buf.append("<script src='").append(path).append("/script.js'></script>");
//			}
//
//			if (f.hasStylesheet()) {
//				buf.append("<link rel='stylesheet' type='text/css' href='").append(path).append("/style.css' />");
//			}
//		}
//
//		var2 = ConsoleAnnotationDescriptor.all().iterator();
//
//		while(var2.hasNext()) {
//			ConsoleAnnotationDescriptor d = (ConsoleAnnotationDescriptor)var2.next();
//			path = cp + "/descriptor/" + d.clazz.getName();
//			if (d.hasScript()) {
//				buf.append("<script src='").append(path).append("/script.js'></script>");
//			}
//
//			if (d.hasStylesheet()) {
//				buf.append("<link rel='stylesheet' type='text/css' href='").append(path).append("/style.css' />");
//			}
//		}
//
//		return buf.toString();
//	}
//
//	public List<String> getLoggerNames() {
//		while(true) {
//			try {
//				List<String> r = new ArrayList();
//				Enumeration<String> e = LogManager.getLogManager().getLoggerNames();
//
//				while(e.hasMoreElements()) {
//					r.add(e.nextElement());
//				}
//
//				return r;
//			} catch (ConcurrentModificationException var3) {
//			}
//		}
//	}
//
//	public String getPasswordValue(Object o) {
//		if (o == null) {
//			return null;
//		} else if (o.equals("<DEFAULT>")) {
//			return o.toString();
//		} else {
//			StaplerRequest req = Stapler.getCurrentRequest();
//			if ((o instanceof Secret || Secret.BLANK_NONSECRET_PASSWORD_FIELDS_WITHOUT_ITEM_CONFIGURE) && req != null) {
//				Item item = (Item)req.findAncestorObject(Item.class);
//				if (item != null && !item.hasPermission(Item.CONFIGURE)) {
//					return "********";
//				}
//
//				Computer computer = (Computer)req.findAncestorObject(Computer.class);
//				if (computer != null && !computer.hasPermission(Computer.CONFIGURE)) {
//					return "********";
//				}
//			}
//
//			if (o instanceof Secret) {
//				return ((Secret)o).getEncryptedValue();
//			} else {
//				if (req != null && (Boolean.getBoolean("hudson.hpi.run") || Boolean.getBoolean("hudson.Main.development"))) {
//					LOGGER.log(Level.WARNING, () -> {
//						return "<f:password/> form control in " + this.getJellyViewsInformationForCurrentRequest() + " is not backed by hudson.util.Secret. Learn more: https://www.jenkins.io/redirect/hudson.util.Secret";
//					});
//				}
//
//				return !Secret.AUTO_ENCRYPT_PASSWORD_CONTROL ? o.toString() : Secret.fromString(o.toString()).getEncryptedValue();
//			}
//		}
//	}
//
//	private String getJellyViewsInformationForCurrentRequest() {
//		Thread thread = Thread.currentThread();
//		String threadName = thread.getName();
//		String views = (String)Arrays.stream(threadName.split(" ")).filter((part) -> {
//			int slash = part.lastIndexOf("/");
//			int firstPeriod = part.indexOf(".");
//			return slash > 0 && firstPeriod > 0 && slash < firstPeriod;
//		}).collect(Collectors.joining(" "));
//		return StringUtils.isBlank(views) ? threadName : views;
//	}
//
//	public List filterDescriptors(Object context, Iterable descriptors) {
//		return DescriptorVisibilityFilter.apply(context, descriptors);
//	}
//
//	public static boolean getIsUnitTest() {
//		return Main.isUnitTest;
//	}
//
//	public static boolean isArtifactsPermissionEnabled() {
//		return SystemProperties.getBoolean("hudson.security.ArtifactsPermission");
//	}
//
//	public static boolean isWipeOutPermissionEnabled() {
//		return SystemProperties.getBoolean("hudson.security.WipeOutPermission");
//	}
//
//	public static String createRenderOnDemandProxy(JellyContext context, String attributesToCapture) {
//		return Stapler.getCurrentRequest().createJavaScriptProxy(new RenderOnDemandClosure(context, attributesToCapture));
//	}
//
//	public static String getCurrentDescriptorByNameUrl() {
//		return Descriptor.getCurrentDescriptorByNameUrl();
//	}
//
//	public static String setCurrentDescriptorByNameUrl(String value) {
//		String o = getCurrentDescriptorByNameUrl();
//		Stapler.getCurrentRequest().setAttribute("currentDescriptorByNameUrl", value);
//		return o;
//	}
//
//	public static void restoreCurrentDescriptorByNameUrl(String old) {
//		Stapler.getCurrentRequest().setAttribute("currentDescriptorByNameUrl", old);
//	}
//
//	public static List<String> getRequestHeaders(String name) {
//		List<String> r = new ArrayList();
//		Enumeration e = Stapler.getCurrentRequest().getHeaders(name);
//
//		while(e.hasMoreElements()) {
//			r.add(e.nextElement().toString());
//		}
//
//		return r;
//	}
//
//	public static Object rawHtml(Object o) {
//		return o == null ? null : new RawHtmlArgument(o);
//	}
//
//	public static ArrayList<CLICommand> getCLICommands() {
//		ArrayList<CLICommand> all = new ArrayList(CLICommand.all());
//		all.sort(Comparator.comparing(CLICommand::getName));
//		return all;
//	}
//
//	public static String getAvatar(User user, String avatarSize) {
//		return UserAvatarResolver.resolve(user, avatarSize);
//	}
//
//	/** @deprecated */
//	@Deprecated
//	public String getUserAvatar(User user, String avatarSize) {
//		return getAvatar(user, avatarSize);
//	}
//
//	public static String humanReadableByteSize(long size) {
//		String measure = "B";
//		if (size < 1024L) {
//			return size + " " + measure;
//		} else {
//			double number = (double)size;
//			if (number >= 1024.0) {
//				number /= 1024.0;
//				measure = "KB";
//				if (number >= 1024.0) {
//					number /= 1024.0;
//					measure = "MB";
//					if (number >= 1024.0) {
//						number /= 1024.0;
//						measure = "GB";
//					}
//				}
//			}
//
//			DecimalFormat format = new DecimalFormat("#0.00");
//			return format.format(number) + " " + measure;
//		}
//	}
//
//	public static String breakableString(final String plain) {
//		return plain == null ? null : plain.replaceAll("([\\p{Punct}&&[^;]]+\\w)", "<wbr>$1").replaceAll("([^\\p{Punct}\\s-]{20})(?=[^\\p{Punct}\\s-]{10})", "$1<wbr>");
//	}
//
//	public static void advertiseHeaders(HttpServletResponse rsp) {
//		Jenkins j = Jenkins.getInstanceOrNull();
//		if (j != null) {
//			rsp.setHeader("X-Hudson", "1.395");
//			rsp.setHeader("X-Jenkins", Jenkins.VERSION);
//			rsp.setHeader("X-Jenkins-Session", Jenkins.SESSION_HASH);
//		}
//
//	}
//
//	@Restricted({NoExternalUse.class})
//	public static boolean isContextMenuVisible(Action a) {
//		return a instanceof ModelObjectWithContextMenu.ContextMenuVisibility ? ((ModelObjectWithContextMenu.ContextMenuVisibility)a).isVisible() : true;
//	}
//
//	private static class ThreadSorter extends ThreadSorterBase implements Comparator<Thread>, Serializable {
//		private static final long serialVersionUID = 5053631350439192685L;
//
//		private ThreadSorter() {
//		}
//
//		public int compare(Thread a, Thread b) {
//			int result = this.compare(a.getId(), b.getId());
//			if (result == 0) {
//				result = a.getName().compareToIgnoreCase(b.getName());
//			}
//
//			return result;
//		}
//	}
//
//	public static class ThreadGroupMap extends ThreadSorterBase implements Comparator<ThreadInfo>, Serializable {
//		private static final long serialVersionUID = 7803975728695308444L;
//
//		public ThreadGroupMap() {
//		}
//
//		public String getThreadGroup(ThreadInfo ti) {
//			return (String)this.map.get(ti.getThreadId());
//		}
//
//		public int compare(ThreadInfo a, ThreadInfo b) {
//			int result = this.compare(a.getThreadId(), b.getThreadId());
//			if (result == 0) {
//				result = a.getThreadName().compareToIgnoreCase(b.getThreadName());
//			}
//
//			return result;
//		}
//	}
//
//	private static class ThreadSorterBase {
//		protected Map<Long, String> map = new HashMap();
//
//		ThreadSorterBase() {
//			ThreadGroup tg;
//			for(tg = Thread.currentThread().getThreadGroup(); tg.getParent() != null; tg = tg.getParent()) {
//			}
//
//			Thread[] threads = new Thread[tg.activeCount() * 2];
//			int threadsLen = tg.enumerate(threads, true);
//
//			for(int i = 0; i < threadsLen; ++i) {
//				ThreadGroup group = threads[i].getThreadGroup();
//				this.map.put(threads[i].getId(), group != null ? group.getName() : null);
//			}
//
//		}
//
//		protected int compare(long idA, long idB) {
//			String tga = (String)this.map.get(idA);
//			String tgb = (String)this.map.get(idB);
//			int result = (tga != null ? -1 : 0) + (tgb != null ? 1 : 0);
//			if (result == 0 && tga != null) {
//				result = tga.compareToIgnoreCase(tgb);
//			}
//
//			return result;
//		}
//	}
//
//	private static class Tag implements Comparable<Tag> {
//		double ordinal;
//		String hierarchy;
//		Descriptor d;
//
//		Tag(double ordinal, Descriptor d) {
//			this.ordinal = ordinal;
//			this.d = d;
//			this.hierarchy = this.buildSuperclassHierarchy(d.clazz, new StringBuilder()).toString();
//		}
//
//		private StringBuilder buildSuperclassHierarchy(Class c, StringBuilder buf) {
//			Class sc = c.getSuperclass();
//			if (sc != null) {
//				this.buildSuperclassHierarchy(sc, buf).append(':');
//			}
//
//			return buf.append(c.getName());
//		}
//
//		public int compareTo(Tag that) {
//			int r = Double.compare(that.ordinal, this.ordinal);
//			return r != 0 ? r : this.hierarchy.compareTo(that.hierarchy);
//		}
//	}
//
//	public static final class RunUrl {
//		private final String head;
//		private final String base;
//		private final String rest;
//		private final Run run;
//
//		public RunUrl(Run run, String head, String base, String rest) {
//			this.run = run;
//			this.head = head;
//			this.base = base;
//			this.rest = rest;
//		}
//
//		public String getBaseUrl() {
//			return this.base;
//		}
//
//		public String getNextBuildUrl() {
//			return this.getUrl(this.run.getNextBuild());
//		}
//
//		public String getPreviousBuildUrl() {
//			return this.getUrl(this.run.getPreviousBuild());
//		}
//
//		private String getUrl(Run n) {
//			return n == null ? null : this.head + n.getNumber() + this.rest;
//		}
//	}

}
