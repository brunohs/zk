/* Configuration.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		Sun Mar 26 16:06:56     2006, Created by tomyeh
}}IS_NOTE

Copyright (C) 2006 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/
package org.zkoss.zk.ui.util;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import org.zkoss.lang.Classes;
import org.zkoss.lang.PotentialDeadLockException;
import org.zkoss.lang.Exceptions;
import org.zkoss.util.WaitLock;
import org.zkoss.util.logging.Log;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.WebApp;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Richlet;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventThreadInit;
import org.zkoss.zk.ui.event.EventThreadCleanup;
import org.zkoss.zk.ui.event.EventThreadSuspend;
import org.zkoss.zk.ui.event.EventThreadResume;
import org.zkoss.zk.ui.sys.WebAppCtrl;
import org.zkoss.zk.ui.sys.UiEngine;
import org.zkoss.zk.ui.sys.DesktopCacheProvider;
import org.zkoss.zk.ui.sys.LocaleProvider;
import org.zkoss.zk.ui.sys.TimeZoneProvider;
import org.zkoss.zk.ui.sys.UiFactory;
import org.zkoss.zk.ui.impl.RichletConfigImpl;

/**
 * The ZK configuration.
 *
 * <p>To retrieve the current configuration, use
 * {@link org.zkoss.zk.ui.WebApp#getConfiguration}.
 *
 * <p>Note: A {@link Configuration} instance can be assigned to at most one
 * {@link WebApp} instance.
 *
 * @author tomyeh
 */
public class Configuration {
	private static final Log log = Log.lookup(Configuration.class);

	private WebApp _wapp;
	private final List
		_evtInits = new LinkedList(), _evtCleans = new LinkedList(),
		_evtSusps = new LinkedList(), _evtResus = new LinkedList(),
		_appInits = new LinkedList(), _appCleans = new LinkedList(),
		_sessInits = new LinkedList(), _sessCleans = new LinkedList(),
		_dtInits = new LinkedList(), _dtCleans = new LinkedList(),
		_execInits = new LinkedList(), _execCleans = new LinkedList(),
		_uriIntcps = new LinkedList();
	private final Map _prefs  = Collections.synchronizedMap(new HashMap()),
		_richlets = new HashMap();
	/** List(ErrorPage). */
	private final List _errpgs = new LinkedList();
	private Monitor _monitor;
	private String _timeoutUri;
	private final List _themeUris = new LinkedList();
	private transient String[] _roThemeUris = new String[0];
	private Class _wappcls, _uiengcls, _dcpcls, _uiftycls, _tzpcls, _lpcls;
	private Integer _dtTimeout, _dtMax, _sessTimeout, _evtThdMax;
	private Integer _maxUploadSize = new Integer(5120);
	private int _promptDelay = 900, _tooltipDelay = 800;
	private String _charset = "UTF-8";
	/** A set of the language name whose theme is disabled. */
	private Set _disabledDefThemes;

	/** Contructor.
	 */
	public Configuration() {
	}

	/** Returns the Web application that this configuration belongs to,
	 * or null if it is not associated yet.
	 */
	public WebApp getWebApp() {
		return _wapp;
	}
	/** Associates it with a web application.
	 */
	public void setWebApp(WebApp wapp) {
		_wapp = wapp;
	}

	/** Adds a listener class.
	 */
	public void addListener(Class klass) throws Exception {
		boolean added = false;
		if (Monitor.class.isAssignableFrom(klass)) {
			if (_monitor != null)
				throw new UiException("Monitor listener can be assigned only once");
			_monitor = (Monitor)klass.newInstance();
			added = true;
		}

		if (EventThreadInit.class.isAssignableFrom(klass)) {
			synchronized (_evtInits) {
				_evtInits.add(klass);
			}
			added = true;
		}
		if (EventThreadCleanup.class.isAssignableFrom(klass)) {
			synchronized (_evtCleans) {
				_evtCleans.add(klass);
			}
			added = true;
		}
		if (EventThreadSuspend.class.isAssignableFrom(klass)) {
			synchronized (_evtSusps) {
				_evtSusps.add(klass);
			}
			added = true;
		}
		if (EventThreadResume.class.isAssignableFrom(klass)) {
			synchronized (_evtResus) {
				_evtResus.add(klass);
			}
			added = true;
		}

		if (WebAppInit.class.isAssignableFrom(klass)) {
			synchronized (_appInits) {
				_appInits.add(klass);
			}
			added = true;
		}
		if (WebAppCleanup.class.isAssignableFrom(klass)) {
			synchronized (_appCleans) {
				_appCleans.add(klass);
			}
			added = true;
		}

		if (SessionInit.class.isAssignableFrom(klass)) {
			synchronized (_sessInits) {
				_sessInits.add(klass);
			}
			added = true;
		}
		if (SessionCleanup.class.isAssignableFrom(klass)) {
			synchronized (_sessCleans) {
				_sessCleans.add(klass);
			}
			added = true;
		}

		if (DesktopInit.class.isAssignableFrom(klass)) {
			synchronized (_dtInits) {
				_dtInits.add(klass);
			}
			added = true;
		}
		if (DesktopCleanup.class.isAssignableFrom(klass)) {
			synchronized (_dtCleans) {
				_dtCleans.add(klass);
			}
			added = true;
		}

		if (ExecutionInit.class.isAssignableFrom(klass)) {
			synchronized (_execInits) {
				_execInits.add(klass);
			}
			added = true;
		}
		if (ExecutionCleanup.class.isAssignableFrom(klass)) {
			synchronized (_execCleans) {
				_execCleans.add(klass);
			}
			added = true;
		}
		if (URIInterceptor.class.isAssignableFrom(klass)) {
			synchronized (_uriIntcps) {
				_uriIntcps.add(klass);
			}
			added = true;
		}

		if (!added)
			throw new UiException("Unknown listener: "+klass);
	}
	/** Removes a listener class.
	 */
	public void removeListener(Class klass) {
		synchronized (_evtInits) {
			_evtInits.remove(klass);
		}
		synchronized (_evtCleans) {
			_evtCleans.remove(klass);
		}
		synchronized (_evtSusps) {
			_evtSusps.remove(klass);
		}
		synchronized (_evtResus) {
			_evtResus.remove(klass);
		}

		synchronized (_appInits) {
			_appInits.remove(klass);
		}
		synchronized (_appCleans) {
			_appCleans.remove(klass);
		}
		synchronized (_sessInits) {
			_sessInits.remove(klass);
		}
		synchronized (_sessCleans) {
			_sessCleans.remove(klass);
		}
		synchronized (_dtInits) {
			_dtInits.remove(klass);
		}
		synchronized (_dtCleans) {
			_dtCleans.remove(klass);
		}
		synchronized (_execInits) {
			_execInits.remove(klass);
		}
		synchronized (_execCleans) {
			_execCleans.remove(klass);
		}
		synchronized (_uriIntcps) {
			_uriIntcps.remove(klass);
		}
	}

