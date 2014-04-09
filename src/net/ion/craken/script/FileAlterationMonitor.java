package net.ion.craken.script;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.monitor.FileAlterationObserver;

public final class FileAlterationMonitor  {

	private final long interval;
	private final List<FileAlterationObserver> observers ;
	private ScheduledExecutorService ses ;
	private transient boolean running = true ;

	public FileAlterationMonitor(long interval, ScheduledExecutorService ses,  FileAlterationObserver first, FileAlterationObserver... observers) {
		this.ses = ses ;
		this.observers = new CopyOnWriteArrayList<FileAlterationObserver>() ;
		this.interval = interval ;
		addObserver(first);
		for (FileAlterationObserver observer : observers) {
			addObserver(observer);
		}
	}

	public long getInterval() {
		return interval;
	}

	public void addObserver(FileAlterationObserver observer) {
		observers.add(observer);
	}

	public void removeObserver(FileAlterationObserver observer) {
		while (observers.remove(observer)) ;
	}

	public Iterable getObservers() {
		return observers;
	}

	public synchronized void start() throws Exception {
		
		Callable<Void> sjob = new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				for (FileAlterationObserver o : observers){
					o.checkAndNotify(); 
				}
				ses.schedule(this, interval, TimeUnit.MILLISECONDS) ;
				return null;
			}
		};
		ses.schedule(sjob, interval, TimeUnit.MILLISECONDS) ;
	}

	public synchronized void stop() throws Exception {
		this.running = false ;
	}


}