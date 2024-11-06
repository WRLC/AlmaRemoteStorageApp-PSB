package com.exlibris.logger;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import com.exlibris.logger.LoggerMain.LoggerJob;

public class SchedulerMain {

    final static Logger logger = Logger.getLogger(SchedulerMain.class);

    public static void main(String[] args) throws Exception {

        logger.info("hello SchedulerMain");
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

        scheduler.start();

        JobDetail jobLoggerJobDetail = newJob(LoggerJob.class).build();

        // 10 minutes after midnight
        Trigger triggerLoggerJob = newTrigger().startNow()
                .withSchedule(
                        CronScheduleBuilder.cronSchedule("00 10 00 * * ?").inTimeZone(TimeZone.getTimeZone("Etc/UTC")))
                .startNow().build();

        scheduler.scheduleJob(jobLoggerJobDetail, triggerLoggerJob);
    }

}