	/** Contructs a list of {@link EventThreadInit} instances and invokes
	 * {@link EventThreadInit#prepare} for
	 * each relevant listener registered by {@link #addListener}.
	 *
	 * <p>It is called by UiEngine before starting an event processing
	 * thread.
	 *
	 * @exception UiException to prevent a thread from being processed
	 * if {@link EventThreadInit#prepare} throws an exception
	 * @return a list of {@link EventThreadInit} instances that are
	 * constructed in this method (and their {@link EventThreadInit#prepare}
	 * are called successfully).
	 */
	public List newEventThreadInits(Component comp, Event evt)
	throws UiException {
		if (_evtInits.isEmpty()) return Collections.EMPTY_LIST;
			//it is OK to test LinkedList.isEmpty without synchronized

		final List inits = new LinkedList();
		synchronized (_evtInits) {
			for (Iterator it = _evtInits.iterator(); it.hasNext();) {
				final Class klass = (Class)it.next();
				try {
					final EventThreadInit init =
						(EventThreadInit)klass.newInstance();
					init.prepare(comp, evt);
					inits.add(init);
				} catch (Throwable ex) {
					throw UiException.Aide.wrap(ex);
					//Don't intercept; to prevent the event being processed
				}
			}
		}
		return inits;
	}
	/** Invokes {@link EventThreadInit#init} for each instance returned
	 * by {@link #newEventThreadInits}.
	 *
	 * @param inits a list of {@link EventThreadInit} instances returned from
	 * {@link #newEventThreadInits}, or null if no instance at all.
	 * @param comp the component which the event is targeting
	 * @param evt the event to process
	 * @exception UiException to prevent a thread from being processed
	 * if {@link EventThreadInit#prepare} throws an exception
	 */
	public void invokeEventThreadInits(List inits, Component comp, Event evt) 
	throws UiException {
		if (inits == null || inits.isEmpty()) return;

		for (Iterator it = inits.iterator(); it.hasNext();) {
			final EventThreadInit fn = (EventThreadInit)it.next();
			try {
				fn.init(comp, evt);
			} catch (Throwable ex) {
				throw UiException.Aide.wrap(ex);
				//Don't intercept; to prevent the event being processed
			}
		}
	}
	/** Invokes {@link EventThreadCleanup#cleanup} for each relevant
	 * listener registered by {@link #addListener}.
	 *
	 * <p>An instance of {@link EventThreadCleanup} is constructed first,
	 * and then invoke {@link EventThreadCleanup#cleanup}.
	 *
	 * <p>It never throws an exception but logs and adds it to the errs argument,
	 * if not null.
	 *
	 * @param comp the component which the event is targeting
	 * @param evt the event to process
	 * @param errs a list of exceptions (java.lang.Throwable) if any exception
	 * occured before this method is called, or null if no exeption at all.
	 * Note: you can manipulate the list directly to add or clean up exceptions.
	 * For example, if exceptions are fixed correctly, you can call errs.clear()
	 * such that no error message will be displayed at the client.
	 */
	public List newEventThreadCleanups(Component comp, Event evt, List errs) {
		if (_evtCleans.isEmpty()) return Collections.EMPTY_LIST;
			//it is OK to test LinkedList.isEmpty without synchronized

		final List cleanups = new LinkedList();
		synchronized (_evtCleans) {
			for (Iterator it = _evtCleans.iterator(); it.hasNext();) {
				final Class klass = (Class)it.next();
				try {
					final EventThreadCleanup cleanup =
						(EventThreadCleanup)klass.newInstance();
					cleanup.cleanup(comp, evt, errs);
					cleanups.add(cleanup);
				} catch (Throwable t) {
					if (errs != null) errs.add(t);
					log.error("Failed to invoke "+klass, t);
				}
			}
		}
		return cleanups;
	}
	/** Invoke {@link EventThreadCleanup#complete} for each instance returned by
	 * {@link #newEventThreadCleanups}.
	 *
	 * <p>It never throws an exception but logs and adds it to the errs argument,
	 * if not null.
	 *
	 * @param cleanups a list of {@link EventThreadCleanup} instances returned from
	 * {@link #newEventThreadCleanups}, or null if no instance at all.
	 * @param errs used to hold the exceptions that are thrown by
	 * {@link EventThreadCleanup#complete}.
	 * If null, all exceptions are ignored (but logged).
	 */
	public void invokeEventThreadCompletes(List cleanups, Component comp, Event evt,
	List errs) {
		if (cleanups == null || cleanups.isEmpty()) return;

		for (Iterator it = cleanups.iterator(); it.hasNext();) {
			final EventThreadCleanup fn = (EventThreadCleanup)it.next();
			try {
				fn.complete(comp, evt);
			} catch (Throwable ex) {
				if (errs != null) errs.add(ex);
				log.error("Failed to invoke "+fn, ex);
			}
		}
	}

