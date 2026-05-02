package com.ctfind.productioncontrol.production.application

import com.ctfind.productioncontrol.notifications.application.CreateNotificationCommand
import com.ctfind.productioncontrol.notifications.application.NotificationCreatePort
import com.ctfind.productioncontrol.notifications.application.NotificationPersistencePort
import com.ctfind.productioncontrol.notifications.domain.NotificationTargetType
import com.ctfind.productioncontrol.notifications.domain.NotificationType
import com.ctfind.productioncontrol.production.domain.ProductionTask
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
class OverdueTaskNotificationJob(
    private val tasks: ProductionTaskPort,
    private val notifications: NotificationCreatePort,
    private val notificationQuery: NotificationPersistencePort,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedRate = 900_000)
    fun run() {
        checkOverdueTasks(LocalDate.now())
    }

    fun checkOverdueTasks(today: LocalDate) {
        val overdueTasks = tasks.findOverdue(today)
        for (task in overdueTasks) {
            val recipients = buildRecipients(task)
            for (recipientId in recipients) {
                if (notificationQuery.existsByTypeAndTargetIdAndRecipient(
                        NotificationType.TASK_OVERDUE, task.taskNumber, recipientId,
                    )
                ) {
                    continue
                }
                try {
                    notifications.create(
                        CreateNotificationCommand(
                            recipientUserId = recipientId,
                            type = NotificationType.TASK_OVERDUE,
                            title = "Задача ${task.taskNumber} просрочена",
                            targetType = NotificationTargetType.PRODUCTION_TASK,
                            targetId = task.taskNumber,
                            targetEntityId = task.id,
                        ),
                    )
                } catch (e: Exception) {
                    log.warn("Failed to create TASK_OVERDUE notification for task {}", task.taskNumber, e)
                }
            }
        }
    }

    private fun buildRecipients(task: ProductionTask): Set<UUID> {
        val recipients = mutableSetOf<UUID>()
        recipients += task.createdByUserId
        task.executorUserId?.let { recipients += it }
        return recipients
    }
}
