package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._

import models._
import models.JiraWebhookAction._

import services._

object Jiras extends Controller {
  
  def handleWebhook = Action(parse.json) { implicit request =>
    println(Json.prettyPrint(request.body))

    request.body.validate[JiraWebhookEvent].fold(
      errors => println(errors),
      event => {
        val action = event.webhookEvent
        val username = request.getQueryString("username") orElse Some("JIRA")
        val channel = request.getQueryString("channel").map("#" + _)
        val iconUrl = request.getQueryString("iconUrl") orElse Some("https://slack.global.ssl.fastly.net/14542/img/services/jira_32.png")

        val issue = event.issue
        val fields = issue.fields

        val issueName = event.issue.key
        val issueLink = "https://zenstudio.atlassian.net/browse/" + issueName
        val issueType =fields.issuetype.name
        val updatedBy = event.user.displayName

        var message = ""
        var attachmentsBuffer = scala.collection.mutable.ListBuffer[IncomingWebHookAttachment]()

        val attachmentIssueSummary = IncomingWebHookAttachmentField("Summary", fields.summary)
        val defaultColor = if (event.created) { Some("good") } else if (event.deleted) { Some("danger") } else None
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
            IncomingWebHookAttachmentField("Description", fields.description)

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

          if (message.length > 1) {
            if (event.newlyCommented) {
              fieldName = s"Also added a comment:"
            } else {
              fieldName = s"Also edited a comment:"
            }
          } else {
            if (event.newlyCommented) {
              message = s"${updatedBy} added a comment to ${issueType} <${issueLink}|${issueName}> (${fields.summary})."
            } else {
              message = s"${updatedBy} edited a comment to ${issueType} <${issueLink}|${issueName}> (${fields.summary})."
            }
          }

          attachmentsBuffer += IncomingWebHookAttachment(
            s"${fieldName}: ${comment.body}", None, None, Some("warning"),
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