	/** Constructs a list of {@link EventThreadSuspend} instances and invokes
	 * {@link EventThreadSuspend#beforeSuspend} for each relevant
	 * listener registered by {@link #addListener}.
	 * Caller shall execute in the event processing thread.
	 *
	 * @param comp the component which the event is targeting
	 * @param evt the event to process
	 * @param obj which object that {@link Executions#wait}
	 * is called with.
	 * @exception UiException to prevent a thread from suspending
	 */
	public List newEventThreadSuspends(Component comp, Event evt, Object obj) {
		if (_evtSusps.isEmpty()) return Collections.EMPTY_LIST;
			//it is OK to test LinkedList.isEmpty without synchronized

		final List suspends = new LinkedList();
		synchronized (_evtSusps) {
			for (Iterator it = _evtSusps.iterator(); it.hasNext();) {
				final Class klass = (Class)it.next();
				try {
					final EventThreadSuspend suspend =
						(EventThreadSuspend)klass.newInstance();
					suspend.beforeSuspend(comp, evt, obj);
					suspends.add(suspend);
				} catch (Throwable ex) {
					throw UiException.Aide.wrap(ex);
					//Don't intercept; to prevent the event being suspended
				}
			}
		}
		return suspends;
	}
	/** Invokes {@link EventThreadSuspend#afterSuspend} for each relevant
	 * listener registered by {@link #addListener}.
	 * Unlike {@link #invokeEventThreadSuspends}, caller shall execute in
	 * the main thread (aka, servlet thread).
	 *
	 * <p>Unlike {@link #invokeEventThreadSuspends}, exceptions are logged
	 * and ignored.
	 *
	 * @param suspends a list of {@link EventThreadSuspend} instances returned
	 * from {@link #newEventThreadSuspends}, or null if no instance at all.
	 * @param comp the component which the event is targeting
	 * @param evt the event to process
	 */
	public void invokeEventThreadSuspends(List suspends, Component comp, Event evt)
	throws UiException {
		if (suspends == null || suspends.isEmpty()) return;

		for (Iterator it = suspends.iterator(); it.hasNext();) {
			final EventThreadSuspend fn = (EventThreadSuspend)it.next();
			try {
				fn.afterSuspend(comp, evt);
			} catch (Throwable ex) {
				log.error("Failed to invoke "+fn+" after suspended", ex);
			}
		}
	}

