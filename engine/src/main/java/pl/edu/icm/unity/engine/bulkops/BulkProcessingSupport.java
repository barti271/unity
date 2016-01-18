/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulkops;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.utils.Key;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.bulkops.ProcessingRule;
import pl.edu.icm.unity.server.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.server.bulkops.ScheduledProcessingRuleParam;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Utility code for bulk processing actions management
 * @author K. Benedyczak
 */
@Component
public class BulkProcessingSupport
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, BulkProcessingSupport.class);
	public static final String RULE_KEY = "rule";
	public static final String TS_KEY = "timeStamp";
	public static final String JOB_GROUP = "bulkEntityProcessing";
	
	@Autowired
	private Scheduler scheduler;
	@Autowired
	private BulkProcessingExecutor executor;
	
	public synchronized Collection<ScheduledProcessingRule> getScheduledRules()
	{
		List<JobExecutionContext> jobs = getCurrentJobs();
		return jobs.stream().
			filter(this::filterProcessingJobs).
			map(context -> (ScheduledProcessingRule) context.get(RULE_KEY)).
			collect(Collectors.toList());
	}
	
	public synchronized Collection<RuleWithTS> getScheduledRulesWithTS()
	{
		List<JobExecutionContext> jobs = getCurrentJobs();
		return jobs.stream().
			filter(this::filterProcessingJobs).
			map(context -> {
				ScheduledProcessingRule rule = (ScheduledProcessingRule) context.get(RULE_KEY);
				Date ts = (Date) context.get(TS_KEY);
				return new RuleWithTS(rule, ts);
			}).
			collect(Collectors.toList());
	}

	private List<JobExecutionContext> getCurrentJobs()
	{
		try
		{
			return scheduler.getCurrentlyExecutingJobs();
		} catch (SchedulerException e)
		{
			throw new InternalException("Error retrieving scheduled jobs from Quartz", e);
		}
	}
	
	public static String generateJobKey()
	{
		return Key.createUniqueName(null);
	}

	public void scheduleImmediateJob(ProcessingRule rule)
	{
		Trigger trigger = createImmediateTrigger();
		scheduleJob(rule, trigger, generateJobKey(), new Date());
	}

	public void scheduleJob(ScheduledProcessingRule rule, Date ts)
	{
		Trigger trigger = createCronTrigger(rule);
		scheduleJob(rule, trigger, rule.getId(), ts);
	}
	
	public synchronized void undeployJob(String id)
	{
		log.debug("Removing job with id " + id);
		try
		{
			scheduler.deleteJob(new JobKey(id, BulkProcessingSupport.JOB_GROUP));
		} catch (SchedulerException e)
		{
			throw new InternalException("Can't undeploy a rule with id " + id, e);
		}
	}

	public synchronized void updateJob(ScheduledProcessingRule rule, Date ts)
	{
		Trigger trigger = createCronTrigger(rule);
		undeployJob(rule.getId());
		scheduleJob(rule, trigger, rule.getId(), ts);
	}
	
	
	private JobDetail createJob(String id, ProcessingRule rule, Date ts)
	{
		JobDataMap dataMap = new JobDataMap();
		dataMap.put(RULE_KEY, rule);
		dataMap.put(TS_KEY, ts);
		JobDetail job = JobBuilder.newJob(EntityRuleJob.class)
				.withIdentity(id, JOB_GROUP)
				.usingJobData(dataMap)
				.build();
		return job;
	}
	
	private Trigger createCronTrigger(ScheduledProcessingRuleParam rule)
	{
		return TriggerBuilder.newTrigger()
				.withSchedule(CronScheduleBuilder.cronSchedule(rule.getCronExpression()))
				.build();
	}

	private Trigger createImmediateTrigger()
	{
		return TriggerBuilder.newTrigger()
				.startNow()
				.withSchedule(SimpleScheduleBuilder.simpleSchedule())
				.build();
	}
	
	private synchronized void scheduleJob(ProcessingRule rule, Trigger trigger, String id, Date ts)
	{
		JobDetail job = createJob(id, rule, ts);
		log.debug("Scheduling job with id " + id + " and trigger " + trigger);
		try
		{
			scheduler.scheduleJob(job, trigger);
		} catch (SchedulerException e)
		{
			throw new InternalException("Can't schedule processing rule", e);
		}
	}
	
	private boolean filterProcessingJobs(JobExecutionContext context)
	{
		return context.getJobDetail().getKey().getGroup().equals(JOB_GROUP) && 
				context.get(RULE_KEY) instanceof ScheduledProcessingRule;
	}
	
	private class EntityRuleJob implements Job 
	{
		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException
		{
			ProcessingRule rule = (ProcessingRule) context.get(RULE_KEY);
			executor.execute(rule);
		}
	}
	
	public static class RuleWithTS
	{
		public final ScheduledProcessingRule rule;
		public final Date ts;

		public RuleWithTS(ScheduledProcessingRule rule, Date ts)
		{
			this.rule = rule;
			this.ts = ts;
		}
	}
}
