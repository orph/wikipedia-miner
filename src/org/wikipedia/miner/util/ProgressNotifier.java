/*
 *    ProgressNotifier.java
 *    Copyright (C) 2007 David Milne, d.n.milne@gmail.com
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.wikipedia.miner.util;

import java.text.DecimalFormat;
import java.util.* ;

/**
 * @author David Milne
 * 
 *  This class provides a naive means of tracking the progress of a task 
 *  or set of tasks, with percentage of completion, time spent, and estimated time remaining.
 */
public class ProgressNotifier {
	
	private int tasks ;
	private int tasksDone ;
	
	private String currTask_message ;
	private long currTask_parts ;
	private long currTask_partsDone ;
	private long currTask_start ;
	
	private long lastReportTime ;
	private long minReportInterval = 1000 ;
	
	private double minReportProgress = 0.01 ;
	private double lastReportProgress ;
	
	private int lastMessageLength ;
	
	private boolean clearPrevMessage = false ;
	
	DecimalFormat percentFormat = new DecimalFormat("#0.00 %") ;
	DecimalFormat digitFormat = new DecimalFormat("00") ;
	
	/**
	 * Creates a ProgressNotifier for tracking a single named task.
	 * 
	 * @param taskParts the number of parts this task involves.
	 * @param message the message to be displayed alongside all progress updates
	 */
	public ProgressNotifier(long taskParts, String message) {
		tasks = 1 ;
		tasksDone = -1 ;
		startTask(taskParts, message) ;
	}
	
	/**
	 * Creates a ProgressNotifier for tracking the given number of tasks. 
	 * You will have to call startTask before starting each one. 
	 * 
	 * @param tasks the task this notifier will track.
	 */
	public ProgressNotifier(int tasks) {
		this.tasks = tasks ;
		tasksDone = -1 ;
	}
	
	/**
	 * Sets the minimum time between display messages. The default is 1 second. 
	 * 
	 * @param val the minimum time between display messages, in milliseconds.
	 */
	public void setMinReportInterval(long val) {
		minReportInterval = val ;
	}
		
	/**
	 * Specifies whether previous display messages are overwritten by new ones. 
	 * This doesn't work within eclipse, and is disabled by default.
	 * 
	 * @param val true if previous messages are to be overwritten, otherwise false.
	 */
	public void setClearPreviousMessages(boolean val) {
		clearPrevMessage = val ;
	}
	
	/**
	 * Starts an unnamed task. Previous tasks are assumed to be completed. 
	 * 
	 * @param taskParts the number of parts this task involves.
	 */
	public void startTask(long taskParts) {
		this.tasksDone ++ ;
		
		currTask_message = "" ;
		currTask_parts = taskParts ;
		currTask_partsDone = 0 ;
		currTask_start = new Date().getTime() ; 
		
		lastReportTime = currTask_start ;
		lastReportProgress = 0 ;
	}	
	
	/**
	 * Starts a task. Previous tasks are assumed to be completed. 
	 * 
	 * @param taskParts the number of parts this task involves.
	 * @param message the message to be displayed alongside all progress updates
	 */
	public void startTask(long taskParts, String message) {
		startTask(taskParts) ;
		currTask_message = message ;		
	}
	
	/**
	 * Increments progress by one step and prints a message, if appropriate.
	 */
	public void update() {
		update(currTask_partsDone+1) ;
	}
	
	/**
	 * Updates progress and prints a message, if appropriate.
	 * 
	 * @param partsDone the number of steps that have been completed so far
	 */
	public void update(long partsDone) {
		currTask_partsDone = partsDone ;
		displayProgress() ;
	}
	
	
	/**
	 * Returns the proportion of the current task that has been completed so far.
	 * 
	 * @return see above
	 */
	public double getTaskProgress() {
		return (double)currTask_partsDone/currTask_parts ;
	}
	
	/**
	 * Returns the proportion of the overall task that has been completed so far.
	 * 
	 * @return see above
	 */
	public double getGlobalProgress() {
		
		double progress = (double)tasksDone/tasks ;
		progress += getTaskProgress()/tasks ;
		
		return progress ;
	}
	
	
	private void displayProgress() {
				
		String output = "" ;
		if (currTask_message != null)
			output = currTask_message + ": " ;

		long now = new Date().getTime() ;
		
		if (currTask_partsDone < 1)
			return ;
		
		if (now - lastReportTime < minReportInterval)
			return ;
		
		double progress = (double)currTask_partsDone/currTask_parts ;
		
		if (progress - lastReportProgress < minReportProgress)
			return ;
		
				
		long timeElapsed = now - currTask_start ;

		long timeTotal = (long)(timeElapsed * ((double)currTask_parts/currTask_partsDone)) ; 
		long timeLeft = timeTotal - timeElapsed ;

		output = output + percentFormat.format(progress) 
		+ " in " + formatTime(timeElapsed) 
		+ ", ETA " + formatTime(timeLeft) ;	

		if (clearPrevMessage && lastMessageLength > 0) {
			StringBuffer sb = new StringBuffer() ;
			for (int i=0 ; i<lastMessageLength ; i++)
				sb.append('\b') ;
				
			System.out.println(sb) ;
		}
				
			
		System.out.println(output) ;
			
		lastReportTime = now ;
		lastMessageLength = output.length() ;
		lastReportProgress = progress ;
	}
	
	private String formatTime(long time) {
		
		int hours = 0 ;
		int minutes = 0 ; 
		int seconds = 0;
		
		seconds= (int)((double)time/1000) ;
		
		if (seconds>60) {
			minutes = (int)((double)seconds/60) ;
			seconds = seconds - (minutes * 60) ;
			
			if (minutes > 60) {
				hours = (int)((double)minutes/60) ;
				minutes = minutes - (hours * 60) ;
			}
		}
		
		return digitFormat.format(hours) + ":" + digitFormat.format(minutes) + ":" + digitFormat.format(seconds) ;
	}
}