	/** Contructs a list of {@link EventThreadResume} instances and invokes
	 * {@link EventThreadResume#beforeResume} for each relevant
	 * listener registered by {@link #addListener}.
	 *
	 * <p>It is called by UiEngine when resuming a suspended event thread.
	 * Notice: it executes in the main thread (i.e., the servlet thread).
	 *
	 * @param comp the component which the event is targeting
	 * @param evt the event to process
	 * @exception UiException to prevent a thread from being resumed
	 * if {@link EventThreadResume#beforeResume} throws an exception
	 * @return a list of {@link EventThreadResume} instances that are constructed
	 * in this method (and their {@link EventThreadResume#beforeResume}
	 * are called successfully).
	 */
	public List newEventThreadResumes(Component comp, Event evt)
	throws UiException {
		if (_evtResus.isEmpty()) return Collections.EMPTY_LIST;
			//it is OK to test LinkedList.isEmpty without synchronized

		final List resumes = new LinkedList();
		synchronized (_evtResus) {
			for (Iterator it = _evtResus.iterator(); it.hasNext();) {
				final Class klass = (Class)it.next();
				try {
					final EventThreadResume resume =
						(EventThreadResume)klass.newInstance();
					resume.beforeResume(comp, evt);
					resumes.add(resume);
				} catch (Throwable ex) {
					throw UiException.Aide.wrap(ex);
					//Don't intercept; to prevent the event being resumed
				}
			}
		}
		return resumes;
	}
	/** Invokes {@link EventThreadResume#afterResume} for each instance returned
	 * by {@link #newEventThreadResumes}.
	 *
	 * <p>It never throws an exception but logs and adds it to the errs argument,
	 * if not null.
	 *
	 * @param resumes a list of {@link EventThreadResume} instances returned from
	 * {@link #newEventThreadResumes}, or null if no instance at all.
	 * @param comp the component which the event is targeting
	 * @param evt the event to process
	 * @param errs used to hold the exceptions that are thrown by
	 * {@link EventThreadResume#afterResume}.
	 * If null, all exceptions are ignored (but logged)
	 */
	public void invokeEventThreadResumes(List resumes, Component comp, Event evt,
	List errs) {
		if (resumes == null || resumes.isEmpty()) return;

		for (Iterator it = resumes.iterator(); it.hasNext();) {
			final EventThreadResume fn = (EventThreadResume)it.next();
			try {
				fn.afterResume(comp, evt);
			} catch (Throwable ex) {
				if (errs != null) errs.add(ex);
				log.error("Failed to invoke "+fn+" after resumed", ex);
			}
		}
	}
	/** Invokes {@link EventThreadResume#abortResume} for each relevant
	 * listener registered by {@link #addListener}.
	 *
	 * <p>An instance of {@link EventThreadResume} is constructed first,
	 * and then invoke {@link EventThreadResume#abortResume}.
	 *
	 * <p>It never throws an exception but logging.
	 *
	 * @param comp the component which the event is targeting
	 * @param evt the event to process
	 */
	public void invokeEventThreadResumeAborts(Component comp, Event evt) {
		if (_evtResus.isEmpty()) return;
			//it is OK to test LinkedList.isEmpty without synchronized

		synchronized (_evtResus) {
			for (Iterator it = _evtResus.iterator(); it.hasNext();) {
				final Class klass = (Class)it.next();
				try {
					((EventThreadResume)klass.newInstance())
						.abortResume(comp, evt);
				} catch (Throwable ex) {
					log.error("Failed to invoke "+klass+" for aborting", ex);
				}
			}
		}
	}

	/** Invokes {@link WebAppInit#init} for each relevant
	 * listener registered by {@link #addListener}.
	 *
	 * <p>An instance of {@link WebAppInit} is constructed first,
	 * and then invoke {@link WebAppInit#init}.
	 *
	 * <p>Unlike {@link #invokeWebAppInits}, it doesn't throw any exceptions.
	 * Rather, it only logs them.
	 */
	public void invokeWebAppInits() throws UiException {
		if (_appInits.isEmpty()) return;
			//it is OK to test LinkedList.isEmpty without synchronized

		synchronized (_appInits) {
			for (Iterator it = _appInits.iterator(); it.hasNext();) {
				final Class klass = (Class)it.next();
				try {
					((WebAppInit)klass.newInstance()).init(_wapp);
				} catch (Throwable ex) {
					log.error("Failed to invoke "+klass, ex);
				}
			}
		}
	}
	/** Invokes {@link WebAppCleanup#cleanup} for each relevant
	 * listener registered by {@link #addListener}.
	 *
	 * <p>An instance of {@link WebAppCleanup} is constructed first,
	 * and then invoke {@link WebAppCleanup#cleanup}.
	 *
	 * <p>It never throws an exception.
	 */
	public void invokeWebAppCleanups() {
		if (_appCleans.isEmpty()) return;
			//it is OK to test LinkedList.isEmpty without synchronized

		synchronized (_appCleans) {
			for (Iterator it = _appCleans.iterator(); it.hasNext();) {
				final Class klass = (Class)it.next();
				try {
					((WebAppCleanup)klass.newInstance()).cleanup(_wapp);
				} catch (Throwable ex) {
					log.error("Failed to invoke "+klass, ex);
				}
			}
		}
	}

	/** Invokes {@link SessionInit#init} for each relevant
	 * listener registered by {@link #addListener}.
	 *
	 * <p>An instance of {@link SessionInit} is constructed first,
	 * and then invoke {@link SessionInit#init}.
	 *
	 * @param sess the session that is created
	 * @exception UiException to prevent a session from being created
	 */
	public void invokeSessionInits(Session sess)
	throws UiException {
		if (_sessInits.isEmpty()) return;
			//it is OK to test LinkedList.isEmpty without synchronized

		synchronized (_sessInits) {
			for (Iterator it = _sessInits.iterator(); it.hasNext();) {
				final Class klass = (Class)it.next();
				try {
					((SessionInit)klass.newInstance()).init(sess);
				} catch (Throwable ex) {
					throw UiException.Aide.wrap(ex);
					//Don't intercept; to prevent the creation of a session
				}
			}
		}
	}
	/** Invokes {@link SessionCleanup#cleanup} for each relevant
	 * listener registered by {@link #addListener}.
	 *
	 * <p>An instance of {@link SessionCleanup} is constructed first,
	 * and then invoke {@link SessionCleanup#cleanup}.
	 *
	 * <p>It never throws an exception.
	 *
	 * @param sess the session that is being destroyed
	 */
	public void invokeSessionCleanups(Session sess) {
		if (_sessCleans.isEmpty()) return;
			//it is OK to test LinkedList.isEmpty without synchronized

		synchronized (_sessCleans) {
			for (Iterator it = _sessCleans.iterator(); it.hasNext();) {
				final Class klass = (Class)it.next();
				try {
					((SessionCleanup)klass.newInstance()).cleanup(sess);
				} catch (Throwable ex) {
					log.error("Failed to invoke "+klass, ex);
				}
			}
		}
	}

