package org.activiti.examples;

import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.query.Pageable;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.runtime.TaskRuntime;
import org.activiti.api.task.runtime.events.TaskAssignedEvent;
import org.activiti.api.task.runtime.events.TaskCompletedEvent;
import org.activiti.api.task.runtime.events.listener.TaskRuntimeEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication implements CommandLineRunner {

    private Logger logger = LoggerFactory.getLogger(DemoApplication.class);

    @Autowired
    private TaskRuntime taskRuntime;

    @Autowired
    private SecurityUtil securityUtil;


    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);

    }

    @Override
    public void run(String... args) {

        // Using Security Util to simulate a logged in user
        securityUtil.logInAs("salaboy");// 手动注册用户

        // Let's create a Group Task (not assigned, all the members of the group can claim it)
        //  Here 'salaboy' is the owner of the created task
        logger.info("> Creating a Group Task for 'activitiTeam'");
        taskRuntime.create(TaskPayloadBuilder.create()
                .withName("First Team Task")
                .withDescription("This is something really important")
                .withCandidateGroup("activitiTeam")
                .withPriority(10)
                .build());

        // Let's log in as 'other' user that doesn't belong to the 'activitiTeam' group
        securityUtil.logInAs("other"); // 注册other用户

        // Let's get all my tasks (as 'other' user)
        logger.info("> Getting all the tasks");
        Page<Task> tasks = taskRuntime.tasks(Pageable.of(0, 10));

        // No tasks are returned
        logger.info(">  Other cannot see the task: " + tasks.getTotalItems());


        // Now let's switch to a user that belongs to the activitiTeam
        securityUtil.logInAs("erdemedeiros");

        // Let's get 'erdemedeiros' tasks
        logger.info("> Getting all the tasks");
        tasks = taskRuntime.tasks(Pageable.of(0, 10));

        // 'erdemedeiros' can see and claim the task
        logger.info(">  erdemedeiros can see the task: " + tasks.getTotalItems());


        String availableTaskId = tasks.getContent().get(0).getId();

        // Let's claim the task, after the claim, nobody else can see the task and 'erdemedeiros' becomes the assignee
        logger.info("> Claiming the task");
        taskRuntime.claim(TaskPayloadBuilder.claim().withTaskId(availableTaskId).build());


        // Let's complete the task
        logger.info("> Completing the task");
        taskRuntime.complete(TaskPayloadBuilder.complete().withTaskId(availableTaskId).build());


    }

    /**
     * 任务事件注册监听
     * @return
     */
    @Bean
    public TaskRuntimeEventListener<TaskAssignedEvent> taskAssignedListener() {
        return taskAssigned -> logger.info(">>> Task Assigned: '"
                + taskAssigned.getEntity().getName() +
                "' We can send a notification to the assginee: " + taskAssigned.getEntity().getAssignee());
    }

    @Bean
    public TaskRuntimeEventListener<TaskCompletedEvent> taskCompletedListener() {
        return taskCompleted -> logger.info(">>> Task Completed: '"
                + taskCompleted.getEntity().getName() +
                "' We can send a notification to the owner: " + taskCompleted.getEntity().getOwner());
    }


}
