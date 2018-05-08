package io.zeebe.client.api.clients;

import io.zeebe.client.api.commands.*;
import io.zeebe.client.api.events.JobEvent;

/**
 * A client with access to all job-related operation:
 * <li>create a (standalone) job
 * <li>complete a job
 * <li>mark a job as failed
 * <li>update the retries of a job
 */
public interface JobClient
{

    /**
     * Command to create a new (standalone) job. The job is not linked
     * to a workflow instance.
     *
     * <pre>
     * jobClient
     *  .newCreateCommand()
     *  .type("my-todos")
     *  .payload(json)
     *  .send();
     * </pre>
     *
     * @return a builder for the command
     */
    CreateJobCommandStep1 newCreateCommand();

    /**
     * Command to complete a job.
     *
     * <pre>
     * jobClient
     *  .newCompleteCommand(jobEvent)
     *  .payload(json)
     *  .send();
     * </pre>
     *
     * The job is specified by the given event. The event must be
     * the latest event of the job to ensure that the command is
     * based on the latest state of the job. If it's not the
     * latest one then the command is rejected.
     * <p>
     * If the job is linked to a workflow instance then this
     * command will complete the related activity and continue
     * the flow.
     *
     * @param event
     *            the latest job event
     *
     * @return a builder for the command
     */
    CompleteJobCommandStep1 newCompleteCommand(JobEvent event);

    /**
     * Command to mark a job as failed.
     *
     * <pre>
     * jobClient
     *  .newFailCommand(jobEvent)
     *  .retries(jobEvent.getRetries() - 1)
     *  .send();
     * </pre>
     *
     * The job is specified by the given event. The event must be
     * the latest event of the job to ensure that the command is
     * based on the latest state of the job. If it's not the
     * latest one then the command is rejected.
     * <p>
     * If the given retries are greater than zero then this job will
     * be picked up again by a job subscription. Otherwise, an incident
     * is created for this job.
     *
     * @param event
     *            the latest job event
     *
     * @return a builder for the command
     */
    FailJobCommandStep1 newFailCommand(JobEvent event);

    /**
     * Command to update the retries of a job.
     *
     * <pre>
     * jobClient
     *  .newUpdateRetriesCommand(jobEvent)
     *  .retries(3)
     *  .send();
     * </pre>
     *
     * The job is specified by the given event. The event must be
     * the latest event of the job to ensure that the command is
     * based on the latest state of the job. If it's not the
     * latest one then the command is rejected.
     * <p>
     * If the given retries are greater than zero then this job will
     * be picked up again by a job subscription and a related
     * incident will be marked as resolved.
     *
     * @param event
     *            the latest job event
     *
     * @return a builder for the command
     */
    UpdateRetriesJobCommandStep1 newUpdateRetriesCommand(JobEvent event);

}