	/** Invokes {@link DesktopInit#init} for each relevant
	 * listener registered by {@link #addListener}.
	 *
	 * <p>An instance of {@link DesktopInit} is constructed first,
	 * and then invoke {@link DesktopInit#init}.
	 *
	 * @param desktop the desktop that is created
	 * @exception UiException to prevent a desktop from being created
	 */
	public void invokeDesktopInits(Desktop desktop)
	throws UiException {
		if (_dtInits.isEmpty()) return;
			//it is OK to test LinkedList.isEmpty without synchronized

		synchronized (_dtInits) {
			for (Iterator it = _dtInits.iterator(); it.hasNext();) {
				final Class klass = (Class)it.next();
				try {
					((DesktopInit)klass.newInstance()).init(desktop);
				} catch (Throwable ex) {
					throw UiException.Aide.wrap(ex);
					//Don't intercept; to prevent the creation of a session
				}
			}
		}
	}
	/** Invokes {@link DesktopCleanup#cleanup} for each relevant
	 * listener registered by {@link #addListener}.
	 *
	 * <p>An instance of {@link DesktopCleanup} is constructed first,
	 * and then invoke {@link DesktopCleanup#cleanup}.
	 *
	 * <p>It never throws an exception.
	 *
	 * @param desktop the desktop that is being destroyed
	 */
	public void invokeDesktopCleanups(Desktop desktop) {
		if (_dtCleans.isEmpty()) return;
			//it is OK to test LinkedList.isEmpty without synchronized

		synchronized (_dtCleans) {
			for (Iterator it = _dtCleans.iterator(); it.hasNext();) {
				final Class klass = (Class)it.next();
				try {
					((DesktopCleanup)klass.newInstance()).cleanup(desktop);
				} catch (Throwable ex) {
					log.error("Failed to invoke "+klass, ex);
				}
			}
		}
	}

	/** Invokes {@link ExecutionInit#init} for each relevant
	 * listener registered by {@link #addListener}.
	 *
	 * <p>An instance of {@link ExecutionInit} is constructed first,
	 * and then invoke {@link ExecutionInit#init}.
	 *
	 * @param exec the execution that is created
	 * @param parent the previous execution, or null if no previous at all
	 * @exception UiException to prevent an execution from being created
	 */
	public void invokeExecutionInits(Execution exec, Execution parent)
	throws UiException {
		if (_execInits.isEmpty()) return;
			//it is OK to test LinkedList.isEmpty without synchronized

		synchronized (_execInits) {
			for (Iterator it = _execInits.iterator(); it.hasNext();) {
				final Class klass = (Class)it.next();
				try {
					((ExecutionInit)klass.newInstance()).init(exec, parent);
				} catch (Throwable ex) {
					throw UiException.Aide.wrap(ex);
					//Don't intercept; to prevent the creation of a session
				}
			}
		}
	}
	/** Invokes {@link ExecutionCleanup#cleanup} for each relevant
	 * listener registered by {@link #addListener}.
	 *
	 * <p>An instance of {@link ExecutionCleanup} is constructed first,
	 * and then invoke {@link ExecutionCleanup#cleanup}.
	 *
	 * <p>It never throws an exception but logs and adds it to the errs argument,
	 * if not null.
	 *
	 * @param exec the execution that is being destroyed
	 * @param parent the previous execution, or null if no previous at all
	 * @param errs a list of exceptions (java.lang.Throwable) if any exception
	 * occured before this method is called, or null if no exeption at all.
	 * Note: you can manipulate the list directly to add or clean up exceptions.
	 * For example, if exceptions are fixed correctly, you can call errs.clear()
	 * such that no error message will be displayed at the client.
	 */
	public void invokeExecutionCleanups(Execution exec, Execution parent, List errs) {
		if (_execCleans.isEmpty()) return;
			//it is OK to test LinkedList.isEmpty without synchronized

		synchronized (_execCleans) {
			for (Iterator it = _execCleans.iterator(); it.hasNext();) {
				final Class klass = (Class)it.next();
				try {
					((ExecutionCleanup)klass.newInstance())
						.cleanup(exec, parent, errs);
				} catch (Throwable ex) {
					log.error("Failed to invoke "+klass, ex);
					if (errs != null) errs.add(ex);
				}
			}
		}
	}

	/** Invokes {@link URIInterceptor#request} for each relevant listner
	 * registered by {@link #addListener}.
	 *
	 * <p>If any of them throws an exception, the exception is propogated to
	 * the caller.
	 *
	 * @exception UiException if it is rejected by the interceptor.
	 * Use {@link UiException#getCause} to retrieve the cause.
	 */
	public void invokeURIInterceptors(String uri) {
		if (_uriIntcps.isEmpty()) return;

		synchronized (_uriIntcps) {
			for (Iterator it = _uriIntcps.iterator(); it.hasNext();) {
				final Class klass = (Class)it.next();
				try {
					((URIInterceptor)klass.newInstance()).request(uri);
				} catch (Exception ex) {
					throw UiException.Aide.wrap(ex);
				}
			}
		}
	}

