package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._

import models._
import models.JiraWebhookAction._

import services._

object Jiras extends Controller with utils.Config with utils.Log {
  lazy val logger = Logger("hooks.jira")

  def handleWebhook = Action(parse.json) { implicit request =>


    request.body.validate[JiraWebhookEvent].fold(
      errors => {
        debug(Json.prettyPrint(request.body))
        debug(errors)
      },
      event => {
        debug(Json.prettyPrint(Json.toJson(event)))
        val action = event.webhookEvent
        val username = request.getQueryString("username") orElse jira.bot.name
        val channel = request.getQueryString("channel").map("#" + _)
        val iconUrl = request.getQueryString("iconUrl") orElse jira.bot.icon

        val issue = event.issue
        val fields = issue.fields

        val issueName = event.issue.key
        val issueLink = JiraApi.issueUrl(issueName)
        val issueType =fields.issuetype.name
        val updatedBy = event.user.displayName

        var message = ""
        var attachmentsBuffer = scala.collection.mutable.ListBuffer[IncomingWebHookAttachment]()

        val attachmentIssueSummary = IncomingWebHookAttachmentField("Summary", fields.summary)
        val defaultColor =
          if (event.created) { jira.colors.issue.created }
          else if (event.deleted) { jira.colors.issue.deleted }
          else jira.colors.issue.updated

        val defaultAttachment = IncomingWebHookAttachment(
          s"Created by ${fields.creator.displayName}. Summary: ${fields.summary}",
          None, None, defaultColor,
          List(attachmentIssueSummary)
        )

        if (event.created) {
          message = s"${issueType} <${issueLink}|${issueName}> has been created by ${updatedBy}."
          val improvedFields =
            IncomingWebHookAttachmentField("Priority", fields.priority.name) +:
            defaultAttachment.fields :+
            IncomingWebHookAttachmentField("Description", fields.description.getOrElse(""))

          attachmentsBuffer += defaultAttachment.copy(fields = improvedFields)
        } else if (event.deleted) {
          message = s"${issueType} <${issueLink}|${issueName}> has been deleted by ${updatedBy}."
          attachmentsBuffer += defaultAttachment
        } else if (event.worklogUpdated) {
          message = s"Worklog of <${issueLink}|${issueName}> has been updated by ${updatedBy}."
          attachmentsBuffer += defaultAttachment
        } else if (event.changedlog) {
          val changelog = event.changelog.get
          message = s"${issueType} <${issueLink}|${issueName}> has been updated by ${updatedBy} (${fields.summary})."

          changelog.items.foreach { item =>
            val from = item.fromStr.getOrElse("")
            val to = item.toStr.getOrElse("")

            attachmentsBuffer += IncomingWebHookAttachment(
              s"${item.field}: ${from} -> ${to}", None, None, None,
              List(
                IncomingWebHookAttachmentField("Field", item.field),
                IncomingWebHookAttachmentField("From", from, true),
                IncomingWebHookAttachmentField("To", to, true)
              )
            )
          }
        }

        if (event.commented) {
          val comment = event.comment.get
          var fieldName = "Content"

          (message.length > 1, event.newlyCommented) match {
            case (true, true) => { fieldName = s"Also added a comment:" }
            case (true, false) => { fieldName = s"Also edited a comment:" }
            case (false, true) => {
              message = s"${updatedBy} added a comment to ${issueType} <${issueLink}|${issueName}> (${fields.summary})."
            }
            case (false, false) => {
              message = s"${updatedBy} edited a comment to ${issueType} <${issueLink}|${issueName}> (${fields.summary})."
            }
          }

          attachmentsBuffer += IncomingWebHookAttachment(
            s"${fieldName}: ${comment.body}", None, None, jira.colors.comment,
            List(IncomingWebHookAttachmentField(fieldName, comment.body))
          )
        }

        val attachments = attachmentsBuffer.result
        val attachmentsOpt = if (attachments.isEmpty) { None } else { Some(attachments) }

        IncomingWebhooks.send(IncomingWebHook(message, username, channel, iconUrl, None, attachmentsOpt))
      }
    )

    Ok
  }
}