	/** Adds an CSS resource that will be generated for each ZUML desktop.
	 */
	public void addThemeURI(String uri) {
		if (uri == null || uri.length() == 0)
			throw new IllegalArgumentException("empty");
		synchronized (_themeUris) {
			_themeUris.add(uri);
			_roThemeUris =
				(String[])_themeUris.toArray(new String[_themeUris.size()]);
		}
	}
	/** Returns a readonly list of the URI of the CSS resources that will be
	 * generated for each ZUML desktop (never null).
	 *
	 * <p>Default: an array with zero length.
	 */
	public String[] getThemeURIs() {
		return _roThemeUris;
	}

	/** Sets the URI that is used when the session timeout or
	 * desktop is no longer available.
	 *
	 * @param uri the URI used if timeout, or null to show an error message
	 * at the client only. If empty, it works as reloading the same URI again.
	 */
	public void setTimeoutURI(String uri) {
		_timeoutUri = uri;
	}
	/** Sets the URI that is used when the session timeout or
	 * desktop is no longer available, or null.
	 *
	 * <p>Default: null.
	 *
	 * <p>If null is returned, an message is shown up at the client.
	 * If empty, it works as reloading the same URI again.
	 * If non null, the browser will be redirected to the returned URI.
	 */
	public String getTimeoutURI() {
		return _timeoutUri;
	}

	/** Sets the class that implements {@link LocaleProvider}, or null to
	 * use the default.
	 */
	public void setLocaleProviderClass(Class cls) {
		if (cls != null && !LocaleProvider.class.isAssignableFrom(cls))
			throw new IllegalArgumentException("LocaleProvider not implemented: "+cls);
		_lpcls = cls;
	}
	/** Returns the class that implements {@link LocaleProvider}, or null if default is used.
	 */
	public Class getLocaleProviderClass() {
		return _lpcls;
	}
	/** Sets the class that implements {@link TimeZoneProvider}, or null to
	 * use the default.
	 */
	public void setTimeZoneProviderClass(Class cls) {
		if (cls != null && !TimeZoneProvider.class.isAssignableFrom(cls))
			throw new IllegalArgumentException("TimeZoneProvider not implemented: "+cls);
		_tzpcls = cls;
	}
	/** Returns the class that implements {@link TimeZoneProvider}, or null if default is used.
	 */
	public Class getTimeZoneProviderClass() {
		return _tzpcls;
	}

	/** Sets the class that implements {@link UiEngine}, or null to
	 * use the default.
	 */
	public void setUiEngineClass(Class cls) {
		if (cls != null && !UiEngine.class.isAssignableFrom(cls))
			throw new IllegalArgumentException("UiEngine not implemented: "+cls);
		_uiengcls = cls;
	}
	/** Returns the class that implements {@link UiEngine}, or null if default is used.
	 */
	public Class getUiEngineClass() {
		return _uiengcls;
	}

	/** Sets the class that implements {@link WebApp} and
	 * {@link WebAppCtrl}, or null to use the default.
	 */
	public void setWebAppClass(Class cls) {
		if (cls != null && (!WebApp.class.isAssignableFrom(cls)
		|| !WebAppCtrl.class.isAssignableFrom(cls)))
			throw new IllegalArgumentException("WebApp or WebAppCtrl not implemented: "+cls);
		_wappcls = cls;
	}
	/** Returns the class that implements {@link WebApp} and
	 * {@link WebAppCtrl}, or null if default is used.
	 */
	public Class getWebAppClass() {
		return _wappcls;
	}

	/** Sets the class that implements {@link DesktopCacheProvider}, or null to
	 * use the default.
	 */
	public void setDesktopCacheProviderClass(Class cls) {
		if (cls != null && !DesktopCacheProvider.class.isAssignableFrom(cls))
			throw new IllegalArgumentException("DesktopCacheProvider not implemented: "+cls);
		_dcpcls = cls;
	}
	/** Returns the class that implements the UI engine, or null if default is used.
	 */
	public Class getDesktopCacheProviderClass() {
		return _dcpcls;
	}

	/** Sets the class that implements {@link UiFactory}, or null to
	 * use the default.
	 */
	public void setUiFactoryClass(Class cls) {
		if (cls != null && !UiFactory.class.isAssignableFrom(cls))
			throw new IllegalArgumentException("UiFactory not implemented: "+cls);
		_uiftycls = cls;
	}
	/** Returns the class that implements the UI engine, or null if default is used.
	 */
	public Class getUiFactoryClass() {
		return _uiftycls;
	}

	/** Specifies the maximal allowed upload size, in kilobytes.
	 * <p>Default: 5120.
	 * @param sz the maximal allowed upload size. If null, there is no
	 * limitation.
	 */
	public void setMaxUploadSize(Integer sz) {
		_maxUploadSize = sz;
	}
	/** Returns the maximal allowed upload size, in kilobytes, or null
	 * if no limiatation.
	 */
	public Integer getMaxUploadSize() {
		return _maxUploadSize;
	}

	/** Specifies the time, in seconds, between client requests
	 * before ZK will invalidate the desktop, or null if default is used (1 hour).
	 */
	public void setDesktopMaxInactiveInterval(Integer secs) {
		_dtTimeout = secs;
	}
	/** Returns the time, in seconds, between client requests
	 * before ZK will invalidate the desktop, or null if default is used.
	 */
	public Integer getDesktopMaxInactiveInterval() {
		return _dtTimeout;
	}

	/** Specifies the time, in milliseconds, before the client shows
	 * a dialog to prompt users that the request is in processming.
	 *
	 * <p>Default: 900
	 */
	public void setProcessingPromptDelay(int minisecs) {
		_promptDelay = minisecs;
	}
	/** Returns the time, in milliseconds, before the client shows
	 * a dialog to prompt users that the request is in processming.
	 */
	public int getProcessingPromptDelay() {
		return _promptDelay;
	}
	/** Specifies the time, in milliseconds, before the client shows
	 * the tooltip when a user moves the mouse over particual UI components.
	 *
	 * <p>Default: 800
	 */
	public void setTooltipDelay(int minisecs) {
		_tooltipDelay = minisecs;
	}
	/** Returns the time, in milliseconds, before the client shows
	 * the tooltip when a user moves the mouse over particual UI components.
	 */
	public int getTooltipDelay() {
		return _tooltipDelay;
	}

	/**  Specifies the time, in seconds, between client requests
	 * before ZK will invalidate the session, or null to use the default.
	 */
	public void setSessionMaxInactiveInterval(Integer secs) {
		_sessTimeout = secs;
	}
	/** Returns the time, in seconds, between client requests
	 * before ZK will invalidate the session, or null if default is used.
	 */
	public Integer getSessionMaxInactiveInterval() {
		return _sessTimeout;
	}

	/** Specifies the maximal allowed number of desktop
	 * per session, or null to use the default (10).
	 */
	public void setMaxDesktops(Integer max) {
		_dtMax = max;
	}
	/** Returns the maximal allowed number of desktop
	 * per session, or null if default is used (10).
	 */
	public Integer getMaxDesktops() {
		return _dtMax;
	}

	/** Specifies the maximal allowed number of event processing threads
	 * per Web application, or null to use the default (100).
	 */
	public void setMaxEventThreads(Integer max) {
		_evtThdMax = max;
	}
	/** Returns the maximal allowed number of event processing threads
	 * per Web application, or null if default is used (100).
	 */
	public Integer getMaxEventThreads() {
		return _evtThdMax;
	}

	/** Returns the monitor for this application, or null if not set.
	 */
	public Monitor getMonitor() {
		return _monitor;
	}
	/** Sets the monitor for this application, or null to disable it.
	 *
	 * <p>In addition to call this method, you could specify a monitor
	 * in web.xml.
	 */
	public void setMonitor(Monitor monitor) {
		_monitor = monitor;
	}

	/** Returns the charset used by {@link org.zkoss.zk.ui.http.DHtmlLayoutServlet},
	 * or null to use the container's default.
	 * <p>Default: UTF-8.
	 */
	public String getCharset() {
		return _charset;
	}
	/** Sets the charset used by {@link org.zkoss.zk.ui.http.DHtmlLayoutServlet}.
	 *
	 * @param charset the charset to use. If null or empty, the container's default
	 * is used.
	 */
	public void setCharset(String charset) {
		_charset = charset != null && charset.length() > 0 ? charset: null;
	}

	/** Returns the value of the preference defined in zk.xml, or by
	 * {@link #setPreference}.
	 *
	 * <p>Preference is application specific. You can specify whatever you want
	 * as you specifying context-param for a Web application.
	 *
	 * @param defaultValue the default value that is used if the specified
	 * preference is not found.
	 */
	public String getPreference(String name, String defaultValue) {
		final String value = (String)_prefs.get(name);
		return value != null ? value: defaultValue;
	}
	/** Sets the value of the preference.
	 */
	public void setPreference(String name, String value) {
		if (name == null || value == null)
			throw new IllegalArgumentException("null");
		_prefs.put(name, value);
	}
	/** Returns a set of all preference names.
	 */
	public Set getPreferenceNames() {
		return _prefs.keySet();
	}

	/** Adds a richlet with its class.
	 *
	 * @param params the initial parameters, or null if no initial parameter at all.
	 * Once called, the caller cannot access <code>params</code> any more.
	 * @return the previous richlet class or class-name with the specified path,
	 * or null if no previous richlet.
	 */
	public Object addRichlet(String path, Class richletClass, Map params) {
		if (!Richlet.class.isAssignableFrom(richletClass))
			throw new IllegalArgumentException("A richlet class, "+richletClass+", must implement "+Richlet.class.getName());

		return addRichlet0(path, richletClass, params);
	}
	/** Adds a richlet with its class name.
	 *
	 *
	 * @param params the initial parameters, or null if no initial parameter at all.
	 * Once called, the caller cannot access <code>params</code> any more.
	 * @return the previous richlet class or class-name with the specified path,
	 * or null if no previous richlet.
	 */
	public Object addRichlet(String path, String richletClassName, Map params) {
		if (richletClassName == null || richletClassName.length() == 0)
			throw new IllegalArgumentException("richletClassName is required");

		return addRichlet0(path, richletClassName, params);
	}
	private Object addRichlet0(String path, Object richletClass, Map params) {
		//richletClass was checked before calling this method
		if (path == null || !path.startsWith("/"))
			throw new IllegalArgumentException("path must start with '/', not "+path);

		final Object o;
		synchronized (_richlets) {
			o = _richlets.put(path, new Object[] {richletClass, params});
		}

		if (o == null)
			return null;
		if (o instanceof Richlet) {
			destroy((Richlet)o);
			return o.getClass();
		}
		return ((Object[])o)[0];
	}
	private static void destroy(Richlet richlet) {
		try {
			richlet.destroy();
		} catch (Throwable ex) {
			log.error("Unable to destroy "+richlet);
		}
	}
	/** Returns an instance of richlet for the specified path, or null if not found.
	 */
	public Richlet getRichlet(String path) {
		WaitLock lock = null;
		final Object[] info;
		for (;;) {
			synchronized (_richlets) {
				Object o = _richlets.get(path);
				if (o == null || (o instanceof Richlet)) { //loaded or not found
					return (Richlet)o;
				} else if (o instanceof WaitLock) { //loading by another thread
					lock = (WaitLock)o;
				} else { //going to load in this thread
					info = (Object[])o;
					_richlets.put(path, lock = new WaitLock());
					break; //then, load it
				}
			} //sync(_richlets)

			if (!lock.waitUntilUnlock(300*1000)) { //5 minute
				final PotentialDeadLockException ex =
					new PotentialDeadLockException(
					"Unable to load from "+path+"\nCause: conflict too long.");
				log.warningBriefly(ex); //very rare, possibly a bug
				throw ex;
			}
		} //for (;;)

		//load it
		try {
			if (info[0] instanceof String) {
				try {
					info[0] = Classes.forNameByThread((String)info[0]);
				} catch (Throwable ex) {
					throw new UiException("Failed to load "+info[0]);
				}
			}

			final Object o = ((Class)info[0]).newInstance();
			if (!(o instanceof Richlet))
				throw new UiException(Richlet.class+" must be implemented by "+info[0]);

			final Richlet richlet = (Richlet)o;
			richlet.init(new RichletConfigImpl(_wapp, (Map)info[1]));

			synchronized (_richlets) {
				_richlets.put(path, richlet);
			}
			return richlet;
		} catch (Throwable ex) {
			synchronized (_richlets) {
				_richlets.put(path, info); //remove lock and restore info
			}
			throw UiException.Aide.wrap(ex, "Unable to instantiate "+info[0]);
		} finally {
			lock.unlock();
		}
	}
	/** Destroyes all richlets.
	 */
	public void detroyRichlets() {
		synchronized (_richlets) {
			for (Iterator it = _richlets.values().iterator(); it.hasNext();) {
				final Object o = it.next();
				if (o instanceof Richlet)
					destroy((Richlet)o);
			}
			_richlets.clear();
		}
	}

	/** Enables or disables the default theme of the specified language.
	 *
	 * @param lang the language name, such as xul/html and xhtml.
	 * @param enable whether to enable or disable.
	 * If false, the default theme of the specified language is disabled.
	 * Default: enabled.
	 */
	public void enableDefaultTheme(String lang, boolean enable) {
		if (lang == null || lang.length() == 0)
			throw new IllegalArgumentException("lang is required");

		synchronized (this) {
			if (enable) {
				if (_disabledDefThemes != null) {
					_disabledDefThemes.remove(lang);
					if (_disabledDefThemes.isEmpty())
						_disabledDefThemes = null;
				}
			} else {
				if (_disabledDefThemes == null)
					_disabledDefThemes =
						Collections.synchronizedSet(new HashSet(3));
				_disabledDefThemes.add(lang);
			}
		}
	}
	/** Returns whether the default theme of the specified language is
	 * enabled.
	 */
	public boolean isDefaultThemeEnabled(String lang) {
		return _disabledDefThemes == null || !_disabledDefThemes.contains(lang);
	}

	/** Adds an error page.
	 *
	 * @param type what type of errors the error page is associated with.
	 * @param location where is the error page.
	 */
	public void addErrorPage(Class type, String location) {
		if (!Throwable.class.isAssignableFrom(type))
			throw new IllegalArgumentException("Throwable or derived is required: "+type);
		if (location == null)
			throw new IllegalArgumentException("location required");
		synchronized (_errpgs) {
			_errpgs.add(new ErrorPage(type, location));
		}
	}
	/** Returns the error page that matches the specified error, or null if not found.
	 */
	public String getErrorPage(Throwable error) {
		if (!_errpgs.isEmpty()) {
			synchronized (_errpgs) {
				for (Iterator it = _errpgs.iterator(); it.hasNext();) {
					final ErrorPage errpg = (ErrorPage)it.next();
					if (errpg.type.isInstance(error))
						return errpg.location;
				}
			}
		}
		return null;
	}
	private static class ErrorPage {
		private final Class type;
		private final String location;
		private ErrorPage(Class type, String location) {
			this.type = type;
			this.location = location;
		}
	};
}
